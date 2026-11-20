
package com.pmtool.backend.service;

import com.pmtool.backend.dto.task.comment.CommentResponse;
import com.pmtool.backend.dto.task.comment.CreateCommentRequest;
import com.pmtool.backend.entity.Comment;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.repository.CommentRepository;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.SpaceRepository;
import com.pmtool.backend.repository.TaskRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final SpaceRepository spaceRepository;
    private final ProjectRepository projectRepository;
    private final TaskRepository taskRepository;

    public CommentService(
            CommentRepository commentRepository,
            MemberRepository memberRepository,
            SpaceRepository spaceRepository,
            ProjectRepository projectRepository,
            TaskRepository taskRepository
    ) {
        this.commentRepository = commentRepository;
        this.memberRepository = memberRepository;
        this.spaceRepository = spaceRepository;
        this.projectRepository = projectRepository;
        this.taskRepository = taskRepository;
    }

    // Identify the authenticated member using their Cognito subject.
    private Long getMemberId(String cognitoSub) {
        return memberRepository.findByCognitoSub(cognitoSub)
                .map(Member::getMemberId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated member not found"
                ));
    }

    
/**
 * Verify that the authenticated member can access the requested task.
 */
private Long requireTaskAccess(
        Long spaceId,
        Long projectId,
        Long taskId,
        String cognitoSub
) {
    Long memberId = getMemberId(cognitoSub);

    // The user must belong to the Space.
    if (spaceRepository.findAccessibleSpace(spaceId, memberId).isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found or inaccessible"
        );
    }

    // The Project must belong to the requested Space.
    if (projectRepository.findByIdAndSpaceId(projectId, spaceId).isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Project not found in this Space"
        );
    }

    boolean isSpaceOwner =
            spaceRepository.isSpaceOwner(spaceId, memberId);

    boolean isProjectMember =
            projectRepository.isProjectMember(projectId, memberId);

    // Space Owners can access all projects in their Space.
    // Other members must belong to the specific Project.
    if (!isSpaceOwner && !isProjectMember) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not a member of this Project"
        );
    }

    // The Task must belong to the requested Project.
    if (taskRepository.findByIdAndProjectId(taskId, projectId).isEmpty()) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found in this Project"
        );
    }

    return memberId;
}


    // Retrieve all comments belonging to a task.
    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(
            Long spaceId,
            Long projectId,
            Long taskId,
            String cognitoSub
    ) {
        requireTaskAccess(spaceId, projectId, taskId, cognitoSub);

        return commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(CommentResponse::from)
                .toList();
    }

    // Create a comment using the authenticated member's identity.
    @Transactional
    public CommentResponse createComment(
            Long spaceId,
            Long projectId,
            Long taskId,
            CreateCommentRequest request,
            String cognitoSub
    ) {
        Long memberId = requireTaskAccess(
                spaceId,
                projectId,
                taskId,
                cognitoSub
        );

        String content = request.comment().trim();

        if (content.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Comment cannot be empty"
            );
        }

        Comment comment = new Comment();
        comment.setTaskId(taskId);
        comment.setMemberId(memberId);
        comment.setComment(content);

        Comment savedComment = commentRepository.saveAndFlush(comment);

        return CommentResponse.from(savedComment);
    }

    // Delete a comment if the caller is its author or an authorized manager.
    @Transactional
    public void deleteComment(
            Long spaceId,
            Long projectId,
            Long taskId,
            Long commentId,
            String cognitoSub
    ) {
        Long memberId = requireTaskAccess(
                spaceId,
                projectId,
                taskId,
                cognitoSub
        );

        Comment comment = commentRepository
                .findByIdAndTaskId(commentId, taskId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Comment not found in this Task"
                ));

        boolean isAuthor = comment.getMemberId().equals(memberId);

        boolean isSpaceOwner =
                spaceRepository.isSpaceOwner(spaceId, memberId);

        boolean isProjectManager =
                projectRepository.isProjectManager(projectId, memberId);

        if (!isAuthor && !isSpaceOwner && !isProjectManager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot delete another member's comment"
            );
        }

        commentRepository.delete(comment);
        commentRepository.flush();
    }
}
