package com.example.modelexecutionservice.entity;

import com.example.modelexecutionservice.domain.ChunkStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "model_execution_chunks",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_chunk_execution_chunkindex", columnNames = {"execution_id", "chunk_index"}),
                @UniqueConstraint(name = "uc_chunk_idempotency_key", columnNames = {"idempotency_key"})
        },
        indexes = {
                @Index(name = "idx_chunks_execution", columnList = "execution_id"),
                @Index(name = "idx_chunks_status", columnList = "status")
        })
public class ModelExecutionChunk {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "execution_id", nullable = false)
    private UUID executionId;


    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex; // 0..totalChunks-1


    @Column(name = "start_offset", nullable = false)
    private Long startOffset; // inclusive row offset in position file


    @Column(name = "end_offset", nullable = false)
    private Long endOffset; // exclusive row offset in position file


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ChunkStatus status;


    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount;


    @Column(name = "worker_id")
    private String workerId; // hostname/pod-id, optional


    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey; // executionId + chunkIndex (and maybe checksum)

    @Column(name = "payload_checksum", length = 64)
    private String payloadChecksum; // optional integrity check


    @Column(name = "started_at")
    private LocalDateTime startedAt;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(name = "processed_loans")
    private Long processedLoans;


    @Column(name = "succeeded_loans")
    private Long succeededLoans;


    @Column(name = "failed_loans")
    private Long failedLoans;


    @Column(name = "last_error")
    private String lastError;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "error_summary")
    private String errorSummary;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ChunkStatus.PENDING;
        if (attemptCount == null) attemptCount = 0;
        if (processedLoans == null) processedLoans = 0L;
        if (succeededLoans == null) succeededLoans = 0L;
        if (failedLoans == null) failedLoans = 0L;
    }


    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}