// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {findStandardMathRanges, markdownInsertion} from '@/util/markdownEditor'

describe('Markdown editor helpers', () => {
	it('finds standard inline and block math while ignoring escaped delimiters', () => {
		const text = '勾股定理 $a^2+b^2=c^2$，价格 \\$5。\n\n$$\n\\sum_{i=1}^{n} i\n$$'
		expect(findStandardMathRanges(text)).toEqual([
			expect.objectContaining({source: 'a^2+b^2=c^2', block: false}),
			expect.objectContaining({source: '\\sum_{i=1}^{n} i', block: true}),
		])
	})

	it('does not render math inside excluded code ranges', () => {
		const text = '`$notMath$` and $math$'
		expect(findStandardMathRanges(text, [{from: 0, to: 11}])).toEqual([
			expect.objectContaining({source: 'math', block: false}),
		])
	})

	it('creates common problem-solution Markdown snippets', () => {
		expect(markdownInsertion('headingLarge').text).toBe('\n\n# 大标题\n\n')
		expect(markdownInsertion('headingMedium').text).toBe('\n\n## 中标题\n\n')
		expect(markdownInsertion('headingSmall').text).toBe('\n\n### 小标题\n\n')
		expect(markdownInsertion('inlineMath', 'x+y').text).toBe('$x+y$')
		expect(markdownInsertion('table').text).toMatch(/^\n\n\|[\s\S]+\n\n$/)
		expect(markdownInsertion('codeBlock', 'return 0;').text).toContain('```cpp\nreturn 0;\n```')
		expect(markdownInsertion('blockMath').text).toMatch(/^\n\n\$\$\n[\s\S]+\n\$\$\n\n$/)
		for (const kind of ['codeBlock', 'table', 'blockMath']) {
			const insertion = markdownInsertion(kind)
			expect(insertion.text.slice(insertion.selectStart, insertion.selectStart + insertion.selectLength)).not.toMatch(/[|$`]/)
		}
	})
})
