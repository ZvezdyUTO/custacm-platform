package top.naccl.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author huangbingrui.awa
 */
@Component
public class ImageAssetCleanupJob {
	private final ImageAssetService imageAssetService;
	private final HomepageBannerService homepageBannerService;

	public ImageAssetCleanupJob(
			ImageAssetService imageAssetService,
			HomepageBannerService homepageBannerService
	) {
		this.imageAssetService = imageAssetService;
		this.homepageBannerService = homepageBannerService;
	}

	@Scheduled(cron = "${image.assets.cleanup-cron:0 25 3 * * *}")
	public void cleanup() {
		imageAssetService.cleanupStaleAssets();
		homepageBannerService.cleanupOrphanFiles();
	}
}
