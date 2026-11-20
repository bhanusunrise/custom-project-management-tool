
package com.pmtool.backend.service;

import com.pmtool.backend.dto.task.CreateTaskRequest;
import com.pmtool.backend.dto.task.TaskResponse;
import com.pmtool.backend.dto.task.UpdateTaskRequest;
import com.pmtool.backend.entity.Task;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.SpaceRepository;
import com.pmtool.backend.repository.TaskRepository;

import jakarta.persistence.EntityManager;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private final TaskRepository tasks;
    private final ProjectRepository projects;
    private final SpaceRepository spaces;
    private final MemberRepository members;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public TaskService(
        TaskRepository tasks,
        ProjectRepository projects,
        SpaceRepository spaces,
        MemberRepository members,
        JdbcTemplate jdbc,
        EntityManager entityManager
    ) {
        this.tasks = tasks;
        this.projects = projects;
        this.spaces = spaces;
        this.members = members;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    // Resolve the authenticated member from the Cognito subject.
    private Long getMemberId(String cognitoSub) {
        return members.findByCognitoSub(cognitoSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Member not synchronized"
            ))
            .getMemberId();
    }

    // Ensure the Project exists inside an accessible Space.
    private void requireProjectAccess(
        Long spaceId,
        Long projectId,
        Long memberId
    ) {
        if (spaces.findAccessibleSpace(spaceId, memberId).isEmpty()) {
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
    }

    // Check that the member can access tasks in this Project.
    private void requireTaskAccess(
        Long spaceId,
        Long projectId,
        Long memberId
    ) {
        requireProjectAccess(spaceId, projectId, memberId);

        
boolean isSpaceOwner = spaces.isSpaceOwner(spaceId, memberId);

boolean isProjectMember = projects.isProjectMember(projectId, memberId);

if (!isSpaceOwner && !isProjectMember) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "You are not a member of this Project"
    );
}

    }

    // Only Space Owners and Project Managers can delete tasks.
    private void requireTaskManager(
        Long spaceId,
        Long projectId,
        Long memberId
    ) {
        requireProjectAccess(spaceId, projectId, memberId);

        if (
            !spaces.isSpaceOwner(spaceId, memberId) &&
            !projects.isProjectManager(projectId, memberId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only Space Owners or Project Managers can delete tasks"
            );
        }
    }

    // Verify the Kanban status belongs to the same Project.
    private void validateStatus(Long projectId, Long statusId) {
        Integer count = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM project_status
            WHERE project_id = ? AND status_id = ?
            """,
            Integer.class,
            projectId,
            statusId
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Invalid Kanban status for this Project"
            );
        }
    }

    // Prevent the due date from being earlier than the start date.
    private void validateDates(LocalDate startDate, LocalDate dueDate) {
        if (
            startDate != null &&
            dueDate != null &&
            dueDate.isBefore(startDate)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Due date cannot be earlier than start date"
            );
        }
    }

    // Record each Kanban status assignment or change.
    private void recordStatusHistory(
        Long taskId,
        Long statusId,
        Long memberId
    ) {
        jdbc.update(
            """
            INSERT INTO task_status_history
                (task_id, status_id, status_name, updated_by)
            SELECT ?, status_id, name, ?
            FROM project_status
            WHERE status_id = ?
            """,
            taskId,
            memberId,
            statusId
        );
    }

    // Create a task inside a Project.
    @Transactional
    public TaskResponse create(
        Long spaceId,
        Long projectId,
        CreateTaskRequest request,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        requireTaskAccess(spaceId, projectId, memberId);
        validateStatus(projectId, request.statusId());
        validateDates(request.startDate(), request.dueDate());

        Task task = new Task();
        task.setProjectId(projectId);
        task.setStatusId(request.statusId());
        task.setName(request.name().trim());
        task.setDescription(request.description());
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
        task.setPriority(
            request.priority() == null ? "MEDIUM" : request.priority()
        );
        task.setCreatedBy(memberId);

        Task saved = tasks.saveAndFlush(task);

        // Save the initial status in the task history.
        recordStatusHistory(saved.getId(), saved.getStatusId(), memberId);

        entityManager.refresh(saved);

        return TaskResponse.from(saved);
    }

    // List all tasks belonging to a Project.
    @Transactional(readOnly = true)
    public List<TaskResponse> list(
        Long spaceId,
        Long projectId,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, memberId);

        return tasks.findByProjectIdOrderByCreatedAtDesc(projectId)
            .stream()
            .map(TaskResponse::from)
            .toList();
    }

    // Retrieve a specific task.
    @Transactional(readOnly = true)
    public TaskResponse get(
        Long spaceId,
        Long projectId,
        Long taskId,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, memberId);

        Task task = tasks.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
            ));

        return TaskResponse.from(task);
    }

    // Update task information and record status changes.
    @Transactional
    public TaskResponse update(
        Long spaceId,
        Long projectId,
        Long taskId,
        UpdateTaskRequest request,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);
        requireTaskAccess(spaceId, projectId, memberId);

        Task task = tasks.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
            ));

        Long newStatusId = request.statusId() == null
            ? task.getStatusId()
            : request.statusId();

        validateStatus(projectId, newStatusId);
        validateDates(request.startDate(), request.dueDate());

        boolean statusChanged = !Objects.equals(
            task.getStatusId(),
            newStatusId
        );

        task.setName(request.name().trim());
        task.setDescription(request.description());
        task.setStatusId(newStatusId);
        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());

        if (request.priority() != null) {
            task.setPriority(request.priority());
        }

        task.setUpdatedAt(OffsetDateTime.now());

        Task saved = tasks.saveAndFlush(task);

        if (statusChanged) {
            recordStatusHistory(saved.getId(), newStatusId, memberId);
        }

        return TaskResponse.from(saved);
    }

    // Delete a task. Related comments, assignments, and history cascade.
    @Transactional
    public void delete(
        Long spaceId,
        Long projectId,
        Long taskId,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);
        requireTaskManager(spaceId, projectId, memberId);

        Task task = tasks.findByIdAndProjectId(taskId, projectId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Task not found"
            ));

        tasks.delete(task);
    }
}

