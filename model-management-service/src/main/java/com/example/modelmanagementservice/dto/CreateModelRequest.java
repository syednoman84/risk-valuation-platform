package com.example.modelmanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateModelRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    private String jsonDefinition;
}


