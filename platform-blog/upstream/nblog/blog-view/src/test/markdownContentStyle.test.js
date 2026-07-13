// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import typoCss from '../assets/css/typo.css?raw'

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
})
