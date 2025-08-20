package com.example.modelmanagementservice.repository;

import com.example.modelmanagementservice.entity.LoanModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LoanModelRepository extends JpaRepository<LoanModel, UUID> {

    List<LoanModel> findAllByName(String name);

    Optional<LoanModel> findByNameAndVersion(String name, int version);

    Optional<LoanModel> findFirstByNameAndActiveTrueOrderByVersionDesc(String name);
}

