// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import blogSource from '@/views/blog/Blog.vue?raw'
import indexSource from '@/views/Index.vue?raw'

describe('article reading header', () => {
	it('keeps the header focused on category, title and date', () => {
		expect(blogSource).toContain('<header class="article-hero article-reading-width">')
		expect(blogSource).toContain('--article-reading-width: 820px;')
		expect(blogSource).toContain('text-align: center;')
		expect(blogSource).toContain('<h1 class="article-title">{{ blog.title }}</h1>')
		expect(blogSource).toContain("$filters.dateFormat(blog.createTime, 'YYYY年M月D日')")
		expect(blogSource).not.toContain('blog.words')
		expect(blogSource).not.toContain('blog.readTime')
	})

	it('places the optional article summary below the cover in a quieter serif style', () => {
		expect(blogSource).toContain("v-if=\"blog.description\" class=\"article-summary article-copy-width\"")
		expect(blogSource).toContain('{{ blog.description }}')
		expect(blogSource).toContain('font-family: "Songti SC", STSong, "Noto Serif CJK SC", "Source Han Serif SC", serif;')
		expect(blogSource).toContain('font-size: 14px;')
		expect(blogSource).toContain('color: var(--anthropic-slate-light);')
	})

	it('places download and author-only edit actions beside the sidebar identity', () => {
		expect(blogSource).not.toContain('downloadArticle')
		expect(blogSource).not.toContain('article-download-link')
		expect(blogSource).not.toContain('article-edit-link')
		expect(indexSource).toContain('<template #article-actions>')
		expect(indexSource).toContain('v-if="canDownloadArticle"')
		expect(indexSource).toContain("articleDownloading ? '打包中' : '下载文章'")
		expect(indexSource).toContain('v-if="canEditArticle"')
		expect(indexSource).toContain('<AppIcon name="edit" /><span>编辑文章</span>')
		expect(indexSource).toContain('const blob = await downloadBlog(token, articleId)')
		expect(indexSource).toContain('this.authUser.username === this.articleAuthor.username')
		expect(indexSource).toContain('height: 18px;')
		expect(indexSource).toContain('background: transparent;')
		expect(indexSource).toContain('font-size: 10px;')
		expect(indexSource).toContain('width: 10px;')
		expect(indexSource).toContain('height: 10px;')
	})

	it('omits deferred reader controls and gives every article image one radius token', () => {
		expect(blogSource).not.toContain('bigFontSize')
		expect(blogSource).not.toContain('changeFocusMode')
		expect(blogSource).toContain('--article-media-radius: 20px;')
		expect(blogSource).toContain('border-radius: var(--article-media-radius) !important;')
		expect(blogSource).toContain('clip-path: inset(0 round var(--article-media-radius));')
		expect(blogSource).not.toContain('html.dark')
	})
})
