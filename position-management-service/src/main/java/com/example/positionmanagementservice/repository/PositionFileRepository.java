package com.example.positionmanagementservice.repository;

import com.example.positionmanagementservice.entity.PositionFile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.UUID;

public interface PositionFileRepository extends JpaRepository<PositionFile, UUID> {
    boolean existsByNameAndPositionDate(String name, LocalDate positionDate);
}
