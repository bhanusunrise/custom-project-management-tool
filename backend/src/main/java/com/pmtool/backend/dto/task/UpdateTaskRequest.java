
package com.pmtool.backend.dto.task;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record UpdateTaskRequest(

    @NotBlank
    @Size(max = 200)
    String name,

    String description,

    Long statusId,

    LocalDate startDate,

    LocalDate dueDate,

    @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT")
    String priority

) {}
