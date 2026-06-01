package com.bobo.aismartcloud.agent;

import cn.hutool.core.collection.CollUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.*;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {  
  
    //可用的工具
    private final ToolCallback[] availableTools;
  
    //保存工具调用信息的响应结果（要调用哪些工具）
    private ChatResponse toolCallChatResponse;
  
    //工具调用管理者
    private final ToolCallingManager toolCallingManager;
  
    
    private final ChatOptions chatOptions;
  
    public ToolCallAgent(ToolCallback[] availableTools) {  
        super();  
        this.availableTools = availableTools;  
        this.toolCallingManager = ToolCallingManager.builder().build();  
//        阿里云百炼 灵积的规范代码
//        this.chatOptions = DashScopeChatOptions.builder()
//                .withProxyToolCalls(true)
//                .build();
        //禁止Spring AI内置的工具调用机制，自己维护选项和消息上下文
        this.chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(availableTools)
                .internalToolExecutionEnabled(false)//关闭由Spring AI控制调用
                .build();
    }

    @Override
    public boolean think() {
        //1.校验提示词，拼接用户提示词
        if (getNextStepPrompt() != null && !getNextStepPrompt().isEmpty()) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        List<Message> messageList = getMessageList();
        Prompt prompt = new Prompt(messageList, chatOptions);
        try {
            //2.调用工具调用模型，获取工具调用结果
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .tools(availableTools)
                    .call()
                    .chatResponse();
            //记录响应，用于等下Act
            this.toolCallChatResponse = chatResponse;
            //3.解析工具调用结果，获取要调用的工具
            //助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            //获取要调用的工具列表
            String result = assistantMessage.getText();
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            //输出提示信息
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("工具名称：%s，参数：%s",
                            toolCall.name(),
                            toolCall.arguments())
                    )
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            //如果不需要调用工具，返回false
            if (toolCallList.isEmpty()) {
                //只有不需要调用工具的时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                //需要调用工具的时，无需手动记录助手消息，因为调用工具时自动记录
                return true;
            }
        } catch (Exception e) {
            //异常处理
            log.error(getName() + "的思考过程遇到了问题: " + e.getMessage());
            getMessageList().add(
                    new AssistantMessage("处理时遇到错误: " + e.getMessage()));
            return false;
        }
    }


    /**
     * 执行工具调用并处理结果
     * @return
     */
    @Override
    public String act() {
        if (!toolCallChatResponse.hasToolCalls()) {
            return "没有工具调用";
        }

        Prompt prompt = new Prompt(getMessageList(), chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);

        setMessageList(toolExecutionResult.conversationHistory());

        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具 " + response.name() + " 完成了它的任务！结果: " + response.responseData())
                .collect(Collectors.joining("\n"));
        log.info(results);
        return results;
    }
}
