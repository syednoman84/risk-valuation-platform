package com.example.modelexecutionservice.service;

import com.example.modelexecutionservice.domain.ChunkStatus;
import com.example.modelexecutionservice.domain.ExecutionStatus;
import com.example.modelexecutionservice.dto.CreateExecutionRequest;
import com.example.modelexecutionservice.entity.ModelExecution;
import com.example.modelexecutionservice.entity.ModelExecutionChunk;
import com.example.modelexecutionservice.external.AssumptionServiceClient;
import com.example.modelexecutionservice.external.ModelServiceClient;
import com.example.modelexecutionservice.external.PositionServiceClient;
import com.example.modelexecutionservice.repository.ModelExecutionChunkRepository;
import com.example.modelexecutionservice.repository.ModelExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ExecutionOrchestratorImpl implements ExecutionOrchestrator {

    private final ModelExecutionRepository executionRepo;
    private final ModelExecutionChunkRepository chunkRepo;
    private final PositionServiceClient positionClient;
    private final AssumptionServiceClient assumptionClient;
    private final ModelServiceClient modelClient;

    // NEW: worker to process chunks asynchronously
    private final ChunkExecutionWorker chunkWorker;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ModelExecution start(CreateExecutionRequest req) {
        // 1) Validate upstream resources

        // Fetch model row and resolve version
        var model = modelClient.getById(req.modelId());
        int resolvedVersion = model.version();

        if (req.modelVersion() != null && !req.modelVersion().equals(resolvedVersion)) {
            throw new IllegalArgumentException(
                    "Model version mismatch: requested " + req.modelVersion()
                            + " but found " + resolvedVersion + " for modelId " + req.modelId());
        }

        // Validate assumption set
        assumptionClient.assertExists(req.assumptionSetId());

        // Get total loans
        long totalLoans = positionClient.getTotalLoans(req.positionFileId());
        if (totalLoans <= 0) {
            throw new IllegalArgumentException("Position file has zero loans");
        }

        int chunkSize = (req.chunkSize() == null || req.chunkSize() <= 0) ? 5000 : req.chunkSize();
        int totalChunks = (int) Math.ceil((double) totalLoans / (double) chunkSize);

        // 2) Create execution
        ModelExecution exec = ModelExecution.builder()
                .modelId(req.modelId())
                .modelVersion(resolvedVersion)
                .positionFileId(req.positionFileId())
                .assumptionSetId(req.assumptionSetId())
                .status(ExecutionStatus.PENDING)
                .totalLoans(totalLoans)
                .chunkSize(chunkSize)
                .totalChunks(totalChunks)
                .options(req.options())
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        exec = executionRepo.saveAndFlush(exec);

        // 3) Create chunks
        List<ModelExecutionChunk> chunks = new ArrayList<>(totalChunks);
        long startOffset = 0;
        for (int i = 0; i < totalChunks; i++) {
            long endExclusive = Math.min(startOffset + chunkSize, totalLoans);
            String idempKey = exec.getId() + ":" + i;

            ModelExecutionChunk chunk = ModelExecutionChunk.builder()
                    .executionId(exec.getId())
                    .chunkIndex(i)
                    .startOffset(startOffset)
                    .endOffset(endExclusive)
                    .status(ChunkStatus.PENDING)
                    .attemptCount(0)
                    .idempotencyKey(idempKey)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            chunks.add(chunk);
            startOffset = endExclusive;
        }
        chunks = chunkRepo.saveAll(chunks);

        // 4) Mark QUEUED
        exec.setStatus(ExecutionStatus.QUEUED);
        exec = executionRepo.save(exec);

        // 5) AFTER COMMIT: dispatch chunks asynchronously
        final UUID executionId = exec.getId();
        final List<UUID> chunkIds = chunks.stream().map(ModelExecutionChunk::getId).toList();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // Fire-and-forget; ChunkExecutionWorker.processChunk is @Async
                for (UUID chunkId : chunkIds) {
                    chunkWorker.processChunk(chunkId);
                }
            }
        });

        return exec;
    }

    @Override
    @Transactional(readOnly = true)
    public ModelExecution get(UUID executionId) {
        return executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
    }
    
    @Override
    @Transactional
    public UUID executeModelWithContext(UUID modelId, String modelVersion, UUID positionFileId, 
                                       UUID assumptionSetId, Map<String, Object> previousResults, 
                                       String outputPrefix) {
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("previousResults", previousResults);
        optionsMap.put("outputPrefix", outputPrefix != null ? outputPrefix : "prev_");
        
        CreateExecutionRequest request = new CreateExecutionRequest(
                modelId, 
                modelVersion != null ? Integer.parseInt(modelVersion) : null,
                positionFileId, 
                assumptionSetId, 
                null, // chunkSize - use default
                objectMapper.valueToTree(optionsMap)
        );
        
        ModelExecution execution = start(request);
        return execution.getId();
    }
    
    @Override
    @Transactional
    public ModelExecution executeModelSynchronously(UUID modelId, String modelVersion, UUID positionFileId, 
                                                   UUID assumptionSetId, Map<String, Object> previousResults, 
                                                   String outputPrefix) {
        UUID executionId = executeModelWithContext(modelId, modelVersion, positionFileId, 
                                                  assumptionSetId, previousResults, outputPrefix);
        
        // Wait for completion
        while (true) {
            ModelExecution execution = get(executionId);
            ExecutionStatus status = execution.getStatus();
            
            if (status == ExecutionStatus.COMPLETED || 
                status == ExecutionStatus.FAILED || 
                status == ExecutionStatus.CANCELED) {
                return execution;
            }
            
            try {
                Thread.sleep(500); // Wait 500ms before checking again
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for execution", e);
            }
        }
    }
}
