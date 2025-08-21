package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.entity.ExecutionResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, UUID> {

    // Basic fetches
    Page<ExecutionResult> findByExecutionId(UUID executionId, Pageable pageable);
    long countByExecutionId(UUID executionId);
    Optional<ExecutionResult> findByExecutionIdAndLoanId(UUID executionId, String loanId);

    // Useful for cleanups (e.g., re-runs)
    void deleteByExecutionId(UUID executionId);

    // Stream large result sets when exporting
    @Query("select r from ExecutionResult r where r.executionId = :executionId")
    Stream<ExecutionResult> streamByExecutionId(UUID executionId);
}
