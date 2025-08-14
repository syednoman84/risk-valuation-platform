package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PaymentSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface PaymentScheduleRepository extends JpaRepository<PaymentSchedule, UUID> {
    @Modifying
    @Transactional
    @Query("DELETE FROM PaymentSchedule ps WHERE ps.loan = :loan")
    void deleteAllByLoan(@Param("loan") Loan loan);

}
