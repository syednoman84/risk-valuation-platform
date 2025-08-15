package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "position_file")
@Data
public class PositionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    private String name; // PF1, PF2, etc.

    private String zipFileName; // PositionFile_MMDDYYYY.zip

    private LocalDate positionDate;

    private String originalFilePath;

    private LocalDateTime uploadedAt;

    @Column(nullable = false)
    private boolean locked = false;
}

