<template>
	<Teleport to="body">
		<div v-if="visible" class="managed-viewer" role="dialog" aria-modal="true" aria-label="文章图片预览" @mousedown.self="close">
			<button class="viewer-close" type="button" aria-label="关闭图片预览" @click="close">×</button>
			<div class="viewer-stage"><img :src="displayUrl" :alt="alt"></div>
			<div class="viewer-toolbar">
				<span>{{ originalLoaded ? '正在查看原图' : '缩略图预览' }}</span>
				<button v-if="!originalLoaded" type="button" :disabled="loading" @click="loadOriginal">{{ loading ? '正在加载…' : '加载原图' }}</button>
			</div>
			<p v-if="errorMessage" role="alert">{{ errorMessage }}</p>
		</div>
	</Teleport>
</template>

<script>
	// Author: huangbingrui.awa
	export default {
		name: 'ManagedImageViewer',
		data() { return {visible: false, thumbnailUrl: '', originalUrl: '', displayUrl: '', alt: '', loading: false, originalLoaded: false, errorMessage: ''} },
		methods: {
			open(thumbnailUrl, originalUrl, alt = '') {
				this.thumbnailUrl = thumbnailUrl
				this.originalUrl = originalUrl
				this.displayUrl = thumbnailUrl
				this.alt = alt
				this.loading = false
				this.originalLoaded = false
				this.errorMessage = ''
				this.visible = true
			},
			close() { if (!this.loading) this.visible = false },
			loadOriginal() {
				if (!this.originalUrl || this.loading) return
				this.loading = true
				this.errorMessage = ''
				const image = new Image()
				image.onload = () => { this.displayUrl = this.originalUrl; this.originalLoaded = true; this.loading = false }
				image.onerror = () => { this.errorMessage = '原图加载失败，请稍后重试'; this.loading = false }
				image.src = this.originalUrl
			},
		},
	}
</script>

<style scoped>
	.managed-viewer { position: fixed; z-index: 10070; inset: 0; display: grid; grid-template-rows: 1fr auto auto; place-items: center; background: rgba(4,10,15,.92); padding: 56px 24px 24px; color: #fff; }
	.viewer-stage { display: grid; place-items: center; width: 100%; height: 100%; min-height: 0; }
	.viewer-stage img { display: block; max-width: min(94vw, 1600px); max-height: 76vh; object-fit: contain; }
	.viewer-close { position: absolute; top: 16px; right: 22px; border: 0; background: transparent; color: #fff; font-size: 36px; }
	.viewer-toolbar { display: flex; align-items: center; gap: 16px; margin-top: 16px; color: #c9d1d8; font-size: 12px; }
	.viewer-toolbar button { border: 1px solid rgba(255,255,255,.55); background: #fff; color: #182b3b; padding: 9px 16px; font-weight: 800; }
	.viewer-toolbar button:disabled { opacity: .65; }
	.managed-viewer p { margin: 10px 0 0; color: #ffb5b5; font-size: 12px; }
</style>
