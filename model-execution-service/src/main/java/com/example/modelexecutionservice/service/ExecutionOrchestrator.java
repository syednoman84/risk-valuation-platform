package com.example.modelexecutionservice.service;

import com.example.modelexecutionservice.dto.CreateExecutionRequest;
import com.example.modelexecutionservice.entity.ModelExecution;

import java.util.UUID;

public interface ExecutionOrchestrator {
    ModelExecution start(CreateExecutionRequest request);
    ModelExecution get(UUID executionId);
}

