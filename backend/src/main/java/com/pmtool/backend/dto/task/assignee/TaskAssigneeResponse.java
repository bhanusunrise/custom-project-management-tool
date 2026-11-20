
package com.pmtool.backend.dto.task.assignee;

import java.time.OffsetDateTime;

public record TaskAssigneeResponse(
    Long memberId,
    String name,
    String email,
    OffsetDateTime assignedAt
) {}
