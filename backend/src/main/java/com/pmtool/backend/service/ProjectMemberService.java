
package com.pmtool.backend.service;

import com.pmtool.backend.dto.project.member.AddProjectMemberRequest;
import com.pmtool.backend.dto.project.member.ProjectMemberResponse;
import com.pmtool.backend.dto.project.member.UpdateProjectMemberRoleRequest;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.SpaceRepository;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProjectMemberService {

    private final MemberRepository members;
    private final SpaceRepository spaces;
    private final ProjectRepository projects;
    private final JdbcTemplate jdbc;

    public ProjectMemberService(
        MemberRepository members,
        SpaceRepository spaces,
        ProjectRepository projects,
        JdbcTemplate jdbc
    ) {
        this.members = members;
        this.spaces = spaces;
        this.projects = projects;
        this.jdbc = jdbc;
    }

    // Resolve the authenticated Cognito user.
    private Long getMemberId(String cognitoSub) {
        return members.findByCognitoSub(cognitoSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Member not synchronized. Call /api/members/me first"
            ))
            .getMemberId();
    }

    // Verify access to the parent Space and Project.
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

    
private void requireProjectMembership(
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


    // Space owners and Project Managers can manage memberships.
    private void requireManager(
        Long spaceId,
        Long projectId,
        Long memberId
    ) {
        requireProjectAccess(spaceId, projectId, memberId);

        if (
            !spaces.isSpaceOwner(spaceId, memberId)
            && !projects.isProjectManager(projectId, memberId)
        ) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only Space owners or Project Managers can manage project members"
            );
        }
    }

    // Retrieve all members assigned to the Project.
    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> list(
        Long spaceId,
        Long projectId,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireProjectMembership(spaceId, projectId, requesterId);

        return jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, pm.role, pm.joined_at
            FROM project_member pm
            JOIN member m ON pm.member_id = m.member_id
            WHERE pm.project_id = ?
            ORDER BY pm.joined_at ASC
            """,
            (rs, rowNum) -> new ProjectMemberResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getObject("joined_at", OffsetDateTime.class)
            ),
            projectId
        );
    }

    // Add an existing Space member to the Project.
    @Transactional
    public ProjectMemberResponse add(
        Long spaceId,
        Long projectId,
        AddProjectMemberRequest request,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireManager(spaceId, projectId, requesterId);

        Member target = members.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Registered member not found"
            ));

        Long targetId = target.getMemberId();

        // A Project member must already belong to the parent Space.
        if (spaces.findAccessibleSpace(spaceId, targetId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Member must belong to the Space before joining this Project"
            );
        }

        Integer existing = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM project_member
            WHERE project_id = ? AND member_id = ?
            """,
            Integer.class,
            projectId,
            targetId
        );

        if (existing != null && existing > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Member already belongs to this Project"
            );
        }

        try {
            jdbc.update(
                """
                INSERT INTO project_member (project_id, member_id, role)
                VALUES (?, ?, 'PROJECT_MEMBER')
                """,
                projectId,
                targetId
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unable to add member. Membership may already exist",
                exception
            );
        }

        return findProjectMember(projectId, targetId);
    }

    // Change a Project member's role.
    @Transactional
    public ProjectMemberResponse updateRole(
        Long spaceId,
        Long projectId,
        Long targetId,
        UpdateProjectMemberRoleRequest request,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);
        requireManager(spaceId, projectId, requesterId);

        ProjectMemberResponse target = findProjectMember(projectId, targetId);

        if (
            target.role().equals("PROJECT_MANAGER")
            && request.role().equals("PROJECT_MEMBER")
        ) {
            requireAnotherManager(projectId, targetId);
        }

        jdbc.update(
            """
            UPDATE project_member
            SET role = ?
            WHERE project_id = ? AND member_id = ?
            """,
            request.role(),
            projectId,
            targetId
        );

        return findProjectMember(projectId, targetId);
    }

//  a member from the Project.
@Transactional
public void remove(
    Long spaceId,
    Long projectId,
    Long targetId,
    String cognitoSub
) {
    Long requesterId = getMemberId(cognitoSub);
    requireManager(spaceId, projectId, requesterId);

    ProjectMemberResponse target = findProjectMember(projectId, targetId);

    if (target.role().equals("PROJECT_MANAGER")) {
        requireAnotherManager(projectId, targetId);
    }

    // Remove the target member's task assignments in this project.
    removeProjectTaskAssignments(projectId, targetId);

    // Remove the target member from the project.
    jdbc.update(
        """
        DELETE FROM project_member
        WHERE project_id = ? AND member_id = ?
        """,
        projectId,
        targetId
    );
}


    // Find a member within the specified Project.
    private ProjectMemberResponse findProjectMember(
        Long projectId,
        Long memberId
    ) {
        List<ProjectMemberResponse> results = jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, pm.role, pm.joined_at
            FROM project_member pm
            JOIN member m ON pm.member_id = m.member_id
            WHERE pm.project_id = ? AND pm.member_id = ?
            """,
            (rs, rowNum) -> new ProjectMemberResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getObject("joined_at", OffsetDateTime.class)
            ),
            projectId,
            memberId
        );

        if (results.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Project member not found"
            );
        }

        return results.get(0);
    }

    // Prevent removing the last Project Manager.
    private void requireAnotherManager(
        Long projectId,
        Long excludedMemberId
    ) {
        Integer count = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM project_member
            WHERE project_id = ?
              AND member_id <> ?
              AND role = 'PROJECT_MANAGER'
            """,
            Integer.class,
            projectId,
            excludedMemberId
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A Project must have at least one Project Manager"
            );
        }
    }

    
/**
 * Remove task assignments belonging to a member within one project.
 * Assignments in other projects are not affected.
 */
    private void removeProjectTaskAssignments(
        Long projectId,
        Long memberId
        ) {
        jdbc.update(
            """
                DELETE FROM task_assignee
                WHERE member_id = ?
                AND task_id IN (
                    SELECT task_id
                    FROM task
                    WHERE project_id = ?
                )
            """,
        memberId,
        projectId
        );
    }
}
