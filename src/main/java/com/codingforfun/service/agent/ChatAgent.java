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
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

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
                    log.info("🔍 Detected intent after trimming: '{}'", intent); // ✅ Log for debugging

                    if ("DATABASE_QUERY".equals(intent)) {  // ✅ Ensure exact comparison
                        log.info("📡 Intent detected as DATABASE_QUERY. Fetching from DB...");
                        return handleDatabaseQuery(query);
                    }

                    log.info("💬 Intent detected as GENERAL_CHAT. Generating AI response...");
                    return handleGeneralChat(query);
                })
                .timeout(Duration.ofSeconds(600), Mono.just("⏰ Request timed out"))
                .onErrorResume(e -> {
                    log.error("❌ Error processing query", e);
                    return Mono.just("Error: " + e.getMessage());
                });
    }



    private Mono<String> detectIntent(String query) {
        String prompt = """
        TASK: Determine if this query requires a database lookup.  Return ONLY 'DATABASE_QUERY' or 'GENERAL_CHAT'.
        - If the query is about general knowledge, casual chat, or AI-generated responses, return: 'GENERAL_CHAT'
        - If the query is about retrieving member details, return: 'DATABASE_QUERY'

        Examples:
        Query: "What is the capital of France?"
        Response: GENERAL_CHAT

        Query: "Show address and occupation for member 10"
        Response: DATABASE_QUERY

        Query: "Tell me a joke"
        Response: GENERAL_CHAT

        Query: "Fetch email for member John Doe"
        Response: DATABASE_QUERY

        Query: "%s"
    """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.0)))
                .map(response -> response.getResponse().trim())
                .map(intent -> {
                    //Improved parsing logic.  Handles extra whitespace.
                    if (intent.toUpperCase().contains("DATABASE_QUERY")) {
                        return "DATABASE_QUERY";
                    } else if (intent.toUpperCase().contains("GENERAL_CHAT")) {
                        return "GENERAL_CHAT";
                    } else {
                        log.warn("Unexpected AI intent response: '{}'. Defaulting to GENERAL_CHAT", intent);
                        return "GENERAL_CHAT"; // Default if the AI returns something unexpected
                    }
                })
                .doOnNext(intent -> log.info("🧠 AI Intent: '{}'", intent))
                .timeout(Duration.ofSeconds(700), Mono.just("GENERAL_CHAT"));
    }

    private Mono<Long> detectMemberId(String query) {
        String prompt = """
        TASK: Extract the member ID if the query refers to a specific member.
        - If the query asks for member details, return ONLY the member ID (e.g., `10`).
        - If no ID is mentioned, return `0`.

        Examples:
        Query: "Show address and occupation for member 10"
        Response: `10`

        Query: "Fetch email for John Doe"
        Response: `0`

        Query: "%s"
    """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.0)))
                .map(response -> {
                    String raw = response.getResponse().trim();
                    return raw.matches("\\d+") ? Long.parseLong(raw) : 0L;
                });
    }
    private String extractMemberName(String query) {
        String prompt = """
        TASK: Extract the member's first and/or last name from the query.
        - If the query contains a member name, return ONLY the name (e.g., "John Doe").
        - If no name is found, return "UNKNOWN".

        Examples:
        Query: "Show address and occupation for John Doe"
        Response: "John Doe"

        Query: "Fetch email for Jane"
        Response: "Jane"

        Query: "Retrieve details for user 25"
        Response: "UNKNOWN"

        Query: "%s"
    """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.0)))
                .map(response -> response.getResponse().trim())
                .map(name -> "UNKNOWN".equalsIgnoreCase(name) ? "" : name)
                .block();  // Using `.block()` because this is part of synchronous validation
    }

    private Mono<String> handleDatabaseQuery(String query) {
        return detectMemberId(query)
                .flatMap(memberId -> {
                    if (memberId > 0) {
                        log.info("🔍 Member ID detected: {}. Fetching details...", memberId);
                        return validateAndExecuteSQL(memberId)
                                .flatMap(this::formatResponse);
                    }
                    log.warn("⚠️ No member ID detected. Extracting name instead...");
                    return fetchByMemberName(query)
                            .switchIfEmpty(Mono.just("❌ No member found with the given details."));
                });
    }



    private Mono<String> generateSQL(String query) {
        String prompt = SCHEMA_INFO + """
            SQL GENERATION:
            - Create a SELECT query to retrieve the required member details.
            - Ensure it joins `ods.member` and `ods.member_detail` tables.
            - If a member ID is mentioned, filter using `WHERE member.memberid = X`.
            - If a name is mentioned, use `WHERE firstname LIKE` or `WHERE lastname LIKE`.

            Query: "%s"

            Example Output:
            SELECT m.firstname, m.lastname, md.address1, md.occupation 
            FROM ods.member m 
            JOIN ods.member_detail md ON m.memberid = md.memberid 
            WHERE m.firstname LIKE 'John%%' OR m.lastname LIKE 'Doe%%';
            """.formatted(query);

        return ollamaService.generateResponse(
                        new OllamaRequest("deepseek-r1:1.5b", prompt, false, Map.of("temperature", 0.1)))
                .map(response -> {
                    String sql = response.getResponse().replace(";", "");
                    if (!sql.toLowerCase().startsWith("select")) {
                        log.warn("⚠️ AI generated invalid SQL. Using fallback query.");
                        return "SELECT * FROM ods.member m JOIN ods.member_detail md ON m.memberid = md.memberid;";
                    }
                    return sql;
                })
                .doOnNext(sql -> log.info("📝 Generated SQL:\n{}", sql));
    }

    private Mono<Member> validateAndExecuteSQL(Long memberId) {
        return Mono.fromCallable(() -> {
            log.info("⚙️ Fetching member details for ID: {}", memberId);
            return memberRepository.findWithDetails(memberId)
                    .orElseThrow(() -> new RuntimeException("🔎 Member not found"));
        });
    }


    private Mono<String> formatResponse(Member member) {
        return Mono.fromCallable(() -> {
            log.info("📦 Formatting response...");
            StringBuilder response = new StringBuilder("✅ Member Details:\n")
                    .append("🆔 ID: ").append(member.getMemberId()).append("\n")
                    .append("👤 Name: ").append(member.getFirstName()).append(" ").append(member.getLastName()).append("\n")
                    .append("📧 Email: ").append(member.getEmail()).append("\n");

            MemberDetail detail = member.getMemberDetail();
            if (detail != null) {
                appendIfPresent(response, "📞 Phone", detail.getPhoneNumber());
                appendIfPresent(response, "🏠 Address", detail.getAddress1());
                appendIfPresent(response, "💼 Occupation", detail.getOccupation());
            }
            return response.toString();
        });
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

    private Mono<String> fetchByMemberName(String query) {
        String extractedName = extractMemberName(query);
        if (extractedName.isEmpty()) {
            return Mono.just("❌ Unable to determine which member you are referring to.");
        }

        return Mono.fromCallable(() -> {
            log.info("🔍 Searching for member by name: {}", extractedName);
            Optional<Member> memberOpt = memberRepository.findByName(extractedName);

            if (memberOpt.isEmpty()) {
                return "❌ No member found with name: " + extractedName;
            }

            return formatResponse(memberOpt.get()).block();
        });
    }

    private void appendIfPresent(StringBuilder sb, String label, Object value) {
        if (value != null) sb.append(label).append(": ").append(value).append("\n");
    }
}
