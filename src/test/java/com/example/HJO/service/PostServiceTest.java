package com.example.HJO.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.LongStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.HJO.dao.CommentDao;
import com.example.HJO.dao.PostDao;
import com.example.HJO.dao.PostDao.PostDetailRow;
import com.example.HJO.domain.Post;
import com.example.HJO.domain.PostSort;
import com.example.HJO.domain.PostStats;
import com.example.HJO.dto.request.PostCreateRequest;
import com.example.HJO.dto.response.CursorPageResponse;
import com.example.HJO.dto.response.PostDetailResponse;
import com.example.HJO.dto.response.PostSummaryResponse;
import com.example.HJO.global.config.TrendingProperties;
import com.example.HJO.global.error.ErrorCode;
import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.repository.PostRepository;
import com.example.HJO.repository.PostStatsRepository;

@ExtendWith(MockitoExtension.class)
class PostServiceTest {

	@Mock
	PostRepository postRepository;

	@Mock
	PostStatsRepository postStatsRepository;

	@Mock
	PostDao postDao;

	@Mock
	CommentDao commentDao;

	PostService postService;

	@BeforeEach
	void setUp() {
		postService = new PostService(postRepository, postStatsRepository, postDao, commentDao,
				new TrendingProperties(Duration.ofHours(24), 20));
	}

	@Test
	@DisplayName("글을 만들 때 같은 id로 카운터 행(post_stats)도 만든다")
	void createAlsoCreatesCounterRow() {
		given(postRepository.save(any(Post.class))).willAnswer(inv -> withId(inv.getArgument(0), 7L));

		Long id = postService.create(1L, new PostCreateRequest(3L, "title", "content"));

		assertThat(id).isEqualTo(7L);
		ArgumentCaptor<PostStats> stats = ArgumentCaptor.forClass(PostStats.class);
		verify(postStatsRepository).save(stats.capture());
		assertThat(stats.getValue().getPostId()).isEqualTo(7L);
		assertThat(stats.getValue().isNew()).as("INSERT만 나가도록 새 엔티티로 표시").isTrue();
	}

	@Test
	@DisplayName("목록: cursor의 offset부터 size + 1건을 읽고, 더 있으면 다음 cursor를 준다")
	void pageReadsOverfetchAndBuildsNextCursor() {
		given(postDao.findPage(PostSort.LATEST, 20, 3)).willReturn(summaries(3));

		CursorPageResponse<PostSummaryResponse> page =
				postService.getPage(PostSort.LATEST, OffsetCursor.encode("latest", 20), 2);

		assertThat(page.items()).hasSize(2);
		assertThat(OffsetCursor.decode(page.nextCursor(), "latest")).isEqualTo(22);
	}

	@Test
	@DisplayName("목록: 마지막 페이지면 nextCursor가 없다")
	void lastPageHasNoCursor() {
		given(postDao.findPage(PostSort.POPULAR, 0, 3)).willReturn(summaries(1));

		assertThat(postService.getPage(PostSort.POPULAR, null, 2).nextCursor()).isNull();
	}

	@Test
	@DisplayName("상세: 게시글이 없으면 POST_NOT_FOUND, 댓글을 조회하지 않는다")
	void detailOfMissingPost() {
		given(postDao.findDetail(99L)).willReturn(Optional.empty());

		CommentServiceTest.assertErrorCode(() -> postService.getDetail(99L), ErrorCode.POST_NOT_FOUND);
		verifyNoInteractions(commentDao);
	}

	@Test
	@DisplayName("상세: 게시글과 최신 최상위 댓글 20개를 합친다")
	void detailCombinesPostAndComments() {
		Instant now = Instant.parse("2026-09-18T12:00:00Z");
		given(postDao.findDetail(7L)).willReturn(Optional.of(
				new PostDetailRow(7L, 3L, 1L, "alice", "t", "c", 5, 2, now, now)));
		given(commentDao.findLatestTopLevel(7L, 20)).willReturn(List.of());

		PostDetailResponse detail = postService.getDetail(7L);

		assertThat(detail.authorNickname()).isEqualTo("alice");
		assertThat(detail.viewCount()).isEqualTo(5);
		assertThat(detail.comments()).isEmpty();
		verify(commentDao).findLatestTopLevel(7L, 20);
	}

	@Test
	@DisplayName("trending: 설정의 기간과 개수로 조회한다")
	void trendingUsesConfiguredWindowAndLimit() {
		postService.getTrending();

		verify(postDao).findTrending(Duration.ofHours(24), 20);
	}

	private static List<PostSummaryResponse> summaries(int count) {
		return LongStream.rangeClosed(1, count)
				.mapToObj(i -> new PostSummaryResponse(i, 1L, 1L, "t" + i, "p", 0, 0, Instant.EPOCH))
				.toList();
	}

	/** JPA가 저장 시 채우는 IDENTITY id를 흉내 낸다 */
	private static Post withId(Post post, long id) {
		try {
			var field = Post.class.getDeclaredField("id");
			field.setAccessible(true);
			field.set(post, id);
			return post;
		}
		catch (ReflectiveOperationException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
