package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.TaskProofRequirement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TaskProofRequirementRepository extends JpaRepository<TaskProofRequirement, UUID> {

    List<TaskProofRequirement> findByTemplateIdOrderByDisplayOrderAsc(UUID templateId);

    List<TaskProofRequirement> findByTemplateIdAndIsRequiredTrue(UUID templateId);
}
