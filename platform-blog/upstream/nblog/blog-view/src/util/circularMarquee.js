// Author: huangbingrui.awa

const MINIMUM_COPY_COUNT = 5

export function middleCopyIndex(copyCount) {
	return Math.floor(Math.max(MINIMUM_COPY_COUNT, copyCount) / 2)
}

export function circularCopyCount(viewportWidth, setWidth) {
	if (!(viewportWidth > 0) || !(setWidth > 0)) return MINIMUM_COPY_COUNT
	const required = Math.max(
		MINIMUM_COPY_COUNT,
		Math.ceil((viewportWidth * 2) / setWidth) + 3
	)
	return required % 2 === 0 ? required + 1 : required
}

export function centeredLoopOffset(copyCount, setWidth) {
	if (!(setWidth > 0)) return 0
	return middleCopyIndex(copyCount) * setWidth
}

export function normalizeLoopOffset(scrollOffset, setWidth, copyCount) {
	if (!Number.isFinite(scrollOffset) || !(setWidth > 0)) return 0
	const center = centeredLoopOffset(copyCount, setWidth)
	const lowerBound = center - setWidth
	const upperBound = center + setWidth
	if (scrollOffset < lowerBound) {
		return scrollOffset + Math.ceil((lowerBound - scrollOffset) / setWidth) * setWidth
	}
	if (scrollOffset >= upperBound) {
		return scrollOffset - (Math.floor((scrollOffset - upperBound) / setWidth) + 1) * setWidth
	}
	return scrollOffset
}
