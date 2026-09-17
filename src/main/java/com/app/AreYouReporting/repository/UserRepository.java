package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Page<User> findByIsActiveTrue(Pageable pageable);

    Optional<User> findByIdAndIsActiveTrue(UUID id);

    @Query("SELECT DISTINCT u FROM User u JOIN u.departments d WHERE d.id = :departmentId AND u.isActive = true AND d.isActive = true")
    List<User> findByDepartmentId(@Param("departmentId") UUID departmentId);

    @Query("SELECT DISTINCT u FROM User u JOIN u.subDepartments sd JOIN sd.department d WHERE sd.id = :subDepartmentId AND u.isActive = true AND sd.isActive = true AND d.isActive = true")
    List<User> findBySubDepartmentId(@Param("subDepartmentId") UUID subDepartmentId);
}
