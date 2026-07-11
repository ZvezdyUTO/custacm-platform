/**
 * 首页横幅的相邻图层透明度计算。
 *
 * @author huangbingrui.awa
 */
const TWO_IMAGE_BLEND_START = 0.44
const TWO_IMAGE_BLEND_END = 0.56

export function homepageBannerOpacity(imageCount, pointerRatio, imageIndex) {
	if (imageCount <= 1) return imageIndex === 0 ? 1 : 0
	const ratio = Math.min(1, Math.max(0, pointerRatio))
	if (imageCount === 2) {
		if (ratio <= TWO_IMAGE_BLEND_START) return imageIndex === 0 ? 1 : 0
		if (ratio >= TWO_IMAGE_BLEND_END) return imageIndex === 1 ? 1 : 0
		const progress = (ratio - TWO_IMAGE_BLEND_START) / (TWO_IMAGE_BLEND_END - TWO_IMAGE_BLEND_START)
		const easedProgress = progress * progress * (3 - 2 * progress)
		return imageIndex === 0 ? 1 - easedProgress : imageIndex === 1 ? easedProgress : 0
	}
	const position = ratio * (imageCount - 1)
	return Math.max(0, 1 - Math.abs(imageIndex - position))
}

export function homepageBannerPointerRatio(clientX, left, width) {
	if (!Number.isFinite(width) || width <= 0) return 0
	return Math.min(1, Math.max(0, (clientX - left) / width))
}
