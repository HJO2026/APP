package com.example.HJO.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 삽입은 네이티브 INSERT ... ON CONFLICT DO NOTHING으로 한다(JPQL에 없다). 엔티티는 스키마 검증과 조회용이다.
 */
@Entity
@Table(name = "post_likes")
public class PostLike {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected PostLike() {
	}

	public Long getId() {
		return id;
	}

	public Long getPostId() {
		return postId;
	}

	public Long getUserId() {
		return userId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
