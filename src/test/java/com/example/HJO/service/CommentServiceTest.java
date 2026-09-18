package com.example.HJO.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.HJO.dao.CommentDao;
import com.example.HJO.domain.Comment;
import com.example.HJO.dto.request.CommentCreateRequest;
import com.example.HJO.global.error.BusinessException;
import com.example.HJO.global.error.ErrorCode;
import com.example.HJO.global.pagination.OffsetCursor;
import com.example.HJO.repository.CommentRepository;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

	private static final long POST_ID = 10L;
	private static final long OTHER_POST_ID = 20L;
	private static final long USER_ID = 1L;

	@Mock
	CommentRepository commentRepository;

	@Mock
	CommentDao commentDao;

	@InjectMocks
	CommentService commentService;

	@Nested
	@DisplayName("댓글 작성")
	class Create {

		@Test
		@DisplayName("parentId가 없으면 부모를 조회하지 않고 최상위 댓글로 저장한다")
		void topLevelCommentSkipsParentLookup() {
			given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

			commentService.create(POST_ID, USER_ID, new CommentCreateRequest("hello", null));

			Comment saved = captureSaved();
			assertThat(saved.getPostId()).isEqualTo(POST_ID);
			assertThat(saved.getUserId()).isEqualTo(USER_ID);
			assertThat(saved.getParentId()).isNull();
			assertThat(saved.getContent()).isEqualTo("hello");
			verify(commentRepository, never()).findById(anyLong());
		}

		@Test
		@DisplayName("부모가 같은 게시글의 최상위 댓글이면 대댓글로 저장한다")
		void replyToTopLevelComment() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(topLevel(POST_ID)));
			given(commentRepository.save(any(Comment.class))).willAnswer(inv -> inv.getArgument(0));

			commentService.create(POST_ID, USER_ID, new CommentCreateRequest("reply", 5L));

			assertThat(captureSaved().getParentId()).isEqualTo(5L);
		}

		@Test
		@DisplayName("부모 댓글이 없으면 COMMENT_NOT_FOUND, 저장하지 않는다")
		void missingParent() {
			given(commentRepository.findById(5L)).willReturn(Optional.empty());

			assertErrorCode(() -> commentService.create(POST_ID, USER_ID, new CommentCreateRequest("r", 5L)),
					ErrorCode.COMMENT_NOT_FOUND);
			verify(commentRepository, never()).save(any());
		}

		@Test
		@DisplayName("부모가 다른 게시글의 댓글이면 COMMENT_NOT_FOUND")
		void parentOfAnotherPost() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(topLevel(OTHER_POST_ID)));

			assertErrorCode(() -> commentService.create(POST_ID, USER_ID, new CommentCreateRequest("r", 5L)),
					ErrorCode.COMMENT_NOT_FOUND);
			verify(commentRepository, never()).save(any());
		}

		@Test
		@DisplayName("부모가 이미 대댓글이면 REPLY_DEPTH_EXCEEDED (깊이 1단계)")
		void replyToReplyIsRejected() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(reply(POST_ID, 3L)));

			assertErrorCode(() -> commentService.create(POST_ID, USER_ID, new CommentCreateRequest("r", 5L)),
					ErrorCode.REPLY_DEPTH_EXCEEDED);
			verify(commentRepository, never()).save(any());
		}

	}

	@Nested
	@DisplayName("대댓글 목록")
	class Replies {

		@Test
		@DisplayName("댓글이 없으면 COMMENT_NOT_FOUND, 목록을 조회하지 않는다")
		void missingComment() {
			given(commentRepository.findById(5L)).willReturn(Optional.empty());

			assertErrorCode(() -> commentService.getReplies(POST_ID, 5L, null, 20), ErrorCode.COMMENT_NOT_FOUND);
			verifyNoInteractions(commentDao);
		}

		@Test
		@DisplayName("다른 게시글 경로로 요청하면 COMMENT_NOT_FOUND")
		void commentOfAnotherPost() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(topLevel(OTHER_POST_ID)));

			assertErrorCode(() -> commentService.getReplies(POST_ID, 5L, null, 20), ErrorCode.COMMENT_NOT_FOUND);
			verifyNoInteractions(commentDao);
		}

		@Test
		@DisplayName("cursor의 offset부터 size + 1건을 읽는다")
		void readsFromCursorOffsetWithOverfetch() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(topLevel(POST_ID)));
			given(commentDao.findReplies(anyLong(), anyLong(), anyInt())).willReturn(java.util.List.of());

			commentService.getReplies(POST_ID, 5L, OffsetCursor.encode("replies-5", 40), 20);

			verify(commentDao).findReplies(5L, 40L, 21);
		}

		@Test
		@DisplayName("다른 댓글에서 받은 cursor는 INVALID_CURSOR")
		void cursorOfAnotherComment() {
			given(commentRepository.findById(5L)).willReturn(Optional.of(topLevel(POST_ID)));

			assertErrorCode(() -> commentService.getReplies(POST_ID, 5L, OffsetCursor.encode("replies-6", 20), 20),
					ErrorCode.INVALID_CURSOR);
		}

	}

	private Comment captureSaved() {
		ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
		verify(commentRepository).save(captor.capture());
		return captor.getValue();
	}

	private static Comment topLevel(long postId) {
		return new Comment(postId, USER_ID, null, "parent");
	}

	private static Comment reply(long postId, long parentId) {
		return new Comment(postId, USER_ID, parentId, "reply");
	}

	static void assertErrorCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable call, ErrorCode expected) {
		assertThatThrownBy(call)
				.isInstanceOf(BusinessException.class)
				.extracting(ex -> ((BusinessException) ex).getErrorCode())
				.isEqualTo(expected);
	}

}
