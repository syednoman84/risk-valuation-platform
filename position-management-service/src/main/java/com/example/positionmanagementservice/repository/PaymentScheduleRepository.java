package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PaymentSchedule;
import com.example.positionmanagementservice.entity.PositionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {

    // Or: delete all rows for a file (helpful on re-upload replace)
    @Modifying
    @Transactional
    void deleteByPositionFile(PositionFile positionFile);

    List<PaymentSchedule> findByPositionFile_IdAndLoanNumber(UUID positionFileId, String loanNumber);

    long countByPositionFile_Id(UUID positionFileId);

    @Modifying
    @Transactional
    void deleteByLoanRef(Loan loan);
}
