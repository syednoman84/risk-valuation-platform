package com.example.modelexecutionservice.external;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

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

}

