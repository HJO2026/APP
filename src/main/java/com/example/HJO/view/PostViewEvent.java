package com.example.HJO.view;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 삽입은 네이티브 INSERT ... ON CONFLICT (event_id) DO NOTHING으로 한다. 엔티티는 스키마 검증과 조회용이다.
 */
@Entity
@Table(name = "post_view_events")
public class PostViewEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "client_ts", nullable = false)
	private Instant clientTs;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PostViewEvent() {
	}

	public Long getId() {
		return id;
	}

	public UUID getEventId() {
		return eventId;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getClientTs() {
		return clientTs;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
