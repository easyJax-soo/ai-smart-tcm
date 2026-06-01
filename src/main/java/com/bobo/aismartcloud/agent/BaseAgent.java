package com.bobo.aismartcloud.agent;

import com.bobo.aismartcloud.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * 智能体基类，定义基本信息和多步骤执行流程
 */
@Data
@Slf4j
public abstract class BaseAgent {  
  
    //核心属性
    private String name;  
  
    //提示词
    private String systemPrompt;  
    private String nextStepPrompt;  
  
    //代理状态
    private AgentState state = AgentState.IDLE;
  
    //执行步骤控制
    private int maxSteps = 10;  
    private int currentStep = 0;  
  
    //LLM 大模型
    private ChatClient chatClient;
  
    //Memory 记忆（需要自主维护会话上下文））
    private List<Message> messageList = new ArrayList<>();


    /**
     * 运行代理
     *
     * @param userPrompt 用户提示词
     * @return 执行结果
     */
    public String run(String userPrompt) {  
        if (this.state != AgentState.IDLE) {  
            throw new RuntimeException("Cannot run agent from state: " + this.state);  
        }  
        if (StringUtil.isBlank(userPrompt)) {
            throw new RuntimeException("Cannot run agent with empty user prompt");  
        }  
        
        state = AgentState.RUNNING;  
        
        messageList.add(new UserMessage(userPrompt));
        
        List<String> results = new ArrayList<>();  
        try {  
            for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {  
                int stepNumber = i + 1;  
                currentStep = stepNumber;  
                log.info("Executing step " + stepNumber + "/" + maxSteps);  
                
                String stepResult = step();  
                String result = "Step " + stepNumber + ": " + stepResult;  
                results.add(result);  
            }  
            
            if (currentStep >= maxSteps) {  
                state = AgentState.FINISHED;  
                results.add("Terminated: Reached max steps (" + maxSteps + ")");  
            }  
            return String.join("\n", results);  
        } catch (Exception e) {  
            state = AgentState.ERROR;  
            log.error("Error executing agent", e);  
            return "执行错误" + e.getMessage();  
        } finally {  
            
            this.cleanup();  
        }  
    }  
  
      
    public abstract String step();  
  
      
    protected void cleanup() {  
        
    }  
}