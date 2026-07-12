// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import getPageTitle from '@/util/get-page-title'

describe('getPageTitle', () => {
	it('uses the custacm brand for route and fallback titles', () => {
		expect(getPageTitle('分类')).toBe('分类 - custacm-platpform')
		expect(getPageTitle()).toBe('custacm-platpform')
	})
})
