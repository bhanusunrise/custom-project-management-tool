
package com.pmtool.backend.service;

import com.pmtool.backend.dto.task.assignee.AddTaskAssigneeRequest;
import com.pmtool.backend.dto.task.assignee.TaskAssigneeResponse;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.SpaceRepository;
import com.pmtool.backend.repository.TaskRepository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskAssigneeService {

    private final MemberRepository members;
    private final SpaceRepository spaces;
    private final ProjectRepository projects;
    private final TaskRepository tasks;
    private final JdbcTemplate jdbc;

    public TaskAssigneeService(
        MemberRepository members,
        SpaceRepository spaces,
        ProjectRepository projects,
        TaskRepository tasks,
        JdbcTemplate jdbc
    ) {
        this.members = members;
        this.spaces = spaces;
        this.projects = projects;
        this.tasks = tasks;
        this.jdbc = jdbc;
    }

    // Resolve the authenticated member.
    private Long getMemberId(String cognitoSub) {
        return members.findByCognitoSub(cognitoSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Member not synchronized"
            ))
            .getMemberId();
    }

    // Verify that the requester can access the task and its project.
    private void requireTaskAccess(
        Long spaceId,
        Long projectId,
        Long taskId,
        Long requesterId
    ) {
        if (spaces.findAccessibleSpace(spaceId, requesterId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found"
            );
        }

        if (projects.findByIdAndSpaceId(projectId, spaceId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Project not found"
            );
        }

        if (tasks.findByIdAndProjectId(taskId, projectId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
            );
        }

        
boolean isSpaceOwner = spaces.isSpaceOwner(spaceId, requesterId);

boolean isProjectMember = projects.isProjectMember(projectId, requesterId);

if (!isSpaceOwner && !isProjectMember) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "You are not a member of this Project"
    );
}

    }

    // Find an existing assignment and return its member details.
    private TaskAssigneeResponse findAssignee(Long taskId, Long memberId) {
        List<TaskAssigneeResponse> results = jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, ta.assigned_at
            FROM task_assignee ta
            JOIN member m ON m.member_id = ta.member_id
            WHERE ta.task_id = ? AND ta.member_id = ?
            """,
            (rs, rowNum) -> new TaskAssigneeResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getObject("assigned_at", OffsetDateTime.class)
            ),
            taskId,
            memberId
        );

        if (results.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task assignee not found"
            );
        }

        return results.get(0);
    }

    // List all assignees for the selected task.
    @Transactional(readOnly = true)
    public List<TaskAssigneeResponse> list(
        Long spaceId,
        Long projectId,
        Long taskId,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, taskId, requesterId);

        return jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, ta.assigned_at
            FROM task_assignee ta
            JOIN member m ON m.member_id = ta.member_id
            WHERE ta.task_id = ?
            ORDER BY ta.assigned_at ASC, m.member_id ASC
            """,
            (rs, rowNum) -> new TaskAssigneeResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getObject("assigned_at", OffsetDateTime.class)
            ),
            taskId
        );
    }

    // Assign an existing Project member to a task.
    @Transactional
    public TaskAssigneeResponse add(
        Long spaceId,
        Long projectId,
        Long taskId,
        AddTaskAssigneeRequest request,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, taskId, requesterId);

        Long targetId = request.memberId();

        // The target must belong to both the Project and its parent Space.
        Integer eligible = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM project_member pm
            JOIN space_member sm
              ON sm.member_id = pm.member_id
            WHERE pm.project_id = ?
              AND sm.space_id = ?
              AND pm.member_id = ?
            """,
            Integer.class,
            projectId,
            spaceId,
            targetId
        );

        if (eligible == null || eligible == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Assignee must belong to the Project and Space"
            );
        }

        Integer existing = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM task_assignee
            WHERE task_id = ? AND member_id = ?
            """,
            Integer.class,
            taskId,
            targetId
        );

        if (existing != null && existing > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Member is already assigned to this task"
            );
        }

        try {
            jdbc.update(
                """
                INSERT INTO task_assignee (task_id, member_id)
                VALUES (?, ?)
                """,
                taskId,
                targetId
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unable to assign member",
                exception
            );
        }

        return findAssignee(taskId, targetId);
    }

    // Remove an assignee without deleting the task.
    @Transactional
    public void remove(
        Long spaceId,
        Long projectId,
        Long taskId,
        Long targetId,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, taskId, requesterId);

        int deleted = jdbc.update(
            """
            DELETE FROM task_assignee
            WHERE task_id = ? AND member_id = ?
            """,
            taskId,
            targetId
        );

        if (deleted == 0) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task assignee not found"
            );
        }
    }
}
