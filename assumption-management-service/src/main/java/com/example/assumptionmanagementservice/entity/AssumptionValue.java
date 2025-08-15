package com.example.assumptionmanagementservice.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "assumption_value")
@Data
public class AssumptionValue {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assumption_set_id")
    @JsonIgnore
    private AssumptionSet assumptionSet;


    private String key;
    private String value;
}
