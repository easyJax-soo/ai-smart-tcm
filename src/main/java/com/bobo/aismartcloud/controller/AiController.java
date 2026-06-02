package com.bobo.aismartcloud.controller;

import com.bobo.aismartcloud.agent.MyManus;
import com.bobo.aismartcloud.app.TCMApp;
import jakarta.annotation.Resource;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private TCMApp tcmAppApp;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private MiniMaxChatModel miniMaxChatModel;

    /**
     * 同步调用智慧云中医
     *
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/tcm_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return tcmAppApp.doChat(message, chatId);
    }

    @GetMapping(value = "/tcm_app/chat/sse",produces = MediaType.TEXT_EVENT_STREAM_VALUE)//添加 SSE对应的MediaType
    public Flux<String> doChatByStream(String message, String chatId) {
        return tcmAppApp.doChatByStream(message, chatId);
    }

    @GetMapping(value = "/tcm_app/chat/sse")
    public Flux<ServerSentEvent<String>> doChatWithTCMSSE(String message, String chatId) {//设置泛型为 ServerSentEvent。使用这种方式可省略 MediaType
        return tcmAppApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    @GetMapping("/tcm_app/chat/sse/emitter")
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {

        SseEmitter emitter = new SseEmitter(180000L);//设置超时时间

        tcmAppApp.doChatByStream(message, chatId)
                .subscribe(
                        chunk -> {
                            try {
                                emitter.send(chunk);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },

                        emitter::completeWithError,

                        emitter::complete
                );

        return emitter;
    }


    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        MyManus yuManus = new MyManus(allTools, miniMaxChatModel);
        return yuManus.runStream(message);
    }


}