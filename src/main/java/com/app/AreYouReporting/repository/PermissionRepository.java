package com.app.AreYouReporting.repository;

import com.app.AreYouReporting.Entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, UUID> {

    Optional<Permission> findByAuthority(String authority);

    boolean existsByAuthority(String authority);

    List<Permission> findByResource(String resource);

    List<Permission> findAllByOrderByResourceAscActionAsc();
}
