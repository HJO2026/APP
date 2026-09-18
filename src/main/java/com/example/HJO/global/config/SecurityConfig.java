package com.example.HJO.global.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import com.example.HJO.global.auth.JsonAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
		JsonAuthenticationEntryPoint entryPoint = new JsonAuthenticationEntryPoint(objectMapper);
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// 헬스체크와 지표 수집은 인증 없이 (공통 인프라의 healthcheck, Prometheus 스크레이프)
						.requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
						// 인증된 요청의 에러 포워드(/error)가 401로 바뀌지 않게 한다
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						// 조회 API는 인증 없이 (2026-09-18 우주 결정, 팀 공유 필요)
						.requestMatchers(HttpMethod.GET, "/posts/**").permitAll()
						// 쓰기 API(작성, 조회수, 좋아요, 댓글)는 user id가 필요하므로 인증 필요
						.anyRequest().authenticated())
				.exceptionHandling(ex -> ex.authenticationEntryPoint(entryPoint))
				.oauth2ResourceServer(oauth2 -> oauth2
						.authenticationEntryPoint(entryPoint)
						.jwt(withDefaults()));
		return http.build();
	}

}
