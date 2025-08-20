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

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExecutionOrchestratorImpl implements ExecutionOrchestrator {

    private final ModelExecutionRepository executionRepo;
    private final ModelExecutionChunkRepository chunkRepo;
    private final PositionServiceClient positionClient;
    private final AssumptionServiceClient assumptionClient;
    private final ModelServiceClient modelClient;

    @Override
    @Transactional
    public ModelExecution start(CreateExecutionRequest req) {
        // 1) Validate upstream resources

        // Fetch model by ROW id (id-only endpoint) and resolve version from the row
        var model = modelClient.getById(req.modelId());
        int resolvedVersion = model.version();

        // If caller sent a version, enforce it matches the row's version
        if (req.modelVersion() != null && !req.modelVersion().equals(resolvedVersion)) {
            throw new IllegalArgumentException(
                    "Model version mismatch: requested " + req.modelVersion()
                            + " but found " + resolvedVersion + " for modelId " + req.modelId());
        }

        // Validate assumption set exists
        assumptionClient.assertExists(req.assumptionSetId());

        // Get total loans from Position service
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

        exec = executionRepo.saveAndFlush(exec); // generate UUID

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
        chunkRepo.saveAll(chunks);

        // 4) Mark QUEUED (publishing happens in Step 3)
        exec.setStatus(ExecutionStatus.QUEUED);
        exec = executionRepo.save(exec);

        return exec;
    }

    @Override
    @Transactional(readOnly = true)
    public ModelExecution get(UUID executionId) {
        return executionRepo.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Execution not found"));
    }
}
