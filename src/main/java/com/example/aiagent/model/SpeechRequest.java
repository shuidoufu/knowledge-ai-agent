package com.example.aiagent.model;

import lombok.Data;

/**
 * 语音合成请求
 * 文本放请求体传输，避免长文本超过 URL 长度限制
 */
@Data
public class SpeechRequest {

    /** 待合成文本 */
    private String text;

}
