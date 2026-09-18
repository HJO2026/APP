package com.example.HJO.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.HJO.dao.CommentDao;
import com.example.HJO.dao.PostDao;
import com.example.HJO.dao.PostDao.PostDetailRow;
import com.example.HJO.domain.Post;
import com.example.HJO.domain.PostSort;
import com.example.HJO.domain.PostStats;
import com.example.HJO.dto.request.PostCreateRequest;
import com.example.HJO.dto.response.CommentResponse;
import com.example.HJO.dto.response.CursorPageResponse;
import com.example.HJO.dto.response.PostDetailResponse;
import com.example.HJO.dto.response.PostSummaryResponse;
import com.example.HJO.global.error.BusinessException;
import com.example.HJO.global.error.ErrorCode;
import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.repository.PostRepository;
import com.example.HJO.repository.PostStatsRepository;

@Service
@Transactional(readOnly = true)
public class PostService {

	static final int DETAIL_COMMENT_LIMIT = 20;

	private final PostRepository postRepository;
	private final PostStatsRepository postStatsRepository;
	private final PostDao postDao;
	private final CommentDao commentDao;

	public PostService(PostRepository postRepository, PostStatsRepository postStatsRepository, PostDao postDao,
			CommentDao commentDao) {
		this.postRepository = postRepository;
		this.postStatsRepository = postStatsRepository;
		this.postDao = postDao;
		this.commentDao = commentDao;
	}

	/**
	 * 게시글과 카운터 행(post_stats)을 한 트랜잭션에서 만든다.
	 * 없는 게시판이나 사용자는 존재 확인 쿼리 없이 FK 위반으로 드러나고, GlobalExceptionHandler가 404/401로 바꾼다.
	 */
	@Transactional
	public Long create(Long authorId, PostCreateRequest request) {
		Post post = postRepository.save(new Post(request.boardId(), authorId, request.title(), request.content()));
		postStatsRepository.save(new PostStats(post.getId()));
		return post.getId();
	}

	/** offset 페이지네이션. size + 1건을 읽어 다음 페이지가 있는지 판단한다 */
	public CursorPageResponse<PostSummaryResponse> getPage(PostSort sort, String cursor, int size) {
		long offset = OffsetCursor.decode(cursor, sort.key());
		List<PostSummaryResponse> rows = postDao.findPage(sort, offset, size + 1);
		return CursorPageResponse.fromOverfetched(rows, size, OffsetCursor.encode(sort.key(), offset + size));
	}

	/** 게시글 1쿼리 + 최상위 댓글(작성자 join, 대댓글 수 포함) 1쿼리. 조회수는 올리지 않는다(views API가 따로 있다) */
	public PostDetailResponse getDetail(long postId) {
		PostDetailRow post = postDao.findDetail(postId)
				.orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
		List<CommentResponse> comments = commentDao.findLatestTopLevel(postId, DETAIL_COMMENT_LIMIT);

		return new PostDetailResponse(post.id(), post.boardId(), post.authorId(), post.authorNickname(),
				post.title(), post.content(), post.viewCount(), post.likeCount(),
				post.createdAt(), post.updatedAt(), comments);
	}

}
