package com.example.positionmanagementservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "custom_fields")
@Data
public class CustomFields {

    @EmbeddedId
    private CustomFieldId id;

    // Helper methods
    public UUID getPositionFileId() {
        return id != null ? id.getPositionFileId() : null;
    }

    public String getLoanNumber() {
        return id != null ? id.getLoanNumber() : null;
    }

    // All custom fields for this loan in this file, aggregated into one JSONB
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fields", columnDefinition = "jsonb", nullable = false)
    private Map<String, Object> fields;

    @Column(name = "source_file_name")
    private String sourceFileName;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
