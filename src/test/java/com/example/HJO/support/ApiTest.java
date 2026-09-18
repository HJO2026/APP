package com.example.HJO.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

/**
 * 통합 테스트: 실제 서버(무작위 포트) + Testcontainers PostgreSQL.
 * 이 어노테이션을 쓰는 테스트는 컨텍스트(와 DB 컨테이너)를 공유하므로, 테스트마다 자기 데이터(게시글, 사용자)를 만들어 격리한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@IntegrationTest(webEnvironment = WebEnvironment.RANDOM_PORT)
public @interface ApiTest {
}
