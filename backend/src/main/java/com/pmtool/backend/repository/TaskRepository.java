
package com.pmtool.backend.repository;

import com.pmtool.backend.entity.Task;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Retrieve all tasks belonging to a Project.
    List<Task> findByProjectIdOrderByCreatedAtDesc(Long projectId);

    // Retrieve a task only if it belongs to the specified Project.
    Optional<Task> findByIdAndProjectId(Long id, Long projectId);
}

