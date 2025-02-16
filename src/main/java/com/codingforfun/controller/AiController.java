package com.codingforfun.controller;

import com.codingforfun.service.agent.ChatAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    private final ChatAgent chatAgent;

    @PostMapping("/chat")
    public Mono<String> handleChat(@RequestBody String message) {
        return chatAgent.processChat(message);
    }
}