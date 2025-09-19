package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.entity.ChainExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChainExecutionRepository extends JpaRepository<ChainExecution, UUID> {
    
    @Query("SELECT ce FROM ChainExecution ce LEFT JOIN FETCH ce.stepExecutions WHERE ce.id = :id")
    Optional<ChainExecution> findByIdWithSteps(@Param("id") UUID id);
    
    List<ChainExecution> findByModelChainIdOrderByCreatedAtDesc(UUID modelChainId);
}