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
                @Index(name = "idx_rs_loannumber", columnList = "loan_number")
        }
)
@Data
public class RateSchedule {

    @EmbeddedId
    private RateScheduleId id;

    // Helper methods
    public UUID getPositionFileId() {
        return id != null ? id.getPositionFileId() : null;
    }

    public String getLoanNumber() {
        return id != null ? id.getLoanNumber() : null;
    }

    public LocalDate getEffectiveDate() {
        return id != null ? id.getEffectiveDate() : null;
    }

    @Column(name = "rate", nullable = false)
    private BigDecimal rate;


}
