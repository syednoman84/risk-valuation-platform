package com.example.modelexecutionservice.dto;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExecutionView(
        UUID id,
        UUID modelId,
        Integer modelVersion,
        UUID positionFileId,
        UUID assumptionSetId,
        ExecutionStatus status,
        Long totalLoans,
        Integer chunkSize,
        Integer totalChunks,
        Long processedLoans,
        Long succeededLoans,
        Long failedLoans,
        LocalDateTime requestedAt,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {}
