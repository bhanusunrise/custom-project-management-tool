
package com.pmtool.backend.dto.project;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateProjectRequest(
    @NotBlank @Size(max = 150) String name,
    String description,
    LocalDate startDate,
    LocalDate endDate
) {}
