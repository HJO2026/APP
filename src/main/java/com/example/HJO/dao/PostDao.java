package com.example.HJO.dao;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.HJO.domain.PostSort;
import com.example.HJO.dto.response.PostSummaryResponse;
import com.example.HJO.dto.response.TrendingPostResponse;

/**
 * 게시글 조회 SQL. baseline이 실행하는 쿼리를 그대로 드러내기 위해 SQL을 직접 쓴다.
 * 조회 인덱스가 없으므로(팀 합의) 정렬과 조회는 테이블 전체 스캔이 된다. 의도한 baseline 동작이다.
 */
@Repository
public class PostDao {

	private static final String PAGE_SELECT = """
			SELECT p.id, p.board_id, p.author_id, p.title, left(p.content, 100) AS preview, p.created_at,
			       s.view_count, s.like_count
			FROM posts p
			JOIN post_stats s ON s.post_id = p.id
			""";

	// ORDER BY는 바인딩할 수 없어서 정렬마다 SQL을 고정해 둔다(사용자 입력을 SQL에 넣지 않는다)
	private static final Map<PostSort, String> PAGE_SQL = Map.of(
			PostSort.LATEST, PAGE_SELECT + "ORDER BY p.created_at DESC, p.id DESC OFFSET :offset LIMIT :limit",
			PostSort.POPULAR, PAGE_SELECT + "ORDER BY s.like_count DESC, p.id DESC OFFSET :offset LIMIT :limit");

	private static final String DETAIL_SQL = """
			SELECT p.id, p.board_id, p.author_id, u.nickname AS author_nickname, p.title, p.content,
			       s.view_count, s.like_count, p.created_at, p.updated_at
			FROM posts p
			JOIN post_stats s ON s.post_id = p.id
			JOIN users u ON u.id = p.author_id
			WHERE p.id = :id
			""";

	/**
	 * trending: 요청 시점에 이벤트 테이블에서 실시간 집계한다. 기준 시각은 서버 기록 시각(created_at)이다.
	 * event_id가 UNIQUE라서 count(*)가 곧 고유 조회 이벤트 수다. 상위 N개 글 id를 먼저 뽑고 그 N건만 join한다.
	 * created_at에 인덱스가 없으므로 요청마다 post_view_events 전체를 스캔한다(의도한 baseline 동작, P2 개선 대상).
	 */
	private static final String TRENDING_SQL = """
			SELECT p.id, p.board_id, p.author_id, p.title, left(p.content, 100) AS preview, p.created_at,
			       s.view_count, s.like_count, t.recent_views
			FROM (
			    SELECT post_id, count(*) AS recent_views
			    FROM post_view_events
			    WHERE created_at >= now() - make_interval(secs => :windowSeconds)
			    GROUP BY post_id
			    ORDER BY recent_views DESC, post_id DESC
			    LIMIT :limit
			) t
			JOIN posts p ON p.id = t.post_id
			JOIN post_stats s ON s.post_id = p.id
			ORDER BY t.recent_views DESC, p.id DESC
			""";

	private final NamedParameterJdbcTemplate jdbc;

	public PostDao(NamedParameterJdbcTemplate jdbc) {
		this.jdbc = jdbc;
	}

	/** offset부터 최대 limit건. 다음 페이지 유무는 호출자가 limit를 size + 1로 줘서 판단한다 */
	public List<PostSummaryResponse> findPage(PostSort sort, long offset, int limit) {
		return jdbc.query(PAGE_SQL.get(sort), Map.of("offset", offset, "limit", limit),
				(rs, rowNum) -> new PostSummaryResponse(
						rs.getLong("id"),
						rs.getLong("board_id"),
						rs.getLong("author_id"),
						rs.getString("title"),
						rs.getString("preview"),
						rs.getLong("view_count"),
						rs.getLong("like_count"),
						instant(rs, "created_at")));
	}

	public Optional<PostDetailRow> findDetail(long id) {
		return jdbc.query(DETAIL_SQL, Map.of("id", id),
				(rs, rowNum) -> new PostDetailRow(
						rs.getLong("id"),
						rs.getLong("board_id"),
						rs.getLong("author_id"),
						rs.getString("author_nickname"),
						rs.getString("title"),
						rs.getString("content"),
						rs.getLong("view_count"),
						rs.getLong("like_count"),
						instant(rs, "created_at"),
						instant(rs, "updated_at")))
				.stream().findFirst();
	}

	public List<TrendingPostResponse> findTrending(Duration window, int limit) {
		return jdbc.query(TRENDING_SQL, Map.of("windowSeconds", (double) window.toSeconds(), "limit", limit),
				(rs, rowNum) -> new TrendingPostResponse(
						rs.getLong("id"),
						rs.getLong("board_id"),
						rs.getLong("author_id"),
						rs.getString("title"),
						rs.getString("preview"),
						rs.getLong("recent_views"),
						rs.getLong("view_count"),
						rs.getLong("like_count"),
						instant(rs, "created_at")));
	}

	static Instant instant(ResultSet rs, String column) throws SQLException {
		OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
		return value == null ? null : value.toInstant();
	}

	public record PostDetailRow(
			Long id,
			Long boardId,
			Long authorId,
			String authorNickname,
			String title,
			String content,
			long viewCount,
			long likeCount,
			Instant createdAt,
			Instant updatedAt) {
	}

}
