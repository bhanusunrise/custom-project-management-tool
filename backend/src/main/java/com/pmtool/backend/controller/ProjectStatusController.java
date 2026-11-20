
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.project.status.CreateProjectStatusRequest;
import com.pmtool.backend.dto.project.status.ProjectStatusResponse;
import com.pmtool.backend.dto.project.status.ReorderProjectStatusesRequest;
import com.pmtool.backend.dto.project.status.UpdateProjectStatusRequest;
import com.pmtool.backend.service.ProjectStatusService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces/{spaceId}/projects/{projectId}/statuses")
public class ProjectStatusController {

    private final ProjectStatusService projectStatusService;

    public ProjectStatusController(
            ProjectStatusService projectStatusService
    ) {
        this.projectStatusService = projectStatusService;
    }

    // Retrieve all Kanban statuses belonging to the Project.
    @GetMapping
    public ResponseEntity<List<ProjectStatusResponse>> listStatuses(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                projectStatusService.listStatuses(
                        spaceId,
                        projectId,
                        jwt.getSubject()
                )
        );
    }

    // Create a new status for the Project.
    @PostMapping
    public ResponseEntity<ProjectStatusResponse> createStatus(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @Valid @RequestBody CreateProjectStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ProjectStatusResponse response = projectStatusService.createStatus(
                spaceId,
                projectId,
                request,
                jwt.getSubject()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Rename an existing Project status.
    @PutMapping("/{statusId}")
    public ResponseEntity<ProjectStatusResponse> updateStatus(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @PathVariable Long statusId,
            @Valid @RequestBody UpdateProjectStatusRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                projectStatusService.updateStatus(
                        spaceId,
                        projectId,
                        statusId,
                        request,
                        jwt.getSubject()
                )
        );
    }

    // Reorder all Kanban statuses belonging to the Project.
    @PutMapping("/reorder")
    public ResponseEntity<List<ProjectStatusResponse>> reorderStatuses(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @Valid @RequestBody ReorderProjectStatusesRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                projectStatusService.reorderStatuses(
                        spaceId,
                        projectId,
                        request,
                        jwt.getSubject()
                )
        );
    }

    // Delete a status only when no tasks are using it.
    @DeleteMapping("/{statusId}")
    public ResponseEntity<Void> deleteStatus(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @PathVariable Long statusId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        projectStatusService.deleteStatus(
                spaceId,
                projectId,
                statusId,
                jwt.getSubject()
        );

        return ResponseEntity.noContent().build();
    }
}
