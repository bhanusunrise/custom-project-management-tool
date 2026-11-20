
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.project.member.AddProjectMemberRequest;
import com.pmtool.backend.dto.project.member.ProjectMemberResponse;
import com.pmtool.backend.dto.project.member.UpdateProjectMemberRoleRequest;
import com.pmtool.backend.service.ProjectMemberService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces/{spaceId}/projects/{projectId}/members")
public class ProjectMemberController {

    private final ProjectMemberService service;

    public ProjectMemberController(ProjectMemberService service) {
        this.service = service;
    }

    // List Project members.
    @GetMapping
    public List<ProjectMemberResponse> list(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(spaceId, projectId, jwt.getSubject());
    }

    // Add a member to the Project.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectMemberResponse add(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @Valid @RequestBody AddProjectMemberRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.add(spaceId, projectId, request, jwt.getSubject());
    }

    // Update a Project member's role.
    @PutMapping("/{memberId}/role")
    public ProjectMemberResponse updateRole(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @Valid @RequestBody UpdateProjectMemberRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.updateRole(
            spaceId,
            projectId,
            memberId,
            request,
            jwt.getSubject()
        );
    }

    // Remove a Project member.
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.remove(spaceId, projectId, memberId, jwt.getSubject());
    }
}

