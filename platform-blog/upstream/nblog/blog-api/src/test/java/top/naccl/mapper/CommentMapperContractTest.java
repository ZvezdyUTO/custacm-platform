package top.naccl.mapper;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author huangbingrui.awa
 */
class CommentMapperContractTest {
	@Test
	void replyTraversalIsAnchoredToTheCurrentRootPageAndDeduplicatesCycles() throws IOException {
		String mapper = resource("/mapper/CommentMapper.xml");
		String normalized = mapper.replaceAll("\\s+", " ").toLowerCase();

		assertTrue(normalized.contains("with recursive reply_ids (id) as"));
		assertTrue(normalized.contains("collection=\"rootcommentids\""));
		assertTrue(normalized.contains("c.parent_comment_id in"));
		assertTrue(normalized.contains("child.parent_comment_id=ancestor.id"));
		assertTrue(normalized.contains("union distinct"));
		assertTrue(normalized.contains("limit #{replylimit}"));
		assertTrue(normalized.contains("order by c1.id asc"));
		assertFalse(normalized.contains("and c1.parent_comment_id&lt;&gt;-1"));
	}

	@Test
	void commentTreeLookupHasAScopeAndParentIndex() throws IOException {
		String migration = resource("/db/migration/V042__index_comment_tree_queries.sql")
				.replaceAll("\\s+", " ").toLowerCase();

		assertTrue(migration.contains("idx_comment_scope_parent_published_created"));
		assertTrue(migration.contains(
				"(page, blog_id, parent_comment_id, is_published, create_time desc, id desc)"));
	}

	private String resource(String path) throws IOException {
		try (InputStream input = getClass().getResourceAsStream(path)) {
			if (input == null) {
				throw new IOException("Missing resource " + path);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
