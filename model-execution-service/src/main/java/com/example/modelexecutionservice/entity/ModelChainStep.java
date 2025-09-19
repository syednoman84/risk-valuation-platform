package com.example.modelexecutionservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "model_chain_step")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelChainStep {
    
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_chain_id", nullable = false)
    private ModelChain modelChain;
    
    @Column(nullable = false)
    private UUID modelId;
    
    private String modelVersion;
    
    @Column(nullable = false)
    private Integer executionOrder;
    
    @Column(nullable = false)
    private String stepName;
    
    private String description;
    
    // Optional: Override assumptions for this step
    private UUID assumptionSetId;
    
    // Fields from previous steps to include in context
    @Column(columnDefinition = "TEXT")
    private String outputFieldsToInclude; // JSON array of field names
    
    // Prefix for previous step outputs (e.g., "prev_" or "step1_")
    private String outputPrefix;
}