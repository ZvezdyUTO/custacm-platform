package top.naccl.model.vo;

import org.junit.jupiter.api.Test;
import top.naccl.util.JacksonUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class BlogInfoTest {

	@Test
	void ignoresFieldsLeftByAnOlderRedisCacheSchema() {
		BlogInfo blogInfo = JacksonUtils.convertValue(Map.of(
				"id", 7L,
				"title", "cached blog",
				"password", "removed-field"
		), BlogInfo.class);

		assertThat(blogInfo.getId()).isEqualTo(7L);
		assertThat(blogInfo.getTitle()).isEqualTo("cached blog");
	}
}
