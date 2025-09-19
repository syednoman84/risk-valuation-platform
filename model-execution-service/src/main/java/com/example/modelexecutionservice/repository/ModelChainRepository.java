package com.example.modelexecutionservice.repository;

import com.example.modelexecutionservice.entity.ModelChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ModelChainRepository extends JpaRepository<ModelChain, UUID> {
    
    @Query("SELECT mc FROM ModelChain mc LEFT JOIN FETCH mc.steps WHERE mc.id = :id")
    Optional<ModelChain> findByIdWithSteps(@Param("id") UUID id);
    
    boolean existsByName(String name);
}