
package com.pmtool.backend.dto.project.status;

import com.pmtool.backend.entity.ProjectStatus;
import java.time.OffsetDateTime;

public record ProjectStatusResponse(
    Long id,
    Long projectId,
    String name,
    Integer position,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static ProjectStatusResponse from(ProjectStatus status) {
        return new ProjectStatusResponse(
            status.getId(),
            status.getProjectId(),
            status.getName(),
            status.getPosition(),
            status.getCreatedAt(),
            status.getUpdatedAt()
        );
    }
}
