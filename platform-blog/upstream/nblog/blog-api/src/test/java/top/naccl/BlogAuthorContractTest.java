package top.naccl;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author huangbingrui.awa
 */
class BlogAuthorContractTest {
	@Test
	void publicBlogDetailMapsAuthorUsernameForOwnerActions() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("property=\"authorUsername\" column=\"author_username\""));
		assertTrue(mapper.contains("u.username as author_username"));
	}

	@Test
	void publicBlogListMapsAuthorUsernameForArticleCards() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("<result property=\"authorUsername\" column=\"author_username\"/>"));
		assertTrue(mapper.contains("u.username as author_username, coalesce(u.nickname,'已注销用户') as author_nickname"));
	}
}
