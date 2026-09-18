package com.example.HJO.user;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JWT sub(= user id)와 작성자 nickname 표시용. 사용자는 시드로만 만든다(가입 API 없음).
 */
@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String nickname;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	protected User() {
	}

	public User(String nickname) {
		this.nickname = nickname;
		this.createdAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public String getNickname() {
		return nickname;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

}
