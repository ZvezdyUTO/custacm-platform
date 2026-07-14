// Author: huangbingrui.awa
import {syntaxTree} from '@codemirror/language'
import {StateField} from '@codemirror/state'
import {Decoration, EditorView, WidgetType} from '@codemirror/view'
import {loadImage, mouseSelectingField} from 'codemirror-live-markdown'

const defaultOptions = {
	maxWidth: '100%',
	showAlt: true,
	showLoading: true,
	errorPlaceholder: '图片加载失败',
	basePath: '',
}

function parseImageSyntax(text) {
	const match = text.match(/^!\[([^\]]*)\]\((.+?)(?:\s+["']([^"']+)["'])?\)$/)
	if (!match) return null
	return {alt: match[1], src: match[2], title: match[3]}
}

export function selectionEditsImage(selection, from, to) {
	return selection.ranges.some(range => range.empty
		? range.head > from && range.head < to
		: range.from < to && range.to > from)
}

function requestImageMeasure(view, container) {
	requestAnimationFrame(() => {
		if (container.isConnected && view.dom.isConnected) view.requestMeasure()
	})
}

class ArticleImageWidget extends WidgetType {
	constructor(data, options, from, to) {
		super()
		this.data = data
		this.options = options
		this.from = from
		this.to = to
	}

	eq(other) {
		return other.from === this.from && other.to === this.to && other.data.src === this.data.src
			&& other.data.alt === this.data.alt && other.data.title === this.data.title
	}

	toDOM(view) {
		const container = document.createElement('div')
		container.className = 'cm-image-widget'
		container.dataset.from = String(this.from)
		container.dataset.to = String(this.to)

		if (this.options.showLoading) {
			const loading = document.createElement('div')
			loading.className = 'cm-image-loading'
			const spinner = document.createElement('span')
			spinner.className = 'cm-image-spinner'
			const message = document.createElement('span')
			message.textContent = 'Loading...'
			loading.append(spinner, message)
			container.appendChild(loading)
		}

		loadImage(this.data.src, {basePath: this.options.basePath}).then(result => {
			container.querySelector('.cm-image-loading')?.remove()
			if (result.loaded) {
				const image = document.createElement('img')
				image.src = result.src
				image.alt = this.data.alt
				image.title = this.data.title || ''
				image.style.maxWidth = this.options.maxWidth
				image.draggable = false
				image.addEventListener('load', () => requestImageMeasure(view, container), {once: true})
				container.appendChild(image)
				if (this.options.showAlt && this.data.alt) {
					const alt = document.createElement('div')
					alt.className = 'cm-image-alt'
					alt.textContent = this.data.alt
					container.appendChild(alt)
				}
			} else {
				const error = document.createElement('div')
				error.className = 'cm-image-error'
				const icon = document.createElement('span')
				icon.className = 'cm-image-error-icon'
				icon.textContent = '⚠'
				const message = document.createElement('span')
				message.textContent = this.options.errorPlaceholder
				error.append(icon, message)
				container.appendChild(error)
			}
			requestImageMeasure(view, container)
		})

		return container
	}

	ignoreEvent() { return false }
}

function buildImagePreview(state, options) {
	const decorations = []
	const atomicRanges = []
	const dragging = state.field(mouseSelectingField, false)

	syntaxTree(state).iterate({enter(node) {
		if (node.name !== 'Image') return
		const data = parseImageSyntax(state.doc.sliceString(node.from, node.to))
		if (!data) return

		if (dragging || selectionEditsImage(state.selection, node.from, node.to)) {
			const line = state.doc.lineAt(node.from)
			decorations.push(Decoration.line({class: 'cm-image-source'}).range(line.from))
			return
		}

		const replacement = Decoration.replace({
			widget: new ArticleImageWidget(data, options, node.from, node.to),
			block: true,
		}).range(node.from, node.to)
		decorations.push(replacement)
		atomicRanges.push(replacement)
	}})

	return {
		decorations: Decoration.set(decorations.sort((left, right) => left.from - right.from), true),
		atomicRanges: Decoration.set(atomicRanges.sort((left, right) => left.from - right.from), true),
	}
}

export function articleImagePreview(options = {}) {
	const resolvedOptions = {...defaultOptions, ...options}
	const field = StateField.define({
		create: state => buildImagePreview(state, resolvedOptions),
		update(value, transaction) {
			const dragging = transaction.state.field(mouseSelectingField, false)
			const wasDragging = transaction.startState.field(mouseSelectingField, false)
			if (dragging) return value
			if (transaction.docChanged || transaction.reconfigured || transaction.selection || wasDragging !== dragging) {
				return buildImagePreview(transaction.state, resolvedOptions)
			}
			return value
		},
		provide: imageField => [
			EditorView.decorations.from(imageField, value => value.decorations),
			EditorView.atomicRanges.of(view => view.state.field(imageField).atomicRanges),
		],
	})

	const clickHandler = EditorView.domEventHandlers({
		mousedown(event, view) {
			const target = event.target instanceof Element ? event.target : null
			const widget = target?.closest('.cm-image-widget')
			if (!widget) return false
			const from = Number.parseInt(widget.dataset.from || '', 10)
			const to = Number.parseInt(widget.dataset.to || '', 10)
			if (!Number.isInteger(from) || !Number.isInteger(to) || to - from < 2) return false
			view.dispatch({selection: {anchor: from + 1}, scrollIntoView: true, userEvent: 'select.pointer'})
			view.focus()
			return true
		},
	})

	return [field, clickHandler]
}
