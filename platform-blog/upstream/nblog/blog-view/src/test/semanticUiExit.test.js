// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {existsSync, readFileSync, readdirSync} from 'node:fs'
import {resolve} from 'node:path'

const root = process.cwd()
const sourceRoot = resolve(root, 'src')
const productionSources = readdirSync(sourceRoot, {recursive: true})
	.filter(path => typeof path === 'string' && /\.(vue|js)$/.test(path) && !path.startsWith('test/'))
	.map(path => ({path, source: readFileSync(resolve(sourceRoot, path), 'utf8')}))
const mainSource = readFileSync(resolve(sourceRoot, 'main.js'), 'utf8')
const packageJson = JSON.parse(readFileSync(resolve(root, 'package.json'), 'utf8'))
const iconSource = readFileSync(resolve(sourceRoot, 'components/common/AppIcon.vue'), 'utf8')

describe('Semantic UI dependency exit', () => {
	it('removes Semantic UI layout classes and font icons from production templates', () => {
		for (const file of productionSources) {
			expect(file.source, file.path).not.toMatch(/class="[^"]*(?:^|\s)ui(?:\s|$)[^"]*"/)
			expect(file.source, file.path).not.toMatch(/<i\b/)
		}
	})

	it('uses the centralized Lucide mapping without loading Semantic UI CSS', () => {
		expect(mainSource).not.toContain('semantic-ui-css')
		expect(mainSource).not.toContain('assets/css/icon')
		expect(existsSync(resolve(sourceRoot, 'assets/css/icon'))).toBe(false)
		expect(iconSource).toContain("from '@lucide/vue'")
		expect(packageJson.dependencies).toHaveProperty('@lucide/vue')
		expect(packageJson.dependencies).not.toHaveProperty('semantic-ui-css')
	})
})
