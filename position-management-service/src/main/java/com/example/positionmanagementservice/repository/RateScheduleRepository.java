package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.Loan;
import com.example.positionmanagementservice.entity.PositionFile;
import com.example.positionmanagementservice.entity.RateSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RateScheduleRepository extends JpaRepository<RateSchedule, UUID> {
    @Modifying
    @Transactional
    void deleteByLoanRef(Loan loan);

    @Modifying
    @Transactional
    void deleteByPositionFile(PositionFile positionFile);

    long countByPositionFile_Id(UUID positionFileId);

    List<RateSchedule> findByPositionFile_IdAndLoanNumberIn(UUID positionFileId, Collection<String> loanNumbers);
}

