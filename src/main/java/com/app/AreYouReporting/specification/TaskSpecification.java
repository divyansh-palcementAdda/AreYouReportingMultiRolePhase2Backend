package com.app.AreYouReporting.specification;

import com.app.AreYouReporting.Entities.*;
import com.app.AreYouReporting.security.ActiveUserContext;
import com.app.AreYouReporting.security.UserPrincipal;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TaskSpecification {

    public static Specification<Task> filter(
            String search,
            List<TaskStatus> statuses,
            TaskPriority priority,
            Boolean isSelfTask,
            TaskCompletionOutcome completionOutcome,
            Boolean isCompletedAfterDelay,
            Boolean isCompletedAfterExtension,
            UUID departmentId,
            UUID subDepartmentId,
            Instant fromDate,
            Instant toDate,
            ActiveUserContext activeContext,
            UserPrincipal principal,
            boolean isSuperAdmin
    ) {
        return (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> predicates = new ArrayList<>();

            // Always unconditionally exclude soft-deleted tasks from all read queries
            predicates.add(cb.notEqual(root.get("status"), TaskStatus.DELETED));

            if (StringUtils.hasText(search)) {
                String searchPattern = "%" + search.toLowerCase().trim() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), searchPattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), searchPattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            if (statuses != null && !statuses.isEmpty()) {
                List<TaskStatus> nonDeletedStatuses = statuses.stream()
                        .filter(s -> s != TaskStatus.DELETED)
                        .toList();
                if (nonDeletedStatuses.isEmpty()) {
                    predicates.add(cb.disjunction());
                } else {
                    predicates.add(root.get("status").in(nonDeletedStatuses));
                }
            }

            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            if (isSelfTask != null) {
                predicates.add(cb.equal(root.get("isSelfTask"), isSelfTask));
            }

            if (completionOutcome != null) {
                predicates.add(cb.equal(root.get("completionOutcome"), completionOutcome));
            }

            if (isCompletedAfterDelay != null) {
                predicates.add(cb.equal(root.get("isCompletedAfterDelay"), isCompletedAfterDelay));
            }

            if (isCompletedAfterExtension != null) {
                predicates.add(cb.equal(root.get("isCompletedAfterExtension"), isCompletedAfterExtension));
            }

            if (departmentId != null) {
                Join<Task, Department> deptJoin = root.join("assignedDepartments", JoinType.LEFT);
                predicates.add(cb.equal(deptJoin.get("id"), departmentId));
                predicates.add(cb.isTrue(deptJoin.get("isActive")));
            }

            if (subDepartmentId != null) {
                Join<Task, SubDepartment> subDeptJoin = root.join("assignedSubDepartments", JoinType.LEFT);
                predicates.add(cb.equal(subDeptJoin.get("id"), subDepartmentId));
                predicates.add(cb.isTrue(subDeptJoin.get("isActive")));
                predicates.add(cb.isTrue(subDeptJoin.get("department").get("isActive")));
            }

            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), fromDate));
            }

            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), toDate));
            }

            // Apply Scope Authorization Predicates
            if (!isSuperAdmin) {
                UUID userId = principal.getId();
                Predicate isCreator = cb.equal(root.get("creator").get("id"), userId);

                Join<Task, User> assigneeJoin = root.join("assignees", JoinType.LEFT);
                Predicate isAssignee = cb.equal(assigneeJoin.get("id"), userId);

                DataScopeType scope = activeContext != null ? activeContext.getEffectiveDataScope() : DataScopeType.SELF;

                if (scope == DataScopeType.GLOBAL) {
                    // Global scope within active context
                } else if (scope == DataScopeType.DEPARTMENT) {
                    Join<Task, Department> taskDeptJoin = root.join("assignedDepartments", JoinType.LEFT);
                    List<Predicate> deptPredicates = new ArrayList<>();
                    if (activeContext != null && activeContext.getDepartmentId() != null) {
                        deptPredicates.add(cb.equal(taskDeptJoin.get("id"), activeContext.getDepartmentId()));
                    }
                    if (activeContext != null && activeContext.getCustomDepartmentIds() != null && !activeContext.getCustomDepartmentIds().isEmpty()) {
                        deptPredicates.add(taskDeptJoin.get("id").in(activeContext.getCustomDepartmentIds()));
                    }
                    if (deptPredicates.isEmpty() && principal.getDepartmentIds() != null && !principal.getDepartmentIds().isEmpty()) {
                        deptPredicates.add(taskDeptJoin.get("id").in(principal.getDepartmentIds()));
                    }

                    if (!deptPredicates.isEmpty()) {
                        predicates.add(cb.or(isCreator, isAssignee, cb.or(deptPredicates.toArray(new Predicate[0]))));
                    } else {
                        predicates.add(cb.or(isCreator, isAssignee));
                    }
                } else if (scope == DataScopeType.SUB_DEPARTMENT) {
                    Join<Task, SubDepartment> taskSubDeptJoin = root.join("assignedSubDepartments", JoinType.LEFT);
                    List<Predicate> subDeptPredicates = new ArrayList<>();
                    if (activeContext != null && activeContext.getSubDepartmentId() != null) {
                        subDeptPredicates.add(cb.equal(taskSubDeptJoin.get("id"), activeContext.getSubDepartmentId()));
                    }
                    if (activeContext != null && activeContext.getCustomSubDepartmentIds() != null && !activeContext.getCustomSubDepartmentIds().isEmpty()) {
                        subDeptPredicates.add(taskSubDeptJoin.get("id").in(activeContext.getCustomSubDepartmentIds()));
                    }
                    if (subDeptPredicates.isEmpty() && principal.getSubDepartmentIds() != null && !principal.getSubDepartmentIds().isEmpty()) {
                        subDeptPredicates.add(taskSubDeptJoin.get("id").in(principal.getSubDepartmentIds()));
                    }

                    if (!subDeptPredicates.isEmpty()) {
                        predicates.add(cb.or(isCreator, isAssignee, cb.or(subDeptPredicates.toArray(new Predicate[0]))));
                    } else {
                        predicates.add(cb.or(isCreator, isAssignee));
                    }
                } else {
                    // SELF scope: only tasks created by user or assigned to user
                    predicates.add(cb.or(isCreator, isAssignee));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
