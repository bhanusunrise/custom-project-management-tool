
package com.pmtool.backend.dto.project.member;

import java.time.OffsetDateTime;

public record ProjectMemberResponse(
    Long memberId,
    String name,
    String email,
    String role,
    OffsetDateTime joinedAt
) {}
