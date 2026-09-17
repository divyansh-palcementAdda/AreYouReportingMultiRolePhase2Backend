package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.SubDepartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubDepartmentRepository extends JpaRepository<SubDepartment, UUID>, JpaSpecificationExecutor<SubDepartment> {

    List<SubDepartment> findByDepartmentId(UUID departmentId);

    List<SubDepartment> findByDepartmentIdAndIsActiveTrue(UUID departmentId);

    Optional<SubDepartment> findByDepartmentIdAndName(UUID departmentId, String name);

    Optional<SubDepartment> findByDepartmentIdAndCode(UUID departmentId, String code);

    boolean existsByDepartmentIdAndName(UUID departmentId, String name);

    boolean existsByDepartmentIdAndCode(UUID departmentId, String code);
}
