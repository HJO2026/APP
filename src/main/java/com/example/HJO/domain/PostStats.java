package com.example.HJO.domain;

import org.springframework.data.domain.Persistable;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * posts와 1:1인 카운터 테이블. 생성할 때만 엔티티로 저장하고,
 * 증가는 반드시 원자적 UPDATE(SET x = x + 1)로 한다. 엔티티로 읽고 고쳐 쓰면 lost update가 난다.
 * <p>
 * id(post_id)를 직접 넣는 엔티티라 save()가 기본으로 merge(INSERT 전 SELECT)를 한다.
 * {@link Persistable}로 새 엔티티임을 알려 INSERT만 실행되게 한다.
 */
@Entity
@Table(name = "post_stats")
public class PostStats implements Persistable<Long> {

	@Id
	@Column(name = "post_id")
	private Long postId;

	@Column(name = "view_count", nullable = false)
	private long viewCount;

	@Column(name = "like_count", nullable = false)
	private long likeCount;

	@Transient
	private boolean isNew = true;

	protected PostStats() {
	}

	public PostStats(Long postId) {
		this.postId = postId;
	}

	@Override
	public Long getId() {
		return postId;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
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
