// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const css = readFileSync(resolve(process.cwd(), 'src/assets/css/blog-redesign.css'), 'utf8')
const nightCss = readFileSync(resolve(process.cwd(), 'src/assets/css/night.css'), 'utf8')
const navSource = readFileSync(resolve(process.cwd(), 'src/components/index/Nav.vue'), 'utf8')
const footerSource = readFileSync(resolve(process.cwd(), 'src/components/index/Footer.vue'), 'utf8')
const mainSource = readFileSync(resolve(process.cwd(), 'src/main.js'), 'utf8')
const indexHtml = readFileSync(resolve(process.cwd(), 'index.html'), 'utf8')
const indexSource = readFileSync(resolve(process.cwd(), 'src/views/Index.vue'), 'utf8')
const articleSource = readFileSync(resolve(process.cwd(), 'src/views/blog/Blog.vue'), 'utf8')
const editorSource = readFileSync(resolve(process.cwd(), 'src/views/article/ArticleEditor.vue'), 'utf8')
const markdownEditorSource = readFileSync(resolve(process.cwd(), 'src/components/article/LiveMarkdownEditor.vue'), 'utf8')

describe('Blog redesign stylesheet contract', () => {
	it('maps Element Plus and project-owned Blog roles to the shared semantic tokens', () => {
		expect(css).toContain('--el-color-primary: var(--color-action)')
		expect(css).toContain('--theme-canvas: var(--color-canvas-alternate)')
		expect(css).toContain('font-family: var(--font-sans)')
	})

	it('uses the sampled Anthropic article palette on reading and writing surfaces', () => {
		expect(css).toContain('--anthropic-ivory-light: #faf9f5')
		expect(css).toContain('--anthropic-slate-dark: #141413')
		expect(css).toContain('--anthropic-clay: #d97757')
		expect(indexSource).toContain("'is-editor': $route.name === 'write'")
		expect(indexSource).toContain('background: var(--anthropic-ivory-light)')
		expect(articleSource).toContain('--color-surface: var(--anthropic-ivory-medium)')
		expect(articleSource).toContain('color: var(--anthropic-slate-dark)')
		expect(editorSource).toContain('--color-action: var(--anthropic-clay)')
		expect(editorSource).toContain('background: var(--anthropic-ivory-light) !important')
		expect(markdownEditorSource).toContain('background: var(--anthropic-ivory-medium) !important')
	})

	it('uses the sampled Anthropic ivory only for the home and competition page canvases', () => {
		expect(indexSource).toContain('--home-canvas: var(--anthropic-ivory-light)')
		expect(indexSource).toContain("'is-competition': competitionRoute")
		expect(indexSource).toContain("return ['competitions', 'competition-detail'].includes(this.$route.name)")
		expect(indexSource).toMatch(/\.site\.is-competition,[\s\S]*\.site\.is-competition \.main \{[\s\S]*background: var\(--anthropic-ivory-light\);/)
	})

	it('loads and reuses the bundled JetBrains Mono font in the Markdown editor', () => {
		expect(mainSource).toContain("import '@fontsource-variable/jetbrains-mono/wght.css'")
		expect(markdownEditorSource).toContain('font-family: var(--font-mono)')
	})

	it('keeps article taxonomy markers on their persisted business colors', () => {
		const taxonomyRule = articleSource.match(/\.taxonomy-chip \{([^}]*)\}/)?.[1] || ''

		expect(articleSource).toContain(":style=\"{'--category-color': blog.category.color || '#60758a'}\"")
		expect(articleSource).toContain('background: var(--category-color);')
		expect(articleSource).toContain(':style="taxonomyStyle(tag.color)"')
		expect(taxonomyRule).not.toContain('background:')
		expect(taxonomyRule).toContain('color: #fff !important;')
	})

	it('wraps the complete article title and summary even when they contain an unbroken word', () => {
		const titleRule = articleSource.match(/\.article-title \{([^}]*)\}/)?.[1] || ''
		const summaryRule = articleSource.match(/\.article-summary \{([^}]*)\}/)?.[1] || ''

		expect(titleRule).toContain('overflow-wrap: anywhere;')
		expect(titleRule).toContain('word-break: break-word;')
		expect(titleRule).not.toContain('line-clamp')
		expect(titleRule).not.toContain('text-overflow')
		expect(summaryRule).toContain('overflow-wrap: anywhere;')
		expect(summaryRule).toContain('word-break: break-word;')
		expect(summaryRule).not.toContain('line-clamp')
		expect(summaryRule).not.toContain('text-overflow')
	})

	it('limits glass to navigation, floating menus and dialogs', () => {
		expect(css).toContain('.site-nav {')
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

	it('does not draw the global blue focus rectangle inside the navigation bar', () => {
		expect(css).toContain('.site-nav :where(a, button, input, [tabindex]):focus-visible')
		expect(css).toMatch(/\.site-nav :where\(a, button, input, \[tabindex\]\):focus-visible \{[\s\S]*outline: none !important;/)
	})

	it('uses an ivory glass treatment outside the dark article catalog', () => {
		expect(navSource).toContain('<div ref="nav" class="site-nav">')
		expect(navSource).not.toContain("'transparent'")
		expect(css).not.toContain('.site-nav:not(.transparent)')
		expect(css).toContain('--glass-background: rgba(250, 249, 245, .72)')
		expect(css).toContain('background: var(--glass-background) !important')
		expect(indexSource).toContain('background: rgba(38, 34, 31, .68) !important')
	})

	it('loads the persistent two-mode theme and exposes one compact navigation switch', () => {
		expect(navSource).toContain('nav-theme-toggle')
		expect(navSource).toContain('role="switch"')
		expect(navSource).toContain("THEME_CHANGE_EVENT")
		expect(mainSource).toContain('night.css')
		expect(mainSource).toContain('dark/css-vars.css')
		expect(indexHtml).toContain('<meta name="color-scheme" content="light dark">')
		expect(indexHtml).toContain('custacm.theme')
		expect(nightCss).toContain('--color-canvas: #141413')
		expect(nightCss).toContain('--color-text: #faf9f5')
		expect(nightCss).toContain('--color-action: #d97757')
		expect(nightCss).not.toMatch(/html\.dark\s+img/)
	})

	it('uses one typography and icon scale across the primary navigation items', () => {
		expect(navSource.match(/class="[^"]*nav-primary-item[^"]*"/g)).toHaveLength(5)
		expect(navSource).toMatch(/\.site-nav \.nav-primary-item \{[\s\S]*font-family: var\(--font-sans\) !important;[\s\S]*font-size: 16px !important;[\s\S]*font-weight: 500 !important;[\s\S]*line-height: 24px !important;/)
		expect(navSource).toMatch(/\.site-nav \.nav-primary-item > \.app-icon \{[\s\S]*width: 20px;[\s\S]*height: 20px;/)
		expect(navSource).toContain('class="nav-menu-chevron"')
	})

	it('keeps the black footer fully visible and resistant to flex shrinking', () => {
		expect(footerSource).toMatch(/\.site-footer \{[\s\S]*min-height: 116px;/)
		expect(footerSource).toContain('padding: 18px 0 max(24px, env(safe-area-inset-bottom)) !important;')
		expect(footerSource).toContain('flex: 0 0 auto;')
		expect(footerSource).toContain('overflow: visible;')
		expect(footerSource).toMatch(/\.footer-links \{[\s\S]*min-height: 36px;/)
	})
})
