// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'
import {buildTrainingFrameSource, isAllowedTrainingRoutePath} from '../utils/trainingRoute'

const trainingHostSource = readFileSync(resolve(process.cwd(), 'src/views/training/TrainingHost.vue'), 'utf8')

describe('training host route bridge', () => {
	it('opens the category and tag admin page in the embedded training app', () => {
		expect(buildTrainingFrameSource('admin/categories')).toBe('/training-app/admin/categories')
		expect(buildTrainingFrameSource('admin/competitions')).toBe('/training-app/admin/competitions')
	})

	it('accepts category route messages while rejecting unknown admin pages', () => {
		expect(isAllowedTrainingRoutePath('/admin/categories?page=2')).toBe(true)
		expect(isAllowedTrainingRoutePath('/admin/competitions?page=2')).toBe(true)
		expect(isAllowedTrainingRoutePath('/admin/unknown')).toBe(false)
	})

	it('keeps the frame in one viewport and forwards the active theme', () => {
		expect(trainingHostSource).toMatch(/\.training-host[\s\S]*box-sizing: border-box;[\s\S]*height: 100vh;[\s\S]*padding-top: 51px;/)
		expect(trainingHostSource).toContain('height: calc(100vh - 51px);')
		expect(trainingHostSource).toContain("document.documentElement.classList.add('training-host-active')")
		expect(trainingHostSource).toContain("{type: 'custacm:theme', theme}")
		expect(trainingHostSource).toContain('THEME_CHANGE_EVENT')
		expect(trainingHostSource).toContain('@load="syncThemeToFrame"')
	})
})
