package com.pmtool.backend.controller;

import com.pmtool.backend.dto.space.member.AddSpaceMemberRequest;
import com.pmtool.backend.dto.space.member.SpaceMemberResponse;
import com.pmtool.backend.dto.space.member.UpdateSpaceMemberRoleRequest;
import com.pmtool.backend.service.SpaceMemberService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces/{spaceId}/members")
public class SpaceMemberController {

    private final SpaceMemberService service;

    public SpaceMemberController(SpaceMemberService service) {
        this.service = service;
    }

    @GetMapping
    public List<SpaceMemberResponse> list(
        @PathVariable Long spaceId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(spaceId, jwt.getSubject());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpaceMemberResponse add(
        @PathVariable Long spaceId,
        @Valid @RequestBody AddSpaceMemberRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.add(spaceId, request, jwt.getSubject());
    }

    @PutMapping("/{memberId}/role")
    public SpaceMemberResponse updateRole(
        @PathVariable Long spaceId,
        @PathVariable Long memberId,
        @Valid @RequestBody UpdateSpaceMemberRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.updateRole(
            spaceId,
            memberId,
            request,
            jwt.getSubject()
        );
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
        @PathVariable Long spaceId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.remove(spaceId, memberId, jwt.getSubject());
    }
}