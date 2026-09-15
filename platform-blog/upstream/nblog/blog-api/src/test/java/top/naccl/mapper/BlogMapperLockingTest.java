package top.naccl.mapper;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbc.JdbcSQLTimeoutException;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import top.naccl.entity.Blog;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BlogMapperLockingTest {
	private static final Instant CREATED = Instant.parse("2026-09-14T00:00:00Z");
	private JdbcDataSource dataSource;
	private SqlSessionFactory sessions;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:blog-lock-" + UUID.randomUUID()
				+ ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=100");
		JdbcTemplate jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("""
				CREATE TABLE blog (
				  id BIGINT PRIMARY KEY, user_id BIGINT, create_time TIMESTAMP, deleted_at TIMESTAMP,
				  is_top BOOLEAN, is_recommend BOOLEAN, is_appreciation BOOLEAN
				)
				""");
		jdbc.update("""
				INSERT INTO blog (id, user_id, create_time, is_top, is_recommend, is_appreciation)
				VALUES (10, 7, ?, true, false, true)
				""", Timestamp.from(CREATED));
		jdbc.update("INSERT INTO blog (id, user_id, deleted_at) VALUES (11, 7, ?)", Timestamp.from(CREATED));
		Configuration configuration = new Configuration(new Environment(
				"test", new JdbcTransactionFactory(), dataSource));
		try (InputStream mapper = getClass().getResourceAsStream("/mapper/BlogMapper.xml")) {
			new XMLMapperBuilder(mapper, configuration, "mapper/BlogMapper.xml",
					configuration.getSqlFragments()).parse();
		}
		sessions = new SqlSessionFactoryBuilder().build(configuration);
	}

	@Test
	void loadsOnlyTheOwnedArticlesServerControlledFields() {
		try (SqlSession session = sessions.openSession()) {
			Blog stored = session.getMapper(BlogMapper.class).getOwnedBlogForUpdate(10L, 7L);

			assertEquals(10L, stored.getId());
			assertEquals(CREATED.toEpochMilli(), stored.getCreateTime().getTime());
			assertEquals(true, stored.getTop());
			assertEquals(false, stored.getRecommend());
			assertEquals(true, stored.getAppreciation());
			assertNull(stored.getContent());
		}
	}

	@Test
	void cannotLockAnotherAuthorsOrDeletedArticleForEditing() {
		try (SqlSession session = sessions.openSession()) {
			BlogMapper mapper = session.getMapper(BlogMapper.class);
			assertNull(mapper.getOwnedBlogForUpdate(10L, 8L));
			assertNull(mapper.getOwnedBlogForUpdate(11L, 7L));
			assertNull(mapper.getOwnedBlogForUpdate(99L, 7L));
		}
	}

	@Test
	void keepsConcurrentWritesOutUntilTheArticleTransactionCommits() throws Exception {
		try (SqlSession session = sessions.openSession();
		     Connection concurrent = dataSource.getConnection();
		     PreparedStatement update = concurrent.prepareStatement("UPDATE blog SET is_top=false WHERE id=10")) {
			session.getMapper(BlogMapper.class).getOwnedBlogForUpdate(10L, 7L);

			assertThrows(JdbcSQLTimeoutException.class, update::executeUpdate);
			session.commit(true);
			assertEquals(1, update.executeUpdate());
		}
	}
}
