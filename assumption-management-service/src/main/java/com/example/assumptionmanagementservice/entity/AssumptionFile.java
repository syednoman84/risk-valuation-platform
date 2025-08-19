package com.example.assumptionmanagementservice.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "assumption_file")
@Data
@EqualsAndHashCode(exclude = "assumptionSet")
public class AssumptionFile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assumption_set_id")
    @JsonIgnore
    @JsonBackReference
    private AssumptionSet assumptionSet;

    private String key;

    private String originalFileName;

    private String filePath;

    private LocalDateTime uploadedAt;
}

