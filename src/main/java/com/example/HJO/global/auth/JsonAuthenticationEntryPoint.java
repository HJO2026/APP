package com.example.HJO.global.auth;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.example.HJO.global.error.ErrorCode;
import com.example.HJO.global.error.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 인증 필터에서 나는 401을 공통 에러 형식(ErrorResponse)으로 응답한다.
 * 상태 코드와 WWW-Authenticate 헤더는 표준 Bearer 진입점에 맡기고 본문만 채운다.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final AuthenticationEntryPoint bearer = new BearerTokenAuthenticationEntryPoint();
	private final ObjectMapper objectMapper;

	public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException, ServletException {
		bearer.commence(request, response, authException);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding("UTF-8");
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(ErrorCode.UNAUTHORIZED));
	}

}
