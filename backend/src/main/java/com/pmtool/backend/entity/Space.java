package com.pmtool.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "space")
public class Space {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "space_id")
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
