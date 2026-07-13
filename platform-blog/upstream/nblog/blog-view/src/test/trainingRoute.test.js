// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {buildTrainingFrameSource, isAllowedTrainingRoutePath} from '../utils/trainingRoute'

describe('training host route bridge', () => {
	it('opens the category and tag admin page in the embedded training app', () => {
		expect(buildTrainingFrameSource('admin/categories')).toBe('/training-app/admin/categories')
	})

	it('accepts category route messages while rejecting unknown admin pages', () => {
		expect(isAllowedTrainingRoutePath('/admin/categories?page=2')).toBe(true)
		expect(isAllowedTrainingRoutePath('/admin/unknown')).toBe(false)
	})
})
