package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.entity.ModelExecutionError;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;


public interface ModelExecutionErrorRepository extends JpaRepository<ModelExecutionError, UUID> {
    List<ModelExecutionError> findByExecutionId(UUID executionId);
    List<ModelExecutionError> findByChunkId(UUID chunkId);
}
