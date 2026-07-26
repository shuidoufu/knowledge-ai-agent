package com.example.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ManusTest {

    @Resource
    private Manus manus;

    @Test
    public void run() {
        String userPrompt = """
                请帮我整理一份关于知识管理的最佳实践清单，
                并结合一些网络图片，制定一份详细的知识管理指南，
                并以 PDF 格式输出""";
        String answer = manus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}