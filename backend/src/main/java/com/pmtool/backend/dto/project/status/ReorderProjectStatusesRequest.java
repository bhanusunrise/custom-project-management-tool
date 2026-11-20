package com.pmtool.backend.dto.project.status;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ReorderProjectStatusesRequest(

    @NotEmpty
    List<@NotNull @Positive Long> statusIds

) {}
