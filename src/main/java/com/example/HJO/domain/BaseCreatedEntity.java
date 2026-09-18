package com.example.HJO.domain;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * created_at만 갖는 엔티티의 상위 클래스. 추가만 하고 수정하지 않는 테이블(좋아요, 조회 이벤트)이 상속한다.
 * JPA로 저장할 때 Auditing이 채운다. SQL로 직접 넣는 행은 DB 기본값(now())으로 채워진다.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseCreatedEntity {

	@CreatedDate
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	public Instant getCreatedAt() {
		return createdAt;
	}

}
