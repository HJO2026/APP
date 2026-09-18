package com.example.HJO.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 댓글과 대댓글. parent_id가 null이면 댓글, 값이 있으면 대댓글이다.
 */
@Entity
@Table(name = "comments")
public class Comment extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "post_id", nullable = false)
	private Long postId;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	/** null이면 댓글, 값이 있으면 대댓글(1단계만 허용, 서비스에서 검증) */
	@Column(name = "parent_id")
	private Long parentId;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	protected Comment() {
	}

	public Comment(Long postId, Long userId, Long parentId, String content) {
		this.postId = postId;
		this.userId = userId;
		this.parentId = parentId;
		this.content = content;
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

	public Long getParentId() {
		return parentId;
	}

	public boolean isReply() {
		return parentId != null;
	}

	public String getContent() {
		return content;
	}

}
