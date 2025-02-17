package com.codingforfun.service.agent;

import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.entity.Member;
import com.codingforfun.model.entity.MemberDetail;
import com.codingforfun.repository.MemberRepository;
import com.codingforfun.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatAgent {

    private final OllamaService ollamaService;
    private final MemberRepository memberRepository;

    private static final String SCHEMA_INFO = """
        Database Schema:
        - ods.member (memberid BIGINT PK, firstname VARCHAR, lastname VARCHAR, email VARCHAR)
        - ods.member_detail (memberid BIGINT PK, address1 CLOB, occupation CHAR, phone_number VARCHAR)
        """;

    public Mono<String> processChat(String query) {
        return detectIntent(query)
                .flatMap(intent -> {
                    if ("DATABASE_QUERY".equals(intent)) {
                        return handleDatabaseQuery(query)
                                .doOnNext(response -> log.info("📤 Final Response: {}", response));
                    } else {
                        return handleGeneralChat(query);
                    }
                });
    }

    private Mono<String> handleDatabaseQuery(String query) {
        return detectMemberId(query)
                .flatMap(memberId -> {
                    if (memberId > 0) {
                        log.info("🔍 Detected Member ID: {}", memberId);
                        return validateAndExecuteSQL(memberId)
                                .flatMap(this::formatResponse)
                                .doOnNext(data -> log.info("📦 Fetched Data: {}", data));
                    }
                    return Mono.just("❌ Could not determine member ID");
                })
                .onErrorResume(e -> {
                    log.error("Database error: {}", e.getMessage());
                    return Mono.just("❌ Error retrieving data: " + e.getMessage());
                });
    }

    private Mono<String> detectIntent(String query) {
        String prompt = """
            Determine if this query requires database access. Return ONLY 'DATABASE_QUERY' or 'GENERAL_CHAT'.
            Examples:
            Q: "Show address for member 123" → DATABASE_QUERY
            Q: "Tell me a joke" → GENERAL_CHAT
            Q: "%s"
            """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.0)))
                .map(response -> response.getResponse().contains("DATABASE_QUERY") ? "DATABASE_QUERY" : "GENERAL_CHAT")
                .timeout(Duration.ofSeconds(5), Mono.just("GENERAL_CHAT"));
    }

    private Mono<Long> detectMemberId(String query) {
        // First try regex extraction
        Pattern pattern = Pattern.compile("\\b(?:member|id)[\\s:]*(\\d+)");
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            try {
                return Mono.just(Long.parseLong(matcher.group(1)));
            } catch (NumberFormatException e) {
                log.warn("Regex parse failed for: {}", matcher.group(1));
            }
        }

        // Fallback to AI extraction
        String prompt = """
            Extract the numeric member ID from the query. Return 0 if none found.
            Q: "Show details for member 456" → 456
            Q: "%s"
            """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.0)))
                .map(response -> {
                    try {
                        return Long.parseLong(response.getResponse().replaceAll("\\D", ""));
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                });
    }

    private Mono<Member> validateAndExecuteSQL(Long memberId) {
        return generateSQL(memberId)
                .flatMap(sql -> {
                    return Mono.fromCallable(() ->
                                    memberRepository.findWithDetails(memberId) // Use the *original* memberId
                                            .orElseThrow(() -> new RuntimeException("Member not found")))
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnNext(member -> log.info("Database result: {}", member));
                })
                .switchIfEmpty(Mono.error(new RuntimeException("Member not found")));
    }



    private Mono<String> generateSQL(Long memberId) {
        String prompt = SCHEMA_INFO + """
        Generate a SQL query for member ID. 
        Use JOIN between ods.member and ods.member_detail.
        Return ONLY the SQL in ```sql ``` markers, WITHOUT the WHERE clause.
        Sample SQL: SELECT  m.memberid, m.firstname, m.lastname, m.email, md.occupation, md.address1
            FROM ods.Member m 
            INNER JOIN ods.member_detail md ON m.memberid = md.memberid

        """; //Removed where clause

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.1)))
                .map(response -> {
                    String sql = response.getResponse().trim();
                    // Inject the WHERE clause here, safely:
                    return sql.replace("```sql", "").replace("```","").trim() + " WHERE m.memberId = " + memberId + ";"; //Directly inject the memberId
                })
                .doOnNext(raw -> log.info("Generated SQL: {}", raw));
    }


    private Mono<String> formatResponse(Member member) {
        String template = """
            Format member details into a friendly response:
            - ID: {id}
            - Name: {name}
            - Email: {email}
            - Address: {address}
            - Occupation: {occupation}
            """;

        Map<String, Object> data = Map.of(
                "id", member.getMemberId(),
                "name", member.getFirstName() + " " + member.getLastName(),
                "email", member.getEmail(),
                "address", Optional.ofNullable(member.getMemberDetail())
                        .map(MemberDetail::getAddress1).orElse("N/A"),
                "occupation", Optional.ofNullable(member.getMemberDetail())
                        .map(MemberDetail::getOccupation).orElse("N/A")
        );

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", template + data.toString(),
                                false, Map.of("temperature", 0.7)))
                .map(response -> response.getResponse());
    }
    private Mono<String> handleGeneralChat(String message) {
        log.info("💭 Handling general chat query: {}", message);
        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", message, false, Map.of("temperature", 0.7)))
                .map(response -> {
                    String res = response.getResponse();
                    return (res == null || res.isBlank()) ? "❌ I couldn't generate a response" : res;
                })
                .timeout(Duration.ofSeconds(600), Mono.just("⏳ Response took too long"));
    }
}