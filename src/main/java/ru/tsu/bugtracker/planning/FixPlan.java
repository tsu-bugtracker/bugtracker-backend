package ru.tsu.bugtracker.planning;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.bug.BugPriority;
import ru.tsu.bugtracker.release.Release;
import ru.tsu.bugtracker.user.User;

@Entity
@Table(name = "fix_plans")
public class FixPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false, unique = true)
    private Bug bug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private BugPriority priority;

    @Column(name = "priority_change_reason", columnDefinition = "text")
    private String priorityChangeReason;

    @Column
    private OffsetDateTime deadline;

    @Column(columnDefinition = "text")
    private String rationale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "planned_by_id", nullable = false)
    private User plannedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_release_id")
    private Release targetRelease;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Bug getBug() {
        return bug;
    }

    public void setBug(Bug bug) {
        this.bug = bug;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public void setPriority(BugPriority priority) {
        this.priority = priority;
    }

    public String getPriorityChangeReason() {
        return priorityChangeReason;
    }

    public void setPriorityChangeReason(String priorityChangeReason) {
        this.priorityChangeReason = priorityChangeReason;
    }

    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(OffsetDateTime deadline) {
        this.deadline = deadline;
    }

    public String getRationale() {
        return rationale;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }

    public User getPlannedBy() {
        return plannedBy;
    }

    public void setPlannedBy(User plannedBy) {
        this.plannedBy = plannedBy;
    }

    public Release getTargetRelease() {
        return targetRelease;
    }

    public void setTargetRelease(Release targetRelease) {
        this.targetRelease = targetRelease;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
