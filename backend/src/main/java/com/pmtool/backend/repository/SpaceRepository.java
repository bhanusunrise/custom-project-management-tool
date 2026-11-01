
package com.pmtool.backend.repository;

import com.pmtool.backend.entity.Space;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpaceRepository extends JpaRepository<Space, Long> {

    // Return only spaces where the member belongs.
    @Query(value = """
        SELECT s.*
        FROM space s
        INNER JOIN space_member sm ON sm.space_id = s.space_id
        WHERE sm.member_id = :memberId
        ORDER BY s.created_at DESC
        """, nativeQuery = true)
    List<Space> findAllByMemberId(@Param("memberId") Long memberId);

    // Retrieve a space only when the member has access.
    @Query(value = """
        SELECT s.*
        FROM space s
        INNER JOIN space_member sm ON sm.space_id = s.space_id
        WHERE s.space_id = :spaceId
          AND sm.member_id = :memberId
        """, nativeQuery = true)
    Optional<Space> findAccessibleSpace(
        @Param("spaceId") Long spaceId,
        @Param("memberId") Long memberId
    );

    // Check whether the member has the SPACE_OWNER role.
    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM space_member
            WHERE space_id = :spaceId
              AND member_id = :memberId
              AND role = 'SPACE_OWNER'
        )
        """, nativeQuery = true)
    boolean isSpaceOwner(
        @Param("spaceId") Long spaceId,
        @Param("memberId") Long memberId
    );
}
