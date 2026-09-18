package com.example.HJO.comment;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 댓글은 시드로만 만든다(작성 API 없음).
 */
@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected Comment() {
	}

	public Comment(Long postId, Long userId, String content, Instant createdAt) {
		this.postId = postId;
		this.userId = userId;
		this.content = content;
		this.createdAt = createdAt;
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

	public String getContent() {
		return content;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
