package com.pmtool.backend.controller;

import com.pmtool.backend.dto.member.MemberResponse;
import com.pmtool.backend.service.MemberService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public MemberResponse getCurrentMember(
            @AuthenticationPrincipal Jwt jwt
    ) {

        // Identify the member using the validated Cognito JWT.
        return memberService.getOrCreateMember(
                jwt.getSubject(),
                jwt.getTokenValue()
        );
    }
} 
