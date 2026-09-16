package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.UserRoleAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRoleAssignmentRepository extends JpaRepository<UserRoleAssignment, UUID> {

    List<UserRoleAssignment> findByUserId(UUID userId);

    List<UserRoleAssignment> findByUserIdAndIsActiveTrue(UUID userId);

    @Query("SELECT DISTINCT ura FROM UserRoleAssignment ura " +
           "LEFT JOIN FETCH ura.role r " +
           "LEFT JOIN FETCH ura.department " +
           "LEFT JOIN FETCH ura.subDepartment " +
           "WHERE ura.user.id = :userId AND ura.isActive = true")
    List<UserRoleAssignment> findAllActiveWithDetailsByUserId(@Param("userId") UUID userId);

    @Query("SELECT ura FROM UserRoleAssignment ura " +
           "WHERE ura.user.id = :userId AND ura.role.id = :roleId " +
           "AND (:deptId IS NULL OR ura.department.id = :deptId) " +
           "AND (:subDeptId IS NULL OR ura.subDepartment.id = :subDeptId) " +
           "AND ura.isActive = true")
    Optional<UserRoleAssignment> findSpecificAssignment(
            @Param("userId") UUID userId,
            @Param("roleId") UUID roleId,
            @Param("deptId") UUID deptId,
            @Param("subDeptId") UUID subDeptId
    );
}
