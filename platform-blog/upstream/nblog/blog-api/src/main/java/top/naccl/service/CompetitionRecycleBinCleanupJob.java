package top.naccl.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期物理清理已满七天的比赛回收站记录。
 *
 * @author huangbingrui.awa
 */
@Component
public class CompetitionRecycleBinCleanupJob {
	private final CompetitionService competitionService;

	public CompetitionRecycleBinCleanupJob(CompetitionService competitionService) {
		this.competitionService = competitionService;
	}

	@Scheduled(cron = "${blog.recycle-bin.cleanup-cron}")
	public void cleanup() {
		competitionService.purgeExpired();
	}
}
