package ru.tsu.bugtracker.project;

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
@Table(name = "project_members",
		uniqueConstraints = @UniqueConstraint(name = "uk_project_member", columnNames = { "project_id", "user_id" }))
public class ProjectMember {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "project_id", nullable = false)
	private Project project;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private ProjectRole role;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 32)
	private MemberStatus status = MemberStatus.ACTIVE;

	@Column(name = "joined_at", nullable = false)
	private OffsetDateTime joinedAt;

	@PrePersist
	void onCreate() {
		this.joinedAt = OffsetDateTime.now();
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

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public ProjectRole getRole() {
		return role;
	}

	public void setRole(ProjectRole role) {
		this.role = role;
	}

	public MemberStatus getStatus() {
		return status;
	}

	public void setStatus(MemberStatus status) {
		this.status = status;
	}

	public OffsetDateTime getJoinedAt() {
		return joinedAt;
	}

	public void setJoinedAt(OffsetDateTime joinedAt) {
		this.joinedAt = joinedAt;
	}

}
