package com.app.AreYouReporting.specification;

import com.app.AreYouReporting.Entities.AuditLog;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class AuditLogSpecification {

    public static Specification<AuditLog> filter(String actorUsername, String action, String entityType,
                                                String entityId, Instant fromDate, Instant toDate) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(actorUsername)) {
                predicates.add(cb.like(cb.lower(root.get("actorUsername")), "%" + actorUsername.toLowerCase().trim() + "%"));
            }
            if (StringUtils.hasText(action)) {
                predicates.add(cb.equal(root.get("action"), action.trim()));
            }
            if (StringUtils.hasText(entityType)) {
                predicates.add(cb.equal(root.get("entityType"), entityType.trim()));
            }
            if (StringUtils.hasText(entityId)) {
                predicates.add(cb.equal(root.get("entityId"), entityId.trim()));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
