package com.example.HJO.controller;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.HJO.domain.PostSort;
import com.example.HJO.dto.request.PostCreateRequest;
import com.example.HJO.dto.response.PostCreateResponse;
import com.example.HJO.dto.response.PostDetailResponse;
import com.example.HJO.dto.response.PostPageResponse;
import com.example.HJO.global.auth.CurrentUserId;
import com.example.HJO.service.PostService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/posts")
public class PostController {

	private final PostService postService;

	public PostController(PostService postService) {
		this.postService = postService;
	}

	/** 게시글 작성 (배경 부하용). 인증 필요 */
	@PostMapping
	public ResponseEntity<PostCreateResponse> create(@CurrentUserId Long userId,
			@Valid @RequestBody PostCreateRequest request) {
		Long id = postService.create(userId, request);
		return ResponseEntity.created(URI.create("/posts/" + id)).body(new PostCreateResponse(id));
	}

	/** 목록. sort=latest|popular, 불투명 cursor, size 1~50 (기본 20). 인증 없음 */
	@GetMapping
	public PostPageResponse list(
			@RequestParam(defaultValue = "latest") PostSort sort,
			@RequestParam(required = false) String cursor,
			@RequestParam(defaultValue = "20") @Min(1) @Max(50) int size) {
		return postService.getPage(sort, cursor, size);
	}

	/** 상세: 게시글 + 최신 최상위 댓글 20개. 인증 없음 */
	@GetMapping("/{id}")
	public PostDetailResponse detail(@PathVariable long id) {
		return postService.getDetail(id);
	}

}
