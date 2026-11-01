package com.pmtool.backend.dto.space.member;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateSpaceMemberRoleRequest(
    @NotNull
    @Pattern(regexp = "SPACE_OWNER|SPACE_MEMBER")
    String role
) {}