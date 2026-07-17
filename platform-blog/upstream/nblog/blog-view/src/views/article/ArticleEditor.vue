<template>
	<section class="article-editor-page content-panel" aria-labelledby="editor-title">
		<header class="editor-page-heading">
			<div>
				<p>ACM NOTEBOOK</p>
				<h1 id="editor-title">{{ editing ? '编辑文章' : '发布文章' }}</h1>
				<span>用实时 Markdown 记录题解和训练复盘，也可以直接导入 Markdown。</span>
			</div>
			<router-link to="/profile">返回我的主页</router-link>
		</header>

		<div v-if="loading" class="editor-loading">正在准备文章…</div>
		<div v-else-if="loadFailed" class="editor-load-failed"><strong>无法打开这篇文章</strong><span>{{ errorMessage }}</span><router-link to="/profile">返回我的主页</router-link></div>
		<form v-else class="article-form" @submit.prevent="save(true)" @input="markDirty" @change="markDirty">
			<section class="article-basics" aria-label="文章标题、描述与首图">
				<div class="article-basics-fields">
					<label class="article-title-field"><span>标题 <small>{{ titleLength }}/{{ limits.title }}</small></span><input v-model="form.title" :maxlength="limits.title" required aria-label="标题"></label>
					<label class="article-description-field"><span>文章简介 <small>{{ descriptionLength }}/{{ limits.description }}</small></span><textarea v-model="form.description" :maxlength="limits.description" rows="5" required aria-label="文章简介"></textarea></label>
				</div>
				<ArticleCoverUpload :cover="form.coverImage" :legacy-url="form.firstPicture" :upload-cover="uploadCover" @uploaded="coverUploaded" @clear="clearCover"/>
			</section>
			<div class="taxonomy-grid">
				<label class="taxonomy-field"><span>分类</span><select v-model="form.categoryId" required><option disabled value="">选择分类</option><option v-for="category in categories" :key="category.id" :value="category.id">{{ category.name }}</option></select></label>
				<div class="taxonomy-field tag-field" role="group" aria-labelledby="article-tag-label">
					<div class="taxonomy-heading"><span id="article-tag-label">标签</span><small>可多选，也可以创建新标签</small></div>
					<div class="tag-add-row"><input v-model="newTagName" maxlength="30" placeholder="输入新标签，按回车添加" @compositionstart="isComposingTag = true" @compositionend="isComposingTag = false" @keydown.enter="addTagFromKeyboard"><button type="button" :disabled="!newTagName.trim()" @click="addTag">添加</button></div>
					<div class="tag-options" aria-label="文章标签">
						<button v-for="tag in tags" :key="`existing-${tag.id}`" type="button" :class="{selected: isTagSelected(tag.id)}" :aria-label="tag.name" :aria-pressed="isTagSelected(tag.id)" @click="toggleTag(tag.id)">{{ tag.name }}</button>
						<button v-for="tagName in customTags" :key="`custom-${tagName}`" type="button" class="selected custom-tag" aria-pressed="true" :title="`移除标签 ${tagName}`" @click="removeCustomTag(tagName)">{{ tagName }} ×</button>
						<em v-if="!tags.length && !customTags.length">暂无已有标签，可以在上方创建</em>
					</div>
				</div>
			</div>

			<section class="markdown-import" aria-label="Markdown 文件导入">
				<div><strong>从 Markdown 开始</strong><span>读取本地 .md/.markdown 文件，内容只会填入下方编辑器。</span></div>
				<input ref="markdownInput" class="visually-hidden" type="file" accept=".md,.markdown,text/markdown" @change="importMarkdown">
				<button type="button" @click="$refs.markdownInput.click()"><AppIcon name="file" />{{ importedFilename || '选择 Markdown' }}</button>
			</section>

			<div class="content-field" role="group" aria-labelledby="article-content-label"><span id="article-content-label">正文</span><LiveMarkdownEditor v-model="form.content" :upload-image="uploadContentImage" @dirty="markDirty"/><small :class="{limitWarning: contentLength > limits.content}">{{ contentLength }}/{{ limits.content }} 字 · 预计 {{ readMinutes }} 分钟</small></div>
			<div class="publish-options">
				<label><input v-model="form.internal" type="checkbox"><span>内部文章</span><small>仅登录用户可阅读</small></label>
				<label><input v-model="form.commentEnabled" type="checkbox"><span>允许评论</span><small>仅登录用户可以发表评论</small></label>
			</div>
			<p v-if="errorMessage" class="editor-error" role="alert">{{ errorMessage }}</p>
			<footer class="editor-footer">
				<span>{{ dirty ? '有未保存的修改' : '已保存' }}</span>
				<div><router-link to="/profile">取消</router-link><button class="draft-button" type="button" :disabled="saving" @click="save(false)">保存为草稿</button><button type="submit" :disabled="saving">{{ saving ? '保存中…' : '立即发布' }}</button></div>
			</footer>
		</form>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {createMyBlog, deleteMyImage, getMyBlog, getPlayerCategoryAndTag, updateMyBlog, uploadMyImage} from '@/api/player-blog'
	import {clearSession, readToken} from '@/auth/session'
	import {articleRequest, markdownTextFromFile} from '@/util/articleForm'
	import LiveMarkdownEditor from '@/components/article/LiveMarkdownEditor.vue'
	import ArticleCoverUpload from '@/components/article/ArticleCoverUpload.vue'
	import {contentUsesAsset} from '@/util/articleImages'

	const ARTICLE_LIMITS = Object.freeze({title: 100, description: 255, content: 200000})

	const emptyForm = () => ({id: null, title: '', firstPicture: '', firstPictureAssetId: null, coverImage: null, description: '', content: '', categoryId: '', tagList: [], published: true, internal: false, commentEnabled: true})

	export default {
		name: 'ArticleEditor',
		components: {LiveMarkdownEditor, ArticleCoverUpload},
		data() { return {form: emptyForm(), limits: ARTICLE_LIMITS, categories: [], tags: [], temporaryAssets: [], newTagName: '', isComposingTag: false, loading: true, loadFailed: false, saving: false, dirty: false, importedFilename: '', errorMessage: ''} },
		computed: {
			articleId() { return this.$route.params.id ? Number(this.$route.params.id) : null },
			editing() { return Number.isInteger(this.articleId) && this.articleId > 0 },
			wordCount() { return Array.from(this.form.content.trim()).length },
			titleLength() { return Array.from(this.form.title).length },
			descriptionLength() { return Array.from(this.form.description).length },
			contentLength() { return Array.from(this.form.content).length },
			readMinutes() { return Math.max(1, Math.round(this.wordCount / 200)) },
			customTags() { return this.form.tagList.filter(tag => typeof tag === 'string') },
		},
		watch: {
			'form.content'(content, previous) { this.cleanupRemovedTemporaryContentImages(content, previous) },
		},
		mounted() { window.addEventListener('beforeunload', this.beforeUnload); this.load() },
		beforeUnmount() { window.removeEventListener('beforeunload', this.beforeUnload) },
		async beforeRouteLeave(to, from, next) {
			if (this.dirty && !window.confirm('文章还没有保存，确定离开吗？')) return next(false)
			await this.cleanupTemporaryAssets(() => false)
			next()
		},
		methods: {
			addTagFromKeyboard(event) {
				if (this.isComposingTag || event.isComposing || event.keyCode === 229) return
				event.preventDefault()
				this.addTag()
			},
			isTagSelected(tagId) { return this.form.tagList.includes(Number(tagId)) },
			toggleTag(tagId) {
				const id = Number(tagId)
				const index = this.form.tagList.indexOf(id)
				if (index >= 0) this.form.tagList.splice(index, 1)
				else this.form.tagList.push(id)
				this.dirty = true
			},
			addTag() {
				const name = this.newTagName.trim()
				if (!name) return
				const existing = this.tags.find(tag => tag.name?.trim().toLowerCase() === name.toLowerCase())
				if (existing) {
					if (!this.isTagSelected(existing.id)) this.form.tagList.push(Number(existing.id))
				} else if (!this.customTags.some(tag => tag.toLowerCase() === name.toLowerCase())) {
					this.form.tagList.push(name)
				}
				this.newTagName = ''
				this.dirty = true
			},
			removeCustomTag(tagName) {
				const index = this.form.tagList.indexOf(tagName)
				if (index >= 0) this.form.tagList.splice(index, 1)
				this.dirty = true
			},
			async uploadCover(file) {
				return this.trackTemporaryAsset(await uploadMyImage(readToken(), file, 'ARTICLE_COVER'))
			},
			async uploadContentImage(file) {
				return this.trackTemporaryAsset(await uploadMyImage(readToken(), file, 'ARTICLE_CONTENT'))
			},
			trackTemporaryAsset(asset) {
				this.temporaryAssets.push(asset)
				return asset
			},
			async coverUploaded(asset) {
				const previous = this.form.coverImage
				if (previous && this.temporaryAssets.some(item => item.id === previous.id)) await this.deleteTemporaryAsset(previous)
				this.form.coverImage = asset
				this.form.firstPictureAssetId = asset.id
				this.form.firstPicture = asset.thumbnailUrl
				this.dirty = true
			},
			async clearCover() {
				if (this.form.coverImage && this.temporaryAssets.some(item => item.id === this.form.coverImage.id)) {
					await this.deleteTemporaryAsset(this.form.coverImage)
				}
				this.form.coverImage = null
				this.form.firstPictureAssetId = null
				this.form.firstPicture = ''
				this.dirty = true
			},
			async deleteTemporaryAsset(asset) {
				try { await deleteMyImage(readToken(), asset.id) } catch (_) { /* 后端定时清理兜底 */ }
				this.temporaryAssets = this.temporaryAssets.filter(item => item.id !== asset.id)
			},
			cleanupRemovedTemporaryContentImages(content, previous) {
				const removed = this.temporaryAssets.filter(asset => asset.purpose === 'ARTICLE_CONTENT'
					&& contentUsesAsset(previous, asset) && !contentUsesAsset(content, asset))
				if (removed.length) void Promise.all(removed.map(asset => this.deleteTemporaryAsset(asset)))
			},
			async cleanupTemporaryAssets(retain) {
				const disposable = this.temporaryAssets.filter(asset => !retain(asset))
				await Promise.allSettled(disposable.map(asset => deleteMyImage(readToken(), asset.id)))
				this.temporaryAssets = this.temporaryAssets.filter(asset => retain(asset))
			},
			async load() {
				const token = readToken()
				if (!token) return this.goLogin()
				this.loading = true
				this.loadFailed = false
				try {
					const [taxonomy, blog] = await Promise.all([getPlayerCategoryAndTag(token), this.editing ? getMyBlog(token, this.articleId) : Promise.resolve(null)])
					this.categories = taxonomy.categories || []
					this.tags = taxonomy.tags || []
					if (blog) this.form = {
						id: blog.id, title: blog.title || '', firstPicture: blog.firstPicture || '', firstPictureAssetId: blog.firstPictureAssetId || null, coverImage: blog.coverImage || null, description: blog.description || '', content: blog.content || '',
						categoryId: blog.category?.id || '', tagList: (blog.tags || []).map(tag => Number(tag.id)), published: Boolean(blog.published), internal: Boolean(blog.internal), commentEnabled: Boolean(blog.commentEnabled),
					}
					this.dirty = false
				} catch (error) { this.loadFailed = true; this.handleError(error, '文章编辑器加载失败') }
				finally { this.loading = false }
			},
			markDirty() { this.dirty = true },
			async importMarkdown(event) {
				const file = event.target.files?.[0]
				if (!file) return
				try {
					const content = await markdownTextFromFile(file)
					if (Array.from(content).length > this.limits.content) throw new Error(`文章正文不能超过 ${this.limits.content} 字`)
					this.form.content = content; this.importedFilename = file.name; this.dirty = true; this.errorMessage = ''
				}
				catch (error) { this.errorMessage = error.message }
				finally { event.target.value = '' }
			},
			async save(published) {
				const token = readToken()
				if (!token) return this.goLogin()
				if (!this.form.categoryId) return this.errorMessage = '请选择文章分类'
				if (!this.form.content.trim()) return this.errorMessage = '请填写文章正文'
				if (this.titleLength > this.limits.title) return this.errorMessage = `文章标题不能超过 ${this.limits.title} 字`
				if (this.descriptionLength > this.limits.description) return this.errorMessage = `文章简介不能超过 ${this.limits.description} 字`
				if (this.contentLength > this.limits.content) return this.errorMessage = `文章正文不能超过 ${this.limits.content} 字`
				this.saving = true
				this.errorMessage = ''
				try {
					const request = articleRequest({...this.form, published})
					const savedId = this.editing ? (await updateMyBlog(token, request), this.articleId) : await createMyBlog(token, request)
					await this.cleanupTemporaryAssets(asset => asset.id === request.firstPictureAssetId || contentUsesAsset(request.content, asset))
					this.temporaryAssets = []
					this.dirty = false
					this.msgSuccess(request.published ? '文章已发布' : '草稿已保存')
					await this.$router.push(request.published ? `/blog/${savedId}` : '/profile')
				} catch (error) { this.handleError(error, '文章保存失败') }
				finally { this.saving = false }
			},
			goLogin() { this.$router.replace({path: '/training/login', query: {returnTo: this.$route.fullPath}}) },
			handleError(error, fallback) {
				if (error?.response?.status === 401) { clearSession(); this.goLogin(); return }
				this.errorMessage = error?.response?.data?.msg || error?.message || fallback
			},
			beforeUnload(event) { if (!this.dirty) return; event.preventDefault(); event.returnValue = '' },
		},
	}
