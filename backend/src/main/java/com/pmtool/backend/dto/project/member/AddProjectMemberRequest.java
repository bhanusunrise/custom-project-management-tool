
package com.pmtool.backend.dto.project.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddProjectMemberRequest(
    @NotBlank @Email String email
) {}

