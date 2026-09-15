package top.naccl.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.naccl.config.properties.UploadProperties;
import top.naccl.entity.Blog;
import top.naccl.entity.ImageAsset;
import top.naccl.entity.User;
import top.naccl.mapper.BlogMapper;
import top.naccl.mapper.CommentMapper;
import top.naccl.mapper.ImageAssetMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ArticleArchiveTransactionTest {
	@TempDir Path uploadRoot;
	private final BlogMapper blogMapper = mock(BlogMapper.class);
	private final CommentMapper commentMapper = mock(CommentMapper.class);
	private final ImageAssetMapper assetMapper = mock(ImageAssetMapper.class);
	private final AtomicInteger snapshotReads = new AtomicInteger();
	private final AtomicReference<Connection> snapshotConnection = new AtomicReference<>();
	private HikariDataSource dataSource;
	private DataSourceTransactionManager transactionManager;
	private JdbcTemplate jdbc;
	private ArticleArchiveService archive;

	@BeforeEach
	void setUp() {
		HikariConfig pool = new HikariConfig();
		pool.setJdbcUrl("jdbc:h2:mem:archive-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
		pool.setMaximumPoolSize(1);
		pool.setMinimumIdle(0);
		pool.setConnectionTimeout(500);
		dataSource = new HikariDataSource(pool);
		transactionManager = new DataSourceTransactionManager(dataSource);
		jdbc = new JdbcTemplate(dataSource);
		jdbc.execute("CREATE TABLE snapshot_marker (version INT)");
		jdbc.update("INSERT INTO snapshot_marker VALUES (1)");
		UploadProperties properties = new UploadProperties();
		properties.setPath(uploadRoot.toString());
		ArticleArchiveSnapshotReader reader = transactional(
				new ArticleArchiveSnapshotReader(blogMapper, commentMapper, assetMapper));
		archive = transactional(new ArticleArchiveService(reader, properties, new ObjectMapper()));
	}

	@AfterEach
	void closePool() {
		dataSource.close();
	}

	@Test
	void fullBackupKeepsOneReadSnapshotAndReleasesItsOnlyConnectionBeforeSlowOutput() throws Exception {
		Blog blog = article();
		ImageAsset avatar = new ImageAsset();
		avatar.setId(9L);
		when(blogMapper.getAllBlogsForBackup()).thenAnswer(ignored -> {
			assertReadSnapshot();
			try (Connection writer = DriverManager.getConnection(dataSource.getJdbcUrl());
					var statement = writer.createStatement()) {
				statement.executeUpdate("UPDATE snapshot_marker SET version=2");
			}
			return List.of(blog);
		});
		when(commentMapper.getArticleBackupComments(List.of(5L))).thenAnswer(ignored -> {
			assertReadSnapshot();
			return List.of();
		});
		when(assetMapper.findByBlogIds(List.of(5L))).thenAnswer(ignored -> {
			assertReadSnapshot();
			return List.of();
		});
		when(assetMapper.findByIds(List.of(9L))).thenAnswer(ignored -> {
			assertReadSnapshot();
			return List.of(avatar);
		});

		assertConnectionAvailableDuringSlowOutput(archive::writeAllArticlesBackup, 4);
		assertEquals(2, jdbc.queryForObject("SELECT version FROM snapshot_marker", Integer.class));
	}

	@Test
	void singleArticleReleasesTheAssetReadTransactionBeforeSlowOutput() throws Exception {
		when(assetMapper.findByBlogId(5L)).thenAnswer(ignored -> {
			assertReadSnapshot();
			return List.of();
		});

		assertConnectionAvailableDuringSlowOutput(output -> archive.writeSingleArticle(article(), output), 1);
	}

	@Test
	void archiveRejectsAnOuterTransactionBeforeItCanRetainAConnectionDuringOutput() {
		TransactionTemplate transaction = new TransactionTemplate(transactionManager);
		assertThrows(IllegalTransactionStateException.class, () -> transaction.executeWithoutResult(ignored -> {
			try {
				archive.writeAllArticlesBackup(new ByteArrayOutputStream());
			} catch (IOException exception) {
				throw new AssertionError(exception);
			}
		}));
		verifyNoInteractions(blogMapper, commentMapper, assetMapper);
	}

	private void assertReadSnapshot() {
		assertTrue(TransactionSynchronizationManager.isActualTransactionActive());
		assertTrue(TransactionSynchronizationManager.isCurrentTransactionReadOnly());
		assertEquals(Connection.TRANSACTION_REPEATABLE_READ,
				TransactionSynchronizationManager.getCurrentTransactionIsolationLevel());
		Connection current = DataSourceUtils.getConnection(dataSource);
		try {
			if (!snapshotConnection.compareAndSet(null, current)) {
				assertSame(snapshotConnection.get(), current);
			}
			assertEquals(1, jdbc.queryForObject("SELECT version FROM snapshot_marker", Integer.class));
			snapshotReads.incrementAndGet();
		} finally {
			DataSourceUtils.releaseConnection(current, dataSource);
		}
	}

	private void assertConnectionAvailableDuringSlowOutput(ArchiveWriter writer, int expectedReads) throws Exception {
		SlowOutput output = new SlowOutput();
		try (var executor = Executors.newSingleThreadExecutor()) {
			var response = executor.submit(() -> {
				writer.write(output);
				return null;
			});
			try {
				assertTrue(output.started.await(5, TimeUnit.SECONDS), "Archive did not reach its output phase");
				assertEquals(expectedReads, snapshotReads.get());
				assertFalse(output.transactionActive, "Slow output retained a database transaction");
				assertFalse(output.connectionBound, "Slow output retained its database connection");
				try (Connection available = dataSource.getConnection()) {
					assertFalse(available.isClosed());
				}
			} finally {
				output.resume.countDown();
			}
			response.get(5, TimeUnit.SECONDS);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T transactional(T target) {
		TransactionInterceptor interceptor = new TransactionInterceptor();
		interceptor.setTransactionManager(transactionManager);
		interceptor.setTransactionAttributeSource(new AnnotationTransactionAttributeSource());
		ProxyFactory factory = new ProxyFactory(target);
		factory.setProxyTargetClass(true);
		factory.addAdvice(interceptor);
		return (T) factory.getProxy();
	}

	private static Blog article() {
		User author = new User();
		author.setId(7L);
		author.setAvatarAssetId(9L);
		Blog blog = new Blog();
		blog.setId(5L);
		blog.setTitle("文章");
		blog.setContent("正文");
		blog.setUser(author);
		return blog;
	}

	private final class SlowOutput extends OutputStream {
		private final CountDownLatch started = new CountDownLatch(1);
		private final CountDownLatch resume = new CountDownLatch(1);
		private boolean firstWrite = true;
		private boolean transactionActive;
		private boolean connectionBound;

		@Override
		public void write(int value) throws IOException {
			if (!firstWrite) return;
			firstWrite = false;
			transactionActive = TransactionSynchronizationManager.isActualTransactionActive();
			connectionBound = TransactionSynchronizationManager.hasResource(dataSource);
			started.countDown();
			try {
				if (!resume.await(5, TimeUnit.SECONDS)) throw new IOException("Slow output was not resumed");
			} catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new IOException(exception);
			}
		}
	}

	@FunctionalInterface
	private interface ArchiveWriter {
		void write(OutputStream output) throws Exception;
	}
}
