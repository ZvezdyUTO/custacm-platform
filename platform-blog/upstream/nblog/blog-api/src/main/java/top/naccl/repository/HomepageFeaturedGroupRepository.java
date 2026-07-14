package top.naccl.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 首页精选组与文章顺序的 JDBC 持久化适配器。
 *
 * @author huangbingrui.awa
 */
@Repository
public class HomepageFeaturedGroupRepository {
	private static final int ORDER_OFFSET = 1000;

	private final JdbcTemplate jdbcTemplate;

	public HomepageFeaturedGroupRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public List<FeaturedRow> findAllRows() {
		return jdbcTemplate.query("""
				SELECT featured_group.id AS group_id,
				       featured_group.title AS group_title,
				       featured_group.sort_order AS group_sort_order,
				       featured_article.blog_id AS article_id,
				       featured_article.sort_order AS article_sort_order,
				       blog.title AS article_title,
				       COALESCE(NULLIF(TRIM(blog.description), ''),
				                NULLIF(TRIM(LEFT(blog.content, 280)), ''),
				                '暂无简介') AS article_description,
				       blog.first_picture,
				       blog.create_time,
				       category.category_name,
				       author.username AS author_username,
				       COALESCE(author.nickname, '已注销用户') AS author_nickname,
				       author.avatar AS author_avatar,
				       CASE WHEN blog.id IS NOT NULL
				                  AND blog.is_published = true
				                  AND blog.is_internal = false
				                  AND blog.deleted_at IS NULL
				            THEN true ELSE false END AS available
				FROM homepage_featured_group AS featured_group
				LEFT JOIN homepage_featured_group_article AS featured_article
				       ON featured_article.group_id = featured_group.id
				LEFT JOIN blog ON blog.id = featured_article.blog_id
				LEFT JOIN category ON category.id = blog.category_id
				LEFT JOIN `user` AS author ON author.id = blog.user_id
				ORDER BY featured_group.sort_order, featured_group.id,
				         featured_article.sort_order, featured_article.blog_id
				""", (resultSet, rowNumber) -> new FeaturedRow(
				resultSet.getLong("group_id"),
				resultSet.getString("group_title"),
				resultSet.getInt("group_sort_order"),
				resultSet.getObject("article_id", Long.class),
				resultSet.getObject("article_title", String.class),
				resultSet.getObject("article_description", String.class),
				resultSet.getObject("first_picture", String.class),
				date(resultSet.getTimestamp("create_time")),
				resultSet.getObject("category_name", String.class),
				resultSet.getObject("author_username", String.class),
				resultSet.getObject("author_nickname", String.class),
				resultSet.getObject("author_avatar", String.class),
				resultSet.getObject("article_sort_order", Integer.class),
				resultSet.getBoolean("available")));
	}

	public List<CandidateRow> findCandidates(String query, int limit) {
		String pattern = "%" + (query == null ? "" : query) + "%";
		return jdbcTemplate.query("""
				SELECT blog.id,
				       blog.title,
				       COALESCE(NULLIF(TRIM(blog.description), ''),
				                NULLIF(TRIM(LEFT(blog.content, 280)), ''),
				                '暂无简介') AS description,
				       blog.first_picture,
				       blog.create_time,
				       category.category_name,
				       author.username AS author_username,
				       COALESCE(author.nickname, '已注销用户') AS author_nickname,
				       author.avatar AS author_avatar,
				       featured_article.sort_order,
				       featured_article.group_id AS featured_group_id
				FROM blog
				LEFT JOIN category ON category.id = blog.category_id
				LEFT JOIN `user` AS author ON author.id = blog.user_id
				LEFT JOIN homepage_featured_group_article AS featured_article
				       ON featured_article.blog_id = blog.id
				WHERE blog.is_published = true
				  AND blog.is_internal = false
				  AND blog.deleted_at IS NULL
				  AND blog.title LIKE ?
				ORDER BY blog.update_time DESC, blog.id DESC
				LIMIT ?
				""", (resultSet, rowNumber) -> new CandidateRow(
				resultSet.getLong("id"),
				resultSet.getString("title"),
				resultSet.getString("description"),
				resultSet.getString("first_picture"),
				date(resultSet.getTimestamp("create_time")),
				resultSet.getString("category_name"),
				resultSet.getString("author_username"),
				resultSet.getString("author_nickname"),
				resultSet.getString("author_avatar"),
				resultSet.getObject("sort_order", Integer.class),
				resultSet.getObject("featured_group_id", Long.class)), pattern, limit);
	}

