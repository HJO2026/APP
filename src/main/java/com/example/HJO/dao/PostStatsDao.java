package com.example.HJO.dao;

import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * 게시글 카운터(post_stats) 갱신. 모든 카운터 SQL을 여기에 모은다.
 * <p>
 * baseline은 요청마다 {@code SET x = x + 1}로 즉시 갱신한다. 인기 글 하나의 행에 UPDATE가 몰리면
 * 행 잠금 때문에 줄을 서게 된다(핫 로우 경합). 의도한 동작이며 P1의 개선 대상이다. 최적화하지 않는다.
 * 엔티티로 읽고 고쳐 쓰면 lost update가 나므로 반드시 원자적 UPDATE로 한다.
 */
@Repository
public class PostStatsDao {

	private static final String INCREMENT_VIEW_SQL =
			"UPDATE post_stats SET view_count = view_count + 1 WHERE post_id = :postId";

	private static final String INCREMENT_LIKE_SQL =
			"UPDATE post_stats SET like_count = like_count + 1 WHERE post_id = :postId RETURNING like_count";

	private static final String FIND_LIKE_COUNT_SQL =
			"SELECT like_count FROM post_stats WHERE post_id = :postId";

	private final NamedParameterJdbcTemplate jdbc;

	public PostStatsDao(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** 갱신된 행 수. 0이면 카운터 행이 없는 것이다 */
	public int incrementViewCount(long postId) {
		return jdbc.update(INCREMENT_VIEW_SQL, Map.of("postId", postId));
	}

	/** 증가 후 like_count. 카운터 행이 없으면 empty */
	public Optional<Long> incrementLikeCount(long postId) {
		return jdbc.queryForList(INCREMENT_LIKE_SQL, Map.of("postId", postId), Long.class).stream().findFirst();
	}

	public Optional<Long> findLikeCount(long postId) {
		return jdbc.queryForList(FIND_LIKE_COUNT_SQL, Map.of("postId", postId), Long.class).stream().findFirst();
	}

}
