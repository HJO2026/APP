package com.example.HJO.schema;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.example.HJO.support.IntegrationTest;

/**
 * S2: 인덱스 마이그레이션(V2)을 빼도(PK만 안) ddl-auto=validate로 앱이 뜬다.
 * FLYWAY_LOCATIONS=classpath:db/migration 과 같은 설정이다. 별도 컨텍스트라 새 컨테이너(빈 DB)에서 돈다.
 */
@IntegrationTest(properties = "spring.flyway.locations=classpath:db/migration")
class SchemaWithoutIndexesTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void appliesOnlyV1AndHasNoBaselineIndexes() {
		List<String> versions = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank", String.class);
		List<String> indexes = jdbc.queryForList(
				"SELECT indexname FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx_%'", String.class);

		assertThat(versions).containsExactly("1");
		assertThat(indexes).isEmpty();
	}

}
