package com.example.HJO.post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * posts와 1:1인 카운터 테이블. 생성할 때만 엔티티로 저장하고,
 * 증가는 반드시 원자적 UPDATE(SET x = x + 1)로 한다. 엔티티로 읽고 고쳐 쓰면 lost update가 난다.
 */
@Entity
@Table(name = "post_stats")
public class PostStats {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private long likeCount;

	protected PostStats() {
	}

	public PostStats(Long postId) {
		this.postId = postId;
	}

	public Long getPostId() {
		return postId;
	}

	public long getViewCount() {
		return viewCount;
	}

	public long getLikeCount() {
		return likeCount;
	}

}
