
package com.pmtool.backend.dto.project.status;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProjectStatusRequest(

    @NotBlank
    @Size(max = 100)
    String name

) {}
