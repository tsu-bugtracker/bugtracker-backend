package ru.tsu.bugtracker.user;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "notification_settings")
public class NotificationSettings {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false, unique = true)
	private User user;

	@Column(name = "in_app_enabled", nullable = false)
	private boolean inAppEnabled = true;

	@Column(name = "email_enabled", nullable = false)
	private boolean emailEnabled = true;

	@Column(name = "events_json", columnDefinition = "text", nullable = false)
	private String eventsJson;

	@Column(name = "updated_at", nullable = false)
	private OffsetDateTime updatedAt;

	protected NotificationSettings() {
	}

	@PrePersist
	@PreUpdate
	void onSave() {
		this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public boolean isInAppEnabled() {
		return inAppEnabled;
	}

	public void setInAppEnabled(boolean inAppEnabled) {
		this.inAppEnabled = inAppEnabled;
	}

	public boolean isEmailEnabled() {
		return emailEnabled;
	}

	public void setEmailEnabled(boolean emailEnabled) {
		this.emailEnabled = emailEnabled;
	}

	public String getEventsJson() {
		return eventsJson;
	}

	public void setEventsJson(String eventsJson) {
		this.eventsJson = eventsJson;
	}

	public OffsetDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(OffsetDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}

}
