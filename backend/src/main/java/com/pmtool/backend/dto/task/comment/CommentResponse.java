
package com.pmtool.backend.dto.task.comment;

import com.pmtool.backend.entity.Comment;
import java.time.OffsetDateTime;

public record CommentResponse(
    Long id,
    Long taskId,
    Long memberId,
    String comment,
    OffsetDateTime createdAt
) {

    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
            comment.getId(),
            comment.getTaskId(),
            comment.getMemberId(),
            comment.getComment(),
            comment.getCreatedAt()
        );
    }
}
