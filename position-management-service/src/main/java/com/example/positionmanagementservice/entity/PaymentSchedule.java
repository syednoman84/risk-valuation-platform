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
                @Index(name = "idx_ps_loannumber", columnList = "loan_number")
        }
)
@Data
public class PaymentSchedule {

    @EmbeddedId
    private PaymentScheduleId id;

    // Helper methods
    public UUID getPositionFileId() {
        return id != null ? id.getPositionFileId() : null;
    }

    public String getLoanNumber() {
        return id != null ? id.getLoanNumber() : null;
    }

    public LocalDate getStartDate() {
        return id != null ? id.getStartDate() : null;
    }

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


}
