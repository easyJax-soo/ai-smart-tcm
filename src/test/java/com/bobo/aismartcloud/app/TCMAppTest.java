package com.bobo.aismartcloud.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class TCMAppTest {

    @Resource
    private MiniMaxChatModel miniMaxChatModel;

    @Resource
    private TCMApp tcmApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();

        // 第一轮：描述症状
        String message = "最近总是感觉身体很累，晚上睡眠也不好";
        String answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);

        // 第二轮：继续描述更多症状
        message = "而且最近胃口也不太好，大便比较稀";
        answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);

        // 第三轮：让 AI 给出调理建议
        message = "我这是什么体质？应该如何调理？";
        answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);
    }

    @Test
    void testChatWithContext() {
        String chatId = UUID.randomUUID().toString();

        // 模拟一个完整的问诊流程
        String[] messages = {
                "医生，我最近总是头晕",
                "持续了大概两周吧",
                "睡眠不太 好，总是做梦",
                "饮食也不太规律，有时候不吃早饭",
                "我是属于什么体质？怎么调理？"
        };

        for (String message : messages) {
            String answer = tcmApp.doChat(message, chatId);
            Assertions.assertNotNull(answer);
            System.out.println("用户: " + message);
            System.out.println("AI: " + answer);
            System.out.println("---");
        }
    }


    @Test
    void testChatMemory() {
        String chatId = UUID.randomUUID().toString();

        // 第一轮：提示我是谁
        String message = "你好，我是阿波";
        String answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);

        // 第二轮：继续描述朋友的症状
        message = "我想知道如果帮我的朋友治疗痤疮，她的名字叫阿爆";
        answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);

        // 第三轮：让 AI 回忆
        message = "我叫什么名字，你帮我回忆一下";
        answer = tcmApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
        System.out.println("用户: " + message);
        System.out.println("AI: " + answer);
    }

    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "日常生活中如何养护脾胃？";
        String answer =  tcmApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);
    }
}
