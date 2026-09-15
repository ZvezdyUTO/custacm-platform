// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'
import {buildTrainingFrameSource, buildTrainingHostPath, isAllowedTrainingRoutePath} from '../utils/trainingRoute'

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

	it('opens a fresh training entry after a build while preserving login return paths', () => {
		const query = {returnTo: '/training/admin/categories', _build: 'old-build'}
		const first = new URL(buildTrainingFrameSource('login', query, 'release-a'), 'https://custacm.top')
		const next = new URL(buildTrainingFrameSource('login', query, 'release-b'), 'https://custacm.top')
		expect(first.pathname).toBe('/training-app/login')
		expect(first.searchParams.get('returnTo')).toBe('/training/admin/categories')
		expect(first.searchParams.get('_build')).toBe('release-a')
		expect(next.href).not.toBe(first.href)
		expect(buildTrainingFrameSource('multiple', {_build: 'old-build'})).toBe('/training-app/multiple')
	})

	it('keeps build metadata out of the public URL without widening the route allowlist', () => {
		expect(buildTrainingHostPath('/admin/categories?_build=release-a&page=2')).toBe('/training/admin/categories?page=2')
		expect(buildTrainingHostPath('/multiple?_build=release-a')).toBe('/training/multiple')
		const login = new URL(buildTrainingHostPath('/login?returnTo=%2Fprofile&_build=release-a'), 'https://custacm.top')
		expect(login.searchParams.get('returnTo')).toBe('/profile')
		expect(buildTrainingHostPath('/admin/unknown?_build=release-a')).toBeNull()
		expect(buildTrainingHostPath('//evil.example/multiple')).toBeNull()
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
