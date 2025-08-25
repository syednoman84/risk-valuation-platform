package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "loan",
        uniqueConstraints = {
                // Enforce exactly one row per (position_file_id, loan_number)
                @UniqueConstraint(name = "uq_loan_file_loannumber", columnNames = {"position_file_id", "loan_number"})
        },
        indexes = {
                @Index(name = "idx_loan_file_loannumber", columnList = "position_file_id, loan_number"),
                @Index(name = "idx_loan_loannumber", columnList = "loan_number")
        }
)
@Data
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // FK to position_file
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_file_id", nullable = false)
    private PositionFile positionFile;

    @Column(name = "loan_number", nullable = false, length = 128)
    private String loanNumber;

    @Column(name = "principal")
    private BigDecimal principal;

    @Column(name = "interest_rate")
    private BigDecimal interestRate;

    @Column(name = "term_months")
    private Integer termMonths;

    @Column(name = "amortization_type", length = 64)
    private String amortizationType;

    @Column(name = "origination_date")
    private LocalDate originationDate;
}

