package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PositionFile;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findAllByPositionFile(PositionFile positionFile);

    long countByPositionFile_Id(UUID positionFileId);

    Page<Loan> findByPositionFile_Id(UUID positionFileId, Pageable pageable);

    @Modifying
    @Transactional
    void deleteByPositionFile(PositionFile positionFile);
}
