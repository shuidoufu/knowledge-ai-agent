package com.example.aiagent.controller;

import com.example.aiagent.model.SpeechRequest;
import com.example.aiagent.service.SpeechSynthesisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 语音合成接口
 * 将文本合成为 MP3 音频，供前端 AI 回复语音播报
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/speech")
public class SpeechController {

    /** 单次合成文本最大长度 */
    private static final int MAX_TEXT_LENGTH = 20000;

    private final SpeechSynthesisService speechSynthesisService;

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

}
