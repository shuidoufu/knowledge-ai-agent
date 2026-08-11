package com.example.aiagent.service;

import com.alibaba.dashscope.audio.asr.recognition.Recognition;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionParam;
import com.alibaba.dashscope.audio.asr.recognition.RecognitionResult;
import com.alibaba.dashscope.common.ResultCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 语音识别服务
 * 解析 WAV 音频为 PCM 数据，通过 DashScope WebSocket 实时识别（Paraformer）转写为文本
 * 供前端麦克风录音后语音转文字
 */
@Slf4j
@Service
public class SpeechRecognitionService {

    /** 采样率：16kHz */
    private static final int SAMPLE_RATE = 16000;

    /** 单声道 16bit 每秒字节数 */
    private static final int BYTES_PER_SECOND = SAMPLE_RATE * 2;

    /** 单帧时长 100ms 对应的字节数 */
    private static final int FRAME_BYTES = BYTES_PER_SECOND / 10;

    /** 识别超时秒数（防止 WS 挂起） */
    private static final long RECOGNITION_TIMEOUT_SECONDS = 30;

    /** 识别失败最大重试次数 */
    private static final int MAX_RETRIES = 3;

    /** 重试间隔毫秒 */
    private static final long RETRY_INTERVAL_MS = 500;

    private final String apiKey;
    private final String model;

    public SpeechRecognitionService(@Value("${spring.ai.dashscope.api-key}") String apiKey,
                                    @Value("${ai.stt.model}") String model) {
        this.apiKey = apiKey;
        this.model = model;
    }

    /**
     * 语音转文字
     * 解析 WAV 后通过 WebSocket 推流识别，WS 偶发断连/超时自动重试
     *
     * @param wavBytes WAV 音频（16kHz 单声道 16bit PCM）
     * @return 识别文本
     */
    public String recognize(byte[] wavBytes) {
        byte[] pcm = parseWav(wavBytes);
        Exception lastError = null;
        for (int attempt = 0; attempt < MAX_RETRIES; attempt++) {
            try {
                String text = recognizeOnce(pcm);
                if (text.isBlank()) {
                    throw new IllegalArgumentException("未识别到语音内容");
                }
                log.info("stt recognized, chars={}", text.length());
                return text;
            } catch (IllegalArgumentException e) {
                throw e;
            } catch (Exception e) {
                lastError = e;
                log.warn("stt recognize failed, attempt={}/{}", attempt + 1, MAX_RETRIES, e);
                try {
                    Thread.sleep(RETRY_INTERVAL_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new RuntimeException("语音识别失败", lastError);
    }

    /**
     * 单次 WS 推流识别
     * 每 100ms 一帧推送 PCM 数据，收集所有句子文本
     *
     * @param pcm PCM 音频数据
     * @return 识别文本
     */
    private String recognizeOnce(byte[] pcm) throws Exception {
        RecognitionParam param = RecognitionParam.builder()
                .model(model)
                .sampleRate(SAMPLE_RATE)
                .format("pcm")
                .apiKey(apiKey)
                .build();
        Recognition recognition = new Recognition();
        StringBuilder text = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Exception> errorRef = new AtomicReference<>();
        recognition.call(param, new ResultCallback<RecognitionResult>() {
            @Override
            public void onEvent(RecognitionResult result) {
                if (result.isSentenceEnd() && result.getSentence() != null
                        && result.getSentence().getText() != null) {
                    text.append(result.getSentence().getText());
                }
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }

            @Override
            public void onError(Exception e) {
                errorRef.set(e);
                latch.countDown();
            }
        });
        // 逐帧推送 PCM 音频
        for (int offset = 0; offset < pcm.length; offset += FRAME_BYTES) {
            int len = Math.min(FRAME_BYTES, pcm.length - offset);
            recognition.sendAudioFrame(ByteBuffer.wrap(pcm, offset, len));
        }
        recognition.stop();
        // 等待识别完成（超时保护，防止 WS 挂起）
        boolean finished = latch.await(RECOGNITION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!finished) {
            throw new RuntimeException("语音识别超时");
        }
        Exception error = errorRef.get();
        if (error != null) {
            throw error;
        }
        return text.toString().trim();
    }

    /**
     * 解析 WAV 文件，校验格式并提取 PCM 音频数据
     *
     * @param wavBytes WAV 文件字节
     * @return PCM 音频数据
     */
    private byte[] parseWav(byte[] wavBytes) {
        if (wavBytes.length < 44 || !isAscii(wavBytes, 0, "RIFF") || !isAscii(wavBytes, 8, "WAVE")) {
            throw new IllegalArgumentException("音频文件格式错误，仅支持 WAV");
        }
        int offset = 12;
        int channels = -1;
        int sampleRate = -1;
        int bitsPerSample = -1;
        int dataOffset = -1;
        int dataLength = -1;
        while (offset + 8 <= wavBytes.length) {
            String chunkId = new String(wavBytes, offset, 4, StandardCharsets.US_ASCII);
            int chunkSize = littleEndianInt(wavBytes, offset + 4);
            if (chunkId.equals("fmt ")) {
                if (offset + 24 > wavBytes.length) {
                    throw new IllegalArgumentException("WAV 文件格式错误");
                }
                int audioFormat = littleEndianShort(wavBytes, offset + 8);
                channels = littleEndianShort(wavBytes, offset + 10);
                sampleRate = littleEndianInt(wavBytes, offset + 12);
                bitsPerSample = littleEndianShort(wavBytes, offset + 22);
                if (audioFormat != 1) {
                    throw new IllegalArgumentException("仅支持 PCM 编码的 WAV");
                }
            } else if (chunkId.equals("data")) {
                dataOffset = offset + 8;
                dataLength = Math.min(chunkSize, wavBytes.length - dataOffset);
                break;
            }
            offset += 8 + chunkSize + (chunkSize % 2);
        }
        if (dataOffset < 0) {
            throw new IllegalArgumentException("WAV 文件缺少音频数据");
        }
        if (channels != 1 || sampleRate != SAMPLE_RATE || bitsPerSample != 16) {
            throw new IllegalArgumentException("仅支持 16kHz 单声道 16bit 的 WAV");
        }
        byte[] pcm = new byte[dataLength];
        System.arraycopy(wavBytes, dataOffset, pcm, 0, dataLength);
        return pcm;
    }

    /**
     * 校验指定偏移处是否为目标 ASCII 字符串
     *
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @param expect 期望字符串
     * @return 是否匹配
     */
    private boolean isAscii(byte[] bytes, int offset, String expect) {
        byte[] expectBytes = expect.getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i < expectBytes.length; i++) {
            if (bytes[offset + i] != expectBytes[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 小端序读取 32 位整数
     *
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @return 整数
     */
    private int littleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    /**
     * 小端序读取 16 位整数
     *
     * @param bytes 字节数组
     * @param offset 起始偏移
     * @return 整数
     */
    private int littleEndianShort(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF) | ((bytes[offset + 1] & 0xFF) << 8);
    }

}
