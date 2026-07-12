package top.naccl.util;

import top.naccl.model.dto.Blog;

/**
 * 文章可编辑文本字段的统一长度约束。
 *
 * @author huangbingrui.awa
 */
public final class BlogContentLimits {
	public static final int TITLE_MAX_LENGTH = 100;
	public static final int DESCRIPTION_MAX_LENGTH = 255;
	public static final int CONTENT_MAX_LENGTH = 200_000;

	private BlogContentLimits() {
	}

	public static String validate(Blog blog) {
		if (blog == null) return "文章参数有误";
		if (length(blog.getTitle()) > TITLE_MAX_LENGTH) return "文章标题不能超过 " + TITLE_MAX_LENGTH + " 字";
		if (length(blog.getDescription()) > DESCRIPTION_MAX_LENGTH) return "文章简介不能超过 " + DESCRIPTION_MAX_LENGTH + " 字";
		if (length(blog.getContent()) > CONTENT_MAX_LENGTH) return "文章正文不能超过 " + CONTENT_MAX_LENGTH + " 字";
		return null;
	}

	private static int length(String value) {
		return value == null ? 0 : value.codePointCount(0, value.length());
	}
}