	public int countGroups() {
		Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM homepage_featured_group", Integer.class);
		return count == null ? 0 : count;
	}

	public boolean existsGroup(long id) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM homepage_featured_group WHERE id = ?", Integer.class, id);
		return count != null && count == 1;
	}

	public Set<Long> findAvailableBlogIds(List<Long> blogIds) {
		if (blogIds == null || blogIds.isEmpty()) {
			return Set.of();
		}
		String placeholders = placeholders(blogIds.size());
		return new LinkedHashSet<>(jdbcTemplate.queryForList("""
				SELECT id
				FROM blog
				WHERE id IN (%s)
				  AND is_published = true
				  AND is_internal = false
				  AND deleted_at IS NULL
				""".formatted(placeholders), Long.class, blogIds.toArray()));
	}

	public Map<Long, Long> findAssignments(List<Long> blogIds) {
		if (blogIds == null || blogIds.isEmpty()) {
			return Map.of();
		}
		String placeholders = placeholders(blogIds.size());
		Map<Long, Long> assignments = new LinkedHashMap<>();
		RowMapper<Map.Entry<Long, Long>> assignmentMapper = (resultSet, rowNumber) -> Map.entry(
				resultSet.getLong("blog_id"), resultSet.getLong("group_id"));
		List<Map.Entry<Long, Long>> rows = jdbcTemplate.query("""
				SELECT blog_id, group_id
				FROM homepage_featured_group_article
				WHERE blog_id IN (%s)
				""".formatted(placeholders), assignmentMapper, blogIds.toArray());
		rows.forEach(row -> assignments.put(row.getKey(), row.getValue()));
		return assignments;
	}

	public long insertGroup(String title, int sortOrder) {
		KeyHolder keyHolder = new GeneratedKeyHolder();
		jdbcTemplate.update(connection -> {
			PreparedStatement statement = connection.prepareStatement(
					"INSERT INTO homepage_featured_group (title, sort_order) VALUES (?, ?)",
					Statement.RETURN_GENERATED_KEYS);
			statement.setString(1, title);
			statement.setInt(2, sortOrder);
			return statement;
		}, keyHolder);
		Number key = keyHolder.getKey();
		if (key == null) {
			throw new IllegalStateException("首页精选组写入后未返回主键");
		}
		return key.longValue();
	}

	public int updateGroup(long id, String title) {
		return jdbcTemplate.update(
				"UPDATE homepage_featured_group SET title = ? WHERE id = ?", title, id);
	}

	public int deleteGroup(long id) {
		return jdbcTemplate.update("DELETE FROM homepage_featured_group WHERE id = ?", id);
	}

	public List<Long> findGroupIdsInOrder() {
		return jdbcTemplate.queryForList(
				"SELECT id FROM homepage_featured_group ORDER BY sort_order, id", Long.class);
	}

	public void replaceGroupOrder(List<Long> ids) {
		jdbcTemplate.update("UPDATE homepage_featured_group SET sort_order = sort_order + ?", ORDER_OFFSET);
		for (int index = 0; index < ids.size(); index++) {
			jdbcTemplate.update("UPDATE homepage_featured_group SET sort_order = ? WHERE id = ?", index, ids.get(index));
		}
	}

	public void replaceArticles(long groupId, List<Long> articleIds) {
		jdbcTemplate.update("DELETE FROM homepage_featured_group_article WHERE group_id = ?", groupId);
		for (int index = 0; index < articleIds.size(); index++) {
			jdbcTemplate.update("""
					INSERT INTO homepage_featured_group_article (group_id, blog_id, sort_order)
					VALUES (?, ?, ?)
					""", groupId, articleIds.get(index), index);
		}
	}

	private static String placeholders(int count) {
		return String.join(",", java.util.Collections.nCopies(count, "?"));
	}

	private static Date date(java.sql.Timestamp timestamp) {
		return timestamp == null ? null : new Date(timestamp.getTime());
	}

	public record FeaturedRow(
			Long groupId,
			String groupTitle,
			Integer groupSortOrder,
			Long articleId,
			String articleTitle,
			String articleDescription,
			String firstPicture,
			Date createTime,
			String categoryName,
			String authorUsername,
			String authorNickname,
			String authorAvatar,
			Integer articleSortOrder,
			boolean available
	) {
	}

	public record CandidateRow(
			Long id,
			String title,
			String description,
			String firstPicture,
			Date createTime,
			String categoryName,
			String authorUsername,
			String authorNickname,
			String authorAvatar,
			Integer sortOrder,
			Long featuredGroupId
	) {
	}
}
