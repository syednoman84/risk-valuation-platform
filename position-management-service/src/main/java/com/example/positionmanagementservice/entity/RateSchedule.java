package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "rate_schedule",
        indexes = {
                @Index(name = "idx_rs_file_loannumber", columnList = "position_file_id, loan_number"),
                @Index(name = "idx_rs_loannumber", columnList = "loan_number")
        }
)
@Data
public class RateSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "loan_number", nullable = false, length = 128)
    private String loanNumber;

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;

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
}
