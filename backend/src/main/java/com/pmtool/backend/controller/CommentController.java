
package com.pmtool.backend.controller;

import com.pmtool.backend.dto.task.comment.CommentResponse;
import com.pmtool.backend.dto.task.comment.CreateCommentRequest;
import com.pmtool.backend.service.CommentService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spaces/{spaceId}/projects/{projectId}/tasks/{taskId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // Retrieve all comments belonging to a specific task.
    @GetMapping
    public ResponseEntity<List<CommentResponse>> listComments(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(
                commentService.listComments(
                        spaceId,
                        projectId,
                        taskId,
                        jwt.getSubject()
                )
        );
    }

    // Create a comment using the authenticated member's identity.
    @PostMapping
    public ResponseEntity<CommentResponse> createComment(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @Valid @RequestBody CreateCommentRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        CommentResponse response = commentService.createComment(
                spaceId,
                projectId,
                taskId,
                request,
                jwt.getSubject()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // Delete a comment if the caller has the required permission.
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long spaceId,
            @PathVariable Long projectId,
            @PathVariable Long taskId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        commentService.deleteComment(
                spaceId,
                projectId,
                taskId,
                commentId,
                jwt.getSubject()
        );

        return ResponseEntity.noContent().build();
    }
}
