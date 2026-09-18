package com.example.HJO.dto.response;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CursorPageResponseTest {

	@Test
	@DisplayName("size + 1건을 읽었으면 size건만 돌려주고 nextCursor를 붙인다")
	void overfetchedMeansHasNext() {
		CursorPageResponse<Integer> page = CursorPageResponse.fromOverfetched(List.of(1, 2, 3), 2, "next");

		assertThat(page.items()).containsExactly(1, 2);
		assertThat(page.nextCursor()).isEqualTo("next");
	}

	@Test
	@DisplayName("size건 이하면 마지막 페이지다 (nextCursor null)")
	void exactSizeIsLastPage() {
		CursorPageResponse<Integer> page = CursorPageResponse.fromOverfetched(List.of(1, 2), 2, "next");

		assertThat(page.items()).containsExactly(1, 2);
		assertThat(page.nextCursor()).isNull();
	}

	@Test
	@DisplayName("빈 결과는 빈 목록과 nextCursor null")
	void emptyIsLastPage() {
		CursorPageResponse<Integer> page = CursorPageResponse.fromOverfetched(List.of(), 20, "next");

		assertThat(page.items()).isEmpty();
		assertThat(page.nextCursor()).isNull();
	}

}
