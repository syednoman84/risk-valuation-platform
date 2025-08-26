package com.example.modelexecutionservice.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class AssumptionServiceClient {
    private final WebClient client;
    private final boolean useHardcodedAssumptions;

    public AssumptionServiceClient(WebClient assumptionWebClient, 
                                   @Value("${assumptions.use-hardcoded:false}") boolean useHardcodedAssumptions) {
        this.client = assumptionWebClient;
        this.useHardcodedAssumptions = useHardcodedAssumptions;
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
        String url = "/api/assumptions/" + assumptionSetId + (useHardcodedAssumptions ? "?hardcoded=true" : "");
        log.info("Making API call to Assumption Service: GET {}", url);
        AssumptionBundleResponse resp = client.get()
                .uri(uriBuilder -> {
                    var builder = uriBuilder.path("/api/assumptions/{id}");
                    if (useHardcodedAssumptions) {
                        builder.queryParam("hardcoded", "true");
                    }
                    return builder.build(assumptionSetId);
                })
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

        if (resp == null) throw new IllegalStateException("Null Assumption Set from assumption service");

        return new AssumptionBundle(
                resp.keyValues == null ? Map.of() : resp.keyValues,
                resp.tables == null ? Map.of() : resp.tables
        );
    }

}

