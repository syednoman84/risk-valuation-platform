package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.CustomFields;
import com.example.positionmanagementservice.entity.PositionFile;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.UUID;

public interface CustomFieldsRepository extends JpaRepository<CustomFields, UUID> {
    @Modifying
    @Transactional
    void deleteByPositionFile(PositionFile positionFile);

    long countByPositionFile_Id(UUID positionFileId);

    java.util.Optional<CustomFields> findByPositionFile_IdAndLoanNumber(UUID positionFileId, String loanNumber);

}
