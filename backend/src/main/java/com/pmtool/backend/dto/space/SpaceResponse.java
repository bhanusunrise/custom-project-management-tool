package com.pmtool.backend.dto.space;

import com.pmtool.backend.entity.Space;
import java.time.OffsetDateTime;

public record SpaceResponse(
    Long id,
    String name,
    String description,
    Long createdByMemberId,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static SpaceResponse from(Space space) {
        return new SpaceResponse(space.getId(), space.getName(),
            space.getDescription(), space.getCreatedBy(),
            space.getCreatedAt(), space.getUpdatedAt());
    }
}
