<template>
	<section class="live-markdown-editor" :class="{'is-loading': loading}" aria-label="Markdown 正文编辑器">
		<div class="markdown-toolbar" role="toolbar" aria-label="插入 Markdown">
			<select aria-label="标题级别" title="插入标题" @change="insertHeading">
				<option value="" selected disabled>标题</option>
				<option value="headingLarge">大标题</option>
				<option value="headingMedium">中标题</option>
				<option value="headingSmall">小标题</option>
			</select>
			<button v-for="action in actions" :key="action.kind" type="button" :title="action.title" @click="insert(action.kind)">{{ action.label }}</button>
			<input ref="imageInput" hidden type="file" accept="image/jpeg,image/png" multiple @change="selectImages">
			<button type="button" title="上传正文图片" :disabled="uploadingImages" @click="$refs.imageInput.click()">{{ uploadingImages ? '上传中…' : '上传图片' }}</button>
			<span class="toolbar-spacer"></span>
			<span class="toolbar-status">{{ imageError || (loading ? '正在加载编辑器…' : '实时预览') }}</span>
		</div>
		<div ref="editorHost" class="editor-host"></div>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {defaultKeymap, history, historyKeymap, indentWithTab} from '@codemirror/commands'
	import {markdown} from '@codemirror/lang-markdown'
	import {EditorState} from '@codemirror/state'
	import {EditorView, highlightActiveLine, keymap} from '@codemirror/view'
	import {Table} from '@lezer/markdown'
	import {
		codeBlockField, collapseOnSelectionFacet, editorTheme, initHighlighter, linkPlugin,
		livePreviewPlugin, markdownStylePlugin, mouseSelectingField, setMouseSelecting, tableEditorPlugin,
	} from 'codemirror-live-markdown'
	import 'katex/dist/katex.css'
	import {articleImagePreview} from '@/plugins/articleImagePreview'
	import {standardMathPreview, tableMathPreview} from '@/plugins/standardMathPreview'
	import {markdownInsertion} from '@/util/markdownEditor'
	import {deleteManagedImageBackward, imageAltFromFilename, markdownForImage, validateArticleImage} from '@/util/articleImages'

	export default {
		name: 'LiveMarkdownEditor',
		props: {
			modelValue: {type: String, default: ''},
			uploadImage: {type: Function, default: null},
		},
		emits: ['update:modelValue', 'dirty'],
		data() {
			return {
				view: null, loading: true, destroyed: false, uploadingImages: false, imageError: '',
				actions: [
					{kind: 'bold', label: 'B', title: '加粗'}, {kind: 'italic', label: 'I', title: '斜体'},
					{kind: 'strike', label: 'S', title: '删除线'},
					{kind: 'inlineCode', label: '</>', title: '行内代码'}, {kind: 'codeBlock', label: '代码块', title: 'C++ 代码块'},
					{kind: 'link', label: '链接', title: '插入链接'}, {kind: 'table', label: '表格', title: '插入 GFM 表格'},
					{kind: 'inlineMath', label: '行内公式', title: '在一行文字中插入公式'}, {kind: 'blockMath', label: '块公式', title: '插入独占一行的公式'},
				],
			}
		},
		watch: {
			modelValue(value) {
				if (!this.view || value === this.view.state.doc.toString()) return
				this.view.dispatch({changes: {from: 0, to: this.view.state.doc.length, insert: value || ''}})
			},
		},
		async mounted() {
			await initHighlighter()
			if (this.destroyed) return
			const updateListener = EditorView.updateListener.of(update => {
				if (!update.docChanged) return
				this.$emit('update:modelValue', update.state.doc.toString())
				this.$emit('dirty')
			})
			this.view = new EditorView({
				state: EditorState.create({doc: this.modelValue, extensions: [
					markdown({extensions: [Table]}), history(), keymap.of([{key: 'Backspace', run: deleteManagedImageBackward}, indentWithTab, ...defaultKeymap, ...historyKeymap]),
					highlightActiveLine(), EditorView.lineWrapping,
					collapseOnSelectionFacet.of(true), mouseSelectingField, livePreviewPlugin, markdownStylePlugin, editorTheme,
					tableEditorPlugin(), codeBlockField({lineNumbers: true, copyButton: true, defaultLanguage: 'text'}),
					articleImagePreview({maxWidth: '100%', showAlt: true, showLoading: true, errorPlaceholder: '图片加载失败'}),
					linkPlugin({openInNewTab: true, showPreview: true}), standardMathPreview, tableMathPreview, updateListener,
				]}),
				parent: this.$refs.editorHost,
			})
			this.view.contentDOM.addEventListener('mousedown', this.onMouseDown, true)
			this.view.contentDOM.addEventListener('paste', this.onPaste)
			this.view.contentDOM.addEventListener('dragover', this.onDragOver)
			this.view.contentDOM.addEventListener('drop', this.onDrop)
			document.addEventListener('mouseup', this.onMouseUp)
			this.loading = false
		},
		beforeUnmount() {
			this.destroyed = true
			document.removeEventListener('mouseup', this.onMouseUp)
			if (this.view) {
				this.view.contentDOM.removeEventListener('mousedown', this.onMouseDown, true)
				this.view.contentDOM.removeEventListener('paste', this.onPaste)
				this.view.contentDOM.removeEventListener('dragover', this.onDragOver)
				this.view.contentDOM.removeEventListener('drop', this.onDrop)
				this.view.destroy()
			}
		},
		methods: {
			onMouseDown(event) {
				if (this.focusCodeBlockAtPointer(event)) return
				this.view?.dispatch({effects: setMouseSelecting.of(true)})
			},
			focusCodeBlockAtPointer(event) {
				const target = event.target instanceof Element ? event.target : null
				const line = target?.closest('.cm-codeblock-line')
				const widget = line?.closest('.cm-codeblock-widget')
				if (!line || !widget || target.closest('.cm-codeblock-copy')) return false

				const lineIndex = Number.parseInt(line.dataset.lineIndex || '', 10)
				let position
				if (lineIndex === -1) position = Number.parseInt(widget.dataset.from || '', 10)
				else if (lineIndex === -2) position = Number.parseInt(widget.dataset.to || '', 10)
				else {
					let lineStarts
					try { lineStarts = JSON.parse(widget.dataset.lineStarts || '[]') } catch { return false }
					if (!Number.isInteger(lineIndex) || !Number.isInteger(lineStarts[lineIndex])) return false
					const caret = document.caretPositionFromPoint?.(event.clientX, event.clientY)
					const fallbackRange = caret ? null : document.caretRangeFromPoint?.(event.clientX, event.clientY)
					const offsetNode = caret?.offsetNode || fallbackRange?.startContainer
					const offset = caret?.offset ?? fallbackRange?.startOffset
					if (!offsetNode || !line.contains(offsetNode) || !Number.isInteger(offset)) return false
					const range = document.createRange()
					range.selectNodeContents(line)
					try { range.setEnd(offsetNode, offset) } catch { return false }
					position = lineStarts[lineIndex] + range.toString().length
				}
				if (!Number.isInteger(position)) return false

				event.preventDefault()
				event.stopImmediatePropagation()
				this.view.dispatch({selection: {anchor: position}, scrollIntoView: true})
				this.view.focus()
				return true
			},
			onMouseUp() { requestAnimationFrame(() => this.view?.dispatch({effects: setMouseSelecting.of(false)})) },
			onDragOver(event) { if ([...(event.dataTransfer?.items || [])].some(item => item.kind === 'file')) event.preventDefault() },
			onDrop(event) {
				const files = [...(event.dataTransfer?.files || [])].filter(file => file.type.startsWith('image/'))
				if (!files.length) return
				event.preventDefault()
				this.uploadFiles(files)
			},
			onPaste(event) {
				const files = [...(event.clipboardData?.files || [])].filter(file => file.type.startsWith('image/'))
				if (!files.length) return
				event.preventDefault()
				this.uploadFiles(files)
			},
			selectImages(event) {
				const files = [...(event.target.files || [])]
				event.target.value = ''
				this.uploadFiles(files)
			},
			async uploadFiles(files) {
				if (!this.uploadImage || this.uploadingImages) return
				this.uploadingImages = true
				this.imageError = ''
				try {
					for (const file of files) {
						validateArticleImage(file)
						const asset = await this.uploadImage(file)
						this.insertText(`\n\n${markdownForImage(asset, imageAltFromFilename(file.name))}\n\n`)
					}
				} catch (error) { this.imageError = error.message || '图片上传失败' }
				finally { this.uploadingImages = false }
			},
			insertText(text) {
				if (!this.view) return
				const selection = this.view.state.selection.main
				const cursor = selection.from + text.length
				this.view.dispatch({changes: {from: selection.from, to: selection.to, insert: text}, selection: {anchor: cursor}, scrollIntoView: true})
				this.view.focus()
			},
			insertHeading(event) {
				const kind = event.target.value
				if (kind) this.insert(kind)
				event.target.value = ''
			},
			insert(kind) {
				if (!this.view) return
				const selection = this.view.state.selection.main
				const selected = this.view.state.doc.sliceString(selection.from, selection.to)
				const insertion = markdownInsertion(kind, selected)
				const anchor = selection.from + insertion.selectStart
				this.view.dispatch({
					changes: {from: selection.from, to: selection.to, insert: insertion.text},
					selection: {anchor, head: anchor + insertion.selectLength}, scrollIntoView: true,
				})
				this.view.focus()
			},
		},
	}
