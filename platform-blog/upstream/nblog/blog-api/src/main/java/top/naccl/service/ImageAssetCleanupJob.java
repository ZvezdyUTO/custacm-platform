package top.naccl.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author huangbingrui.awa
 */
@Component
public class ImageAssetCleanupJob {
	private final ImageAssetService imageAssetService;
	private final HomepageFeaturedImageService homepageFeaturedImageService;

	public ImageAssetCleanupJob(
			ImageAssetService imageAssetService,
			HomepageFeaturedImageService homepageFeaturedImageService
	) {
		this.imageAssetService = imageAssetService;
		this.homepageFeaturedImageService = homepageFeaturedImageService;
	}

	@Scheduled(cron = "${image.assets.cleanup-cron:0 25 3 * * *}")
	public void cleanup() {
		imageAssetService.cleanupStaleAssets();
		homepageFeaturedImageService.cleanupOrphanFiles();
	}
}
