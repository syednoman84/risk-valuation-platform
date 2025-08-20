package com.example.modelexecutionservice.entity;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "model_executions",
        indexes = {
                @Index(name = "idx_model_executions_status", columnList = "status"),
                @Index(name = "idx_model_executions_requested_at", columnList = "requested_at")
        })
public class ModelExecution {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "model_id", nullable = false)
    private UUID modelId;


    @Column(name = "model_version", nullable = false)
    private Integer modelVersion;


    @Column(name = "position_file_id", nullable = false)
    private UUID positionFileId;


    @Column(name = "assumption_set_id", nullable = false)
    private UUID assumptionSetId;


    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExecutionStatus status;


    @Column(name = "total_loans")
    private Long totalLoans;


    @Column(name = "chunk_size")
    private Integer chunkSize;


    @Column(name = "total_chunks")
    private Integer totalChunks;


    @Column(name = "processed_loans", nullable = false)
    private Long processedLoans;


    @Column(name = "succeeded_loans", nullable = false)
    private Long succeededLoans;
    @Column(name = "failed_loans", nullable = false)
    private Long failedLoans;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options", columnDefinition = "jsonb")
    private JsonNode options; // e.g., {"dryRun":false, "maxConcurrency":4}


    @Column(name = "error_summary")
    private String errorSummary; // top-level failure message, if any


    @Column(name = "cancel_requested", nullable = false)
    private boolean cancelRequested;


    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;


    @Column(name = "started_at")
    private LocalDateTime startedAt;


    @Column(name = "completed_at")
    private LocalDateTime completedAt;


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @Version
    @Column(name = "version", nullable = false)
    private Long version;


    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (requestedAt == null) requestedAt = now;
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ExecutionStatus.PENDING;
        if (processedLoans == null) processedLoans = 0L;
        if (succeededLoans == null) succeededLoans = 0L;
        if (failedLoans == null) failedLoans = 0L;
    }


    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
