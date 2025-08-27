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
        indexes = {
                @Index(name = "idx_loan_loannumber", columnList = "loan_number")
        }
)
@Data
public class Loan {

    @EmbeddedId
    private LoanId id;

    // Helper method to get position file ID
    public UUID getPositionFileId() {
        return id != null ? id.getPositionFileId() : null;
    }

    // Helper method to get loan number
    public String getLoanNumber() {
        return id != null ? id.getLoanNumber() : null;
    }

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

