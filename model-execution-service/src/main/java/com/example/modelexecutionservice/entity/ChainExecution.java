package com.example.modelexecutionservice.entity;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "chain_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    private UUID modelChainId; // Nullable for template executions
    
    @Column(nullable = false)
    private UUID positionFileId;
    
    // Global assumption set (can be overridden per step)
    private UUID globalAssumptionSetId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    private Integer currentStep;
    private Integer totalSteps;
    
    @OneToMany(mappedBy = "chainExecution", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<ChainStepExecution> stepExecutions;
    
    private Long totalLoans;
    private Long processedLoans;
    private Long succeededLoans;
    private Long failedLoans;
    
    private LocalDateTime createdAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String errorMessage;
    
    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}