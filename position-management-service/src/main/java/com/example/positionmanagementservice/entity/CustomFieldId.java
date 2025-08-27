package com.example.positionmanagementservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomFieldId implements Serializable {
    
    @Column(name = "position_file_id", nullable = false)
    private UUID positionFileId;
    
    @Column(name = "loan_number", nullable = false, length = 128)
    private String loanNumber;
}