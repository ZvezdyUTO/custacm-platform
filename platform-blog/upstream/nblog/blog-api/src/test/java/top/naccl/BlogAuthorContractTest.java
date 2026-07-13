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

	@Test
	void publicBlogResponsesIncludeArticleCover() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("<result property=\"firstPicture\" column=\"first_picture\"/>"));
		assertTrue(mapper.contains("b.title, b.first_picture"));
	}

	@Test
	void normalReadAndWriteQueriesExcludeRecycleBinArticles() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("where b.user_id=#{userId} and b.deleted_at is null"));
		assertTrue(mapper.contains("where b.is_published=true and b.deleted_at is null"));
		assertTrue(mapper.contains("where b.id=#{id} and b.is_published=true and b.deleted_at is null"));
		assertTrue(mapper.contains("where id=#{id} and deleted_at is null"));
		assertTrue(mapper.contains("select is_comment_enabled from blog where id=#{blogId} and deleted_at is null"));
	}

	@Test
	void recycleBinRestoreAndCleanupQueriesEnforceTheSevenDayBoundary() throws IOException {
		String mapper = new String(getClass().getResourceAsStream("/mapper/BlogMapper.xml").readAllBytes(), StandardCharsets.UTF_8);

		assertTrue(mapper.contains("b.deleted_at is not null and b.deleted_at &gt; #{cutoff}"));
		assertTrue(mapper.contains("user_id=#{userId} and deleted_at is not null and deleted_at &gt; #{cutoff}"));
		assertTrue(mapper.contains("deleted_at is not null and deleted_at &lt;= #{cutoff}"));
		assertTrue(mapper.contains("order by deleted_at, id for update"));
	}
}
