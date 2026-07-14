// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const css = readFileSync(resolve(process.cwd(), 'src/assets/css/blog-redesign.css'), 'utf8')

describe('Blog redesign stylesheet contract', () => {
	it('maps Element Plus and project-owned Blog roles to the shared semantic tokens', () => {
		expect(css).toContain('--el-color-primary: var(--color-action)')
		expect(css).toContain('--theme-canvas: var(--color-canvas-alternate)')
		expect(css).toContain('font-family: var(--font-sans)')
	})

	it('limits glass to navigation, floating menus and dialogs', () => {
		expect(css).toContain('.site-nav:not(.transparent)')
		expect(css).toContain('.el-dropdown__popper.el-popper')
		expect(css).toContain('.crop-dialog')
		expect(css).toContain('backdrop-filter: var(--glass-filter)')
		expect(css).toMatch(/\.blog-list-card,[\s\S]*background: var\(--color-surface\) !important;/)
	})

	it('provides a solid fallback and reduced-motion behavior', () => {
		expect(css).toContain('@supports not ((-webkit-backdrop-filter: blur(1px)) or (backdrop-filter: blur(1px)))')
		expect(css).toContain('@media (prefers-reduced-motion: reduce)')
		expect(css).toContain('transition: none !important')
	})

	it('uses a rounded neutral page scrollbar in every Blog route', () => {
		expect(css).toContain('body::-webkit-scrollbar-thumb')
		expect(css).toContain('html.training-host-active body::-webkit-scrollbar-thumb')
		expect(css).toContain('border-radius: 999px')
		expect(css).toContain('background: #c7c7cc')
	})

	it('suppresses the native blue search input outline while retaining a neutral wrapper focus state', () => {
		expect(css).toContain('.m-search input:focus-visible')
		expect(css).toContain('outline: none !important')
		expect(css).toContain('.m-search .el-input__wrapper:focus-within')
		expect(css).toContain('var(--color-border-strong) inset')
	})

	it('keeps the search background on the rounded wrapper instead of the inner rectangular input', () => {
		expect(css).toMatch(/\.m-search \.el-input__wrapper \{[\s\S]*overflow: hidden;[\s\S]*background: rgba\(255, 255, 255, \.94\) !important;/)
		expect(css).toMatch(/\.m-search \.el-input__inner \{[\s\S]*background: transparent !important;/)
	})

	it('does not draw the global blue focus rectangle inside the navigation bar', () => {
		expect(css).toContain('.site-nav :where(a, button, input, [tabindex]):focus-visible')
		expect(css).toMatch(/\.site-nav :where\(a, button, input, \[tabindex\]\):focus-visible \{[\s\S]*outline: none !important;/)
	})
})
