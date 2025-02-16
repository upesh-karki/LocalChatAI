package com.codingforfun.service;

import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.dto.OllamaResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OllamaService {

    private final WebClient ollamaWebClient;

    public Mono<OllamaResponse> generateResponse(OllamaRequest request) {
        return ollamaWebClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse.class)
                .timeout(Duration.ofMinutes(1)) // Per-request timeout
                .retry(2) // Retry twice on failures
                .doOnSubscribe(sub -> log.info("Starting request for model: {}", request.getModel()))
                .doOnError(e -> log.error("Ollama request failed: {}", e.getMessage()))
                .doOnSuccess(res -> log.info("Received response from Ollama"));
    }

    private Mono<Throwable> handleErrorResponse(ClientResponse response) {
        return response.bodyToMono(String.class)
                .flatMap(body -> {
                    String errorMsg = "Ollama API error: " + response.statusCode() + " - " + body;
                    log.error(errorMsg);
                    // Explicitly create Mono<Throwable>
                    return Mono.error(new RuntimeException(errorMsg));
                });
    }
}