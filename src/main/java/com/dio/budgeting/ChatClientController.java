package com.dio.budgeting;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatClientController {
    private final ChatClient chatClient;

    public ChatClientController(ChatClient chatClient){
        this.chatClient = chatClient;
    }

    @GetMapping("/chat")
    String chat(String prompt){
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("O prompt é obrigatório.");
        }
        return this.chatClient.prompt().user(prompt).call().content();
    }
}
