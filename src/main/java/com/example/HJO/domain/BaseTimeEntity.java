package com.example.HJO.domain;

import java.time.Instant;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;

/**
 * created_at + updated_at을 갖는 엔티티의 상위 클래스. 수정될 수 있는 테이블(게시판, 사용자, 게시글, 댓글)이 상속한다.
 * updated_at은 생성 시 created_at과 같은 값으로 채워지고, JPA로 수정할 때 갱신된다.
 * SQL로 직접 UPDATE하면 갱신되지 않으므로, 그런 쿼리는 updated_at도 직접 설정해야 한다.
 */
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public Instant getUpdatedAt() {
		return updatedAt;
	}

}
