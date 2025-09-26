package com.example.modelexecutionservice.service;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import com.example.modelexecutionservice.entity.*;
import com.example.modelexecutionservice.repository.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.Set;
import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
public class ModelChainService {
    
    private final ModelChainRepository chainRepository;
    private final ChainExecutionRepository chainExecutionRepository;
    private final ModelExecutionRepository modelExecutionRepository;
    private final ExecutionResultRepository resultRepository;
    private final ExecutionOrchestratorImpl executionOrchestrator;
    private final ObjectMapper objectMapper;
    
    @Transactional
    public UUID createModelChain(String name, String description, List<ModelChainStepRequest> steps) {
        if (chainRepository.existsByName(name)) {
            throw new IllegalArgumentException("Model chain with name '" + name + "' already exists");
        }
        
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("Chain must have at least one step");
        }
        
        // Validate step names are unique
        Set<String> stepNames = new HashSet<>();
        for (ModelChainStepRequest step : steps) {
            if (stepNames.contains(step.stepName())) {
                throw new IllegalArgumentException("Duplicate step name: " + step.stepName());
            }
            stepNames.add(step.stepName());
        }
        
        ModelChain chain = ModelChain.builder()
                .name(name)
                .description(description)
                .build();
        
        chain = chainRepository.save(chain);
        
        List<ModelChainStep> chainSteps = new ArrayList<>();
        for (int i = 0; i < steps.size(); i++) {
            ModelChainStepRequest stepReq = steps.get(i);
            
            String outputFieldsJson = null;
            if (stepReq.outputFieldsToInclude() != null) {
                try {
                    outputFieldsJson = objectMapper.writeValueAsString(stepReq.outputFieldsToInclude());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid outputFieldsToInclude for step: " + stepReq.stepName(), e);
                }
            }
            
            ModelChainStep step = ModelChainStep.builder()
                    .modelChain(chain)
                    .modelId(stepReq.modelId())
                    .modelVersion(stepReq.modelVersion())
                    .executionOrder(i + 1)
                    .stepName(stepReq.stepName())
                    .description(stepReq.description())
                    .assumptionSetId(stepReq.assumptionSetId())
                    .outputFieldsToInclude(outputFieldsJson)
                    .outputPrefix(stepReq.outputPrefix())
                    .build();
            
            chainSteps.add(step);
        }
        
        chain.setSteps(chainSteps);
        chainRepository.save(chain);
        
        return chain.getId();
    }
    
    @Transactional
    public UUID executeChain(UUID chainId, UUID positionFileId, UUID globalAssumptionSetId, Map<String, StepOverride> stepOverrides) {
        ModelChain chain = chainRepository.findByIdWithSteps(chainId)
                .orElseThrow(() -> new IllegalArgumentException("Model chain not found: " + chainId));
        
        ChainExecution chainExecution = ChainExecution.builder()
                .modelChainId(chainId)
                .positionFileId(positionFileId)
                .globalAssumptionSetId(globalAssumptionSetId)
                .status(ExecutionStatus.QUEUED)
                .currentStep(0)
                .totalSteps(chain.getSteps().size())
                .build();
        
        chainExecution = chainExecutionRepository.save(chainExecution);
        
        // Execute steps sequentially
        executeChainSteps(chainExecution, chain, stepOverrides != null ? stepOverrides : Map.of());
        
        return chainExecution.getId();
    }
    
