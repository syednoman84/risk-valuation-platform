package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.LoanId;
import com.example.positionmanagementservice.entity.PositionFile;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, LoanId> {

    List<Loan> findAllById_PositionFileId(UUID positionFileId);

    long countById_PositionFileId(UUID positionFileId);

    Page<Loan> findById_PositionFileId(UUID positionFileId, Pageable pageable);

    @Modifying
    @Transactional
    void deleteById_PositionFileId(UUID positionFileId);
}
