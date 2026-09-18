package com.example.HJO.dto.request;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

/**
 * 조회 이벤트. eventId는 클라이언트가 조회 한 번마다 만드는 UUID다.
 * 같은 eventId는 몇 번 보내도(재시도 포함) 1회만 반영된다. clientTs는 ISO-8601 시각이다.
 */
public record ViewRequest(
		@NotNull UUID eventId,
		@NotNull Instant clientTs) {
}
