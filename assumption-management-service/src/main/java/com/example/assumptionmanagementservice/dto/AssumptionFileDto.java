package com.example.assumptionmanagementservice.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class AssumptionFileDto {
    private UUID id;
    private String key;
    private String originalFileName;
    private String filePath;
    private LocalDateTime uploadedAt;
}

