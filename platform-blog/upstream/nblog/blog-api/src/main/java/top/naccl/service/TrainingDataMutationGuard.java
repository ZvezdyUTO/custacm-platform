package top.naccl.service;

import com.custacm.platform.trainingdata.common.collector.OjCollectionExecutionCoordinator;
import com.custacm.platform.trainingdata.common.domain.oj.value.OjNames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import top.naccl.exception.ConflictException;

import java.util.ArrayList;
import java.util.List;

/**
 * 账号变更与 OJ 采集、刷新互斥，租约覆盖实际数据库提交或回滚。
 */
@Service
public class TrainingDataMutationGuard {
	private static final List<String> OJ_LOCK_ORDER = List.of(OjNames.ATCODER, OjNames.CODEFORCES);
	private final OjCollectionExecutionCoordinator coordinator;

	public TrainingDataMutationGuard(OjCollectionExecutionCoordinator coordinator) {
		this.coordinator = coordinator;
	}

	public void acquireUntilTransactionCompletes() {
		if (!TransactionSynchronizationManager.isActualTransactionActive()
				|| !TransactionSynchronizationManager.isSynchronizationActive()) {
			throw new IllegalStateException("Training data mutation requires an active transaction");
		}
		List<OjCollectionExecutionCoordinator.Permit> acquired = new ArrayList<>(OJ_LOCK_ORDER.size());
		try {
			for (String ojName : OJ_LOCK_ORDER) {
				acquired.add(coordinator.tryAcquire(ojName).orElseThrow(() ->
						new ConflictException("训练采集、刷新或账号变更正在进行，请稍后重试")));
			}
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCompletion(int status) {
					acquired.forEach(OjCollectionExecutionCoordinator.Permit::close);
				}
			});
		} catch (RuntimeException exception) {
			acquired.forEach(OjCollectionExecutionCoordinator.Permit::close);
			throw exception;
		}
	}
}
