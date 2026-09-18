package com.example.HJO.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;

class DataIntegrityErrorMappingTest {

	@ParameterizedTest(name = "{0} → {1}")
	@CsvSource({
			"fk_posts_board, BOARD_NOT_FOUND",
			"fk_post_stats_post, POST_NOT_FOUND",
			"fk_comments_post, POST_NOT_FOUND",
			"fk_post_likes_post, POST_NOT_FOUND",
			"fk_post_view_events_post, POST_NOT_FOUND",
			"fk_comments_parent, COMMENT_NOT_FOUND",
			"fk_posts_author, UNAUTHORIZED",
			"fk_comments_user, UNAUTHORIZED",
			"fk_post_likes_user, UNAUTHORIZED",
			"fk_post_view_events_user, UNAUTHORIZED",
	})
	@DisplayName("FK 제약 이름을 ErrorCode로 바꾼다")
	void mapsForeignKeyViolations(String constraint, ErrorCode expected) {
		assertThat(DataIntegrityErrorMapping.resolve(fkViolation(constraint))).contains(expected);
	}

	@Test
	@DisplayName("원인 체인 안쪽의 PostgreSQL 메시지에서도 제약 이름을 찾는다")
	void findsConstraintInNestedCause() {
		Exception nested = new DataIntegrityViolationException("could not execute statement",
				new RuntimeException("wrapper", new SQLException(
						"ERROR: insert or update on table \"post_likes\" violates foreign key constraint \"fk_post_likes_post\"",
						"23503")));

		assertThat(DataIntegrityErrorMapping.resolve(nested)).contains(ErrorCode.POST_NOT_FOUND);
	}

	@Test
	@DisplayName("대응표에 없는 제약(예: PK 중복)은 empty → 서버 버그로 보고 500")
	void unknownConstraintIsUnmapped() {
		Exception duplicatePk = new DataIntegrityViolationException("dup", new SQLException(
				"ERROR: duplicate key value violates unique constraint \"posts_pkey\"", "23505"));

		assertThat(DataIntegrityErrorMapping.resolve(duplicatePk)).isEmpty();
	}

	@Test
	@DisplayName("데이터 오류(SQLState 22xxx, 예: NUL 문자)는 클라이언트 입력 오류 → INVALID_INPUT")
	void dataExceptionIsInvalidInput() {
		Exception nulByte = new DataIntegrityViolationException("bad data", new SQLException(
				"ERROR: invalid byte sequence for encoding \"UTF8\": 0x00", "22021"));

		assertThat(DataIntegrityErrorMapping.resolve(nulByte)).contains(ErrorCode.INVALID_INPUT);
	}

	@Test
	@DisplayName("메시지도 SQLState도 없으면 empty")
	void noInformationIsUnmapped() {
		assertThat(DataIntegrityErrorMapping.resolve(new DataIntegrityViolationException(null))).isEmpty();
	}

	private static Exception fkViolation(String constraint) {
		return new DataIntegrityViolationException("could not execute statement", new SQLException(
				"ERROR: insert or update on table \"x\" violates foreign key constraint \"" + constraint + "\"",
				"23503"));
	}

}
