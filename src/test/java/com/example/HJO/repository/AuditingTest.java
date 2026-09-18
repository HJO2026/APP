package com.example.HJO.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.HJO.domain.Board;
import com.example.HJO.domain.Comment;
import com.example.HJO.domain.Post;
import com.example.HJO.domain.User;
import com.example.HJO.support.IntegrationTest;

/**
 * RP-01 JPA Auditing: 엔티티를 저장하면 BaseTimeEntity의 created_at, updated_at이 자동으로 채워진다.
 * (앱에는 수정 경로가 없어서 수정 시 updated_at 갱신은 검증하지 않는다)
 */
@IntegrationTest
class AuditingTest {

	@Autowired
	BoardRepository boardRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PostRepository postRepository;

	@Autowired
	CommentRepository commentRepository;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	@DisplayName("RP-01 저장하면 created_at과 updated_at이 저장 시각으로 채워진다 (게시판, 사용자, 게시글, 댓글)")
	void auditingFillsTimestamps() {
		Instant before = Instant.now();

		Board board = boardRepository.save(new Board("audit-board"));
		User user = userRepository.save(new User("audit-user"));
		Post post = postRepository.save(new Post(board.getId(), user.getId(), "t", "c"));
		Comment comment = commentRepository.save(new Comment(post.getId(), user.getId(), null, "c"));

		for (var entity : new com.example.HJO.domain.BaseTimeEntity[] { board, user, post, comment }) {
			assertThat(entity.getCreatedAt()).as(entity.getClass().getSimpleName()).isNotNull()
					.isAfterOrEqualTo(before.minusSeconds(1));
			assertThat(entity.getUpdatedAt()).as(entity.getClass().getSimpleName()).isNotNull();
		}
		Map<String, Object> row = jdbc.queryForMap("SELECT created_at, updated_at FROM comments WHERE id = ?",
				comment.getId());
		assertThat(row.get("created_at")).as("DB에도 기록").isNotNull();
		assertThat(row.get("updated_at")).as("DB에도 기록").isNotNull();
	}

}
