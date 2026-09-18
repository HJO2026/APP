package com.example.HJO.support;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.test.context.ActiveProfiles;

/**
 * Testcontainers PostgreSQL + test 프로파일(테스트용 JWT 비밀키) + 테스트 토큰 유틸.
 * 설정이 같은 테스트끼리는 Spring 컨텍스트(와 컨테이너)를 공유한다.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@SpringBootTest
@ActiveProfiles("test")
@Import({ TestcontainersConfiguration.class, JwtTestTokens.class })
public @interface IntegrationTest {

	@AliasFor(annotation = SpringBootTest.class, attribute = "properties")
	String[] properties() default {};

	@AliasFor(annotation = SpringBootTest.class, attribute = "webEnvironment")
	SpringBootTest.WebEnvironment webEnvironment() default SpringBootTest.WebEnvironment.MOCK;

}
