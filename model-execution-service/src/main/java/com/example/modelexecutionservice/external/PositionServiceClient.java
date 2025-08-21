package com.example.modelexecutionservice.external;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class PositionServiceClient {
    private final WebClient client;

    public PositionServiceClient(WebClient positionWebClient) {
        this.client = positionWebClient;
    }

    public long getTotalLoans(UUID positionFileId) {
        // Example endpoint: GET /position-files/{id}/loans/count -> { "count": 12345 }
        record CountResp(long count) {}

        CountResp resp = client.get()
                .uri(uriBuilder -> uriBuilder.path("/api/positions/{id}/loans/count")
                        .build(positionFileId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid positionFileId"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Position service unavailable"))
                )
                .bodyToMono(CountResp.class)
                .block();

        if (resp == null) {
            throw new IllegalStateException("Null response from position service");
        }
        long count = resp.count();
        if (count < 0) {
            throw new IllegalStateException("Position service returned negative loan count");
        }
        return count;
    }

    public void assertExists(UUID positionFileId) {
        client.head()
                .uri(uriBuilder -> uriBuilder.path("/api/positions/{id}").build(positionFileId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid positionFileId"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Position service unavailable"))
                )
                .toBodilessEntity()
                .block();
    }
}

