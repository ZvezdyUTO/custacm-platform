// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {dateFormat} from '@/util/dateTimeFormatUtils'

describe('Blog date formatting', () => {
	it('preserves the existing date format contract after removing Vue filters', () => {
		expect(dateFormat('2026-07-12T08:30:00+08:00', 'YYYY-MM-DD HH:mm')).toBe('2026-07-12 08:30')
	})
})
