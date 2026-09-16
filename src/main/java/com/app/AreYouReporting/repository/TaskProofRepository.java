package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.TaskProof;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskProofRepository extends JpaRepository<TaskProof, UUID> {

    List<TaskProof> findByTaskId(UUID taskId);

    List<TaskProof> findByTaskRequestId(UUID taskRequestId);

    List<TaskProof> findByTaskIdAndProofRequirementId(UUID taskId, UUID proofRequirementId);
}
