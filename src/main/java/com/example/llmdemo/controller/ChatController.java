package com.example.llmdemo.controller;

import com.example.llmdemo.tool.OrderTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {
    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder chatClientBuilder, OrderTools orderTools) {
        this.chatClient = chatClientBuilder
                .defaultSystem("你是一个简洁的中文助手，回答控制在三句话以内")  // 人格/规则
                .defaultTools(orderTools)
                .build();
    }

    @GetMapping("/chat")  // 暴露成 HTTP 接口
    public String chat(@RequestParam String message) {
        return chatClient.prompt().user(message).call().content();  // 发请求取文本
        // prompt() 建一次对话 → user() 放用户消息 → call() 发请求 → content() 取回文本
        // 注意：代码一行没变，但模型现在"看得见" OrderTools 里的两个方法了
    }
}