</script>

<style scoped>
	.article-editor-page { border-top: 3px solid #17324d !important; padding: 36px 40px !important; color: #28343e; }
	.editor-page-heading { display: flex; align-items: flex-start; justify-content: space-between; gap: 24px; border-bottom: 1px solid #dfe4e9; padding-bottom: 24px; }
	.editor-page-heading p { margin: 0 0 8px; color: #6d7a87; font-size: 11px; font-weight: 800; letter-spacing: .18em; }
	.editor-page-heading h1 { margin: 0; font-size: 30px; }
	.editor-page-heading span { display: block; margin-top: 9px; color: #7b8792; font-size: 13px; }
	.editor-page-heading > a { border: 1px solid #ccd4dc; color: #43515d; padding: 9px 12px; font-size: 12px; font-weight: 700; }
	.article-form { display: grid; gap: 20px; margin-top: 26px; }
	.article-basics { display: grid; grid-template-columns: minmax(0, 1fr) minmax(420px, 1fr); align-items: stretch; gap: 22px; border: 1px solid #d8e0e6; background: #fafbfc; padding: 20px; }
	.article-basics-fields { display: grid; grid-template-rows: auto minmax(0, 1fr); gap: 18px; min-width: 0; }
	.article-basics-fields > label { display: grid; align-content: start; gap: 8px; min-width: 0; }
	.article-title-field input { height: 44px; }
	.article-description-field { grid-template-rows: auto minmax(0, 1fr); }
	.article-description-field textarea { min-height: 170px; height: 100%; }
	.article-form label > span, .content-field > span, .taxonomy-heading > span { color: #394752; font-size: 12px; font-weight: 800; }
	.article-form label > span small { float: right; color: #8a949d; font-size: 11px; font-weight: 500; }
	.article-form input[type='text'], .article-form input:not([type]), .article-form textarea, .article-form select { width: 100%; border: 1px solid #d2d9e0; background: #fff; color: #26323c; padding: 11px 12px; font: inherit; font-size: 14px; outline: none; }
	.article-form input:focus, .article-form textarea:focus, .article-form select:focus { border-color: #17324d; box-shadow: 0 0 0 2px rgba(23, 50, 77, .1); }
	.article-form textarea { resize: vertical; line-height: 1.65; }
	.taxonomy-grid { display: grid; grid-template-columns: minmax(240px, .8fr) minmax(360px, 1.2fr); align-items: start; gap: 22px; }
	.taxonomy-field { display: grid; align-content: start; gap: 8px; min-width: 0; }
	.taxonomy-heading { display: flex; align-items: baseline; justify-content: space-between; gap: 12px; min-height: 17px; }
	.taxonomy-heading small { color: #8a96a0; font-size: 10px; }
	.tag-add-row { display: grid; grid-template-columns: minmax(0, 1fr) auto; overflow: hidden; border: 1px solid #d2d9e0; background: #fff; }
	.tag-add-row:focus-within { border-color: #17324d; box-shadow: 0 0 0 2px rgba(23, 50, 77, .1); }
	.tag-add-row input { min-width: 0; border: 0 !important; box-shadow: none !important; padding: 10px 11px !important; font-size: 12px !important; }
	.tag-add-row button { min-width: 58px; border: 0; border-left: 1px solid #17324d; background: #17324d; color: #fff; padding: 0 13px; font: inherit; font-size: 11px; font-weight: 800; cursor: pointer; }
	.tag-add-row button:disabled { border-left-color: #e0e5e9; background: #f3f5f7; color: #a0a9b1; cursor: default; }
	.tag-options { display: flex; min-height: 27px; flex-wrap: wrap; align-items: center; gap: 7px; }
	.tag-options button { border: 1px solid #d4dce3; border-radius: 999px; background: #f7f9fa; color: #5c6b77; padding: 5px 10px; font: inherit; font-size: 11px; line-height: 1; cursor: pointer; transition: border-color .15s, background-color .15s, color .15s; }
	.tag-options button:hover { border-color: #8293a1; background: #eef2f5; color: #273944; }
	.tag-options button.selected { border-color: #294c6b; background: #294c6b; color: #fff; }
	.tag-options button.selected::before { content: '✓'; margin-right: 5px; font-size: 9px; }
	.tag-options button.custom-tag { border-color: #9eb1c1; background: #eaf0f5; color: #294c6b; }
	.tag-options button.custom-tag::before { content: none; }
	.tag-options em { color: #929ca5; font-size: 11px; }
	.markdown-import { display: flex; align-items: center; justify-content: space-between; gap: 20px; border-left: 4px solid #17324d; background: #eef2f5; padding: 15px 17px; }
	.markdown-import div { display: grid; gap: 4px; }
	.markdown-import strong { font-size: 13px; }
	.markdown-import span { color: #71808c; font-size: 11px; }
	.markdown-import button { border: 1px solid #17324d; background: #fff; color: #17324d; padding: 9px 12px; font: inherit; font-size: 12px; font-weight: 800; cursor: pointer; }
	.visually-hidden { position: absolute; width: 1px; height: 1px; overflow: hidden; clip: rect(0 0 0 0); }
	.content-field { position: relative; display: grid; gap: 8px; }
	.content-field small { position: absolute; z-index: 2; right: 12px; bottom: 10px; background: rgba(255,255,255,.9); color: #8a949d; padding-left: 8px; font-size: 11px; }
	.content-field small.limitWarning { color: #a14b4b; font-weight: 800; }
	.publish-options { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
	.publish-options label { display: grid; grid-template-columns: auto 1fr; align-items: center; gap: 3px 9px; border: 1px solid #dbe1e6; padding: 14px; cursor: pointer; }
	.publish-options input { grid-row: 1 / 3; }
	.publish-options small { color: #87929c; font-size: 11px; }
	.editor-footer { display: flex; align-items: center; justify-content: space-between; border-top: 1px solid #dfe4e9; padding-top: 20px; }
	.editor-footer > span { color: #8a949d; font-size: 11px; }
	.editor-footer div { display: flex; align-items: center; gap: 10px; }
	.editor-footer a { color: #65727d; padding: 10px 14px; }
	.editor-footer button { border: 1px solid #17324d; background: #17324d; color: #fff; min-width: 120px; padding: 10px 16px; font: inherit; font-weight: 800; cursor: pointer; }
	.editor-footer .draft-button { border-color: #b8c4cf; background: #eef2f5; color: #526272; }
	.editor-footer button:disabled { cursor: wait; opacity: .55; }
	.editor-error { margin: 0; border-left: 3px solid #a14b4b; background: #f8eeee; color: #7b3434; padding: 10px 12px; }
	.editor-loading { min-height: 360px; display: grid; place-items: center; color: #7f8b95; }
	.editor-load-failed { min-height: 320px; display: grid; place-content: center; justify-items: center; gap: 10px; color: #7f8b95; text-align: center; }
	.editor-load-failed strong { color: #34424d; font-size: 20px; }
	.editor-load-failed a { margin-top: 8px; border: 1px solid #17324d; color: #17324d; padding: 9px 13px; font-weight: 700; }

	.article-editor-page {
		--color-action: var(--anthropic-clay);
		--color-action-hover: color-mix(in srgb, var(--anthropic-clay) 82%, var(--anthropic-dark));
		--color-on-action: #fff;
		--color-surface: var(--anthropic-ivory-light);
		--color-surface-subtle: var(--anthropic-ivory-medium);
		--color-border: var(--anthropic-cloud-light);
		--color-border-strong: var(--anthropic-cloud-medium);
		--color-text: var(--anthropic-slate-dark);
		--color-text-muted: var(--anthropic-slate-light);
		--color-text-faint: var(--anthropic-cloud-dark);
		border-top-color: var(--anthropic-clay) !important;
		background: var(--anthropic-ivory-light) !important;
		color: var(--anthropic-slate-dark);
	}
	.editor-page-heading, .editor-footer { border-color: var(--anthropic-cloud-light); }
	.editor-page-heading p { color: var(--anthropic-slate-light); }
	.editor-page-heading h1, .markdown-import strong, .editor-load-failed strong { color: var(--anthropic-slate-dark); }
	.editor-page-heading span, .markdown-import span, .publish-options small, .editor-footer > span, .editor-loading, .editor-load-failed { color: var(--anthropic-slate-light); }
	.editor-page-heading > a, .editor-footer a { border-color: var(--anthropic-cloud-medium); color: var(--anthropic-slate-medium); }
	.article-basics { border-color: var(--anthropic-cloud-light) !important; background: var(--anthropic-ivory-medium) !important; }
	.article-form label > span, .content-field > span, .taxonomy-heading > span { color: var(--anthropic-slate-medium); }
	.article-form label > span small, .taxonomy-heading small, .tag-options em { color: var(--anthropic-cloud-dark); }
	.article-form input[type='text'], .article-form input:not([type]), .article-form textarea, .article-form select, .tag-add-row {
		border-color: var(--anthropic-cloud-light);
		background: var(--anthropic-ivory-light);
		color: var(--anthropic-slate-dark);
	}
	.article-form input:focus, .article-form textarea:focus, .article-form select:focus, .tag-add-row:focus-within {
		border-color: var(--anthropic-clay);
		box-shadow: 0 0 0 2px color-mix(in srgb, var(--anthropic-clay) 18%, transparent);
	}
	.tag-add-row button { border-left-color: var(--anthropic-dark); background: var(--anthropic-dark); color: var(--anthropic-ivory-light); }
	.tag-add-row button:disabled { border-left-color: var(--anthropic-cloud-light); background: var(--anthropic-ivory-dark); color: var(--anthropic-cloud-dark); }
	.tag-options button { border-color: var(--anthropic-cloud-light); background: var(--anthropic-ivory-medium); color: var(--anthropic-slate-light); }
	.tag-options button:hover { border-color: var(--anthropic-cloud-medium); background: var(--anthropic-ivory-dark); color: var(--anthropic-slate-dark); }
	.tag-options button.selected { border-color: var(--anthropic-clay); background: var(--anthropic-clay); color: #fff; }
	.tag-options button.custom-tag { border-color: var(--anthropic-cloud-medium); background: var(--anthropic-ivory-dark); color: var(--anthropic-slate-medium); }
	.markdown-import { border-left-color: var(--anthropic-clay) !important; background: var(--anthropic-ivory-medium) !important; }
	.markdown-import button { border-color: var(--anthropic-slate-dark); background: var(--anthropic-ivory-light); color: var(--anthropic-slate-dark); }
	.content-field small { background: color-mix(in srgb, var(--anthropic-ivory-light) 90%, transparent); color: var(--anthropic-cloud-dark); }
	.content-field small.limitWarning { color: var(--anthropic-error); }
	.publish-options label { border-color: var(--anthropic-cloud-light); background: var(--anthropic-ivory-light); }
	.editor-footer button { border-color: var(--anthropic-dark); background: var(--anthropic-dark); color: var(--anthropic-ivory-light); }
	.editor-footer button[type='submit'] { border-color: var(--anthropic-clay) !important; background: var(--anthropic-clay) !important; color: #fff !important; }
	.editor-footer .draft-button { border-color: var(--anthropic-cloud-medium); background: var(--anthropic-ivory-medium); color: var(--anthropic-slate-medium); }
	.editor-error { border-left-color: var(--anthropic-error); background: #ebcece; color: var(--anthropic-dark); }
	.editor-load-failed a { border-color: var(--anthropic-slate-dark); color: var(--anthropic-slate-dark); }
	@media (max-width: 1100px) { .article-basics { grid-template-columns: 1fr; } .article-description-field textarea { min-height: 130px; } }
	@media (max-width: 900px) { .taxonomy-grid, .publish-options { grid-template-columns: 1fr; } .markdown-import, .editor-page-heading { align-items: stretch; flex-direction: column; } }
</style>
