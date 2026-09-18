package com.example.HJO.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.HJO.dao.LikeDao;
import com.example.HJO.dao.PostStatsDao;
import com.example.HJO.dto.response.LikeResponse;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

	@Mock
	LikeDao likeDao;

	@Mock
	PostStatsDao postStatsDao;

	@InjectMocks
	LikeService likeService;

	@Test
	@DisplayName("새 좋아요면 like_count를 1 올리고 증가 후 값을 돌려준다")
	void newLikeIncrementsCounter() {
		given(likeDao.insertIfAbsent(10L, 1L)).willReturn(true);
		given(postStatsDao.incrementLikeCount(10L)).willReturn(Optional.of(5L));

		LikeResponse response = likeService.like(10L, 1L);

		assertThat(response).isEqualTo(new LikeResponse(true, 5L));
		verify(postStatsDao, never()).findLikeCount(anyLong());
	}

	@Test
	@DisplayName("이미 누른 좋아요면 카운터를 올리지 않고 현재 값을 돌려준다 (멱등)")
	void duplicateLikeReturnsCurrentState() {
		given(likeDao.insertIfAbsent(10L, 1L)).willReturn(false);
		given(postStatsDao.findLikeCount(10L)).willReturn(Optional.of(4L));

		LikeResponse response = likeService.like(10L, 1L);

		assertThat(response).isEqualTo(new LikeResponse(true, 4L));
		verify(postStatsDao, never()).incrementLikeCount(anyLong());
	}

	@Test
	@DisplayName("카운터 행(post_stats)이 없으면 예외 → 롤백 + 500 (시드 오류를 드러낸다)")
	void missingCounterRowFails() {
		given(likeDao.insertIfAbsent(10L, 1L)).willReturn(true);
		given(postStatsDao.incrementLikeCount(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> likeService.like(10L, 1L))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("post_stats");
	}

}
