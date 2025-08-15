package com.example.assumptionmanagementservice.repository;

import com.example.assumptionmanagementservice.entity.AssumptionSet;
import com.example.assumptionmanagementservice.entity.AssumptionValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AssumptionValueRepository extends JpaRepository<AssumptionValue, UUID> {
    void deleteAllByAssumptionSet(AssumptionSet set);
    List<AssumptionValue> findAllByAssumptionSet(AssumptionSet set);
}

