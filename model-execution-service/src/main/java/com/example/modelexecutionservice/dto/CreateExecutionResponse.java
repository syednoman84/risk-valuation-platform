package com.example.modelexecutionservice.dto;

import com.example.modelexecutionservice.domain.ExecutionStatus;

import java.util.UUID;

public record CreateExecutionResponse(
        UUID executionId,
        ExecutionStatus status,
        long totalLoans,
        int chunkSize,
        int totalChunks
) {}

