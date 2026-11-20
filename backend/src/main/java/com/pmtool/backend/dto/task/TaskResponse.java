
package com.pmtool.backend.dto.task;

import com.pmtool.backend.entity.Task;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record TaskResponse(
    Long id,
    Long projectId,
    Long statusId,
    String name,
    String description,
    LocalDate startDate,
    LocalDate dueDate,
    String priority,
    Long createdByMemberId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(
            task.getId(),
            task.getProjectId(),
            task.getStatusId(),
            task.getName(),
            task.getDescription(),
            task.getStartDate(),
            task.getDueDate(),
            task.getPriority(),
            task.getCreatedBy(),
            task.getCreatedAt(),
            task.getUpdatedAt()
        );
    }
}
