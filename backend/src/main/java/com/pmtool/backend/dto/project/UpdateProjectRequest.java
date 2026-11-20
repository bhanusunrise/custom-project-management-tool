
package com.pmtool.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateProjectRequest(
    @NotBlank @Size(max = 150) String name,
    String description,
    @Pattern(regexp = "PLANNED|ACTIVE|ON_HOLD|COMPLETED|CANCELLED")
    String status,
    LocalDate startDate,
    LocalDate endDate
) {}

