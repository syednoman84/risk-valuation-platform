package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PositionFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findAllByPositionFile(PositionFile positionFile);

}