</script>

<style scoped>
	.live-markdown-editor { overflow: hidden; border: 1px solid #d2d9e0; background: #fff; }
	.live-markdown-editor:focus-within { border-color: #17324d; box-shadow: 0 0 0 2px rgba(23, 50, 77, .1); }
	.markdown-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 5px; border-bottom: 1px solid #dce2e7; background: #f4f6f8; padding: 8px 10px; }
	.markdown-toolbar button, .markdown-toolbar select { min-width: 32px; height: 30px; border: 1px solid #cbd4dc; background: #fff; color: #31414e; padding: 0 9px; font: inherit; font-size: 11px; font-weight: 800; cursor: pointer; }
	.markdown-toolbar select { min-width: 66px; padding-right: 5px; }
	.markdown-toolbar button:hover, .markdown-toolbar select:hover { border-color: #17324d; color: #17324d; }
	.markdown-toolbar button:disabled { cursor: wait; opacity: .65; }
	.toolbar-spacer { flex: 1; }
	.toolbar-status { color: #71808c; font-size: 10px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
	.editor-host { min-height: 460px; }
	:deep(.cm-editor) { min-height: 460px; color: #27343e; font-family: 'SFMono-Regular', Consolas, monospace; font-size: 13px; }
	:deep(.cm-scroller) { min-height: 460px; max-height: 68vh; line-height: 1.75; }
	:deep(.cm-content) { padding: 18px 20px 42px; caret-color: #17324d; }
	:deep(.cm-content)::selection, :deep(.cm-content *)::selection { background-color: #315a7d !important; color: #fff !important; text-shadow: none !important; }
	:deep(.cm-focused) { outline: none; }
	:deep(.cm-activeLine) { background: rgba(23, 50, 77, .035); }
	:deep(.cm-standard-math-block) { overflow-x: auto; margin: 0; padding: 14px; background: #f7f9fa; text-align: center; }
	:deep(.cm-standard-math-inline) { color: #17324d; }
	:deep(.cm-standard-math-source), :deep(.cm-standard-math-source-block) { background: rgba(23, 50, 77, .06); }
	:deep(.cm-table-editor), :deep(.cm-table-widget table) { width: 100%; }
	:deep(.cm-codeblock-widget) { overflow: hidden; box-sizing: border-box; margin: 0; border: 1px solid #d0d7de; border-radius: 5px; background: #f6f8fa !important; color: #24292f; padding: 10px 0; box-shadow: 0 8px 22px rgba(31, 35, 40, .08); }
	:deep(.cm-codeblock-widget pre) { background: transparent; color: #24292f; }
	:deep(.cm-codeblock-widget code) { background: transparent; color: inherit; font-size: 11px; }
	:deep(.cm-codeblock-line) { min-height: 18px; padding: 0 12px; font-size: 11px !important; line-height: 1.55; }
	:deep(.cm-codeblock-fence) { color: #57606a; background: #eef1f4; font-size: 10px !important; }
	:deep(.cm-codeblock-copy) { top: 6px; right: 7px; border: 1px solid #afb8c1; border-radius: 3px; background: #ffffff; color: #57606a; padding: 2px 7px; font-size: 10px; }
	:deep(.cm-codeblock-copy:hover) { background: #eaeef2; color: #24292f; }
	:deep(.cm-codeblock-widget .hljs-comment), :deep(.cm-codeblock-widget .hljs-quote) { color: #57606a; font-style: italic; }
	:deep(.cm-codeblock-widget .hljs-keyword), :deep(.cm-codeblock-widget .hljs-selector-tag), :deep(.cm-codeblock-widget .hljs-literal), :deep(.cm-codeblock-widget .hljs-section), :deep(.cm-codeblock-widget .hljs-link) { color: #cf222e; font-weight: 700; }
	:deep(.cm-codeblock-widget .hljs-string), :deep(.cm-codeblock-widget .hljs-title), :deep(.cm-codeblock-widget .hljs-name), :deep(.cm-codeblock-widget .hljs-type), :deep(.cm-codeblock-widget .hljs-attribute), :deep(.cm-codeblock-widget .hljs-symbol), :deep(.cm-codeblock-widget .hljs-bullet), :deep(.cm-codeblock-widget .hljs-addition) { color: #0a3069; }
	:deep(.cm-codeblock-widget .hljs-number), :deep(.cm-codeblock-widget .hljs-built_in), :deep(.cm-codeblock-widget .hljs-builtin-name), :deep(.cm-codeblock-widget .hljs-meta), :deep(.cm-codeblock-widget .hljs-variable), :deep(.cm-codeblock-widget .hljs-template-variable) { color: #0550ae; }
	:deep(.cm-codeblock-widget .hljs-function), :deep(.cm-codeblock-widget .hljs-title.function_), :deep(.cm-codeblock-widget .hljs-params), :deep(.cm-codeblock-widget .hljs-property), :deep(.cm-codeblock-widget .hljs-attr) { color: #8250df; }
	:deep(.cm-codeblock-widget .hljs-operator), :deep(.cm-codeblock-widget .hljs-punctuation) { color: #24292f; }
	:deep(.cm-image-widget) { overflow: hidden; box-sizing: border-box; margin: 0; border: 1px solid #d8e0e6; background: #f7f9fa; padding: 10px; }
	:deep(.cm-image-widget img) { display: block; max-width: 100%; max-height: 520px; margin: 0 auto; object-fit: contain; }
	:deep(.cm-image-alt) { margin-top: 7px; color: #7b8792; font-size: 11px; text-align: center; }
	@media (max-width: 900px) { .toolbar-status { display: none; } :deep(.cm-content) { padding-inline: 12px; } }
</style>
