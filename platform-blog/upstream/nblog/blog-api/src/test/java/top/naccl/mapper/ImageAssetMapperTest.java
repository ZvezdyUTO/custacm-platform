package top.naccl.mapper;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import top.naccl.entity.ImageAsset;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageAssetMapperTest {
	private static final Instant CUTOFF = Instant.parse("2026-09-14T00:00:00Z");
	private JdbcTemplate jdbc;
	private SqlSessionFactory sessions;

	@BeforeEach
	void setUp() {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:image-assets-" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1");
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE image_asset (id BIGINT PRIMARY KEY, status VARCHAR(16), create_time TIMESTAMP, update_time TIMESTAMP)");
		jdbc.execute("CREATE TABLE blog_image_reference (image_asset_id BIGINT PRIMARY KEY, blog_id BIGINT, role VARCHAR(16))");
		Configuration configuration = new Configuration(new Environment(
				"test", new JdbcTransactionFactory(), dataSource));
		configuration.setMapUnderscoreToCamelCase(true);
		configuration.addMapper(ImageAssetMapper.class);
		sessions = new SqlSessionFactoryBuilder().build(configuration);
	}

	@Test
	void cleanupSelectsOnlyDeletingOrExpiredTemporaryAssets() {
		insert(jdbc, 1L, "TEMP", CUTOFF.minusSeconds(1));
		insert(jdbc, 2L, "TEMP", CUTOFF);
		insert(jdbc, 3L, "TEMP", CUTOFF.plusSeconds(1));
		insert(jdbc, 4L, "ACTIVE", CUTOFF.minusSeconds(1));
		insert(jdbc, 5L, "DELETING", CUTOFF.plusSeconds(1));
		try (SqlSession session = sessions.openSession()) {
			List<ImageAsset> candidates = session.getMapper(ImageAssetMapper.class)
					.findCleanupCandidates(Date.from(CUTOFF));
			assertEquals(List.of(1L, 5L), candidates.stream().map(ImageAsset::getId).toList());
		}
	}

	@Test
	void activationAndCleanupCannotBothClaimAnExpiredAsset() {
		insert(jdbc, 1L, "TEMP", CUTOFF.minusSeconds(1));
		insert(jdbc, 2L, "TEMP", CUTOFF.minusSeconds(1));
		try (SqlSession session = sessions.openSession()) {
			ImageAssetMapper mapper = session.getMapper(ImageAssetMapper.class);
			assertEquals(1, mapper.activateIfBindable(1L));
			assertEquals(0, mapper.markCleanupCandidateDeleting(1L, Date.from(CUTOFF)));
			assertEquals(0, mapper.markUnboundDeleting(1L));
			assertEquals(1, mapper.markCleanupCandidateDeleting(2L, Date.from(CUTOFF)));
			assertEquals(0, mapper.activateIfBindable(2L));
			assertEquals("ACTIVE", mapper.findById(1L).getStatus());
			assertEquals("DELETING", mapper.findById(2L).getStatus());
		}
	}

	@Test
	void cleanupCannotClaimFreshOrStillReferencedAssets() {
		insert(jdbc, 1L, "TEMP", CUTOFF);
		insert(jdbc, 2L, "DELETING", CUTOFF.minusSeconds(1));
		jdbc.update("INSERT INTO blog_image_reference (image_asset_id, blog_id) VALUES (2, 7)");
		try (SqlSession session = sessions.openSession()) {
			ImageAssetMapper mapper = session.getMapper(ImageAssetMapper.class);
			assertEquals(0, mapper.markCleanupCandidateDeleting(1L, Date.from(CUTOFF)));
			assertEquals(0, mapper.markCleanupCandidateDeleting(2L, Date.from(CUTOFF)));
			assertEquals(0, mapper.markUnboundDeleting(2L));
		}
	}

	private static void insert(JdbcTemplate jdbc, long id, String status, Instant createdAt) {
		jdbc.update("INSERT INTO image_asset (id, status, create_time) VALUES (?, ?, ?)",
				id, status, Timestamp.from(createdAt));
	}
}
