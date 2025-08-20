package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.domain.ExecutionStatus;
import com.example.modelexecutionservice.entity.ModelExecution;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;
import java.util.UUID;


public interface ModelExecutionRepository extends JpaRepository<ModelExecution, UUID> {
    List<ModelExecution> findTop50ByStatusOrderByRequestedAtAsc(ExecutionStatus status);
}
