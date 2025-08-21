package com.example.modelexecutionservice.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateExecutionRequest(
        @NotNull UUID modelId,
        Integer modelVersion,
        @NotNull UUID positionFileId,
        @NotNull UUID assumptionSetId,
        @Min(1) Integer chunkSize,
        JsonNode options
) {}
