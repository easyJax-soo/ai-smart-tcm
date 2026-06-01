package com.bobo.aismartcloud.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest
class MyManusTest {

    @Resource
    private MyManus myManus;

    @Test
    void run() {
        String userPrompt = """  
                我想调理一下身体胃部，请帮我找到 5 公里内合适的中医馆，  
                并结合一些网络图片，制定一份详细的问诊指南，  
                并以 PDF 格式输出""";
        String answer = myManus.run(userPrompt);
        Assertions.assertNotNull(answer);
    }
}