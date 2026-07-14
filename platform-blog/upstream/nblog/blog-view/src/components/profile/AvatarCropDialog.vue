<template>
	<Teleport to="body">
		<div v-if="visible" class="crop-backdrop" @mousedown.self="close">
			<section ref="dialog" class="crop-dialog" role="dialog" aria-modal="true" aria-labelledby="avatar-crop-title" tabindex="-1"
			         @keydown.esc.stop="close" @keydown.tab="trapTab">
				<header>
					<div>
						<p class="crop-eyebrow">个人头像</p>
						<h2 id="avatar-crop-title">裁剪正方形头像</h2>
					</div>
					<button type="button" class="crop-close" aria-label="关闭" :disabled="saving" @click="close">×</button>
				</header>
				<div class="crop-stage">
					<canvas
						ref="canvas"
						width="360"
						height="360"
						aria-label="拖动图片调整头像裁剪区域"
						@pointerdown="startDrag"
						@pointermove="drag"
						@pointerup="endDrag"
						@pointercancel="endDrag"
					></canvas>
					<div class="crop-frame" aria-hidden="true"></div>
				</div>
				<label class="zoom-control">
					<span>缩放</span>
					<input v-model.number="zoom" type="range" min="1" max="3" step="0.01" @input="applyZoom">
					<output>{{ Math.round(zoom * 100) }}%</output>
				</label>
				<p class="crop-help">拖动图片调整位置，保存后统一生成 512×512 PNG。</p>
				<p v-if="localError || errorMessage" class="crop-error" role="alert">{{ localError || errorMessage }}</p>
				<footer>
					<button type="button" class="crop-secondary" :disabled="saving" @click="close">取消</button>
					<button type="button" class="crop-primary" :disabled="saving || !image" @click="save">
						{{ saving ? '正在保存…' : '保存头像' }}
					</button>
				</footer>
			</section>
		</div>
	</Teleport>
</template>

<script>
	// Author: huangbingrui.awa
	import {focusDialog, restoreDialogFocus, trapDialogTab} from '@/util/dialogFocus'

	const PREVIEW_SIZE = 360
	const OUTPUT_SIZE = 512

	export default {
		name: 'AvatarCropDialog',
		props: {
			saving: {type: Boolean, default: false},
			errorMessage: {type: String, default: ''},
		},
		emits: ['save'],
		data() {
			return {
				visible: false,
				image: null,
				objectUrl: '',
				baseScale: 1,
				zoom: 1,
				offsetX: 0,
				offsetY: 0,
				dragging: false,
				lastX: 0,
				lastY: 0,
				localError: '',
				returnFocus: null,
			}
		},
		beforeUnmount() {
			this.releaseObjectUrl()
			restoreDialogFocus(this.returnFocus)
		},
		methods: {
			open(file) {
				this.returnFocus = document.activeElement
				this.localError = ''
				if (!['image/png', 'image/jpeg'].includes(file.type)) {
					this.visible = true
					this.localError = '请选择 PNG 或 JPEG 图片。'
					this.$nextTick(() => focusDialog(this.$refs.dialog, '.crop-close'))
					return
				}
				if (file.size > 10 * 1024 * 1024) {
					this.visible = true
					this.localError = '原始图片不能超过 10MB。'
					this.$nextTick(() => focusDialog(this.$refs.dialog, '.crop-close'))
					return
				}
				this.releaseObjectUrl()
				this.objectUrl = URL.createObjectURL(file)
				const image = new Image()
				image.onload = () => {
					this.image = image
					this.baseScale = Math.max(PREVIEW_SIZE / image.naturalWidth, PREVIEW_SIZE / image.naturalHeight)
					this.zoom = 1
					this._previousScale = this.baseScale
					this.offsetX = (PREVIEW_SIZE - image.naturalWidth * this.baseScale) / 2
					this.offsetY = (PREVIEW_SIZE - image.naturalHeight * this.baseScale) / 2
					this.visible = true
					this.$nextTick(() => {
						this.draw()
						focusDialog(this.$refs.dialog, '.crop-close')
					})
				}
				image.onerror = () => {
					this.visible = true
					this.localError = '无法读取这张图片。'
					this.$nextTick(() => focusDialog(this.$refs.dialog, '.crop-close'))
				}
				image.src = this.objectUrl
			},
			close() {
				if (this.saving) return
				this.visible = false
				this.image = null
				this.localError = ''
				this.releaseObjectUrl()
				this.$nextTick(() => restoreDialogFocus(this.returnFocus))
			},
			trapTab(event) {
				trapDialogTab(event, this.$refs.dialog)
			},
			releaseObjectUrl() {
				if (this.objectUrl) URL.revokeObjectURL(this.objectUrl)
				this.objectUrl = ''
			},
			currentScale() {
				return this.baseScale * this.zoom
			},
			constrainOffsets() {
				if (!this.image) return
				const width = this.image.naturalWidth * this.currentScale()
				const height = this.image.naturalHeight * this.currentScale()
				this.offsetX = Math.min(0, Math.max(PREVIEW_SIZE - width, this.offsetX))
				this.offsetY = Math.min(0, Math.max(PREVIEW_SIZE - height, this.offsetY))
			},
			applyZoom() {
				if (!this.image) return
				const previousScale = this._previousScale || this.baseScale
				const nextScale = this.currentScale()
				const centerX = (PREVIEW_SIZE / 2 - this.offsetX) / previousScale
				const centerY = (PREVIEW_SIZE / 2 - this.offsetY) / previousScale
				this.offsetX = PREVIEW_SIZE / 2 - centerX * nextScale
				this.offsetY = PREVIEW_SIZE / 2 - centerY * nextScale
				this._previousScale = nextScale
				this.constrainOffsets()
				this.draw()
			},
			startDrag(event) {
				if (!this.image) return
				this.dragging = true
				this.lastX = event.clientX
				this.lastY = event.clientY
				event.currentTarget.setPointerCapture(event.pointerId)
			},
			drag(event) {
				if (!this.dragging) return
				this.offsetX += event.clientX - this.lastX
				this.offsetY += event.clientY - this.lastY
				this.lastX = event.clientX
				this.lastY = event.clientY
				this.constrainOffsets()
				this.draw()
			},
			endDrag() {
				this.dragging = false
			},
			draw() {
				const canvas = this.$refs.canvas
				if (!canvas || !this.image) return
				const context = canvas.getContext('2d')
				context.clearRect(0, 0, PREVIEW_SIZE, PREVIEW_SIZE)
				context.drawImage(
					this.image,
					this.offsetX,
					this.offsetY,
					this.image.naturalWidth * this.currentScale(),
					this.image.naturalHeight * this.currentScale(),
				)
			},
			async save() {
				if (!this.image) return
				const output = document.createElement('canvas')
				output.width = OUTPUT_SIZE
				output.height = OUTPUT_SIZE
				output.getContext('2d').drawImage(this.$refs.canvas, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE)
				const blob = await new Promise(resolve => output.toBlob(resolve, 'image/png'))
				if (!blob) {
					this.localError = '头像生成失败，请重新选择图片。'
					return
				}
				this.$emit('save', blob)
			},
		},
	}
