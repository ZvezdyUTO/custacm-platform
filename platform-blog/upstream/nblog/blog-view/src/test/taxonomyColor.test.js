// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {taxonomyStyle, taxonomyTextColor} from '@/util/taxonomyColor'

function luminance(hex) {
	const channels = [1, 3, 5].map(offset => parseInt(hex.slice(offset, offset + 2), 16) / 255)
	return channels.map(value => value <= .04045 ? value / 12.92 : ((value + .055) / 1.055) ** 2.4)
		.reduce((sum, value, index) => sum + value * [.2126, .7152, .0722][index], 0)
}

describe('taxonomy label readability', () => {
	it('preserves each user color and selects a foreground with AA contrast', () => {
		for (const red of [0, 51, 102, 153, 204, 255]) {
			for (const green of [0, 51, 102, 153, 204, 255]) {
				for (const blue of [0, 51, 102, 153, 204, 255]) {
					const background = `#${[red, green, blue].map(value => value.toString(16).padStart(2, '0')).join('')}`
					const style = taxonomyStyle(background)
					const levels = [luminance(style.color), luminance(background)].sort((a, b) => a - b)
					expect(style.backgroundColor).toBe(background)
					expect((levels[1] + .05) / (levels[0] + .05)).toBeGreaterThanOrEqual(4.5)
				}
			}
		}
	})

	it('keeps lime and dark legacy labels readable without changing their hue', () => {
		expect(taxonomyTextColor('#CEFF50')).toBe('#000000')
		expect(taxonomyTextColor('#17324d')).toBe('#ffffff')
		expect(taxonomyStyle('').backgroundColor).toBe('#8B1E3F')
		expect(taxonomyStyle('hsl(120 70% 24%)').color).toBe('#ffffff')
	})
})
