package com.example.modelexecutionservice.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class AssumptionServiceClient {
    private final WebClient client;

    public AssumptionServiceClient(WebClient assumptionWebClient) {
        this.client = assumptionWebClient;
    }

    public void assertExists(UUID assumptionSetId) {
        client.head()
                .uri(uriBuilder -> uriBuilder.path("/api/assumptions/{id}").build(assumptionSetId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid assumptionSetId"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Assumption service unavailable"))
                )
                .toBodilessEntity()
                .block();
    }

    /** Wrapper record used by workers/orchestrators. */
    public record AssumptionBundle(
            Map<String, Object> keyValues,
            Map<String, List<Map<String, Object>>> tables
    ) {}

    /** DTO shape expected from your Assumption service; adjust field names if needed. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class AssumptionBundleResponse {
        public Map<String, Object> keyValues;
        public Map<String, List<Map<String, Object>>> tables;
    }

    /** GET /api/assumptions/{id}/bundle -> { keyValues: {...}, tables: {tableName: [ {col:val}, ... ] } } */
    public AssumptionBundle getBundle(UUID assumptionSetId) {
        AssumptionBundleResponse resp = client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/assumptions/{id}/bundle").build(assumptionSetId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid assumptionSetId"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Assumption service unavailable"))
                )
                .bodyToMono(AssumptionBundleResponse.class)
                .block();

        if (resp == null) throw new IllegalStateException("Null bundle from assumption service");

        return new AssumptionBundle(
                resp.keyValues == null ? Map.of() : resp.keyValues,
                resp.tables == null ? Map.of() : resp.tables
        );
    }

}

