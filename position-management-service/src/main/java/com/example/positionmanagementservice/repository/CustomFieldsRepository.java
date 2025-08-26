package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.CustomFields;
import com.example.positionmanagementservice.entity.PositionFile;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomFieldsRepository extends JpaRepository<CustomFields, UUID> {
    @Modifying
    @Transactional
    void deleteByPositionFile(PositionFile positionFile);

    long countByPositionFile_Id(UUID positionFileId);

    Optional<CustomFields> findByPositionFile_IdAndLoanNumber(UUID positionFileId, String loanNumber);

    List<CustomFields> findByPositionFile_IdAndLoanNumberIn(UUID positionFileId, Collection<String> loanNumbers);
}
