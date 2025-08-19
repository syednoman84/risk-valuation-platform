package com.example.assumptionmanagementservice.repository;

import com.example.assumptionmanagementservice.entity.AssumptionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssumptionSetRepository extends JpaRepository<AssumptionSet, UUID> {
    boolean existsByName(String name);

    @Query("SELECT s FROM AssumptionSet s LEFT JOIN FETCH s.keyValues WHERE s.id = :id")
    Optional<AssumptionSet> findWithValuesById(@Param("id") UUID id);

    @Query("SELECT s FROM AssumptionSet s " +
            "LEFT JOIN FETCH s.keyValues " +
            "LEFT JOIN FETCH s.files " +
            "WHERE s.id = :id")
    Optional<AssumptionSet> findWithValuesAndFilesById(@Param("id") UUID id);



}

