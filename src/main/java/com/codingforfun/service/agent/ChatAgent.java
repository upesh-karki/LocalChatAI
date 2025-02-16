package com.codingforfun.service.agent;

import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.dto.OllamaResponse;
import com.codingforfun.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgent {

    private final OllamaService ollamaService;

    public Mono<String> processChat(String message) {
        log.info("Processing chat message: {}", message);

        OllamaRequest request = new OllamaRequest(
                "deepseek-r1:1.5b",
                message,
                false,
                Map.of("temperature", 0.7)
        );

        return ollamaService.generateResponse(request)
                .map(OllamaResponse::getResponse)
                .onErrorResume(e -> {
                    log.error("Chat processing failed", e);
                    return Mono.just("Error: " + e.getMessage());
                });
    }
}