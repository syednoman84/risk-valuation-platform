package com.example.modelexecutionservice.entity;

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
@Table(name = "execution_results",
        indexes = {
                @Index(name = "idx_execution_results_execution_id", columnList = "execution_id"),
                @Index(name = "idx_execution_results_loan_id", columnList = "loan_id")
        })
public class ExecutionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "loan_id", nullable = false)
    private String loanId;  // from position file

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "output", columnDefinition = "jsonb", nullable = false)
    private JsonNode output;  // all outputs (derived fields, contractual, expected cashflows)

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}

