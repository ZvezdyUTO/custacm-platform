// Author: huangbingrui.awa

const escapedAt = (text, index) => {
	let slashes = 0
	for (let cursor = index - 1; cursor >= 0 && text[cursor] === '\\'; cursor--) slashes++
	return slashes % 2 === 1
}

const overlaps = (range, excludedRanges) => excludedRanges.some(excluded => range.from < excluded.to && range.to > excluded.from)

export function findStandardMathRanges(text, excludedRanges = []) {
	const ranges = []
	const blockRanges = []
	const lines = text.split('\n')
	let offset = 0
	let opener = null

	for (const line of lines) {
		if (/^\s*\$\$\s*$/.test(line)) {
			if (opener === null) opener = {from: offset, contentFrom: offset + line.length + 1}
			else {
				const range = {from: opener.from, to: offset + line.length, source: text.slice(opener.contentFrom, offset).trim(), block: true}
				if (range.source && !overlaps(range, excludedRanges)) {
					ranges.push(range)
					blockRanges.push(range)
				}
				opener = null
			}
		}
		offset += line.length + 1
	}

	for (let index = 0; index < text.length; index++) {
		if (text[index] !== '$' || escapedAt(text, index) || text[index + 1] === '$' || text[index - 1] === '$') continue
		const from = index
		let to = -1
		for (index++; index < text.length && text[index] !== '\n'; index++) {
			if (text[index] === '$' && !escapedAt(text, index) && text[index - 1] !== '$' && text[index + 1] !== '$') {
				to = index + 1
				break
			}
		}
		if (to < 0) continue
		const range = {from, to, source: text.slice(from + 1, to - 1).trim(), block: false}
		if (range.source && !overlaps(range, excludedRanges) && !overlaps(range, blockRanges)) ranges.push(range)
	}

	return ranges.sort((left, right) => left.from - right.from)
}

export function markdownInsertion(kind, selected = '') {
	const value = selected || ''
	const templates = {
		headingLarge: {text: `\n\n# ${value || '大标题'}\n\n`, selectStart: 4, selectLength: value.length || 3},
		headingMedium: {text: `\n\n## ${value || '中标题'}\n\n`, selectStart: 5, selectLength: value.length || 3},
		headingSmall: {text: `\n\n### ${value || '小标题'}\n\n`, selectStart: 6, selectLength: value.length || 3},
		bold: {text: `**${value || '加粗文字'}**`, selectStart: 2, selectLength: value.length || 4},
		italic: {text: `*${value || '斜体文字'}*`, selectStart: 1, selectLength: value.length || 4},
		strike: {text: `~~${value || '删除文字'}~~`, selectStart: 2, selectLength: value.length || 4},
		inlineCode: {text: `\`${value || 'code'}\``, selectStart: 1, selectLength: value.length || 4},
		codeBlock: {text: `\n\n\`\`\`cpp\n${value || '// 在这里写代码'}\n\`\`\`\n\n`, selectStart: 9, selectLength: value.length || 9},
		link: {text: `[${value || '链接文字'}](https://)`, selectStart: 1, selectLength: value.length || 4},
		table: {text: `\n\n| 列 1 | 列 2 |\n| --- | --- |\n| ${value || '内容'} | 内容 |\n\n`, selectStart: 32, selectLength: value.length || 2},
		inlineMath: {text: `$${value || 'a^2+b^2=c^2'}$`, selectStart: 1, selectLength: value.length || 11},
		blockMath: {text: `\n\n$$\n${value || '\\sum_{i=1}^{n} i = \\frac{n(n+1)}{2}'}\n$$\n\n`, selectStart: 5, selectLength: value.length || 35},
	}
	return templates[kind] || {text: value, selectStart: 0, selectLength: value.length}
}
