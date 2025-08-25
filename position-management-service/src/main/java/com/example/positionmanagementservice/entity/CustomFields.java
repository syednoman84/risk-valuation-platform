package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(
        name = "custom_fields",
        uniqueConstraints = {
                // One row of aggregated custom fields per (file, loanNumber)
                @UniqueConstraint(name = "uq_cf_file_loannumber", columnNames = {"position_file_id", "loan_number"})
        },
        indexes = {
                @Index(name = "idx_cf_file_loannumber", columnList = "position_file_id, loan_number")
        }
)
@Data
public class CustomFields {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "loan_number", nullable = false, length = 128)
    private String loanNumber;

    // FK to position_file
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_file_id", nullable = false)
    private PositionFile positionFile;

    // Composite ref to Loan (see note in PaymentSchedule)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(value = {
            @JoinColumn(name = "position_file_id", referencedColumnName = "position_file_id", insertable = false, updatable = false),
            @JoinColumn(name = "loan_number", referencedColumnName = "loan_number", insertable = false, updatable = false)
    })
    private Loan loanRef;

    // All custom fields for this loan in this file, aggregated into one JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> fields;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
