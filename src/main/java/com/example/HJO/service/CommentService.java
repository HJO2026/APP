package com.example.HJO.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.HJO.dao.CommentDao;
import com.example.HJO.domain.Comment;
import com.example.HJO.dto.request.CommentCreateRequest;
import com.example.HJO.dto.response.CursorPageResponse;
import com.example.HJO.dto.response.ReplyResponse;
import com.example.HJO.global.error.BusinessException;
import com.example.HJO.global.error.ErrorCode;
import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.repository.CommentRepository;

@Service
@Transactional(readOnly = true)
public class CommentService {

	private final CommentRepository commentRepository;
	private final CommentDao commentDao;

	public CommentService(CommentRepository commentRepository, CommentDao commentDao) {
		this.commentRepository = commentRepository;
		this.commentDao = commentDao;
	}

	/**
	 * 댓글 또는 대댓글을 만든다.
	 * - 댓글: INSERT 1번. 없는 게시글은 FK 위반으로 드러나고 404 POST_NOT_FOUND로 바뀐다
	 * - 대댓글: 부모를 PK로 조회해 같은 게시글의 최상위 댓글인지 확인한 뒤 INSERT (깊이 1단계만 허용)
	 */
	@Transactional
	public Long create(long postId, Long userId, CommentCreateRequest request) {
		if (request.parentId() != null) {
			Comment parent = findCommentOfPost(postId, request.parentId());
			if (parent.isReply()) {
				throw new BusinessException(ErrorCode.REPLY_DEPTH_EXCEEDED);
			}
		}
		Comment comment = commentRepository.save(new Comment(postId, userId, request.parentId(), request.content()));
		return comment.getId();
	}

	/**
	 * 대댓글 목록(오래된 순). 댓글이 없거나 다른 게시글의 댓글이면 404.
	 * 대댓글에는 답글이 없으므로 대댓글 id로 요청하면 빈 목록이다.
	 */
	public CursorPageResponse<ReplyResponse> getReplies(long postId, long commentId, String cursor, int size) {
		findCommentOfPost(postId, commentId);

		String scope = "replies-" + commentId;
		long offset = OffsetCursor.decode(cursor, scope);
		List<ReplyResponse> rows = commentDao.findReplies(commentId, offset, size + 1);
		return CursorPageResponse.fromOverfetched(rows, size, OffsetCursor.encode(scope, offset + size));
	}

	private Comment findCommentOfPost(long postId, long commentId) {
		return commentRepository.findById(commentId)
				.filter(comment -> comment.getPostId() == postId)
				.orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));
	}

}
