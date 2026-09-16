package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.TaskRequest;
import com.app.AreYouReporting.Entities.TaskRequestStatus;
import com.app.AreYouReporting.Entities.TaskRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskRequestRepository extends JpaRepository<TaskRequest, UUID> {

    List<TaskRequest> findByTaskId(UUID taskId);

    List<TaskRequest> findByTaskIdAndStatus(UUID taskId, TaskRequestStatus status);

    @Query("SELECT tr FROM TaskRequest tr " +
           "LEFT JOIN FETCH tr.task t " +
           "LEFT JOIN FETCH t.creator " +
           "LEFT JOIN FETCH tr.reviewer " +
           "WHERE tr.id = :id")
    Optional<TaskRequest> findByIdWithDetails(@Param("id") UUID id);

    boolean existsByTaskIdAndRequestTypeAndStatus(UUID taskId, TaskRequestType requestType, TaskRequestStatus status);
}
