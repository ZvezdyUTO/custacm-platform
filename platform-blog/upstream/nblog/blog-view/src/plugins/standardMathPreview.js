// Author: huangbingrui.awa
import {syntaxTree} from '@codemirror/language'
import {StateField} from '@codemirror/state'
import {Decoration, EditorView, ViewPlugin, WidgetType} from '@codemirror/view'
import katex from 'katex'
import renderMathInElement from 'katex/contrib/auto-render'
import {mouseSelectingField, shouldShowSource} from 'codemirror-live-markdown'
import {findStandardMathRanges} from '@/util/markdownEditor'

class MathWidget extends WidgetType {
	constructor(source, block) { super(); this.source = source; this.block = block }
	eq(other) { return other.source === this.source && other.block === this.block }
	toDOM() {
		const element = document.createElement(this.block ? 'div' : 'span')
		element.className = this.block ? 'cm-standard-math-block' : 'cm-standard-math-inline'
		element.innerHTML = katex.renderToString(this.source, {displayMode: this.block, throwOnError: false, trust: false, strict: 'warn'})
		return element
	}
	ignoreEvent() { return false }
}

function excludedCodeRanges(state) {
	const ranges = []
	syntaxTree(state).iterate({enter(node) {
		if (['FencedCode', 'CodeBlock', 'InlineCode'].includes(node.name)) ranges.push({from: node.from, to: node.to})
	}})
	return ranges
}

function decorationsFor(state) {
	const dragging = state.field(mouseSelectingField, false)
	const decorations = []
	for (const range of findStandardMathRanges(state.doc.toString(), excludedCodeRanges(state))) {
		if (shouldShowSource(state, range.from, range.to) || dragging) {
			decorations.push(Decoration.mark({class: range.block ? 'cm-standard-math-source-block' : 'cm-standard-math-source'}).range(range.from, range.to))
		} else {
			decorations.push(Decoration.replace({widget: new MathWidget(range.source, range.block), block: range.block}).range(range.from, range.to))
		}
	}
	return Decoration.set(decorations, true)
}

export const standardMathPreview = StateField.define({
	create: decorationsFor,
	update(value, transaction) {
		const dragging = transaction.state.field(mouseSelectingField, false)
		const wasDragging = transaction.startState.field(mouseSelectingField, false)
		if (dragging) return value
		if (transaction.docChanged || transaction.reconfigured || transaction.selection || wasDragging !== dragging) return decorationsFor(transaction.state)
		return value
	},
	provide: field => EditorView.decorations.from(field),
})

const tableMathOptions = {
	delimiters: [{left: '$', right: '$', display: false}],
	ignoredTags: [],
	throwOnError: false,
	trust: false,
}

function showTableCellSource(cell) {
	if (!cell.dataset.markdownSource) return
	cell.textContent = cell.dataset.markdownSource
}

function renderTableCellMath(cell) {
	const source = cell.dataset.markdownSource || cell.textContent || ''
	cell.dataset.markdownSource = source
	cell.textContent = source
	if (source.includes('$')) renderMathInElement(cell, tableMathOptions)
}

export const tableMathPreview = ViewPlugin.fromClass(class {
	constructor(view) {
		this.view = view
		this.frame = null
		this.onFocusIn = event => {
			const cell = event.target.closest?.('.cm-table-cell')
			if (cell) showTableCellSource(cell)
		}
		this.onFocusOut = event => {
			const cell = event.target.closest?.('.cm-table-cell')
			if (!cell) return
			cell.dataset.markdownSource = cell.textContent || ''
			requestAnimationFrame(() => renderTableCellMath(cell))
		}
		view.dom.addEventListener('focusin', this.onFocusIn)
		view.dom.addEventListener('focusout', this.onFocusOut)
		this.schedule()
	}
	update() { this.schedule() }
	schedule() {
		if (this.frame !== null) cancelAnimationFrame(this.frame)
		this.frame = requestAnimationFrame(() => {
			this.frame = null
			this.view.dom.querySelectorAll('.cm-table-cell').forEach(cell => {
				if (!cell.matches(':focus')) renderTableCellMath(cell)
			})
		})
	}
	destroy() {
		if (this.frame !== null) cancelAnimationFrame(this.frame)
		this.view.dom.removeEventListener('focusin', this.onFocusIn)
		this.view.dom.removeEventListener('focusout', this.onFocusOut)
	}
})
