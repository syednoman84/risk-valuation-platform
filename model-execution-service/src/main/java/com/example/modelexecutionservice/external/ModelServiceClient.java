package com.example.modelexecutionservice.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class ModelServiceClient {
    private final WebClient client;
    private final ObjectMapper objectMapper;

    public ModelServiceClient(WebClient modelWebClient, ObjectMapper objectMapper) {
        this.client = modelWebClient;
        this.objectMapper = objectMapper;
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

    /**
     * Prefer GET /api/models/{id}/definition -> JSON.
     * If your service doesn't expose it, falls back to GET /api/models/{id} and parses modelDefinition string.
     */
    public JsonNode getDefinition(UUID modelId, Integer version) {
        // Try dedicated definition endpoint (if your model service supports it)
        try {
            JsonNode viaDefinitionEndpoint = client.get()
                    .uri(b -> b.path("/api/models/{id}/definition").build(modelId))
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();
            if (viaDefinitionEndpoint != null && !viaDefinitionEndpoint.isMissingNode()) {
                return viaDefinitionEndpoint;
            }
        } catch (Exception ignore) {
            // fall back to GET by id
        }

        // Fallback: parse modelDefinition string from details
        ModelDetails details = getById(modelId);
        if (details == null || details.modelDefinition() == null) {
            throw new IllegalStateException("Model definition not available for modelId=" + modelId);
        }
        try {
            return objectMapper.readTree(details.modelDefinition());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse modelDefinition for modelId=" + modelId, e);
        }
    }
}
