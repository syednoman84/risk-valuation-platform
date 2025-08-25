package com.example.modelmanagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "loan_models")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoanModel {

    @Id @GeneratedValue
    private UUID id;

    private String name;
    private int version;
    private boolean active;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false) // Use "json" or omit columnDefinition if MySQL
    private JsonNode modelDefinition;
}


