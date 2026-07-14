// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const indexHtml = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')
const mainSource = readFileSync(resolve(process.cwd(), 'src/main.js'), 'utf8')
const nightCss = readFileSync(resolve(process.cwd(), 'src/assets/css/night.css'), 'utf8')
const tokensCss = readFileSync(resolve(process.cwd(), 'src/assets/css/tokens.css'), 'utf8')
const typoCss = readFileSync(resolve(process.cwd(), 'src/assets/css/typo.css'), 'utf8')
const headerSource = readFileSync(resolve(process.cwd(), 'src/components/index/Header.vue'), 'utf8')
const liveEditorSource = readFileSync(resolve(process.cwd(), 'src/components/article/LiveMarkdownEditor.vue'), 'utf8')

describe('night theme stylesheet contract', () => {
	it('uses the warm charcoal and copper palette across critical surfaces', () => {
		expect(tokensCss).toContain('--color-canvas: #171513')
		expect(tokensCss).toContain('--color-surface: #25211e')
		expect(tokensCss).toContain('--color-text: #f2ede7')
		expect(tokensCss).toContain('--color-text-muted: #c5bbb1')
		expect(tokensCss).toContain('--color-action: #c98542')
		expect(nightCss).toContain('--theme-canvas: var(--color-canvas)')
		expect(nightCss).toContain('--theme-accent: var(--color-action)')
	})

	it('uses a light syntax theme by day and restores the high-contrast palette at night', () => {
		expect(typoCss).toMatch(/\.typo pre\[class\*="language-"\][^{]*\{[^}]*background: #f6f8fa;/s)
		expect(typoCss).toContain('.typo .token.keyword')
		expect(typoCss).toContain('color: #cf222e;')
		expect(liveEditorSource).toMatch(/:deep\(\.cm-codeblock-widget\)[^{]*\{[^}]*background: #f6f8fa !important;/s)
		expect(liveEditorSource).toContain('color: #0550ae;')
		expect(nightCss).toMatch(/html\.dark \.typo pre\[class\*='language-'\][^{]*\{[^}]*background: #17130f;/s)
		expect(nightCss).toContain('html.dark .typo .token.keyword')
		expect(nightCss).toContain('html.dark .live-markdown-editor .cm-codeblock-widget .hljs-keyword')
	})

	it('covers vendor, content, editor, profile, comments, pagination and iframe surfaces', () => {
		for (const selector of [
			'.content-panel',
			'.el-dropdown-menu',
			'.blog-list-card',
			'.profile-page',
			'.article-editor-page',
			'.live-markdown-editor',
			'.emoji-box',
			'.el-pagination.is-background',
			'.m-toc .is-active-link',
			'.training-frame',
		]) {
			expect(nightCss).toContain(selector)
		}
	})

	it('keeps decorative waves smooth, dims images gradually and disables reduced motion', () => {
		expect(nightCss).toContain('header .wave1')
		expect(nightCss).toContain('background-image: none !important')
		expect(nightCss).toContain("mask-image: url('/img/header/wave1.png')")
		expect(nightCss).toContain("mask-image: url('/img/header/wave2.png')")
		expect(headerSource).toContain('background-color: color-mix(in srgb, var(--home-canvas, var(--color-canvas-alternate)) 72%, transparent)')
		expect(headerSource).toContain('background-color: var(--home-canvas, var(--color-canvas-alternate))')
		expect(headerSource).toContain("mask-image: url('/img/header/wave1.png')")
		expect(headerSource).toContain("mask-image: url('/img/header/wave2.png')")
		expect(nightCss).toContain('html.dark img')
		expect(nightCss).toContain('filter: brightness(.84) saturate(.95)')
		expect(nightCss).toContain('transition: filter var(--theme-image-duration) ease')
		expect(headerSource).toContain('transition: opacity .2s ease-in, filter var(--theme-image-duration, 260ms) ease')
		expect(nightCss).toContain('.article-download-link > .app-icon')
		expect(nightCss).toContain('--theme-duration: var(--duration-fast)')
		expect(nightCss).toContain('@media (prefers-reduced-motion: reduce)')
		expect(nightCss).toContain('transition: none !important')
	})

	it('preboots before styles and loads Element dark variables before the final override sheet', () => {
		expect(indexHtml.indexOf("localStorage.getItem('custacm.theme')")).toBeLessThan(indexHtml.indexOf('<link rel="icon"'))
		expect(indexHtml).toContain('root.dataset.theme = theme')
		expect(indexHtml).toContain("root.classList.toggle('dark', theme === 'dark')")
		expect(indexHtml).toContain('root.style.colorScheme = theme')
		expect(mainSource).toContain("element-plus/theme-chalk/dark/css-vars.css")
		expect(mainSource).toContain("./assets/css/tokens.css")
		expect(mainSource.indexOf("./assets/css/tokens.css")).toBeLessThan(mainSource.indexOf("./assets/css/base.css"))
		expect(mainSource.indexOf("./assets/css/blog-redesign.css")).toBeLessThan(mainSource.indexOf("./assets/css/night.css"))
		expect(mainSource.indexOf("element-plus/theme-chalk/dark/css-vars.css")).toBeLessThan(mainSource.indexOf("./assets/css/night.css"))
	})
})
