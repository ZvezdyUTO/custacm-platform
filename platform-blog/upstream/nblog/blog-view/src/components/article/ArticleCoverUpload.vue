<template>
	<section class="cover-upload" aria-label="文章首图">
		<div class="cover-heading"><span>文章首图（可选）</span><small>上传后裁剪为 1920×1080，并自动生成列表缩略图</small></div>
		<div v-if="previewUrl" class="cover-preview">
			<img :src="previewUrl" alt="文章首图预览">
			<div><label><input type="file" accept="image/jpeg,image/png" @change="selectFile">替换首图</label><button type="button" @click="$emit('clear')">移除</button></div>
		</div>
		<label v-else class="cover-empty"><input type="file" accept="image/jpeg,image/png" @change="selectFile"><strong>选择文章首图</strong><span>支持 JPEG/PNG，最大 10MB</span></label>
		<p v-if="errorMessage" class="cover-error" role="alert">{{ errorMessage }}</p>

		<Teleport to="body">
			<div v-if="cropSource" class="cover-crop-backdrop" @mousedown.self="closeCrop">
				<section ref="cropDialog" class="cover-crop-dialog" role="dialog" aria-modal="true" aria-labelledby="article-cover-crop-title"
				         tabindex="-1" @keydown.esc.stop="closeCrop" @keydown.tab="trapCropTab">
					<header><div><p>ARTICLE COVER</p><h2 id="article-cover-crop-title">裁剪文章首图</h2></div><button type="button" :disabled="uploading" @click="closeCrop">×</button></header>
					<div class="cover-crop-stage"><img ref="cropImage" :src="cropSource" alt="待裁剪首图" @load="initializeCropper"></div>
					<footer><button type="button" :disabled="uploading" @click="closeCrop">取消</button><button class="primary" type="button" :disabled="uploading" @click="uploadCrop">{{ uploading ? '正在上传…' : '裁剪并上传' }}</button></footer>
				</section>
			</div>
		</Teleport>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import Cropper from 'cropperjs'
	import 'cropperjs/dist/cropper.css'
	import {ARTICLE_COVER_MAX_BYTES, validateArticleImage} from '@/util/articleImages'
	import {focusDialog, restoreDialogFocus, trapDialogTab} from '@/util/dialogFocus'

	export default {
		name: 'ArticleCoverUpload',
		props: {
			cover: {type: Object, default: null},
			legacyUrl: {type: String, default: ''},
			uploadCover: {type: Function, required: true},
		},
		emits: ['uploaded', 'clear'],
		data() { return {cropSource: '', cropper: null, uploading: false, errorMessage: '', returnFocus: null} },
		computed: { previewUrl() { return this.cover?.thumbnailUrl || this.legacyUrl || '' } },
		beforeUnmount() { this.destroyCropper(); this.releaseSource(); restoreDialogFocus(this.returnFocus) },
		methods: {
			selectFile(event) {
				this.returnFocus = document.activeElement
				const file = event.target.files?.[0]
				event.target.value = ''
				if (!file) return
				try { validateArticleImage(file, ARTICLE_COVER_MAX_BYTES) }
				catch (error) { this.errorMessage = error.message; return }
				this.errorMessage = ''
				this.releaseSource()
				this.cropSource = URL.createObjectURL(file)
				this.$nextTick(() => focusDialog(this.$refs.cropDialog, 'header button'))
			},
			initializeCropper() {
				this.destroyCropper()
				this.cropper = new Cropper(this.$refs.cropImage, {aspectRatio: 16 / 9, autoCropArea: 1, background: false, dragMode: 'move', viewMode: 1})
			},
			async uploadCrop() {
				if (!this.cropper) return
				const canvas = this.cropper.getCroppedCanvas({width: 1920, height: 1080, imageSmoothingEnabled: true, imageSmoothingQuality: 'high'})
				const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', .9))
				if (!blob) { this.errorMessage = '首图裁剪失败，请换一张图片重试'; return }
				this.uploading = true
				let uploaded = false
				try {
					const asset = await this.uploadCover(new File([blob], 'article-cover.jpg', {type: 'image/jpeg'}))
					this.$emit('uploaded', asset)
					uploaded = true
				} catch (error) { this.errorMessage = error.message || '首图上传失败' }
				finally {
					this.uploading = false
					if (uploaded) this.closeCrop()
				}
			},
			closeCrop() {
				if (this.uploading) return
				this.destroyCropper()
				this.releaseSource()
				this.$nextTick(() => restoreDialogFocus(this.returnFocus))
			},
			trapCropTab(event) { trapDialogTab(event, this.$refs.cropDialog) },
			destroyCropper() { this.cropper?.destroy(); this.cropper = null },
			releaseSource() { if (this.cropSource) URL.revokeObjectURL(this.cropSource); this.cropSource = '' },
		},
	}
</script>

<style scoped>
	.cover-upload { display: grid; grid-template-rows: auto minmax(0, 1fr) auto; align-content: stretch; gap: 9px; height: 100%; }
	.cover-heading { display: flex; justify-content: space-between; gap: 16px; color: #34434f; font-weight: 700; }
	.cover-heading small { color: #7b8792; font-size: 11px; font-weight: 500; }
	.cover-preview { display: grid; grid-template-rows: minmax(0, 1fr) auto; overflow: hidden; border: 1px solid #d3dbe2; background: #f5f7f9; }
	.cover-preview img { display: block; width: 100%; height: 100%; min-height: 0; object-fit: cover; }
	.cover-preview > div { display: flex; justify-content: flex-end; gap: 8px; padding: 10px; }
	.cover-preview label, .cover-preview button { border: 1px solid #c9d2da; background: #fff; color: #344552; padding: 7px 11px; font-size: 12px; font-weight: 700; cursor: pointer; }
	.cover-preview input, .cover-empty input { position: absolute; width: 1px; height: 1px; opacity: 0; }
	.cover-empty { display: grid; place-items: center; min-height: 160px; height: 100%; border: 1px dashed #aebbc6; background: #f7f9fa; color: #344552; cursor: pointer; }
	.cover-empty span { color: #84909a; font-size: 11px; }
	.cover-error { margin: 0; color: #b13a3a; font-size: 12px; }
	.cover-crop-backdrop { position: fixed; z-index: 10060; inset: 0; display: grid; place-items: center; background: rgba(9,18,27,.75); padding: 24px; }
	.cover-crop-dialog { width: min(900px, 100%); background: #fff; padding: 20px; box-shadow: 0 28px 80px rgba(0,0,0,.35); }
	.cover-crop-dialog header, .cover-crop-dialog footer { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
	.cover-crop-dialog header p { margin: 0 0 4px; color: #7b8792; font-size: 10px; font-weight: 800; letter-spacing: .18em; }
	.cover-crop-dialog h2 { margin: 0; color: #202b34; }
	.cover-crop-dialog header button { border: 0; background: transparent; font-size: 28px; }
	.cover-crop-stage { height: min(58vh, 560px); margin: 18px 0; background: #18232d; }
	.cover-crop-stage img { display: block; max-width: 100%; }
	.cover-crop-dialog footer { justify-content: flex-end; }
	.cover-crop-dialog footer button { border: 1px solid #c7d0d8; background: #fff; padding: 9px 15px; font-weight: 700; }
	.cover-crop-dialog footer .primary { border-color: #17324d; background: #17324d; color: #fff; }
</style>
