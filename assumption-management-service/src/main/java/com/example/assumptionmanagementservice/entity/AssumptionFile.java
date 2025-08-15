package com.example.assumptionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assumption_file")
@Data
public class AssumptionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assumption_set_id")
    private AssumptionSet assumptionSet;

    private String key;

    private String originalFileName;

    private String filePath;

    private LocalDateTime uploadedAt;
}

