package com.example.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class KnowledgeAppTest {

    @Resource
    private KnowledgeApp knowledgeApp;
    @Test
    void test() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是程序员鱼皮";
        String answer = knowledgeApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

}
