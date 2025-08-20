package com.example.assumptionmanagementservice.dto;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.Map;

@Data
@Builder
public class GlobalErrorResponse {
    private String error;
    private String message;
    private int statusCode;
    private String status;
    private String requestUrl;
    private String method;
    private Map<String, String> requestHeaders;
    private Object requestBody;
    private Map<String, String> responseHeaders;
    private Object responseBody;
    private ZonedDateTime timestamp;
}
