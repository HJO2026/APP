package com.example.HJO.support;

import static io.restassured.config.EncoderConfig.encoderConfig;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import io.restassured.RestAssured;
import io.restassured.config.RestAssuredConfig;

/**
 * 통합 테스트 공통 부모. HTTP 호출({@link ApiClient}), 준비 데이터({@link TestData}), 검증 조회({@link Db})를 제공한다.
 * <p>
 * 규칙 (운영 가이드):
 * - 블랙박스: 앱에는 HTTP로만 요청하고, 결과는 API 응답과 공통 테이블(JDBC)로 확인한다
 * - 격리: 테스트마다 자기 게시글·사용자·event_id를 만든다. 테이블 전체 초기화에 기대지 않는다
 * - 테스트 전용 API를 앱에 만들지 않는다. 사용자·게시판처럼 만드는 API가 없는 데이터만 JDBC로 준비한다
 * - 동시성 테스트에 @Transactional을 쓰지 않는다
 */
@ApiTest
public abstract class ApiTestSupport {

	@LocalServerPort
	int port;

	@Autowired
	protected JdbcTemplate jdbc;

	@Autowired
	protected JwtTestTokens tokens;

	protected ApiClient api;
	protected TestData data;
	protected Db db;

	@BeforeEach
	void setUpApiTestSupport() {
		RestAssured.port = port;
		RestAssured.config = RestAssuredConfig.config()
				.encoderConfig(encoderConfig().defaultContentCharset("UTF-8"));
		api = new ApiClient(tokens);
		data = new TestData(jdbc);
		db = new Db(jdbc);
	}

}
