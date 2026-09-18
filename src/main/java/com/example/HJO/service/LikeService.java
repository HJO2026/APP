package com.example.HJO.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.HJO.dao.LikeDao;
import com.example.HJO.dao.PostStatsDao;
import com.example.HJO.dto.response.LikeResponse;

@Service
public class LikeService {

	private final LikeDao likeDao;
	private final PostStatsDao postStatsDao;

	public LikeService(LikeDao likeDao, PostStatsDao postStatsDao) {
		this.likeDao = likeDao;
		this.postStatsDao = postStatsDao;
	}

	/**
	 * 한 트랜잭션: 좋아요 INSERT ... ON CONFLICT DO NOTHING
	 * → 새 좋아요면 like_count + 1 (RETURNING), 이미 눌렀으면 현재 like_count 조회.
	 * 어느 쪽이든 현재 상태를 돌려준다(멱등). 취소 API는 없다(팀 논의 중).
	 */
	@Transactional
	public LikeResponse like(long postId, long userId) {
		boolean inserted = likeDao.insertIfAbsent(postId, userId);
		long likeCount = (inserted ? postStatsDao.incrementLikeCount(postId) : postStatsDao.findLikeCount(postId))
				// 게시글은 있는데 카운터 행이 없다(시드 데이터 오류). 롤백 + 500으로 드러낸다
				.orElseThrow(() -> new IllegalStateException("post_stats row is missing for post " + postId));
		return LikeResponse.of(likeCount);
	}

}
