package com.example.HJO.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청. parentId가 없으면 댓글, 있으면 그 댓글의 대댓글이다(깊이 1단계만 허용).
 * 작성자는 토큰의 sub에서 가져온다.
 */
public record CommentCreateRequest(
		@NotBlank @Size(max = 1_000) String content,
		@Positive Long parentId) {
}