</script>

<style scoped>
	.crop-backdrop {
		position: fixed;
		z-index: 10050;
		inset: 0;
		display: grid;
		place-items: center;
		background: rgba(11, 18, 25, .72);
		padding: 24px;
	}

	.crop-dialog {
		width: min(480px, 100%);
		border-radius: 6px;
		background: #fff;
		box-shadow: 0 24px 70px rgba(0, 0, 0, .28);
		padding: 22px;
	}

	.crop-dialog header,
	.crop-dialog footer,
	.zoom-control {
		display: flex;
		align-items: center;
	}

	.crop-dialog header {
		justify-content: space-between;
		margin-bottom: 18px;
	}

	.crop-eyebrow {
		margin: 0 0 3px;
		color: #7b8794;
		font-size: 11px;
		font-weight: 700;
		letter-spacing: .14em;
		text-transform: uppercase;
	}

	.crop-dialog h2 {
		margin: 0;
		color: #1e2730;
		font-size: 22px;
	}

	.crop-close {
		border: 0;
		background: transparent;
		color: #6f7984;
		font-size: 28px;
		line-height: 1;
	}

	.crop-stage {
		position: relative;
		width: min(360px, 100%);
		aspect-ratio: 1;
		margin: 0 auto;
		overflow: hidden;
		background: #dfe5ea;
		cursor: grab;
		touch-action: none;
	}

	.crop-stage:active {
		cursor: grabbing;
	}

	.crop-stage canvas {
		display: block;
		width: 100%;
		height: 100%;
	}

	.crop-frame {
		position: absolute;
		inset: 0;
		border: 2px solid rgba(255, 255, 255, .92);
		box-shadow: inset 0 0 0 1px rgba(23, 50, 77, .35);
		pointer-events: none;
	}

	.zoom-control {
		gap: 12px;
		margin-top: 18px;
		color: #53606c;
		font-size: 13px;
	}

	.zoom-control input {
		flex: 1;
		accent-color: #17324d;
	}

	.zoom-control output {
		width: 42px;
		color: #7b8794;
		text-align: right;
	}

	.crop-help,
	.crop-error {
		margin: 10px 0 0;
		font-size: 12px;
	}

	.crop-help {
		color: #7b8794;
	}

	.crop-error {
		color: #b12d2d;
	}

	.crop-dialog footer {
		justify-content: flex-end;
		gap: 8px;
		margin-top: 18px;
	}

	.crop-dialog footer button {
		min-height: 38px;
		border-radius: 3px;
		padding: 0 16px;
		font-weight: 600;
	}

	.crop-secondary {
		border: 1px solid #cfd6dd;
		background: #fff;
		color: #4f5b66;
	}

	.crop-primary {
		border: 1px solid #17324d;
		background: #17324d;
		color: #fff;
	}
</style>
