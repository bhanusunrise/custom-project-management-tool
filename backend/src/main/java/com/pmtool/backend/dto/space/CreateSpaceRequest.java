
package com.pmtool.backend.dto.space;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSpaceRequest(
    @NotBlank @Size(max = 150) String name,
    String description
) {}
