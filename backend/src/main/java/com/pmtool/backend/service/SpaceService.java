
package com.pmtool.backend.service;

import com.pmtool.backend.dto.space.CreateSpaceRequest;
import com.pmtool.backend.dto.space.SpaceResponse;
import com.pmtool.backend.dto.space.UpdateSpaceRequest;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.entity.Space;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.SpaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;

@Service
public class SpaceService {

    private final SpaceRepository spaces;
    private final MemberRepository members;
    private final JdbcTemplate jdbc;
    private final EntityManager entityManager;

    public SpaceService(
    SpaceRepository spaces,
    MemberRepository members,
    JdbcTemplate jdbc,
    EntityManager entityManager
) {
    this.spaces = spaces;
    this.members = members;
    this.jdbc = jdbc;
    this.entityManager = entityManager;
}

    // Resolve the authenticated Cognito user to a PostgreSQL member.
    private Long getMemberId(String cognitoSub) {
        Member member = members.findByCognitoSub(cognitoSub)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Member not synchronized. Call /api/members/me first"
            ));

        return member.getMemberId();
    }

    
@Transactional
public SpaceResponse create(CreateSpaceRequest request, String cognitoSub) {
    Long memberId = getMemberId(cognitoSub);

    Space space = new Space();
    space.setName(request.name().trim());
    space.setDescription(request.description());
    space.setCreatedBy(memberId);

    Space saved = spaces.saveAndFlush(space);

    // Automatically assign the creator as SPACE_OWNER.
    jdbc.update(
        """
        INSERT INTO space_member (space_id, member_id, role)
        VALUES (?, ?, 'SPACE_OWNER')
        """,
        saved.getId(),
        memberId
    );

    // Reload the database-generated timestamps from PostgreSQL.
    entityManager.refresh(saved);

    return SpaceResponse.from(saved);
}
    @Transactional(readOnly = true)
    public List<SpaceResponse> list(String cognitoSub) {
        Long memberId = getMemberId(cognitoSub);

        return spaces.findAllByMemberId(memberId)
            .stream()
            .map(SpaceResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse get(Long id, String cognitoSub) {
        Long memberId = getMemberId(cognitoSub);

        Space space = spaces.findAccessibleSpace(id, memberId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found"
            ));

        return SpaceResponse.from(space);
    }

    @Transactional
    public SpaceResponse update(
        Long id,
        UpdateSpaceRequest request,
        String cognitoSub
    ) {
        Long memberId = getMemberId(cognitoSub);

        Space space = findAccessible(id, memberId);
        requireOwner(id, memberId);

        space.setName(request.name().trim());
        space.setDescription(request.description());
        space.setUpdatedAt(OffsetDateTime.now());

        return SpaceResponse.from(spaces.saveAndFlush(space));
    }

    @Transactional
    public void delete(Long id, String cognitoSub) {
        Long memberId = getMemberId(cognitoSub);

        Space space = findAccessible(id, memberId);
        requireOwner(id, memberId);

        // Prevent deleting spaces that still contain projects.
        Integer projects = jdbc.queryForObject(
            "SELECT COUNT(*) FROM project WHERE space_id = ?",
            Integer.class,
            id
        );

        if (projects != null && projects > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Delete or move projects before deleting this space"
            );
        }

        spaces.delete(space);
    }

    private Space findAccessible(Long spaceId, Long memberId) {
        return spaces.findAccessibleSpace(spaceId, memberId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found"
            ));
    }

    private void requireOwner(Long spaceId, Long memberId) {
        if (!spaces.isSpaceOwner(spaceId, memberId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the space owner can perform this action"
            );
        }
    }
}
