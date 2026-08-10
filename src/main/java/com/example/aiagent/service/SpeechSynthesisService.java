package com.example.aiagent.service;

import com.alibaba.cloud.ai.dashscope.audio.DashScopeSpeechSynthesisOptions;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisModel;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisPrompt;
import com.alibaba.cloud.ai.dashscope.audio.synthesis.SpeechSynthesisResponse;
import com.example.aiagent.constant.FileConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音合成服务
 * 调用阿里云百炼 CosyVoice 大模型，将文本合成为 MP3 音频
 * 长文本自动分段合成后拼接，供前端语音播报
 * 合成结果按内容 MD5 缓存到本地文件，相同文本直接复用，避免重复调用接口
 */
@Slf4j
@Service
public class SpeechSynthesisService {

    /** CosyVoice 单次合成最大字符数（超出需分段） */
    private static final int MAX_SEGMENT_LENGTH = 900;

    /** 单段合成失败最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 重试间隔毫秒 */
    private static final long RETRY_INTERVAL_MS = 500;

    /** TTS 音频缓存目录（按内容 MD5 命名，重启不丢失） */
    private static final String TTS_CACHE_DIR = FileConstant.FILE_SAVE_DIR + "/tts";

    /** 合成中缓存 key 的锁（防止相同内容并发重复合成） */
    private final ConcurrentHashMap<String, Object> cacheLocks = new ConcurrentHashMap<>();

    private final SpeechSynthesisModel speechSynthesisModel;
    private final String model;
    private final String voice;

    public SpeechSynthesisService(SpeechSynthesisModel speechSynthesisModel,
                                  @Value("${ai.tts.model}") String model,
                                  @Value("${ai.tts.voice}") String voice) {
        this.speechSynthesisModel = speechSynthesisModel;
        this.model = model;
        this.voice = voice;
    }

    /**
     * 文本合成语音
     * 先清洗 markdown 标记保留纯文本，再按内容 MD5 查缓存（命中直接返回）
     * 未命中则分段合成并拼接 MP3 字节流，结果写入缓存
     *
     * @param text 待合成文本
     * @return MP3 音频字节流
     */
    public byte[] synthesize(String text) {
        String cleanText = cleanText(text);
        if (cleanText.isEmpty()) {
            throw new IllegalArgumentException("语音播报文本为空");
        }
        String cacheKey = md5(cleanText);
        byte[] cached = readCache(cacheKey);
        if (cached != null) {
            log.info("tts cache hit, key={}, bytes={}", cacheKey, cached.length);
            return cached;
        }
        // 相同内容并发请求时加锁，避免重复合成
        Object lock = cacheLocks.computeIfAbsent(cacheKey, k -> new Object());
        synchronized (lock) {
            byte[] cachedAgain = readCache(cacheKey);
            if (cachedAgain != null) {
                log.info("tts cache hit, key={}, bytes={}", cacheKey, cachedAgain.length);
                return cachedAgain;
            }
            List<String> segments = splitText(cleanText);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (String segment : segments) {
                byte[] audio = synthesizeSegment(segment);
                output.writeBytes(audio);
            }
            byte[] result = output.toByteArray();
            writeCache(cacheKey, result);
            log.info("tts synthesized, chars={}, segments={}, bytes={}", cleanText.length(), segments.size(), result.length);
            return result;
        }
    }

    /**
     * 文本 MD5（缓存 key）
     *
     * @param text 清洗后的纯文本
     * @return MD5 十六进制字符串
     */
    private String md5(String text) {
        return DigestUtils.md5DigestAsHex(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 读取缓存音频
     *
     * @param key 内容 MD5
     * @return 音频字节流，未命中返回 null
     */
    private byte[] readCache(String key) {
        File file = new File(TTS_CACHE_DIR, key + ".mp3");
        if (file.exists() && file.length() > 0) {
            try {
                return Files.readAllBytes(file.toPath());
            } catch (IOException e) {
                log.warn("tts cache read failed, key={}", key, e);
            }
        }
        return null;
    }

    /**
     * 写入缓存音频
     *
     * @param key   内容 MD5
     * @param audio 音频字节流
     */
    private void writeCache(String key, byte[] audio) {
        try {
            Path dir = Path.of(TTS_CACHE_DIR);
            Files.createDirectories(dir);
            Files.write(dir.resolve(key + ".mp3"), audio);
        } catch (IOException e) {
            log.warn("tts cache write failed, key={}", key, e);
        }
    }

    /**
     * 合成单段文本
     * DashScope WebSocket 合成偶发断连，失败时自动重试
     *
     * @param segment 单段文本（不超过单次合成上限）
     * @return MP3 音频字节流
     */
    private byte[] synthesizeSegment(String segment) {
        Exception lastError = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                DashScopeSpeechSynthesisOptions options = DashScopeSpeechSynthesisOptions.builder()
                        .withModel(model)
                        .withVoice(voice)
                        .build();
                SpeechSynthesisPrompt prompt = new SpeechSynthesisPrompt(segment, options);
                SpeechSynthesisResponse response = speechSynthesisModel.call(prompt);
                ByteBuffer audio = response.getResult().getOutput().getAudio();
                byte[] bytes = new byte[audio.remaining()];
                audio.get(bytes);
                return bytes;
            } catch (Exception e) {
                lastError = e;
                log.warn("tts segment synthesize failed, attempt={}/{}", attempt + 1, MAX_RETRIES, e);
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("语音合成失败", lastError);
    }

    /**
     * 清洗 markdown 标记，只保留可朗读的纯文本
     *
     * @param text 原始文本
     * @return 纯文本
     */
    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "")          // 图片
                .replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1")        // 链接（保留文字）
                .replaceAll("```[\\s\\S]*?```", "")                    // 代码块
                .replaceAll("`([^`]*)`", "$1")                         // 行内代码
                .replaceAll("(?m)^#{1,6}\\s*", "")                     // 标题符号
                .replaceAll("\\*\\*([^*]*)\\*\\*", "$1")               // 加粗
                .replaceAll("\\*([^*]*)\\*", "$1")                     // 斜体
                .replaceAll("(?m)^[-*+]\\s+", "")                      // 列表符号
                .replaceAll("(?m)^>\\s?", "")                          // 引用
                .replaceAll("\\|", "，")                               // 表格分隔符
                .replaceAll("\\s+", " ")                               // 合并空白
                .trim();
    }

    /**
     * 长文本按字符数分段
     *
     * @param text 清洗后的纯文本
     * @return 分段列表
     */
    private List<String> splitText(String text) {
        List<String> segments = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_SEGMENT_LENGTH) {
            segments.add(text.substring(i, Math.min(i + MAX_SEGMENT_LENGTH, text.length())));
        }
        return segments;
    }

}
