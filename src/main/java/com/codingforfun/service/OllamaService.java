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
                .onStatus(
                        // Fixed: Use lambda instead of method reference
                        status -> status == HttpStatus.NOT_FOUND,
                        response -> {
                            log.error("Model not found: {}", request.getModel());
                            return Mono.error(new RuntimeException("Model not found"));
                        }
                )
                .onStatus(
                        // Fixed: Correct status check
                        status -> status.isError(),
                        response -> handleErrorResponse(response)
                )
                .bodyToMono(OllamaResponse.class);
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