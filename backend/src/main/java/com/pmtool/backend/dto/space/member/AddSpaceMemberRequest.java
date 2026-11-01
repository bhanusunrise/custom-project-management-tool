package com.pmtool.backend.dto.space.member;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AddSpaceMemberRequest(
    @NotBlank @Email String email
) {}