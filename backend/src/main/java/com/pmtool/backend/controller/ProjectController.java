
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.project.CreateProjectRequest;
import com.pmtool.backend.dto.project.ProjectResponse;
import com.pmtool.backend.dto.project.UpdateProjectRequest;
import com.pmtool.backend.service.ProjectService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces/{spaceId}/projects")
public class ProjectController {

    private final ProjectService service;

    public ProjectController(ProjectService service) {
        this.service = service;
    }

    // Create a new project within a Space.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProjectResponse create(
        @PathVariable Long spaceId,
        @Valid @RequestBody CreateProjectRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            spaceId,
            request,
            jwt.getSubject()
        );
    }

    // Retrieve all projects belonging to a Space.
    @GetMapping
    public List<ProjectResponse> list(
        @PathVariable Long spaceId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(spaceId, jwt.getSubject());
    }

    // Retrieve a specific project.
    @GetMapping("/{projectId}")
    public ProjectResponse get(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.get(
            spaceId,
            projectId,
            jwt.getSubject()
        );
    }

    // Update an existing project.
    @PutMapping("/{projectId}")
    public ProjectResponse update(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @Valid @RequestBody UpdateProjectRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            spaceId,
            projectId,
            request,
            jwt.getSubject()
        );
    }

    // Delete a project.
    @DeleteMapping("/{projectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.delete(
            spaceId,
            projectId,
            jwt.getSubject()
        );
    }
}
