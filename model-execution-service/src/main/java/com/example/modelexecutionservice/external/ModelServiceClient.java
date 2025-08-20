package com.example.modelexecutionservice.external;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ModelServiceClient {
    private final WebClient client;

    public ModelServiceClient(WebClient modelWebClient) {
        this.client = modelWebClient;
    }

    // HEAD /api/models/{id}
    public void assertExists(UUID modelId) {
        client.head()
                .uri(b -> b.path("/api/models/{id}").build(modelId))
                .retrieve()
                .onStatus(s -> s.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid modelId")))
                .onStatus(s -> s.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Model service unavailable")))
                .toBodilessEntity()
                .block();
    }

    // GET /api/models/{id}
    public record ModelDetails(
            UUID id,
            String name,
            Integer version,
            Boolean active,
            String description,
            String modelDefinition,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}

    public ModelDetails getById(UUID modelId) {
        return client.get()
                .uri(b -> b.path("/api/models/{id}").build(modelId))
                .retrieve()
                .onStatus(s -> s.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid modelId")))
                .onStatus(s -> s.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Model service unavailable")))
                .bodyToMono(ModelDetails.class)
                .block();
    }
}
