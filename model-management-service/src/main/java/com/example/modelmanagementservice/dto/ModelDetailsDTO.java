package com.example.modelmanagementservice.dto;

import com.example.modelmanagementservice.entity.LoanModel;

import java.time.LocalDateTime;
import java.util.UUID;

public record ModelDetailsDTO(
        UUID id,
        String name,
        Integer version,
        Boolean active,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String modelDefinition   // keep String since your entity stores a String
) {
    public static ModelDetailsDTO from(LoanModel m) {
        return new ModelDetailsDTO(
                m.getId(),
                m.getName(),
                m.getVersion(),
                m.isActive(),
                m.getDescription(),
                m.getCreatedAt(),
                m.getUpdatedAt(),
                m.getModelDefinition()
        );
    }
}



