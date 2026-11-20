
package com.pmtool.backend.repository;

import com.pmtool.backend.entity.ProjectStatus;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectStatusRepository
        extends JpaRepository<ProjectStatus, Long> {

    // Retrieve statuses in Kanban column order.
    List<ProjectStatus> findByProjectIdOrderByPositionAsc(Long projectId);

    // Retrieve a status belonging to a specific Project.
    Optional<ProjectStatus> findByIdAndProjectId(
        Long id,
        Long projectId
    );

    // Check whether the Project already has a status with this name.
    boolean existsByProjectIdAndNameIgnoreCase(
        Long projectId,
        String name
    );

    // Check whether tasks are currently using this status.
    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM task
                WHERE project_id = :projectId
                  AND status_id = :statusId
            )
            """,
        nativeQuery = true
    )
    boolean hasTasks(
        @Param("projectId") Long projectId,
        @Param("statusId") Long statusId
    );
}
