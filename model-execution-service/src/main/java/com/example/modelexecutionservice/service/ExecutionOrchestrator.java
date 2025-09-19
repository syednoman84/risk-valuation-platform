package com.example.modelexecutionservice.service;

import com.example.modelexecutionservice.dto.CreateExecutionRequest;
import com.example.modelexecutionservice.entity.ModelExecution;

import java.util.Map;
import java.util.UUID;

public interface ExecutionOrchestrator {
    ModelExecution start(CreateExecutionRequest request);
    ModelExecution get(UUID executionId);
    UUID executeModelWithContext(UUID modelId, String modelVersion, UUID positionFileId, 
                                UUID assumptionSetId, Map<String, Object> previousResults, 
                                String outputPrefix);
    ModelExecution executeModelSynchronously(UUID modelId, String modelVersion, UUID positionFileId, 
                                            UUID assumptionSetId, Map<String, Object> previousResults, 
                                            String outputPrefix);
}

