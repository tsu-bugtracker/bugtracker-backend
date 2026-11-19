package com.bugtracker.specification;

import com.bugtracker.domain.entity.Issue;
import com.bugtracker.domain.enums.IssuePriority;
import com.bugtracker.domain.enums.IssueStatus;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public final class IssueSpecification {

    private IssueSpecification() {
    }

    public static Specification<Issue> filter(UUID projectId,
                                              String search,
                                              IssueStatus status,
                                              IssuePriority priority,
                                              UUID assigneeId,
                                              UUID reporterId,
                                              UUID componentId,
                                              UUID releaseId) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("project").get("id"), projectId));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(root.get("stepsToReproduce")), pattern)
                ));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }
            if (reporterId != null) {
                predicates.add(cb.equal(root.get("reporter").get("id"), reporterId));
            }
            if (componentId != null) {
                predicates.add(cb.equal(root.get("component").get("id"), componentId));
            }
            if (releaseId != null) {
                predicates.add(cb.equal(root.get("release").get("id"), releaseId));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
