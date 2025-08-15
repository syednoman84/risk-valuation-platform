package com.example.assumptionmanagementservice.repository;

import com.example.assumptionmanagementservice.entity.AssumptionFile;
import com.example.assumptionmanagementservice.entity.AssumptionSet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssumptionFileRepository extends JpaRepository<AssumptionFile, UUID> {
    void deleteAllByAssumptionSet(AssumptionSet set);
    List<AssumptionFile> findAllByAssumptionSet(AssumptionSet set);
}

