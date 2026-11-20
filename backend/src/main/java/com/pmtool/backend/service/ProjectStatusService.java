
package com.pmtool.backend.service;

import com.pmtool.backend.dto.project.status.CreateProjectStatusRequest;
import com.pmtool.backend.dto.project.status.ProjectStatusResponse;
import com.pmtool.backend.dto.project.status.ReorderProjectStatusesRequest;
import com.pmtool.backend.dto.project.status.UpdateProjectStatusRequest;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.entity.ProjectStatus;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.ProjectRepository;
import com.pmtool.backend.repository.ProjectStatusRepository;
import com.pmtool.backend.repository.SpaceRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ProjectStatusService {

    private final MemberRepository memberRepository;
    private final SpaceRepository spaceRepository;
    private final ProjectRepository projectRepository;
    private final ProjectStatusRepository statusRepository;

    public ProjectStatusService(
            MemberRepository memberRepository,
            SpaceRepository spaceRepository,
            ProjectRepository projectRepository,
            ProjectStatusRepository statusRepository
    ) {
        this.memberRepository = memberRepository;
        this.spaceRepository = spaceRepository;
        this.projectRepository = projectRepository;
        this.statusRepository = statusRepository;
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

    // Verify that the Space and Project exist and the caller belongs to the Space.
    private Long requireProjectAccess(
            Long spaceId,
            Long projectId,
            String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        if (spaceRepository.findAccessibleSpace(spaceId, memberId).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Space not found or inaccessible"
            );
        }

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

if (!isSpaceOwner && !isProjectMember) {
    throw new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "You are not a member of this Project"
    );
}

        return memberId;
    }

    // Only Space Owners and Project Managers can configure Kanban statuses.
    private void requireManagementPermission(
            Long spaceId,
            Long projectId,
            Long memberId
    ) {
        boolean isSpaceOwner = spaceRepository.isSpaceOwner(spaceId, memberId);
        boolean isProjectManager = projectRepository.isProjectManager(
                projectId,
                memberId
        );

        if (!isSpaceOwner && !isProjectManager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only Space Owners and Project Managers can manage statuses"
            );
        }
    }

    // Ensure the status belongs to the requested Project.
    private ProjectStatus requireStatus(Long projectId, Long statusId) {
        return statusRepository.findByIdAndProjectId(statusId, projectId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Project status not found"
                ));
    }

    // Normalize status names before storing or comparing them.
    private String normalizeName(String name) {
        String normalized = name.trim();

        if (normalized.isEmpty() || normalized.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Status name must contain between 1 and 100 characters"
            );
        }

        return normalized;
    }

    // Retrieve the statuses in their configured Kanban column order.
    @Transactional(readOnly = true)
    public List<ProjectStatusResponse> listStatuses(
            Long spaceId,
            Long projectId,
            String cognitoSub
    ) {
        requireProjectAccess(spaceId, projectId, cognitoSub);

        return statusRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(ProjectStatusResponse::from)
                .toList();
    }

    // Create a new status at the end of the Kanban board.
    @Transactional
    public ProjectStatusResponse createStatus(
            Long spaceId,
            Long projectId,
            CreateProjectStatusRequest request,
            String cognitoSub
    ) {
        Long memberId = requireProjectAccess(spaceId, projectId, cognitoSub);
        requireManagementPermission(spaceId, projectId, memberId);

        String name = normalizeName(request.name());

        if (statusRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A status with this name already exists in the Project"
            );
        }

        List<ProjectStatus> existingStatuses =
                statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        int nextPosition = existingStatuses.stream()
                .mapToInt(ProjectStatus::getPosition)
                .max()
                .orElse(-1) + 1;

        ProjectStatus status = new ProjectStatus();
        status.setProjectId(projectId);
        status.setName(name);
        status.setPosition(nextPosition);

        try {
            return ProjectStatusResponse.from(statusRepository.saveAndFlush(status));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to create status. The name may already exist",
                    exception
            );
        }
    }

    // Rename a status without changing its position or assigned tasks.
    @Transactional
    public ProjectStatusResponse updateStatus(
            Long spaceId,
            Long projectId,
            Long statusId,
            UpdateProjectStatusRequest request,
            String cognitoSub
    ) {
        Long memberId = requireProjectAccess(spaceId, projectId, cognitoSub);
        requireManagementPermission(spaceId, projectId, memberId);

        ProjectStatus status = requireStatus(projectId, statusId);
        String name = normalizeName(request.name());

        if (status.getName().equals(name)) {
            return ProjectStatusResponse.from(status);
        }

        if (statusRepository.existsByProjectIdAndNameIgnoreCase(projectId, name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A status with this name already exists in the Project"
            );
        }

        status.setName(name);
        status.setUpdatedAt(OffsetDateTime.now());

        try {
            return ProjectStatusResponse.from(statusRepository.saveAndFlush(status));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Unable to rename status. The name may already exist",
                    exception
            );
        }
    }

    // Reorder all statuses belonging to the Project.
    @Transactional
    public List<ProjectStatusResponse> reorderStatuses(
            Long spaceId,
            Long projectId,
            ReorderProjectStatusesRequest request,
            String cognitoSub
    ) {
        Long memberId = requireProjectAccess(spaceId, projectId, cognitoSub);
        requireManagementPermission(spaceId, projectId, memberId);

        List<ProjectStatus> statuses =
                statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        List<Long> requestedIds = request.statusIds();

        if (requestedIds.size() != statuses.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide every Project status exactly once"
            );
        }

        Set<Long> uniqueIds = new HashSet<>(requestedIds);

        if (uniqueIds.size() != requestedIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate status IDs are not allowed"
            );
        }

        Set<Long> existingIds = statuses.stream()
                .map(ProjectStatus::getId)
                .collect(java.util.stream.Collectors.toSet());

        if (!existingIds.equals(uniqueIds)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "All status IDs must belong to the requested Project"
            );
        }

        java.util.Map<Long, ProjectStatus> statusById = statuses.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ProjectStatus::getId,
                        status -> status
                ));

        OffsetDateTime updatedAt = OffsetDateTime.now();

        for (int position = 0; position < requestedIds.size(); position++) {
            ProjectStatus status = statusById.get(requestedIds.get(position));

            status.setPosition(position);
            status.setUpdatedAt(updatedAt);
        }

        statusRepository.saveAllAndFlush(statuses);

        return statusRepository.findByProjectIdOrderByPositionAsc(projectId)
                .stream()
                .map(ProjectStatusResponse::from)
                .toList();
    }

    // Delete a status only when no tasks currently use it.
    @Transactional
    public void deleteStatus(
            Long spaceId,
            Long projectId,
            Long statusId,
            String cognitoSub
    ) {
        Long memberId = requireProjectAccess(spaceId, projectId, cognitoSub);
        requireManagementPermission(spaceId, projectId, memberId);

        ProjectStatus status = requireStatus(projectId, statusId);

        List<ProjectStatus> statuses =
                statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        if (statuses.size() <= 1) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The last Project status cannot be deleted"
            );
        }

        if (statusRepository.hasTasks(projectId, statusId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Move all tasks to another status before deleting this status"
            );
        }

        statusRepository.delete(status);
        statusRepository.flush();

        // Close gaps in Kanban column positions after deletion.
        List<ProjectStatus> remainingStatuses =
                statusRepository.findByProjectIdOrderByPositionAsc(projectId);

        OffsetDateTime updatedAt = OffsetDateTime.now();

        for (int position = 0; position < remainingStatuses.size(); position++) {
            ProjectStatus remaining = remainingStatuses.get(position);
            remaining.setPosition(position);
            remaining.setUpdatedAt(updatedAt);
        }

        statusRepository.saveAllAndFlush(remainingStatuses);
    }
}
