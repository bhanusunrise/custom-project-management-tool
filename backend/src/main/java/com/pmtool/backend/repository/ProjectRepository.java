
package com.pmtool.backend.repository;

import com.pmtool.backend.entity.Project;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Retrieve all projects belonging to a Space.
    List<Project> findBySpaceIdOrderByCreatedAtDesc(Long spaceId);

    // Retrieve a project only when it belongs to the specified Space.
    Optional<Project> findByIdAndSpaceId(Long id, Long spaceId);

    // Check whether a project has tasks before deletion.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM task
            WHERE project_id = :projectId
        )
        """, nativeQuery = true)
    boolean hasTasks(@Param("projectId") Long projectId);

    // Check whether the member is a Project Manager.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM project_member
            WHERE project_id = :projectId
              AND member_id = :memberId
              AND role = 'PROJECT_MANAGER'
        )
        """, nativeQuery = true)
    boolean isProjectManager(
        @Param("projectId") Long projectId,
        @Param("memberId") Long memberId
    );


    /**
        * Check whether a member belongs to a specific project.
    */
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM project_member
            WHERE project_id = :projectId
            AND member_id = :memberId
        )
        """, nativeQuery = true)
    boolean isProjectMember(
        @Param("projectId") Long projectId,
        @Param("memberId") Long memberId
    );

}
