
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.task.CreateTaskRequest;
import com.pmtool.backend.dto.task.TaskResponse;
import com.pmtool.backend.dto.task.UpdateTaskRequest;
import com.pmtool.backend.service.TaskService;

import jakarta.validation.Valid;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spaces/{spaceId}/projects/{projectId}/tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    // Create a new task.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse create(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @Valid @RequestBody CreateTaskRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.create(
            spaceId,
            projectId,
            request,
            jwt.getSubject()
        );
    }

    // Retrieve all tasks belonging to the Project.
    @GetMapping
    public List<TaskResponse> list(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.list(spaceId, projectId, jwt.getSubject());
    }

    // Retrieve one task.
    @GetMapping("/{taskId}")
    public TaskResponse get(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.get(
            spaceId,
            projectId,
            taskId,
            jwt.getSubject()
        );
    }

    // Update a task.
    @PutMapping("/{taskId}")
    public TaskResponse update(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @Valid @RequestBody UpdateTaskRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return service.update(
            spaceId,
            projectId,
            taskId,
            request,
            jwt.getSubject()
        );
    }

    // Delete a task.
    @DeleteMapping("/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @PathVariable Long spaceId,
        @PathVariable Long projectId,
        @PathVariable Long taskId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        service.delete(
            spaceId,
            projectId,
            taskId,
            jwt.getSubject()
        );
    }
}

