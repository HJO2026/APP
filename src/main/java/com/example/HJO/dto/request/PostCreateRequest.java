package com.example.HJO.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * 게시글 작성 요청. 작성자는 토큰의 sub에서 가져온다.
 */
public record PostCreateRequest(
		@NotNull @Positive Long boardId,
		@NotBlank @Size(max = 200) String title,
		@NotBlank @Size(max = 10_000) String content) {
}
