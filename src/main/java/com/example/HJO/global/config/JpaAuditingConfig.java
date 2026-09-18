package com.example.HJO.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * BaseCreatedEntity/BaseTimeEntity의 created_at, updated_at을 JPA 저장 시 자동으로 채운다.
 */
@Configuration(proxyBeanMethods = false)
@EnableJpaAuditing
public class JpaAuditingConfig {
}
