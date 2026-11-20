
package com.pmtool.backend.dto.task.comment;

import jakarta.validation.constraints.NotBlank;

public record CreateCommentRequest(

    @NotBlank(message = "Comment cannot be empty")
    String comment

) {}
