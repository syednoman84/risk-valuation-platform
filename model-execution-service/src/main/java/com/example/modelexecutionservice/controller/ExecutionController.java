package com.example.modelexecutionservice.controller;

import com.example.modelexecutionservice.dto.CreateExecutionRequest;
import com.example.modelexecutionservice.dto.CreateExecutionResponse;
import com.example.modelexecutionservice.dto.ExecutionView;
import com.example.modelexecutionservice.entity.ModelExecution;
import com.example.modelexecutionservice.service.ExecutionOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/executions")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionOrchestrator orchestrator;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CreateExecutionResponse create(@Valid @RequestBody CreateExecutionRequest req) {
        ModelExecution exec = orchestrator.start(req);
        return new CreateExecutionResponse(
                exec.getId(),
                exec.getStatus(),
                exec.getTotalLoans() == null ? 0 : exec.getTotalLoans(),
                exec.getChunkSize() == null ? 0 : exec.getChunkSize(),
                exec.getTotalChunks() == null ? 0 : exec.getTotalChunks()
        );
    }

    @GetMapping("/{executionId}")
    public ExecutionView get(@PathVariable UUID executionId) {
        ModelExecution e = orchestrator.get(executionId);
        return new ExecutionView(
                e.getId(),
                e.getModelId(),
                e.getModelVersion(),
                e.getPositionFileId(),
                e.getAssumptionSetId(),
                e.getStatus(),
                e.getTotalLoans(),
                e.getChunkSize(),
                e.getTotalChunks(),
                e.getProcessedLoans(),
                e.getSucceededLoans(),
                e.getFailedLoans(),
                e.getRequestedAt(),
                e.getStartedAt(),
                e.getCompletedAt()
        );
    }
}

