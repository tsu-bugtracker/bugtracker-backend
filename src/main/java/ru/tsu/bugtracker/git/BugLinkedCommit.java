package ru.tsu.bugtracker.git;

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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import ru.tsu.bugtracker.bug.Bug;
import ru.tsu.bugtracker.user.User;

@Entity
@Table(
        name = "bug_linked_commits",
        indexes = @Index(name = "idx_bug_linked_commits_bug", columnList = "bug_id"))
public class BugLinkedCommit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bug_id", nullable = false)
    private Bug bug;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GitCommitType type;

    @Column(name = "commit_hash", length = 128)
    private String commitHash;

    @Column(name = "merge_request_url", length = 1024)
    private String mergeRequestUrl;

    @Column(name = "author_name", length = 255)
    private String authorName;

    @Column(name = "committed_at")
    private Instant committedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "linked_by_id", nullable = false)
    private User linkedBy;

    @Column(name = "linked_at", nullable = false)
    private Instant linkedAt;

    @PrePersist
    void onCreate() {
        if (linkedAt == null) {
            linkedAt = Instant.now();
        }
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

    public GitCommitType getType() {
        return type;
    }

    public void setType(GitCommitType type) {
        this.type = type;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public void setCommitHash(String commitHash) {
        this.commitHash = commitHash;
    }

    public String getMergeRequestUrl() {
        return mergeRequestUrl;
    }

    public void setMergeRequestUrl(String mergeRequestUrl) {
        this.mergeRequestUrl = mergeRequestUrl;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Instant getCommittedAt() {
        return committedAt;
    }

    public void setCommittedAt(Instant committedAt) {
        this.committedAt = committedAt;
    }

    public User getLinkedBy() {
        return linkedBy;
    }

    public void setLinkedBy(User linkedBy) {
        this.linkedBy = linkedBy;
    }

    public Instant getLinkedAt() {
        return linkedAt;
    }

    public void setLinkedAt(Instant linkedAt) {
        this.linkedAt = linkedAt;
    }
}
