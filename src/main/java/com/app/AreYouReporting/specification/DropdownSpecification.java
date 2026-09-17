package com.app.AreYouReporting.specification;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.UserPrincipal;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.*;

public class DropdownSpecification {

    public static Specification<Department> departmentFilter(
            String search,
            Boolean activeOnly,
            Set<UUID> allowedDeptIds,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly == null || Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), searchPattern);
                predicates.add(cb.or(nameMatch, codeMatch));
            }

            if (!isSuperAdmin) {
                if (allowedDeptIds == null || allowedDeptIds.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("id").in(allowedDeptIds));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<SubDepartment> subDepartmentFilter(
            UUID departmentId,
            String search,
            Boolean activeOnly,
            Set<UUID> allowedSubDeptIds,
            Set<UUID> allowedDeptIds,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly == null || Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
                predicates.add(cb.isTrue(root.get("department").get("isActive")));
            }

            if (departmentId != null) {
                predicates.add(cb.equal(root.get("department").get("id"), departmentId));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate codeMatch = cb.like(cb.lower(root.get("code")), searchPattern);
                predicates.add(cb.or(nameMatch, codeMatch));
            }

            if (!isSuperAdmin) {
                List<Predicate> scopePredicates = new ArrayList<>();
                if (allowedSubDeptIds != null && !allowedSubDeptIds.isEmpty()) {
                    scopePredicates.add(root.get("id").in(allowedSubDeptIds));
                }
                if (allowedDeptIds != null && !allowedDeptIds.isEmpty()) {
                    scopePredicates.add(root.get("department").get("id").in(allowedDeptIds));
                }

                if (scopePredicates.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.or(scopePredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<User> userFilter(
            UUID departmentId,
            UUID subDepartmentId,
            String roleName,
            String search,
            Boolean activeOnly,
            Set<UUID> allowedDeptIds,
            Set<UUID> allowedSubDeptIds,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly == null || Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("fullName")), searchPattern);
                Predicate usernameMatch = cb.like(cb.lower(root.get("username")), searchPattern);
                Predicate emailMatch = cb.like(cb.lower(root.get("email")), searchPattern);
                predicates.add(cb.or(nameMatch, usernameMatch, emailMatch));
            }

            if (departmentId != null) {
                Join<User, Department> deptJoin = root.join("departments", JoinType.LEFT);
                predicates.add(cb.equal(deptJoin.get("id"), departmentId));
                predicates.add(cb.isTrue(deptJoin.get("isActive")));
            }

            if (subDepartmentId != null) {
                Join<User, SubDepartment> subDeptJoin = root.join("subDepartments", JoinType.LEFT);
                predicates.add(cb.equal(subDeptJoin.get("id"), subDepartmentId));
                predicates.add(cb.isTrue(subDeptJoin.get("isActive")));
                predicates.add(cb.isTrue(subDeptJoin.get("department").get("isActive")));
            }

            if (StringUtils.hasText(roleName)) {
                Join<User, UserRoleAssignment> uraJoin = root.join("roleAssignments", JoinType.LEFT);
                predicates.add(cb.isTrue(uraJoin.get("isActive")));
                predicates.add(cb.equal(cb.upper(uraJoin.get("role").get("name")), roleName.trim().toUpperCase()));
            }

            if (!isSuperAdmin) {
                List<Predicate> scopePredicates = new ArrayList<>();

                if (allowedSubDeptIds != null && !allowedSubDeptIds.isEmpty()) {
                    Join<User, SubDepartment> subDeptJoin = root.join("subDepartments", JoinType.LEFT);
                    scopePredicates.add(subDeptJoin.get("id").in(allowedSubDeptIds));
                }

                if (allowedDeptIds != null && !allowedDeptIds.isEmpty()) {
                    Join<User, Department> deptJoin = root.join("departments", JoinType.LEFT);
                    scopePredicates.add(deptJoin.get("id").in(allowedDeptIds));
                }

                if (scopePredicates.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(cb.or(scopePredicates.toArray(new Predicate[0])));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<TaskTemplate> templateFilter(
            UUID departmentId,
            UUID subDepartmentId,
            String search,
            Boolean activeOnly,
            Set<UUID> allowedDeptIds,
            Set<UUID> allowedSubDeptIds,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (activeOnly == null || Boolean.TRUE.equals(activeOnly)) {
                predicates.add(cb.isTrue(root.get("isActive")));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            if (departmentId != null) {
                Join<TaskTemplate, Department> deptJoin = root.join("applicableDepartments", JoinType.LEFT);
                predicates.add(cb.or(cb.isNull(deptJoin.get("id")), cb.equal(deptJoin.get("id"), departmentId)));
            }

            if (subDepartmentId != null) {
                Join<TaskTemplate, SubDepartment> subDeptJoin = root.join("applicableSubDepartments", JoinType.LEFT);
                predicates.add(cb.or(cb.isNull(subDeptJoin.get("id")), cb.equal(subDeptJoin.get("id"), subDepartmentId)));
            }

            if (!isSuperAdmin) {
                Join<TaskTemplate, Department> deptJoin = root.join("applicableDepartments", JoinType.LEFT);
                Join<TaskTemplate, SubDepartment> subDeptJoin = root.join("applicableSubDepartments", JoinType.LEFT);

                // Global templates (no department and no sub-department constraints)
                Predicate isGlobalTemplate = cb.and(cb.isEmpty(root.get("applicableDepartments")), cb.isEmpty(root.get("applicableSubDepartments")));

                List<Predicate> orPredicates = new ArrayList<>();
                orPredicates.add(isGlobalTemplate);

                if (allowedDeptIds != null && !allowedDeptIds.isEmpty()) {
                    orPredicates.add(deptJoin.get("id").in(allowedDeptIds));
                }
                if (allowedSubDeptIds != null && !allowedSubDeptIds.isEmpty()) {
                    orPredicates.add(subDeptJoin.get("id").in(allowedSubDeptIds));
                }

                predicates.add(cb.or(orPredicates.toArray(new Predicate[0])));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Role> roleFilter(
            String search,
            List<String> allowedRoleNames,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(nameMatch, descMatch));
            }

            if (!isSuperAdmin && allowedRoleNames != null && !allowedRoleNames.isEmpty()) {
                predicates.add(root.get("name").in(allowedRoleNames));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Permission> permissionFilter(
            String resource,
            String search
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(resource)) {
                predicates.add(cb.equal(cb.lower(root.get("resource")), resource.toLowerCase().trim()));
            }

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate authMatch = cb.like(cb.lower(root.get("authority")), searchPattern);
                Predicate resourceMatch = cb.like(cb.lower(root.get("resource")), searchPattern);
                Predicate actionMatch = cb.like(cb.lower(root.get("action")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(authMatch, resourceMatch, actionMatch, descMatch));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
