
package com.pmtool.backend.dto.project.member;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateProjectMemberRoleRequest(
    @NotNull
    @Pattern(regexp = "PROJECT_MANAGER|PROJECT_MEMBER")
    String role
) {}
