package top.naccl.service;

import com.custacm.platform.trainingdata.common.collector.OjCollectionExecutionCoordinator;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import top.naccl.exception.ConflictException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrainingDataMutationGuardTest {
	private final OjCollectionExecutionCoordinator coordinator = new OjCollectionExecutionCoordinator();
	private final TrainingDataMutationGuard guard = new TrainingDataMutationGuard(coordinator);
	private TransactionTemplate transaction;

	@BeforeEach
	void setUp() {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:training-mutation-" + UUID.randomUUID());
		transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
	}

	@Test
	void holdsBothOjPermitsThroughCommitAndReleasesAfterCompletion() {
		transaction.executeWithoutResult(status -> {
			guard.acquireUntilTransactionCompletes();
			assertBothOjPermitsHeld();
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void beforeCommit(boolean readOnly) {
					assertBothOjPermitsHeld();
				}

				@Override
				public void afterCommit() {
					assertBothOjPermitsHeld();
				}
			});
		});

		assertBothOjPermitsAvailable();
	}

	@Test
	void rollbackAlsoReleasesBothOjPermits() {
		assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
			guard.acquireUntilTransactionCompletes();
			throw new IllegalStateException("database mutation failed");
		}));

		assertBothOjPermitsAvailable();
	}

	@Test
	void busySecondOjReleasesTheFirstPermitWithoutWaitingForCollection() {
		try (var collecting = coordinator.tryAcquire(OjNames.CODEFORCES).orElseThrow()) {
			assertThrows(ConflictException.class,
					() -> transaction.executeWithoutResult(status -> guard.acquireUntilTransactionCompletes()));

			try (var atcoder = coordinator.tryAcquire(OjNames.ATCODER).orElseThrow()) {
				assertTrue(coordinator.tryAcquire(OjNames.CODEFORCES).isEmpty());
			}
		}
		assertBothOjPermitsAvailable();
	}

	@Test
	void requiresATransactionBeforeAcquiringAnyPermit() {
		assertThrows(IllegalStateException.class, guard::acquireUntilTransactionCompletes);

		assertBothOjPermitsAvailable();
	}

	private void assertBothOjPermitsHeld() {
		assertTrue(coordinator.tryAcquire(OjNames.ATCODER).isEmpty());
		assertTrue(coordinator.tryAcquire(OjNames.CODEFORCES).isEmpty());
	}

	private void assertBothOjPermitsAvailable() {
		try (var atcoder = coordinator.tryAcquire(OjNames.ATCODER).orElseThrow();
		     var codeforces = coordinator.tryAcquire(OjNames.CODEFORCES).orElseThrow()) {
			// Both permits can be acquired by the next collection or mutation.
		}
	}
}
