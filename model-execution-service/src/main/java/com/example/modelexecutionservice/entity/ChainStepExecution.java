package com.example.modelexecutionservice.entity;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "chain_step_execution")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainStepExecution {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chain_execution_id", nullable = false)
    private ChainExecution chainExecution;
    
    @Column(nullable = false)
    private UUID modelChainStepId;
    
    @Column(nullable = false)
    private Integer stepOrder;
    
    @Column(nullable = false)
    private String stepName;
    
    // Reference to the underlying ModelExecution
    private UUID modelExecutionId;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;
    
    private Long processedLoans;
    private Long succeededLoans;
    private Long failedLoans;
    
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    
    private String errorMessage;
}