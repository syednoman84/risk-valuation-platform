package com.example.modelexecutionservice.controller;

import com.example.modelexecutionservice.entity.ModelExecutionChunk;
import com.example.modelexecutionservice.repository.ModelExecutionChunkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/executions")
public class ExecutionChunksController {

    private final ModelExecutionChunkRepository chunkRepo;

    @GetMapping("/{executionId}/chunks")
    public Page<ChunkSummaryDto> listChunks(
            @PathVariable UUID executionId,
            @PageableDefault(size = 20, sort = "chunkIndex") Pageable pageable
    ) {
        Page<ModelExecutionChunk> page = chunkRepo.findByExecutionId(executionId, pageable);
        return page.map(ChunkSummaryDto::from);
    }

    // DTO kept minimal but useful for UI/monitoring
    public record ChunkSummaryDto(
            UUID id,
            int chunkIndex,
            long startOffset,
            long endOffset,
            String status,
            int attemptCount,
            String errorSummary,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startedAt,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime completedAt,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime updatedAt
    ) {
        public static ChunkSummaryDto from(ModelExecutionChunk c) {
            return new ChunkSummaryDto(
                    c.getId(),
                    c.getChunkIndex(),
                    c.getStartOffset(),
                    c.getEndOffset(),
                    c.getStatus() == null ? null : c.getStatus().name(),
                    c.getAttemptCount() == null ? 0 : c.getAttemptCount(),
                    c.getErrorSummary(),
                    c.getCreatedAt(),
                    c.getStartedAt(),
                    c.getCompletedAt(),
                    c.getUpdatedAt()
            );
        }
    }
}
