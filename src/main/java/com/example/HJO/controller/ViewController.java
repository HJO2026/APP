package com.example.HJO.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.HJO.dto.request.ViewRequest;
import com.example.HJO.dto.response.ViewResponse;
import com.example.HJO.global.auth.CurrentUserId;
import com.example.HJO.service.ViewService;

import jakarta.validation.Valid;

@RestController
public class ViewController {

	private final ViewService viewService;

	public ViewController(ViewService viewService) {
		this.viewService = viewService;
	}

	/** 조회 이벤트 기록. 인증 필요. 중복 eventId도 200(counted=false) */
	@PostMapping("/posts/{id}/views")
	public ViewResponse record(@PathVariable long id, @CurrentUserId Long userId,
			@Valid @RequestBody ViewRequest request) {
		return new ViewResponse(viewService.record(id, userId, request));
	}

}
