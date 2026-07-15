import {describe, expect, it} from 'vitest'
import {
	centeredLoopOffset,
	circularCopyCount,
	middleCopyIndex,
	normalizeLoopOffset,
} from '@/util/circularMarquee'

describe('circular featured image marquee geometry', () => {
	it('creates enough odd-numbered copies for sparse images and wide viewports', () => {
		expect(circularCopyCount(1440, 400)).toBe(11)
		expect(circularCopyCount(2560, 410)).toBe(17)
		expect(circularCopyCount(1440, 3200)).toBe(5)
		expect(circularCopyCount(0, 0)).toBe(5)
	})

	it('centers the active copy inside the repeated track', () => {
		expect(middleCopyIndex(11)).toBe(5)
		expect(centeredLoopOffset(11, 400)).toBe(2000)
	})

	it('wraps either direction by complete set widths without changing visual phase', () => {
		const width = 400
		const copies = 11
		expect(normalizeLoopOffset(0, width, copies)).toBe(1600)
		expect(normalizeLoopOffset(1599, width, copies)).toBe(1999)
		expect(normalizeLoopOffset(2400, width, copies)).toBe(2000)
		expect(normalizeLoopOffset(4000, width, copies)).toBe(2000)
		expect(normalizeLoopOffset(2000, width, copies)).toBe(2000)
	})
})