private void executeChainSteps(ChainExecution chainExecution, ModelChain chain, Map<String, StepOverride> stepOverrides) {
        log.info("\n=== CHAIN EXECUTION STARTED ===\nChain: {}\nExecution ID: {}\nTotal Steps: {}", 
                chain.getName(), chainExecution.getId(), chain.getSteps().size());
        
        chainExecution.setStatus(ExecutionStatus.RUNNING);
        chainExecution.setStartedAt(LocalDateTime.now());
        chainExecutionRepository.save(chainExecution);
        
        // Execute first step only
        if (!chain.getSteps().isEmpty()) {
            ModelChainStep firstStep = chain.getSteps().get(0);
            chainExecution.setCurrentStep(firstStep.getExecutionOrder());
            chainExecutionRepository.save(chainExecution);
            
            log.info("\n>>> STEP 1/3: {} <<<\nModel ID: {}\nDescription: {}", 
                    firstStep.getStepName(), firstStep.getModelId(), firstStep.getDescription());
            
            UUID stepExecutionId = executeStep(chainExecution, firstStep, new HashMap<>(), stepOverrides, chain.getSteps());
            
            // Schedule continuation after first step completes
            scheduleChainContinuation(chainExecution.getId(), chain, stepOverrides, 1);
        }
    }
    
    @Async
    private void scheduleChainContinuation(UUID chainExecutionId, ModelChain chain, Map<String, StepOverride> stepOverrides, int nextStepIndex) {
        try {
            // Wait longer for previous step to complete
            Thread.sleep(8000); // Wait 8 seconds
            
            // Check if previous step actually completed
            boolean prevStepCompleted = checkPreviousStepCompleted(chainExecutionId, chain, nextStepIndex - 1);
            if (!prevStepCompleted) {
                log.warn("Previous step not completed yet, waiting longer...");
                Thread.sleep(5000); // Wait additional 5 seconds
                
                // Check again
                prevStepCompleted = checkPreviousStepCompleted(chainExecutionId, chain, nextStepIndex - 1);
                if (!prevStepCompleted) {
                    log.error("Previous step still not completed after 13 seconds, proceeding anyway");
                }
            }
            
            continueChainExecution(chainExecutionId, chain, stepOverrides, nextStepIndex);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private boolean checkPreviousStepCompleted(UUID chainExecutionId, ModelChain chain, int stepIndex) {
        try {
            ChainExecution chainExecution = chainExecutionRepository.findById(chainExecutionId).orElse(null);
            if (chainExecution == null) return false;
            
            ModelChainStep prevStep = chain.getSteps().get(stepIndex);
            List<ModelExecution> executions = modelExecutionRepository.findByPositionFileIdOrderByCreatedAtDesc(chainExecution.getPositionFileId());
            
            for (ModelExecution exec : executions) {
                if (exec.getModelId().equals(prevStep.getModelId()) && 
                    exec.getStatus() == ExecutionStatus.COMPLETED) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Error checking previous step completion", e);
            return false;
        }
    }
    
    private synchronized void continueChainExecution(UUID chainExecutionId, ModelChain chain, Map<String, StepOverride> stepOverrides, int stepIndex) {
        ChainExecution chainExecution = chainExecutionRepository.findById(chainExecutionId).orElse(null);
        if (chainExecution == null || stepIndex >= chain.getSteps().size()) {
            return;
        }
        
        // Check if this step is already running or completed
        if (chainExecution.getCurrentStep() >= stepIndex + 1) {
            log.info("Step {} already running or completed, skipping", stepIndex + 1);
            return;
        }
        
        try {
            // Collect results from all previous steps
            Map<String, Object> accumulatedResults = new HashMap<>();
            if (stepIndex > 0) {
                List<ModelExecution> chainExecutions = modelExecutionRepository.findByPositionFileIdOrderByCreatedAtDesc(chainExecution.getPositionFileId());
                
                // Collect results from all previous steps
                for (int i = 0; i < stepIndex; i++) {
                    ModelChainStep prevStep = chain.getSteps().get(i);
                    
                    // Find the most recent execution that matches this step's model
                    for (ModelExecution exec : chainExecutions) {
                        if (exec.getModelId().equals(prevStep.getModelId()) && 
                            exec.getStatus() == ExecutionStatus.COMPLETED) {
                            log.info("Collecting results from execution: {} for step: {}", exec.getId(), prevStep.getStepName());
                            collectStepResults(exec.getId(), prevStep, accumulatedResults);
                            log.info("Collected results from step {}: {} loans with fields: {}", 
                                    prevStep.getStepName(), accumulatedResults.size(), 
                                    accumulatedResults.isEmpty() ? "none" : 
                                    accumulatedResults.values().iterator().next().getClass().getSimpleName());
                            break;
                        }
                    }
                }
            }
            
            // Execute current step
            ModelChainStep currentStep = chain.getSteps().get(stepIndex);
            chainExecution.setCurrentStep(currentStep.getExecutionOrder());
            chainExecutionRepository.save(chainExecution);
            
            log.info("\n>>> STEP {}/{}: {} <<<\nModel ID: {}\nDescription: {}\nPrevious Results: {} loans", 
                    stepIndex + 1, chain.getSteps().size(), currentStep.getStepName(), 
                    currentStep.getModelId(), currentStep.getDescription(), accumulatedResults.size());
            
            executeStep(chainExecution, currentStep, accumulatedResults, stepOverrides, chain.getSteps());
            
            // Check if more steps remain
            if (stepIndex + 1 < chain.getSteps().size()) {
                scheduleChainContinuation(chainExecutionId, chain, stepOverrides, stepIndex + 1);
            } else {
                // Chain steps scheduled - actual completion will be determined by individual executions
                log.info("\n=== CHAIN STEPS SCHEDULED ===\nChain: {}\nExecution ID: {}\nAll {} steps have been scheduled", 
                        chain.getName(), chainExecution.getId(), chain.getSteps().size());
                chainExecution.setStatus(ExecutionStatus.RUNNING); // Keep as running until all executions complete
                chainExecutionRepository.save(chainExecution);
            }
            
        } catch (Exception e) {
            log.error("Chain execution failed at step {}", stepIndex, e);
            chainExecution.setStatus(ExecutionStatus.FAILED);
            chainExecution.setErrorMessage(e.getMessage());
            chainExecution.setCompletedAt(LocalDateTime.now());
            chainExecutionRepository.save(chainExecution);
        }
    }
    
    private UUID executeStep(ChainExecution chainExecution, ModelChainStep step, Map<String, Object> previousResults, Map<String, StepOverride> stepOverrides, List<ModelChainStep> allSteps) {
        // Create step execution record
        ChainStepExecution stepExecution = ChainStepExecution.builder()
                .chainExecution(chainExecution)
                .modelChainStepId(step.getId())
                .stepOrder(step.getExecutionOrder())
                .stepName(step.getStepName())
                .status(ExecutionStatus.RUNNING)
                .startedAt(LocalDateTime.now())
                .build();
        
        // Apply step overrides if provided
        StepOverride override = stepOverrides.get(step.getStepName());
        
        UUID modelId = (override != null && override.modelId() != null) ? 
                override.modelId() : step.getModelId();
        String modelVersion = (override != null && override.modelVersion() != null) ? 
                override.modelVersion() : step.getModelVersion();
        UUID assumptionSetId = (override != null && override.assumptionSetId() != null) ? 
                override.assumptionSetId() : 
                (step.getAssumptionSetId() != null ? step.getAssumptionSetId() : chainExecution.getGlobalAssumptionSetId());
        
        // For step execution, we need to determine the input prefix based on previous step's output prefix
        String inputPrefix = "prev_"; // default
        if (step.getExecutionOrder() > 1) {
            // Find the previous step to get its output prefix
            for (ModelChainStep prevStep : allSteps) {
                if (prevStep.getExecutionOrder() == step.getExecutionOrder() - 1) {
                    inputPrefix = prevStep.getOutputPrefix() != null ? prevStep.getOutputPrefix() : "prev_";
                    break;
                }
            }
        }
        
        UUID modelExecutionId = executionOrchestrator.executeModelWithContext(
                modelId,
                modelVersion,
                chainExecution.getPositionFileId(),
                assumptionSetId,
                previousResults,
                inputPrefix
        );
        
        stepExecution.setModelExecutionId(modelExecutionId);
        stepExecution.setStatus(ExecutionStatus.COMPLETED);
        stepExecution.setCompletedAt(LocalDateTime.now());
        
        return modelExecutionId;
    }
    
    private void collectStepResults(UUID executionId, ModelChainStep step, Map<String, Object> accumulatedResults) {
        try {
            List<String> fieldsToInclude = step.getOutputFieldsToInclude() != null ?
                    objectMapper.readValue(step.getOutputFieldsToInclude(), new TypeReference<List<String>>() {}) :
                    Collections.emptyList();
            
            if (!fieldsToInclude.isEmpty()) {
                // Collect specified fields from execution results
                List<ExecutionResult> results = resultRepository.streamByExecutionId(executionId)
                        .collect(java.util.stream.Collectors.toList());
                
                Map<String, Map<String, Object>> loanResultsMap = new HashMap<>();
                
                log.info("Processing {} execution results for fields: {}", results.size(), fieldsToInclude);
                
                for (ExecutionResult result : results) {
                    String loanId = result.getLoanId();
                    Map<String, Object> loanResults = new HashMap<>();
                    
                    log.info("Processing result for loan {}, output structure: {}", loanId, result.getOutput().fieldNames());
                    
                    // Extract specified fields from results (not outputs)
                    if (result.getOutput().has("results")) {
                        var outputs = result.getOutput().get("results");
                        log.info("Found results section with fields: {}", outputs.fieldNames());
                        for (String field : fieldsToInclude) {
                            if (outputs.has(field)) {
                                var value = outputs.get(field);
                                if (value.isNumber()) {
                                    loanResults.put(field, value.asDouble());
                                    log.info("Collected field {} = {} for loan {}", field, value.asDouble(), loanId);
                                } else if (value.isBoolean()) {
                                    loanResults.put(field, value.asBoolean());
                                    log.info("Collected field {} = {} for loan {}", field, value.asBoolean(), loanId);
                                } else {
                                    loanResults.put(field, value.asText());
                                    log.info("Collected field {} = {} for loan {}", field, value.asText(), loanId);
                                }
                            } else {
                                log.warn("Field {} not found in results for loan {}", field, loanId);
                            }
                        }
                    } else {
                        log.warn("No 'results' section found in output for loan {}", loanId);
                    }
                    
                    loanResultsMap.put(loanId, loanResults);
                }
                
                accumulatedResults.putAll(loanResultsMap);
            }
        } catch (Exception e) {
            log.error("Failed to collect step results", e);
        }
    }
    
    public List<ModelChain> getAllChains() {
        return chainRepository.findAll();
    }
    
    public ModelChain getChainById(UUID chainId) {
        return chainRepository.findByIdWithSteps(chainId)
                .orElseThrow(() -> new IllegalArgumentException("Model chain not found: " + chainId));
    }
    
    public ChainExecution getChainExecution(UUID executionId) {
        return chainExecutionRepository.findByIdWithSteps(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Chain execution not found: " + executionId));
    }
    
    public List<ChainExecution> getChainExecutions(UUID chainId) {
        return chainExecutionRepository.findByModelChainIdOrderByCreatedAtDesc(chainId);
    }
    
    @Transactional
    public UUID executeChainTemplate(ExecuteChainTemplateRequest request) {
        // Create temporary chain from template
        ModelChain tempChain = ModelChain.builder()
                .name(request.name())
                .description(request.description())
                .build();
        
        List<ModelChainStep> steps = new ArrayList<>();
        for (int i = 0; i < request.steps().size(); i++) {
            var stepReq = request.steps().get(i);
            
            String outputFieldsJson = null;
            if (stepReq.outputFieldsToInclude() != null) {
                try {
                    outputFieldsJson = objectMapper.writeValueAsString(stepReq.outputFieldsToInclude());
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid outputFieldsToInclude for step: " + stepReq.stepName(), e);
                }
            }
            
            ModelChainStep step = ModelChainStep.builder()
                    .modelChain(tempChain)
                    .modelId(stepReq.modelId())
                    .modelVersion(stepReq.modelVersion())
                    .executionOrder(i + 1)
                    .stepName(stepReq.stepName())
                    .description(stepReq.description())
                    .assumptionSetId(stepReq.assumptionSetId())
                    .outputFieldsToInclude(outputFieldsJson)
                    .outputPrefix(stepReq.outputPrefix())
                    .build();
            
            steps.add(step);
        }
        tempChain.setSteps(steps);
        
        // Create execution without saving chain
        ChainExecution chainExecution = ChainExecution.builder()
                .modelChainId(UUID.fromString("00000000-0000-0000-0000-000000000000")) // Template execution placeholder
                .positionFileId(request.positionFileId())
                .globalAssumptionSetId(null)
                .status(ExecutionStatus.QUEUED)
                .currentStep(0)
                .totalSteps(steps.size())
                .build();
        
        chainExecution = chainExecutionRepository.save(chainExecution);
        
        // Execute steps
        executeChainSteps(chainExecution, tempChain, Map.of());
        
        return chainExecution.getId();
    }
    
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
    
    public record ModelChainStepRequest(
            UUID modelId,
            String modelVersion,
            String stepName,
            String description,
            UUID assumptionSetId,
            List<String> outputFieldsToInclude,
            String outputPrefix
    ) {}
    
    public record StepOverride(
            UUID modelId,
            String modelVersion,
            UUID assumptionSetId
    ) {}
}
