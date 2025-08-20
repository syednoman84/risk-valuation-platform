package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.domain.ChunkStatus;
import com.example.modelexecutionservice.entity.ModelExecutionChunk;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ModelExecutionChunkRepository extends JpaRepository<ModelExecutionChunk, UUID> {
    List<ModelExecutionChunk> findByExecutionId(UUID executionId);
    List<ModelExecutionChunk> findByExecutionIdAndStatus(UUID executionId, ChunkStatus status);
    Optional<ModelExecutionChunk> findByExecutionIdAndChunkIndex(UUID executionId, Integer chunkIndex);
}