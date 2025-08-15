package com.example.assumptionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "assumption_set")
@Data
public class AssumptionSet {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name;

    private String description;

    private boolean locked = false;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "assumptionSet", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AssumptionValue> keyValues = new ArrayList<>();

}

