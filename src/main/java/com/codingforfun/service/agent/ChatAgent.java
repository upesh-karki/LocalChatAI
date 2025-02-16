package com.codingforfun.service.agent;

import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.dto.OllamaResponse;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgent {

    private final OllamaService ollamaService;
    private final MemberRepository memberRepository;

    private static final String SCHEMA_INFO = """
        Database Schema:
        - member (memberid BIGINT PK, firstname VARCHAR, lastname VARCHAR, email VARCHAR)
        - member_detail (memberid BIGINT PK, address1 CLOB, occupation CHAR, phone_number VARCHAR)
        """;

    // Main entry point
    public Mono<String> processChat(String query) {
        return detectMemberIntent(query)
                .flatMap(memberId -> handleMemberQuery(memberId, query))
                .timeout(Duration.ofSeconds(30), fallback())
                .switchIfEmpty(handleGeneralChat(query))
                .onErrorResume(e -> errorHandler(e, query));
    }

    // Step 1: AI Decision Making
    private Mono<Long> detectMemberIntent(String query) {
        String prompt = SCHEMA_INFO + """
            
            INSTRUCTIONS:
            1. Does this query request member information?
            2. If yes, return ONLY the numeric member ID
            3. If no, return 0
            
            Examples:
            "What's member 123's phone?" → 123
            "Explain quantum physics" → 0
            "Show details for user 456" → 456
            
            Query: """ + query;

        return ollamaService.generateResponse(
                        new OllamaRequest(
                                "deepseek-r1:1.5b",
                                prompt,  // Critical fix: Use the prompt, not raw query
                                false,
                                Map.of("temperature", 0.0, "max_tokens", 10)
                        ))
                .map(response -> {
                    String raw = response.getResponse().replaceAll("\\D", "");
                    return raw.isEmpty() ? 0L : Long.parseLong(raw);
                })
                .timeout(Duration.ofSeconds(10), Mono.just(0L));
    }

    // Step 2: Handle Member Queries
    private Mono<String> handleMemberQuery(Long memberId, String originalQuery) {
        if (memberId == 0L) return Mono.empty();

        return generateSQL(memberId, originalQuery)
                .flatMap(this::validateAndExecuteSQL)
                .flatMap(member -> formatResponse(member, originalQuery))
                .timeout(Duration.ofSeconds(20),
                        Mono.just("Database operation timed out"));
    }

    // Step 2a: Generate SQL with AI
    private Mono<String> generateSQL(Long memberId, String query) {
        String prompt = SCHEMA_INFO + """
            Generate SAFE SQL for:
            Query: %s
            Rules:
            - SELECT only
            - JOIN member + member_detail
            - WHERE memberid = %d
            - Return requested fields
            
            SQL:""".formatted(query, memberId);

        return ollamaService.generateResponse(
                        new OllamaRequest(
                                "deepseek-r1:1.5b",
                                prompt,
                                false,
                                Map.of("temperature", 0.1)
                        ))
                .map(response -> sanitizeSQL(response.getResponse()));
    }

    private String sanitizeSQL(String sql) {
        return sql.replace(";", "")
                .replaceAll("(?i)password", "pass")
                .replaceAll("--.*", ""); // Remove comments
    }

    // Step 2b: Validate & Execute
    private Mono<Member> validateAndExecuteSQL(String sql) {
        return Mono.fromCallable(() -> {
            // Security checks
            if (!sql.toLowerCase().startsWith("select")) {
                throw new SecurityException("Only SELECT queries allowed");
            }
            if (!sql.toLowerCase().contains("where member.memberid =")) {
                throw new SecurityException("Missing member ID filter");
            }

            // Extract ID and query
            long id = Long.parseLong(sql.replaceAll(".*memberid\\s*=\\s*(\\d+).*", "$1"));
            return memberRepository.findWithDetails(id)
                    .orElseThrow(() -> new RuntimeException("Member not found"));
        });
    }

    // Step 2c: Format Response
    private Mono<String> formatResponse(Member member, String originalQuery) {
        return Mono.fromCallable(() -> {
            StringBuilder response = new StringBuilder()
                    .append("Member ID: ").append(member.getMemberId()).append("\n");

            String lq = originalQuery.toLowerCase();
            MemberDetail detail = member.getMemberDetail();

            if (containsAny(lq, "name", "full")) {
                response.append("Name: ")
                        .append(member.getFirstName()).append(" ")
                        .append(member.getLastName()).append("\n");
            }
            if (containsAny(lq, "email", "mail")) {
                response.append("Email: ").append(member.getEmail()).append("\n");
            }

            if (detail != null) {
                if (containsAny(lq, "phone", "number")) {
                    appendIfPresent(response, "Phone", detail.getPhoneNumber());
                }
                if (containsAny(lq, "address")) {
                    appendAddress(response, detail);
                }
                if (containsAny(lq, "occupation", "job")) {
                    appendIfPresent(response, "Occupation", detail.getOccupation());
                }
            }

            return response.toString();
        });
    }

    // Step 3: Handle General Chat
    private Mono<String> handleGeneralChat(String message) {
        return ollamaService.generateResponse(
                        new OllamaRequest(
                                "deepseek-r1:1.5b",
                                message,
                                false,
                                Map.of("temperature", 0.7)
                        )
                )
                .map(response -> {
                    if (response.getResponse() == null || response.getResponse().isBlank()) {
                        return "I couldn't generate a response";
                    }
                    return response.getResponse();
                })
                .timeout(Duration.ofSeconds(25),
                        Mono.just("Response took too long"));
    }

    // Helper Methods
    private boolean containsAny(String input, String... terms) {
        for (String term : terms) if (input.contains(term)) return true;
        return false;
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) sb.append(label).append(": ").append(value).append("\n");
    }

    private void appendAddress(StringBuilder sb, MemberDetail detail) {
        if (detail.getAddress1() != null) {
            sb.append("Address:\n").append(detail.getAddress1());
            if (detail.getAddress2() != null) sb.append("\n").append(detail.getAddress2());
            sb.append("\n").append(detail.getCity())
                    .append(", ").append(detail.getCountry())
                    .append(" ").append(detail.getZipcode()).append("\n");
        }
    }

    private Mono<String> errorHandler(Throwable e, String query) {
        log.error("Error processing query: {}", query, e);
        return Mono.just("Error processing request: " + e.getMessage());
    }

    private Mono<String> fallback() {
        return Mono.just("Request timed out. Please try again later.");
    }
}