package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.TaskTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaskTemplateRepository extends JpaRepository<TaskTemplate, UUID>, JpaSpecificationExecutor<TaskTemplate> {

    Optional<TaskTemplate> findByName(String name);

    boolean existsByName(String name);

    List<TaskTemplate> findByIsActiveTrue();

    Optional<TaskTemplate> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT DISTINCT tt FROM TaskTemplate tt " +
           "LEFT JOIN FETCH tt.proofRequirements pr " +
           "LEFT JOIN FETCH tt.applicableDepartments " +
           "LEFT JOIN FETCH tt.applicableSubDepartments " +
           "WHERE tt.id = :id AND tt.isActive = true")
    Optional<TaskTemplate> findActiveByIdWithDetails(@Param("id") UUID id);

    @Query("SELECT DISTINCT tt FROM TaskTemplate tt " +
           "LEFT JOIN FETCH tt.proofRequirements pr " +
           "LEFT JOIN FETCH tt.applicableDepartments " +
           "LEFT JOIN FETCH tt.applicableSubDepartments " +
           "WHERE tt.id = :id")
    Optional<TaskTemplate> findByIdWithDetails(@Param("id") UUID id);
}
