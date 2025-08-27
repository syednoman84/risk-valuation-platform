package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.RateSchedule;
import com.example.positionmanagementservice.entity.RateScheduleId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RateScheduleRepository extends JpaRepository<RateSchedule, RateScheduleId> {

    @Modifying
    @Transactional
    void deleteById_PositionFileId(UUID positionFileId);

    long countById_PositionFileId(UUID positionFileId);

    List<RateSchedule> findById_PositionFileIdAndId_LoanNumberIn(UUID positionFileId, Collection<String> loanNumbers);
}

