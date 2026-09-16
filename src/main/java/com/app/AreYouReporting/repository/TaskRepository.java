package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.Task;
import com.app.AreYouReporting.Entities.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRepository extends JpaRepository<Task, UUID>, JpaSpecificationExecutor<Task> {

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.creator " +
           "LEFT JOIN FETCH t.assignedDepartments " +
           "LEFT JOIN FETCH t.assignedSubDepartments " +
           "LEFT JOIN FETCH t.assignees " +
           "LEFT JOIN FETCH t.template " +
           "WHERE t.id = :id AND t.status <> 'DELETED'")
    Optional<Task> findActiveByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT t FROM Task t " +
           "LEFT JOIN FETCH t.creator " +
           "LEFT JOIN FETCH t.assignedDepartments " +
           "LEFT JOIN FETCH t.assignedSubDepartments " +
           "LEFT JOIN FETCH t.assignees " +
           "LEFT JOIN FETCH t.template " +
           "WHERE t.id = :id")
    Optional<Task> findByIdWithDetails(@Param("id") UUID id);

    List<Task> findByStatusIn(List<TaskStatus> statuses);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status IN ('PENDING', 'IN_PROGRESS', 'UPCOMING', 'EXTENDED')")
    List<Task> findOverdueTasks(@Param("now") Instant now);
}
