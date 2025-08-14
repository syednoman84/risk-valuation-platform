package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "custom_field")
@Data
public class CustomField {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_id")
    private Loan loan;

    private String fieldName;
    private String fieldValue;

    // No extraFields needed here since it's already key-value
}
