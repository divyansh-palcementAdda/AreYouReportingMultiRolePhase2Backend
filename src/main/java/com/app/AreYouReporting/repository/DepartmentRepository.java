package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByName(String name);

    Optional<Department> findByCode(String code);

    boolean existsByName(String name);

    boolean existsByCode(String code);

    List<Department> findByIsActiveTrue();

    @Query("SELECT DISTINCT d FROM Department d LEFT JOIN FETCH d.subDepartments WHERE d.id = :id")
    Optional<Department> findByIdWithSubDepartments(@Param("id") UUID id);
}
