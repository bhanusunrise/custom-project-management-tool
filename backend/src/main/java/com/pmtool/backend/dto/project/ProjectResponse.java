
package com.pmtool.backend.dto.project;

import com.pmtool.backend.entity.Project;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record ProjectResponse(
    Long id,
    Long spaceId,
    String name,
    String description,
    String status,
    LocalDate startDate,
    LocalDate endDate,
    Long createdByMemberId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ProjectResponse from(Project project) {
        return new ProjectResponse(
            project.getId(),
            project.getSpaceId(),
            project.getName(),
            project.getDescription(),
            project.getStatus(),
            project.getStartDate(),
            project.getEndDate(),
            project.getCreatedBy(),
            project.getCreatedAt(),
            project.getUpdatedAt()
        );
    }
}

