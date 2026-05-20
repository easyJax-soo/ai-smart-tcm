package com.bobo.aismartcloud.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring AI 框架调用 AI 大模型（MiniMax）
 */
@Component
public class SpringAiAiInvoke implements CommandLineRunner {

    @Resource
    private MiniMaxChatModel chatModel;

    @Override
    public void run(String... args) throws Exception {
        try {
            AssistantMessage assistantMessage = chatModel.call(new Prompt("你好，请问使用的是什么模型?"))
                    .getResult()
                    .getOutput();
            if (assistantMessage != null) {
                System.out.println(assistantMessage.getText());
            }
        } catch (Exception e) {
            System.err.println("MiniMax API 调用失败: " + e.getMessage());
        }
    }
}
