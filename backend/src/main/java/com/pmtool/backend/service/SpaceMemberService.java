package com.pmtool.backend.service;

import com.pmtool.backend.dto.space.member.AddSpaceMemberRequest;
import com.pmtool.backend.dto.space.member.SpaceMemberResponse;
import com.pmtool.backend.dto.space.member.UpdateSpaceMemberRoleRequest;
import com.pmtool.backend.entity.Member;
import com.pmtool.backend.repository.MemberRepository;
import com.pmtool.backend.repository.SpaceRepository;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SpaceMemberService {

    private final MemberRepository members;
    private final SpaceRepository spaces;
    private final JdbcTemplate jdbc;

    public SpaceMemberService(
        MemberRepository members,
        SpaceRepository spaces,
        JdbcTemplate jdbc
    ) {
        this.members = members;
        this.spaces = spaces;
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

    // Ensure the authenticated member belongs to the Space.
    private void requireMembership(Long spaceId, Long memberId) {
        if (spaces.findAccessibleSpace(spaceId, memberId).isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space not found"
            );
        }
    }

    // Only Space owners can manage members.
    private void requireOwner(Long spaceId, Long memberId) {
        requireMembership(spaceId, memberId);

        if (!spaces.isSpaceOwner(spaceId, memberId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only Space owners can manage members"
            );
        }
    }

    // Retrieve all members of a Space.
    @Transactional(readOnly = true)
    public List<SpaceMemberResponse> list(Long spaceId, String cognitoSub) {
        Long memberId = getMemberId(cognitoSub);

        requireMembership(spaceId, memberId);

        return jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, sm.role, sm.joined_at
            FROM space_member sm
            JOIN member m ON sm.member_id = m.member_id
            WHERE sm.space_id = ?
            ORDER BY sm.joined_at ASC
            """,
            (rs, rowNum) -> new SpaceMemberResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getObject("joined_at", OffsetDateTime.class)
            ),
            spaceId
        );
    }

    // Add an existing registered member to the Space.
    @Transactional
    public SpaceMemberResponse add(
        Long spaceId,
        AddSpaceMemberRequest request,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);

        requireOwner(spaceId, requesterId);

        Member target = members.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Registered member not found"
            ));

        Long targetId = target.getMemberId();

        Integer existing = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM space_member
            WHERE space_id = ? AND member_id = ?
            """,
            Integer.class,
            spaceId,
            targetId
        );

        if (existing != null && existing > 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Member already belongs to this Space"
            );
        }

        try {
            jdbc.update(
                """
                INSERT INTO space_member (space_id, member_id, role)
                VALUES (?, ?, 'SPACE_MEMBER')
                """,
                spaceId,
                targetId
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Unable to add member. Membership may already exist",
                exception
            );
        }

        return findSpaceMember(spaceId, targetId);
    }

    // Update a member's role.
    @Transactional
    public SpaceMemberResponse updateRole(
        Long spaceId,
        Long targetId,
        UpdateSpaceMemberRoleRequest request,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);

        requireOwner(spaceId, requesterId);

        SpaceMemberResponse target = findSpaceMember(spaceId, targetId);

        // Prevent removing the last owner through role changes.
        if (
            target.role().equals("SPACE_OWNER")
            && request.role().equals("SPACE_MEMBER")
        ) {
            requireAnotherOwner(spaceId, targetId);
        }

        jdbc.update(
            """
            UPDATE space_member
            SET role = ?
            WHERE space_id = ? AND member_id = ?
            """,
            request.role(),
            spaceId,
            targetId
        );

        return findSpaceMember(spaceId, targetId);
    }

    // Remove a member from the Space.
    @Transactional
    public void remove(
        Long spaceId,
        Long targetId,
        String cognitoSub
    ) {
        Long requesterId = getMemberId(cognitoSub);

        requireOwner(spaceId, requesterId);

        SpaceMemberResponse target = findSpaceMember(spaceId, targetId);

        // A Space must retain at least one owner.
        if (target.role().equals("SPACE_OWNER")) {
            requireAnotherOwner(spaceId, targetId);
        }

        jdbc.update(
            """
            DELETE FROM space_member
            WHERE space_id = ? AND member_id = ?
            """,
            spaceId,
            targetId
        );
    }

    // Retrieve a single member's Space membership.
    private SpaceMemberResponse findSpaceMember(
        Long spaceId,
        Long memberId
    ) {
        List<SpaceMemberResponse> results = jdbc.query(
            """
            SELECT m.member_id, m.name, m.email, sm.role, sm.joined_at
            FROM space_member sm
            JOIN member m ON sm.member_id = m.member_id
            WHERE sm.space_id = ? AND sm.member_id = ?
            """,
            (rs, rowNum) -> new SpaceMemberResponse(
                rs.getLong("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("role"),
                rs.getObject("joined_at", OffsetDateTime.class)
            ),
            spaceId,
            memberId
        );

        if (results.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Space member not found"
            );
        }

        return results.get(0);
    }

    // Prevent deleting or demoting the last Space owner.
    private void requireAnotherOwner(Long spaceId, Long excludedMemberId) {
        Integer count = jdbc.queryForObject(
            """
            SELECT COUNT(*)
            FROM space_member
            WHERE space_id = ?
              AND member_id <> ?
              AND role = 'SPACE_OWNER'
            """,
            Integer.class,
            spaceId,
            excludedMemberId
        );

        if (count == null || count == 0) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "A Space must have at least one owner"
            );
        }
    }
} 
