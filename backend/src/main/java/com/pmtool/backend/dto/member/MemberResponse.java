package com.pmtool.backend.dto.member;

import com.pmtool.backend.entity.Member;

import java.time.OffsetDateTime;

public record MemberResponse(
        Long memberId,
        String name,
        String email,
        String cognitoSub,
        OffsetDateTime createdAt
) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getMemberId(),
                member.getName(),
                member.getEmail(),
                member.getCognitoSub(),
                member.getCreatedAt()
        );
    }
}