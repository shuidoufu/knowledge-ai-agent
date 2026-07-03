package com.example.aiagent.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatHistoryDTO {
    private String id;
    private String chatId;
    private String title;
    private Instant createdAt;
}
