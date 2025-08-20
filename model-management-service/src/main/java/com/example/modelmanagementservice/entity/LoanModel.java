package com.example.modelmanagementservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loan_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanModel {

    @Id
    @GeneratedValue
    private UUID id;

    private String name;
    private int version;
    private boolean active;
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private String modelDefinition; // or Object if using Jackson serialization
}

