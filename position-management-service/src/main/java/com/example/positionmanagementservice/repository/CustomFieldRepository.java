package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.CustomField;
import com.example.positionmanagementservice.entity.Loan;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CustomFieldRepository extends JpaRepository<CustomField, UUID> {

    @Modifying
    @Transactional
    @Query("DELETE FROM CustomField cf WHERE cf.loan = :loan")
    void deleteAllByLoan(@Param("loan") Loan loan);
}