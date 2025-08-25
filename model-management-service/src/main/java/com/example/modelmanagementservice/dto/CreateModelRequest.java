package com.example.modelmanagementservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;

@Data
public class CreateModelRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @NotNull
    private JsonNode modelDefinition; 
}



