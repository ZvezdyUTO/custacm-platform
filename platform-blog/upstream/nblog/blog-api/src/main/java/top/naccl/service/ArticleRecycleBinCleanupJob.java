package top.naccl.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期物理清理已满七天的回收站文章。
 *
 * @author huangbingrui.awa
 */
@Component
public class ArticleRecycleBinCleanupJob {
	private final ArticleRecycleBinService recycleBinService;

	public ArticleRecycleBinCleanupJob(ArticleRecycleBinService recycleBinService) {
		this.recycleBinService = recycleBinService;
	}

	@Scheduled(cron = "${blog.recycle-bin.cleanup-cron}")
	public void cleanup() {
		recycleBinService.purgeExpired();
	}
}
