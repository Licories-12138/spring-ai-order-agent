package com.example.llmdemo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;
    public ChatController(ChatClient.Builder chatClientBuilder) {  // 注入
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个简洁的中文助手，回答控制在三句话以内")  // 人格/规则
                .build();
    }

    @GetMapping("/chat")  // 暴露成 HTTP 接口
    public String chat(@RequestParam String message) {
        return chatClient.prompt().user(message).call().content();  // 发请求取文本
        // prompt() 建一次对话 → user() 放用户消息 → call() 发请求 → content() 取回文本
    }
}
