package com.codingforfun.controller;

import com.codingforfun.service.agent.ChatAgent;
import com.codingforfun.service.agent.DatabaseAgent;
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
    private final DatabaseAgent databaseAgent;

    @PostMapping("/chat")
    public Mono<String> handleChat(@RequestBody String message) {
        return chatAgent.processChat(message);
    }

    @PostMapping("/db-operation")
    public Mono<String> handleDbOperation(@RequestBody String instruction) {
        return databaseAgent.processDatabaseOperation(instruction);
    }
}