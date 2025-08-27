package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.CustomFields;
import com.example.positionmanagementservice.entity.CustomFieldId;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomFieldsRepository extends JpaRepository<CustomFields, CustomFieldId> {
    @Modifying
    @Transactional
    void deleteById_PositionFileId(UUID positionFileId);

    long countById_PositionFileId(UUID positionFileId);

    Optional<CustomFields> findById_PositionFileIdAndId_LoanNumber(UUID positionFileId, String loanNumber);

    List<CustomFields> findById_PositionFileIdAndId_LoanNumberIn(UUID positionFileId, Collection<String> loanNumbers);
}
