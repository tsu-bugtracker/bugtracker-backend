package ru.tsu.bugtracker.bug;

import java.time.OffsetDateTime;
import java.util.UUID;

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import ru.tsu.bugtracker.user.User;

@Entity
@Table(name = "bug_links", uniqueConstraints = @UniqueConstraint(
		name = "uk_bug_link",
		columnNames = { "source_bug_id", "target_bug_id", "link_type" }))
public class BugLink {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "source_bug_id", nullable = false)
	private Bug sourceBug;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "target_bug_id", nullable = false)
	private Bug targetBug;

	@Enumerated(EnumType.STRING)
	@Column(name = "link_type", nullable = false, length = 32)
	private BugLinkType linkType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "created_by_id", nullable = false)
	private User createdBy;

	@Column(name = "created_at", nullable = false)
	private OffsetDateTime createdAt;

	public BugLink() {
	}

	@PrePersist
	void onCreate() {
		if (createdAt == null) {
			createdAt = OffsetDateTime.now();
		}
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Bug getSourceBug() {
		return sourceBug;
	}

	public void setSourceBug(Bug sourceBug) {
		this.sourceBug = sourceBug;
	}

	public Bug getTargetBug() {
		return targetBug;
	}

	public void setTargetBug(Bug targetBug) {
		this.targetBug = targetBug;
	}

	public BugLinkType getLinkType() {
		return linkType;
	}

	public void setLinkType(BugLinkType linkType) {
		this.linkType = linkType;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public OffsetDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(OffsetDateTime createdAt) {
		this.createdAt = createdAt;
	}

}
