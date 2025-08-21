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

    /** Lightweight DTO used by the worker */
    public record LoanRow(String loanId, Map<String, Object> fields) {}

    /** Response DTO coming from the Position service; adjust to your actual payload. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class LoanRowResponse {
        public String loanId;
        public Map<String, Object> fields;
    }

    /** GET /api/positions/{id}/loans?offset=...&limit=... -> [ {loanId, fields{...}}, ... ] */
    public List<LoanRow> getLoansSlice(UUID positionFileId, long offset, int limit) {
        LoanRowResponse[] resp = client.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/positions/{id}/loans")
                        .queryParam("offset", offset)
                        .queryParam("limit", limit)
                        .build(positionFileId))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError(),
                        r -> Mono.error(new IllegalArgumentException("Invalid positionFileId or slice params"))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        r -> Mono.error(new IllegalStateException("Position service unavailable"))
                )
                .bodyToMono(LoanRowResponse[].class)
                .block();

        if (resp == null) throw new IllegalStateException("Null loans slice from position service");

        return java.util.Arrays.stream(resp)
                .map(r -> new LoanRow(r.loanId, r.fields))
                .toList();
    }
}

