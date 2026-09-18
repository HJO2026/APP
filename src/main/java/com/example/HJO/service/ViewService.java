package com.example.HJO.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.HJO.dao.PostStatsDao;
import com.example.HJO.dao.ViewDao;
import com.example.HJO.dto.request.ViewRequest;

@Service
public class ViewService {

	private final ViewDao viewDao;
	private final PostStatsDao postStatsDao;

	public ViewService(ViewDao viewDao, PostStatsDao postStatsDao) {
		this.viewDao = viewDao;
		this.postStatsDao = postStatsDao;
	}

	/**
	 * 한 트랜잭션: 이벤트 INSERT ... ON CONFLICT DO NOTHING → 새 이벤트일 때만 view_count + 1.
	 * 같은 eventId는 몇 번 와도 1회만 반영된다. 새로 반영됐으면 true.
	 */
	@Transactional
	public boolean record(long postId, long userId, ViewRequest request) {
		boolean inserted = viewDao.insertIfAbsent(request.eventId(), postId, userId, request.clientTs());
		if (inserted && postStatsDao.incrementViewCount(postId) == 0) {
			// 게시글은 있는데 카운터 행이 없다(시드 데이터 오류). 조용히 넘기지 않고 롤백 + 500으로 드러낸다
			throw new IllegalStateException("post_stats row is missing for post " + postId);
		}
		return inserted;
	}

}
