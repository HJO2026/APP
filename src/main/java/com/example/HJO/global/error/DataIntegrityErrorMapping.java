package com.example.HJO.global.error;

import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DB 무결성 오류를 ErrorCode로 바꾸는 대응표.
 * <ul>
 * <li>FK 위반: baseline은 쓰기 전에 존재 확인 쿼리를 하지 않는다. 없는 게시글, 댓글, 게시판은 FK 위반으로 드러나고 여기서 404로 바뀐다.
 * 제약 이름은 V1__baseline_schema.sql과 같아야 한다.</li>
 * <li>데이터 오류(SQLState 22xxx): 클라이언트가 보낸 값을 DB가 받을 수 없는 경우(예: 문자열 안의 NUL 문자)다. 400으로 바꾼다.</li>
 * </ul>
 */
final class DataIntegrityErrorMapping {

	private static final Map<String, ErrorCode> BY_CONSTRAINT = Map.ofEntries(
			Map.entry("fk_posts_board", ErrorCode.BOARD_NOT_FOUND),
			Map.entry("fk_post_stats_post", ErrorCode.POST_NOT_FOUND),
			Map.entry("fk_comments_post", ErrorCode.POST_NOT_FOUND),
			Map.entry("fk_post_likes_post", ErrorCode.POST_NOT_FOUND),
			Map.entry("fk_post_view_events_post", ErrorCode.POST_NOT_FOUND),
			Map.entry("fk_comments_parent", ErrorCode.COMMENT_NOT_FOUND),
			// TODO(team): 토큰의 user가 users에 없을 때의 응답 코드 미정. 잠정 401
			Map.entry("fk_posts_author", ErrorCode.UNAUTHORIZED),
			Map.entry("fk_comments_user", ErrorCode.UNAUTHORIZED),
			Map.entry("fk_post_likes_user", ErrorCode.UNAUTHORIZED),
			Map.entry("fk_post_view_events_user", ErrorCode.UNAUTHORIZED));

	// PostgreSQL 에러 메시지: ... violates foreign key constraint "fk_posts_board" ...
	private static final Pattern CONSTRAINT_NAME = Pattern.compile("constraint \"([^\"]+)\"");

	/** SQLState 클래스 22 = data exception (잘못된 문자 인코딩, 범위 초과 등) */
	private static final String DATA_EXCEPTION_CLASS = "22";

	private DataIntegrityErrorMapping() {
	}

	/** 대응표에 없는 오류면 empty (서버 버그로 보고 500) */
	static Optional<ErrorCode> resolve(Throwable ex) {
		for (Throwable t = ex; t != null; t = t.getCause()) {
			if (t instanceof SQLException sql && sql.getSQLState() != null
					&& sql.getSQLState().startsWith(DATA_EXCEPTION_CLASS)) {
				return Optional.of(ErrorCode.INVALID_INPUT);
			}
			String message = t.getMessage();
			if (message == null) {
				continue;
			}
			Matcher matcher = CONSTRAINT_NAME.matcher(message);
			if (matcher.find()) {
				return Optional.ofNullable(BY_CONSTRAINT.get(matcher.group(1)));
			}
		}
		return Optional.empty();
	}

}
