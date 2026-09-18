package com.example.HJO.common.auth;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.DispatcherType;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						// 헬스체크와 지표 수집은 인증 없이 (공통 인프라의 healthcheck, Prometheus 스크레이프)
						.requestMatchers(EndpointRequest.to("health", "prometheus")).permitAll()
						// 인증된 요청의 에러 포워드(/error)가 401로 바뀌지 않게 한다
						.dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
						// TODO(team): GET API도 인증을 요구할지 미정. 잠정으로 모든 API에 인증 필요
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()));
		return http.build();
	}

}
