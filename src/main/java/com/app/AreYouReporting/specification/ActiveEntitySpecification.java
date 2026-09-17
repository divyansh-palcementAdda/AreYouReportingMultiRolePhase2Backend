package com.app.AreYouReporting.specification;

import com.app.AreYouReporting.Entities.*;
import org.springframework.data.jpa.domain.Specification;

public final class ActiveEntitySpecification {

    private ActiveEntitySpecification() {}

    public static Specification<Department> activeDepartment() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<SubDepartment> activeSubDepartment() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isActive")),
                cb.isTrue(root.get("department").get("isActive"))
        );
    }

    public static Specification<User> activeUser() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<TaskTemplate> activeTaskTemplate() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    public static Specification<Task> nonDeletedTask() {
        return (root, query, cb) -> cb.notEqual(root.get("status"), TaskStatus.DELETED);
    }
}
