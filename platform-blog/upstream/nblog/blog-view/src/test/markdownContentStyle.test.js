// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const baseCss = readFileSync(resolve(process.cwd(), 'src/assets/css/base.css'), 'utf8')
const typoCss = readFileSync(resolve(process.cwd(), 'src/assets/css/typo.css'), 'utf8')

describe('Markdown content styles', () => {
	it('keeps ordered and unordered list markers inside article content', () => {
		document.head.innerHTML = `<style>${typoCss}</style>`
		document.body.innerHTML = `
			<article class="typo">
				<ol><li>ordered item</li></ol>
				<ul><li>unordered item</li></ul>
			</article>
		`

		const orderedList = document.querySelector('.typo ol')
		const unorderedList = document.querySelector('.typo ul')
		const listItems = document.querySelectorAll('.typo li')

		expect(getComputedStyle(orderedList).listStyleType).toBe('decimal')
		expect(getComputedStyle(unorderedList).listStyleType).toBe('disc')
		for (const item of listItems) {
			expect(getComputedStyle(item).overflowX).not.toBe('auto')
		}
	})

	it('uses the bundled JetBrains Mono family for inline and fenced code', () => {
		expect(baseCss).toContain('--font-mono: "JetBrains Mono Variable"')
		expect(typoCss).toMatch(/\.typo :not\(pre\) > code \{[\s\S]*font-family: var\(--font-mono\);/)
		expect(typoCss).toMatch(/\.typo pre \{[\s\S]*font-family: var\(--font-mono\);/)
		expect(typoCss).toMatch(/\.typo pre code \{[\s\S]*font-family: inherit;/)
	})
})
