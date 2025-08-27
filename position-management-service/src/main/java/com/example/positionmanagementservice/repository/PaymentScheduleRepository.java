package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.PaymentSchedule;
import com.example.positionmanagementservice.entity.PaymentScheduleId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, PaymentScheduleId> {

    @Modifying
    @Transactional
    void deleteById_PositionFileId(UUID positionFileId);

    List<PaymentSchedule> findById_PositionFileIdAndId_LoanNumber(UUID positionFileId, String loanNumber);

    long countById_PositionFileId(UUID positionFileId);

    List<PaymentSchedule> findById_PositionFileIdAndId_LoanNumberIn(UUID positionFileId, Collection<String> loanNumbers);
}
