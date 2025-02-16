package com.codingforfun.service.agent;

import com.codingforfun.model.dto.OllamaRequest;
import com.codingforfun.model.entity.Task;
import com.codingforfun.repository.TaskRepository;
import com.codingforfun.service.OllamaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DatabaseAgent {
    private final OllamaService ollamaService;
    private final TaskRepository taskRepository;

    public Mono<String> processDatabaseOperation(String instruction) {
        OllamaRequest request = new OllamaRequest(
                "deepseek-r1:1.5b",
                "Convert this to SQL: " + instruction,
                false,
                Map.of("temperature", 0.3)
        );

        return ollamaService.generateResponse(request)
                .flatMap(response -> executeDatabaseOperation(response.getResponse()));
    }

    private Mono<String> executeDatabaseOperation(String sql) {
        return Mono.fromCallable(() -> {
            // Simple insert parsing example
            if (sql.toLowerCase().contains("insert into tasks")) {
                String description = sql.split("'")[1];
                Task task = new Task();
                task.setDescription(description);
                task.setCompleted(false);
                taskRepository.save(task);
                return "Created task: " + description;
            }
            return "Executed: " + sql;
        });
    }
}