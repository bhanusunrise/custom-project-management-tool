
package com.pmtool.backend.repository;

import com.pmtool.backend.entity.Comment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository
        extends JpaRepository<Comment, Long> {

    // Retrieve comments belonging to a task in chronological order.
    List<Comment> findByTaskIdOrderByCreatedAtAsc(Long taskId);

    // Retrieve a comment only when it belongs to the requested task.
    Optional<Comment> findByIdAndTaskId(
        Long id,
        Long taskId
    );
}
