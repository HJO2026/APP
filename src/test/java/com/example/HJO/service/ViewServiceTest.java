package com.example.HJO.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.HJO.dao.PostStatsDao;
import com.example.HJO.dao.ViewDao;
import com.example.HJO.dto.request.ViewRequest;

@ExtendWith(MockitoExtension.class)
class ViewServiceTest {

	private static final UUID EVENT_ID = UUID.fromString("3f1c2b8e-1111-4a4a-9a9a-000000000001");
	private static final Instant CLIENT_TS = Instant.parse("2026-09-18T12:00:00Z");

	@Mock
	ViewDao viewDao;

	@Mock
	PostStatsDao postStatsDao;

	@InjectMocks
	ViewService viewService;

	@Test
	@DisplayName("새 이벤트면 view_count를 1 올리고 true")
	void newEventIncrementsCounter() {
		given(viewDao.insertIfAbsent(EVENT_ID, 10L, 1L, CLIENT_TS)).willReturn(true);
		given(postStatsDao.incrementViewCount(10L)).willReturn(1);

		assertThat(viewService.record(10L, 1L, new ViewRequest(EVENT_ID, CLIENT_TS))).isTrue();
		verify(postStatsDao).incrementViewCount(10L);
	}

	@Test
	@DisplayName("이미 반영된 eventId면 카운터를 올리지 않고 false")
	void duplicateEventDoesNotIncrement() {
		given(viewDao.insertIfAbsent(EVENT_ID, 10L, 1L, CLIENT_TS)).willReturn(false);

		assertThat(viewService.record(10L, 1L, new ViewRequest(EVENT_ID, CLIENT_TS))).isFalse();
		verify(postStatsDao, never()).incrementViewCount(anyLong());
	}

	@Test
	@DisplayName("카운터 행(post_stats)이 없어 0행이 갱신되면 예외 → 롤백 + 500")
	void missingCounterRowFails() {
		given(viewDao.insertIfAbsent(EVENT_ID, 10L, 1L, CLIENT_TS)).willReturn(true);
		given(postStatsDao.incrementViewCount(10L)).willReturn(0);

		assertThatThrownBy(() -> viewService.record(10L, 1L, new ViewRequest(EVENT_ID, CLIENT_TS)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("post_stats");
	}

}
