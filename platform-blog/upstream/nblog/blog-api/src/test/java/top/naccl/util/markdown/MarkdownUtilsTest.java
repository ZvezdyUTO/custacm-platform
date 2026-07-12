package top.naccl.util.markdown;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownUtilsTest {

	@Test
	void removesExecutableHtmlAndDangerousUrls() {
		String markdown = "<img src=x onerror=alert(1)>"
				+ "<script>alert(2)</script>"
				+ "[bad](javascript:alert(3))"
				+ "[safe](https://example.com)";

		String html = MarkdownUtils.markdownToHtmlExtensions(markdown);

		assertThat(html)
				.doesNotContain("onerror", "<script", "javascript:")
				.contains("href=\"https://example.com\"");
	}

	@Test
	void keepsSupportedMarkdownFormattingAndSafeImageUrls() {
		String html = MarkdownUtils.markdownToHtmlExtensions(
				"# Heading\n\n| A | B |\n|---|---|\n| 1 | 2 |\n\n![image](/api/image/assets/id/thumbnail.jpg)");

		assertThat(html)
				.contains("<h1 id=\"heading\">Heading</h1>")
				.contains("class=\"ui celled table\"")
				.contains("data-src=\"/api/image/assets/id/thumbnail.jpg\"");
	}
}
