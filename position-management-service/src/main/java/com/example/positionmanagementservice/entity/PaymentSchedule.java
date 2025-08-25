package com.example.positionmanagementservice.entity;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PositionFile;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "payment_schedule",
        indexes = {
                @Index(name = "idx_ps_file_loannumber", columnList = "position_file_id, loan_number"),
                @Index(name = "idx_ps_loannumber", columnList = "loan_number")
        }
)
@Data
public class PaymentSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    // Store loanNumber explicitly (part of composite FK to Loan)
    @Column(name = "loan_number", nullable = false, length = 128)
    private String loanNumber;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "monthly_payment")
    private BigDecimal monthlyPayment;

    @Column(name = "interest_payment")
    private BigDecimal interestPayment;

    @Column(name = "principal_payment")
    private BigDecimal principalPayment;

    @Column(name = "payment_type", length = 64)
    private String paymentType;

    // FK to position_file
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "position_file_id", nullable = false)
    private PositionFile positionFile;

    /**
     * Composite reference back to Loan via (position_file_id, loan_number).
     * This enforces the "LoanNumber and Position_file_id should match" rule.
     * NOTE: The referenced columns must be UNIQUE on Loan (we added uq_loan_file_loannumber).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns(value = {
            @JoinColumn(name = "position_file_id", referencedColumnName = "position_file_id", insertable = false, updatable = false),
            @JoinColumn(name = "loan_number", referencedColumnName = "loan_number", insertable = false, updatable = false)
    })
    private Loan loanRef;
}
