package com.example.modelexecutionservice.controller;

import com.example.modelexecutionservice.entity.ChainExecution;
import com.example.modelexecutionservice.entity.ModelChain;
import com.example.modelexecutionservice.service.ModelChainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/chains")
@RequiredArgsConstructor
public class ModelChainController {
    
    private final ModelChainService modelChainService;
    
    @PostMapping
    public ResponseEntity<Map<String, UUID>> createChain(@RequestBody CreateChainRequest request) {
        UUID chainId = modelChainService.createModelChain(
                request.name(), 
                request.description(), 
                request.steps()
        );
        return ResponseEntity.ok(Map.of("chainId", chainId));
    }
    
    @GetMapping
    public ResponseEntity<List<ModelChain>> getAllChains() {
        return ResponseEntity.ok(modelChainService.getAllChains());
    }
    
    @GetMapping("/{chainId}")
    public ResponseEntity<ModelChain> getChain(@PathVariable UUID chainId) {
        return ResponseEntity.ok(modelChainService.getChainById(chainId));
    }
    
    @PostMapping("/{chainId}/execute")
    public ResponseEntity<Map<String, UUID>> executeChain(
            @PathVariable UUID chainId,
            @RequestBody ExecuteChainRequest request) {
        UUID executionId = modelChainService.executeChain(
                chainId, 
                request.positionFileId(), 
                request.globalAssumptionSetId(),
                request.stepOverrides()
        );
        return ResponseEntity.ok(Map.of("executionId", executionId));
    }
    
    @PostMapping("/execute-template")
    public ResponseEntity<Map<String, UUID>> executeChainTemplate(
            @RequestBody ExecuteChainTemplateRequest request) {
        // Convert controller request to service request
        List<ModelChainService.TemplateStep> serviceSteps = request.steps().stream()
                .map(step -> new ModelChainService.TemplateStep(
                        step.modelId(),
                        step.modelVersion(),
                        step.stepName(),
                        step.description(),
                        step.assumptionSetId(),
                        step.outputFieldsToInclude(),
                        step.outputPrefix()
                ))
                .toList();
        
        ModelChainService.ExecuteChainTemplateRequest serviceRequest = 
                new ModelChainService.ExecuteChainTemplateRequest(
                        request.name(),
                        request.description(),
                        request.positionFileId(),
                        serviceSteps
                );
        
        UUID executionId = modelChainService.executeChainTemplate(serviceRequest);
        return ResponseEntity.ok(Map.of("executionId", executionId));
    }
    
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<ChainExecution> getChainExecution(@PathVariable UUID executionId) {
        return ResponseEntity.ok(modelChainService.getChainExecution(executionId));
    }
    
    @GetMapping("/{chainId}/executions")
    public ResponseEntity<List<ChainExecution>> getChainExecutions(@PathVariable UUID chainId) {
        return ResponseEntity.ok(modelChainService.getChainExecutions(chainId));
    }
    
    public record CreateChainRequest(
            String name,
            String description,
            List<ModelChainService.ModelChainStepRequest> steps
    ) {}
    
    public record ExecuteChainRequest(
            UUID positionFileId,
            UUID globalAssumptionSetId,
            Map<String, ModelChainService.StepOverride> stepOverrides
    ) {}
    
    public record ExecuteChainTemplateRequest(
            String name,
            String description,
            UUID positionFileId,
            List<TemplateStep> steps
    ) {}
    
    public record TemplateStep(
            UUID modelId,
            String modelVersion,
            String stepName,
            String description,
            UUID assumptionSetId,
            List<String> outputFieldsToInclude,
            String outputPrefix
    ) {}
}