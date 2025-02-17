package com.codingforfun.controller;

import com.codingforfun.service.agent.ChatAgent;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;
import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.entity.Member;
import com.codingforfun.model.entity.MemberDetail;
import com.codingforfun.repository.MemberRepository;
import com.codingforfun.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final ChatAgent chatAgent;

    @PostMapping("/chat")
    public Mono<ResponseEntity<String>> handleChat(@RequestBody String message) {
        return chatAgent.processChat(message)
                .timeout(Duration.ofSeconds(600)) // Overall request timeout (10 minutes)
                .map(response -> ResponseEntity.ok(response)) // Return 200 OK with the response
                .onErrorResume(e -> {
                    if (e instanceof ResponseStatusException) {
                        return Mono.just(ResponseEntity.status(((ResponseStatusException) e).getStatusCode()).body(e.getMessage()));
                    } else if (e instanceof TimeoutException || e instanceof AsyncRequestTimeoutException) {
                        return Mono.just(ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT).body("Request timed out. Please try again."));
                    } else {
//                        log.error("Error processing chat request", e); // Log the actual error
                        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An unexpected error occurred."));
                    }
                });
    }
}