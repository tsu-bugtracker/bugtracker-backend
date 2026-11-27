package ru.tsu.bugtracker.bug;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import ru.tsu.bugtracker.project.Label;
import ru.tsu.bugtracker.project.Project;
import ru.tsu.bugtracker.project.SoftwareComponent;
import ru.tsu.bugtracker.release.Release;
import ru.tsu.bugtracker.user.User;

@Entity
@Table(
        name = "bugs",
        indexes = {
            @Index(name = "idx_bugs_project_status", columnList = "project_id, status"),
            @Index(name = "idx_bugs_assignee", columnList = "assignee_id"),
            @Index(name = "idx_bugs_key", columnList = "bug_key", unique = true)
        })
public class Bug {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "bug_key", nullable = false, unique = true, length = 32)
    private String bugKey;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "steps_to_reproduce", nullable = false, columnDefinition = "text")
    private String stepsToReproduce;

    @Column(nullable = false, columnDefinition = "text")
    private String environment;

    @Column(name = "expected_result", columnDefinition = "text")
    private String expectedResult;

    @Column(name = "actual_result", columnDefinition = "text")
    private String actualResult;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BugPriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_priority", nullable = false)
    private BugPriority recommendedPriority;

    @Enumerated(EnumType.STRING)
    private BugResolution resolution;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "component_id")
    private SoftwareComponent component;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_release_id")
    private Release targetRelease;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fixed_in_release_id")
    private Release fixedInRelease;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "bug_labels",
            joinColumns = @JoinColumn(name = "bug_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id"))
    private Set<Label> labels = new HashSet<>();

    private Instant deadline;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public String getBugKey() {
        return bugKey;
    }

    public void setBugKey(String bugKey) {
        this.bugKey = bugKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStepsToReproduce() {
        return stepsToReproduce;
    }

    public void setStepsToReproduce(String stepsToReproduce) {
        this.stepsToReproduce = stepsToReproduce;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getExpectedResult() {
        return expectedResult;
    }

    public void setExpectedResult(String expectedResult) {
        this.expectedResult = expectedResult;
    }

    public String getActualResult() {
        return actualResult;
    }

    public void setActualResult(String actualResult) {
        this.actualResult = actualResult;
    }

    public BugStatus getStatus() {
        return status;
    }

    public void setStatus(BugStatus status) {
        this.status = status;
    }

    public BugPriority getPriority() {
        return priority;
    }

    public void setPriority(BugPriority priority) {
        this.priority = priority;
    }

    public BugPriority getRecommendedPriority() {
        return recommendedPriority;
    }

    public void setRecommendedPriority(BugPriority recommendedPriority) {
        this.recommendedPriority = recommendedPriority;
    }

    public BugResolution getResolution() {
        return resolution;
    }

    public void setResolution(BugResolution resolution) {
        this.resolution = resolution;
    }

    public User getAuthor() {
        return author;
    }

    public void setAuthor(User author) {
        this.author = author;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public SoftwareComponent getComponent() {
        return component;
    }

    public void setComponent(SoftwareComponent component) {
        this.component = component;
    }

    public Release getTargetRelease() {
        return targetRelease;
    }

    public void setTargetRelease(Release targetRelease) {
        this.targetRelease = targetRelease;
    }

    public Release getFixedInRelease() {
        return fixedInRelease;
    }

    public void setFixedInRelease(Release fixedInRelease) {
        this.fixedInRelease = fixedInRelease;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public void setLabels(Set<Label> labels) {
        this.labels = labels;
    }

    public Instant getDeadline() {
        return deadline;
    }

    public void setDeadline(Instant deadline) {
        this.deadline = deadline;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
