package com.pmtool.backend.dto.space.member;

import java.time.OffsetDateTime;

public record SpaceMemberResponse(
    Long memberId,
    String name,
    String email,
    String role,
    OffsetDateTime joinedAt
)
{}