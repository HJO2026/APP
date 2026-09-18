package com.example.HJO.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.HJO.dto.response.LikeResponse;
import com.example.HJO.global.auth.CurrentUserId;
import com.example.HJO.service.LikeService;

@RestController
public class LikeController {

	private final LikeService likeService;

	public LikeController(LikeService likeService) {
		this.likeService = likeService;
	}

	/** 좋아요. 인증 필요. 처음과 재요청 모두 200 + 현재 상태(멱등) */
	@PostMapping("/posts/{id}/likes")
	public LikeResponse like(@PathVariable long id, @CurrentUserId Long userId) {
		return likeService.like(id, userId);
	}

}
