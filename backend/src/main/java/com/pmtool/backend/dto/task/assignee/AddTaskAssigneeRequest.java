
package com.pmtool.backend.dto.task.assignee;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddTaskAssigneeRequest(
    @NotNull @Positive Long memberId
) {}
