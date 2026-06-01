package com.bobo.aismartcloud.agent;

import com.bobo.aismartcloud.agent.model.AgentState;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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

    /**
     *
     * @param userPrompt 用户请求
     * @return
     */
    public SseEmitter runStream(String userPrompt) {
        //创建一个超时时间较长的SseEmitter
        SseEmitter emitter = new SseEmitter(300000L);

        //创建异步调用，
        CompletableFuture.runAsync(() -> {
            try {
                //1.基础校验
                if (this.state != AgentState.IDLE) {
                    emitter.send("错误：无法从状态运行代理: " + this.state);
                    emitter.complete();
                    return;
                }
                if (StringUtil.isBlank(userPrompt)) {
                    emitter.send("错误：不能使用空提示词运行代理");
                    emitter.complete();
                    return;
                }

                //执行，更改状态
                state = AgentState.RUNNING;
                //记录消息上下文
                messageList.add(new UserMessage(userPrompt));
                //保存结果列表
                try {
                    for (int i = 0; i < maxSteps && state != AgentState.FINISHED; i++) {
                        int stepNumber = i + 1;
                        currentStep = stepNumber;
                        log.info("Executing step " + stepNumber + "/" + maxSteps);

                        //单步执行
                        String stepResult = step();
                        String result = "Step " + stepNumber + ": " + stepResult;

                        //输出当前的每一步结果到SSE
                        emitter.send(result);
                    }

                    //检查是否超出步骤限制
                    if (currentStep >= maxSteps) {
                        state = AgentState.FINISHED;
                        emitter.send("执行结束: 达到最大步骤 (" + maxSteps + ")");
                    }
                    //正常完成，必须加否则会等待超时
                    emitter.complete();
                } catch (Exception e) {
                    state = AgentState.ERROR;
                    log.error("执行智能体失败", e);
                    try {
                        emitter.send("执行错误: " + e.getMessage());
//                        emitter.complete(); //这一步可写可不写，异常可以让agent loop再去执行
                    } catch (Exception ex) {
                        emitter.completeWithError(ex);
                    }
                } finally {
                    //清理资源
                    this.cleanup();
                }
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        //设置超时回调
        emitter.onTimeout(() -> {
            this.state = AgentState.ERROR;
            this.cleanup();
            log.warn("SSE connection timed out");
        });
        //正常完成operation
        emitter.onCompletion(() -> {
            if (this.state == AgentState.RUNNING) {
                this.state = AgentState.FINISHED;
            }
            this.cleanup();
            log.info("SSE connection completed");
        });

        return emitter;
    }
      
    public abstract String step();  
  
      
    protected void cleanup() {  
        
    }  
}