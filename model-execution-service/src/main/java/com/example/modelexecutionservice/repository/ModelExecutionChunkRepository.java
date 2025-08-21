package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.domain.ChunkStatus;
import com.example.modelexecutionservice.entity.ModelExecutionChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface ModelExecutionChunkRepository extends JpaRepository<ModelExecutionChunk, UUID> {
    List<ModelExecutionChunk> findByExecutionId(UUID executionId);
    // for the paginated controller endpoint
    Page<ModelExecutionChunk> findByExecutionId(UUID executionId, Pageable pageable);
}