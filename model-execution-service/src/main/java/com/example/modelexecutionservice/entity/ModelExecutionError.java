package com.example.modelexecutionservice.entity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;


import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@Entity
@Table(name = "model_execution_errors",
        indexes = {
                @Index(name = "idx_errors_execution", columnList = "execution_id"),
                @Index(name = "idx_errors_chunk", columnList = "chunk_id")
        })
public class ModelExecutionError {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;


    @Column(name = "execution_id", nullable = false)
    private UUID executionId;


    @Column(name = "chunk_id")
    private UUID chunkId;


    @Column(name = "loan_id")
    private String loanId;


    @Column(name = "error_code")
    private String errorCode;


    @Column(name = "message", nullable = false)
    private String message;


    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_input", columnDefinition = "jsonb")
    private JsonNode rawInput; // optional: input row for debugging


    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;


    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
