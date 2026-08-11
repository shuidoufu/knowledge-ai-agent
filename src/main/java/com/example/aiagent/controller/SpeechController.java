package com.example.aiagent.controller;

import com.example.aiagent.model.SpeechRequest;
import com.example.aiagent.service.SpeechRecognitionService;
import com.example.aiagent.service.SpeechSynthesisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 语音接口
 * 文本转语音（TTS）：将文本合成为 MP3 音频，供前端 AI 回复语音播报
 * 语音转文字（STT）：将上传的 WAV 音频转写为文本，供前端麦克风语音输入
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/speech")
public class SpeechController {

    /** 单次合成文本最大长度 */
    private static final int MAX_TEXT_LENGTH = 20000;

    /** 上传音频最大字节数（10MB） */
    private static final long MAX_AUDIO_BYTES = 10 * 1024 * 1024;

    private final SpeechSynthesisService speechSynthesisService;
    private final SpeechRecognitionService speechRecognitionService;

    /**
     * 文本转语音
     * 文本经请求体传输，长文本（数千字符）不受 URL 长度限制
     *
     * @param request 请求体（text 字段为待合成文本）
     * @return MP3 音频字节流
     */
    @PostMapping(value = "/tts", produces = "audio/mpeg")
    public ResponseEntity<byte[]> tts(@RequestBody SpeechRequest request) {
        String text = request.getText();
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        if (text.length() > MAX_TEXT_LENGTH) {
            log.warn("tts text too long, length={}", text.length());
            return ResponseEntity.badRequest().build();
        }
        byte[] audio = speechSynthesisService.synthesize(text);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("audio/mpeg"))
                .body(audio);
    }

    /**
     * 语音转文字
     * 上传 WAV 音频（16kHz 单声道 16bit），返回识别文本
     *
     * @param file WAV 音频文件
     * @return 识别文本
     */
    @PostMapping(value = "/stt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> stt(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            log.warn("stt audio too large, size={}", file.getSize());
            return ResponseEntity.badRequest().build();
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !originalName.toLowerCase().endsWith(".wav")) {
            return ResponseEntity.badRequest().build();
        }
        try {
            String text = speechRecognitionService.recognize(file.getBytes());
            return ResponseEntity.ok(Map.of("text", text));
        } catch (java.io.IOException e) {
            log.warn("stt read audio failed", e);
            return ResponseEntity.badRequest().build();
        }
    }

}
