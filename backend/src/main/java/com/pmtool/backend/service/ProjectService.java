
package com.pmtool.backend.service;

import com.pmtool.backend.dto.project.CreateProjectRequest;
import com.pmtool.backend.dto.project.ProjectResponse;
import com.pmtool.backend.dto.project.UpdateProjectRequest;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.entity.Project;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.SpaceRepository;

import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectService {

    private final ProjectRepository projects;
    private final SpaceRepository spaces;
    private final MemberRepository members;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public ProjectService(
        ProjectRepository projects,
        SpaceRepository spaces,
        MemberRepository members,
        JdbcTemplate jdbc,
        EntityManager entityManager
    ) {
        this.projects = projects;
        this.spaces = spaces;
        this.members = members;
        this.jdbc = jdbc;
        this.entityManager = entityManager;
    }

    // Resolve the authenticated Cognito user to a database member.
    private Long getMemberId(String cognitoSub) {
        Member member = members.findByCognitoSub(cognitoSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Member not synchronized. Call /api/members/me first"
            ));

        return member.getMemberId();
    }

    // Ensure the user belongs to the requested Space.
    private void requireSpaceMembership(Long spaceId, Long memberId) {
        if (spaces.findAccessibleSpace(spaceId, memberId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found"
            );
        }
    }

    // Retrieve a project within an accessible Space.
    private Project findProject(Long spaceId, Long projectId, Long memberId) {
        requireSpaceMembership(spaceId, memberId);

        
Project project = projects.findByIdAndSpaceId(projectId, spaceId)
    .orElseThrow(() -> new ResponseStatusException(
        HttpStatus.NOT_FOUND,
        "Project not found"
    ));

boolean isSpaceOwner = spaces.isSpaceOwner(spaceId, memberId);
boolean isProjectMember = projects.isProjectMember(projectId, memberId);

if (!isSpaceOwner && !isProjectMember) {
    throw new ResponseStatusException(
        HttpStatus.FORBIDDEN,
        "You are not a member of this Project"
    );
}

return project;

    }

    // Space owners and Project Managers can manage a project.
    private void requireProjectManager(
        Long spaceId,
        Long projectId,
        Long memberId
    ) {
        if (
            !spaces.isSpaceOwner(spaceId, memberId)
            && !projects.isProjectManager(projectId, memberId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only Space owners or Project Managers can modify this project"
            );
        }
    }

    // Ensure the project's date range is valid.
    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (
            startDate != null
            && endDate != null
            && endDate.isBefore(startDate)
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "End date cannot be earlier than start date"
            );
        }
    }

    // Create a project, assign its manager, and initialize Kanban statuses.
    @Transactional
    public ProjectResponse create(
        Long spaceId,
        CreateProjectRequest request,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);
        requireSpaceMembership(spaceId, memberId);

        validateDates(request.startDate(), request.endDate());

        Project project = new Project();
        project.setSpaceId(spaceId);
        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStatus("PLANNED");
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setCreatedBy(memberId);

        Project saved = projects.saveAndFlush(project);

        // Automatically assign the creator as Project Manager.
        jdbc.update(
            """
            INSERT INTO project_member (project_id, member_id, role)
            VALUES (?, ?, 'PROJECT_MANAGER')
            """,
            saved.getId(),
            memberId
        );

        // Initialize the project's default Kanban columns.
        jdbc.update(
            """
            INSERT INTO project_status (project_id, name, position)
            VALUES
                (?, 'To Do', 0),
                (?, 'In Progress', 1),
                (?, 'Done', 2)
            """,
            saved.getId(),
            saved.getId(),
            saved.getId()
        );

        entityManager.refresh(saved);

        return ProjectResponse.from(saved);
    }

    // List all projects within an accessible Space.
    @Transactional(readOnly = true)
    public List<ProjectResponse> list(Long spaceId, String cognitoSub) {
        Long memberId = getMemberId(cognitoSub);
        requireSpaceMembership(spaceId, memberId);

        
List<Project> projectList =
    projects.findBySpaceIdOrderByCreatedAtDesc(spaceId);

if (spaces.isSpaceOwner(spaceId, memberId)) {
    return projectList.stream()
        .map(ProjectResponse::from)
        .toList();
}

return projectList.stream()
    .filter(project ->
        projects.isProjectMember(project.getId(), memberId)
    )
    .map(ProjectResponse::from)
    .toList();

    }

    // Retrieve a project by ID.
    @Transactional(readOnly = true)
    public ProjectResponse get(
        Long spaceId,
        Long projectId,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        return ProjectResponse.from(
            findProject(spaceId, projectId, memberId)
        );
    }

    // Update project details.
    @Transactional
    public ProjectResponse update(
        Long spaceId,
        Long projectId,
        UpdateProjectRequest request,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        Project project = findProject(spaceId, projectId, memberId);
        requireProjectManager(spaceId, projectId, memberId);

        validateDates(request.startDate(), request.endDate());

        project.setName(request.name().trim());
        project.setDescription(request.description());
        project.setStatus(request.status() == null ? project.getStatus() : request.status());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setUpdatedAt(OffsetDateTime.now());

        return ProjectResponse.from(projects.saveAndFlush(project));
    }

    // Delete a project only when it contains no tasks.
    @Transactional
    public void delete(
        Long spaceId,
        Long projectId,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        Project project = findProject(spaceId, projectId, memberId);
        requireProjectManager(spaceId, projectId, memberId);

        if (projects.hasTasks(projectId)) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delete or move tasks before deleting this project"
            );
        }

        // Dependent memberships and statuses are removed by database cascades.
        projects.delete(project);
    }
}
