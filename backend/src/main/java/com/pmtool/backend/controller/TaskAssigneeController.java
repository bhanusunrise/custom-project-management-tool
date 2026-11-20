
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.task.assignee.AddTaskAssigneeRequest;
import com.pmtool.backend.dto.task.assignee.TaskAssigneeResponse;
import com.pmtool.backend.service.TaskAssigneeService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(
    "/api/spaces/{spaceId}/projects/{projectId}/tasks/{taskId}/assignees"
)
public class TaskAssigneeController {

    private final TaskAssigneeService service;

    public TaskAssigneeController(TaskAssigneeService service) {
        this.service = service;
    }

    // List members assigned to a task.
    @GetMapping
    public List<TaskAssigneeResponse> list(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(
            spaceId, projectId, taskId, jwt.getSubject()
        );
    }

    // Assign a Project member to a task.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskAssigneeResponse add(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @Valid @RequestBody AddTaskAssigneeRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.add(
            spaceId, projectId, taskId, request, jwt.getSubject()
        );
    }

    // Remove a member from a task.
    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @PathVariable Long memberId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.remove(
            spaceId, projectId, taskId, memberId, jwt.getSubject()
        );
    }
}
