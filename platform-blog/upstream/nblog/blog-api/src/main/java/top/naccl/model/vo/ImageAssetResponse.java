package top.naccl.model.vo;

import top.naccl.entity.ImageAsset;

/**
 * @author huangbingrui.awa
 */
public record ImageAssetResponse(
		Long id,
		String publicId,
		String purpose,
		String originalUrl,
		String thumbnailUrl,
		Integer width,
		Integer height,
		Long originalBytes,
		Long thumbnailBytes
) {
	public ImageAssetResponse(ImageAsset asset) {
		this(asset.getId(), asset.getPublicId(), asset.getPurpose(), asset.getOriginalUrl(), asset.getThumbnailUrl(),
				asset.getWidth(), asset.getHeight(), asset.getOriginalBytes(), asset.getThumbnailBytes());
	}
}
