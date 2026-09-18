package com.example.HJO.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.HJO.dto.request.CommentCreateRequest;
import com.example.HJO.dto.response.CursorPageResponse;
import com.example.HJO.dto.response.IdResponse;
import com.example.HJO.dto.response.ReplyResponse;
import com.example.HJO.global.auth.CurrentUserId;
import com.example.HJO.service.CommentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/posts/{postId}/comments")
public class CommentController {

	private final CommentService commentService;

	public CommentController(CommentService commentService) {
		this.commentService = commentService;
	}

	/** 댓글 작성. parentId가 있으면 대댓글. 인증 필요. 단건 조회 API가 없어서 Location은 주지 않는다 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public IdResponse create(@PathVariable long postId, @CurrentUserId Long userId,
			@Valid @RequestBody CommentCreateRequest request) {
		return new IdResponse(commentService.create(postId, userId, request));
	}

	/** 대댓글 목록. 오래된 순, 불투명 cursor, size 1~50 (기본 20). 인증 없음 */
	@GetMapping("/{commentId}/replies")
	public CursorPageResponse<ReplyResponse> replies(
			@PathVariable long postId,
			@PathVariable long commentId,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return commentService.getReplies(postId, commentId, cursor, size);
	}

}
