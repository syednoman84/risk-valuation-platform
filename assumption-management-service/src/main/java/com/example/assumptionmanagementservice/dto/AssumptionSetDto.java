package com.example.assumptionmanagementservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
public class AssumptionSetDto {
    private UUID id;
    private String name;
    private String description;
    private boolean locked;
    private List<AssumptionValueDto> textBasedAssumptions;
    private List<AssumptionFileDto> fileBasedAssumptions;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}


