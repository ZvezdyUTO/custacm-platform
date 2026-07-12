package top.naccl.util;

import org.junit.jupiter.api.Test;
import top.naccl.model.dto.Blog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author huangbingrui.awa
 */
class BlogContentLimitsTest {
	@Test
	void acceptsFieldsAtTheirLimitsIncludingSupplementaryCharacters() {
		Blog blog = blog("题".repeat(99) + "😀", "简".repeat(255), "文".repeat(200_000));

		assertNull(BlogContentLimits.validate(blog));
	}

	@Test
	void reportsEachOverlongField() {
		assertEquals("文章标题不能超过 100 字", BlogContentLimits.validate(blog("题".repeat(101), "简介", "正文")));
		assertEquals("文章简介不能超过 255 字", BlogContentLimits.validate(blog("标题", "简".repeat(256), "正文")));
		assertEquals("文章正文不能超过 200000 字", BlogContentLimits.validate(blog("标题", "简介", "文".repeat(200_001))));
		assertEquals("文章参数有误", BlogContentLimits.validate(null));
	}

	private Blog blog(String title, String description, String content) {
		Blog blog = new Blog();
		blog.setTitle(title);
		blog.setDescription(description);
		blog.setContent(content);
		return blog;
	}
}
