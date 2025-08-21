package com.example.modelexecutionservice.controller;

import com.example.modelexecutionservice.entity.ExecutionResult;
import com.example.modelexecutionservice.repository.ExecutionResultRepository;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/executions")
public class ExecutionResultsController {

    private final ExecutionResultRepository resultRepo;

    @GetMapping("/{executionId}/results")
    public Page<ResultSummaryDto> listResults(
            @PathVariable UUID executionId,
            @PageableDefault(size = 50, sort = "loanId") Pageable pageable
    ) {
        Page<ExecutionResult> page = resultRepo.findByExecutionId(executionId, pageable);
        return page.map(ResultSummaryDto::from);
    }

    // DTO to avoid exposing internal entity internals
    public record ResultSummaryDto(
            UUID id,
            String loanId,
            JsonNode output,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAt
    ) {
        public static ResultSummaryDto from(ExecutionResult r) {
            return new ResultSummaryDto(
                    r.getId(),
                    r.getLoanId(),
                    r.getOutput(),
                    r.getCreatedAt()
            );
        }
    }
}

