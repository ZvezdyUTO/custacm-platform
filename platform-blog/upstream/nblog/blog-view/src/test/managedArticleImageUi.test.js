// Author: huangbingrui.awa
import {describe, expect, it, vi} from 'vitest'
import Blog from '@/views/blog/Blog.vue'

const thumbnail = '/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg'

describe('managed article image keyboard behavior', () => {
	it('marks managed thumbnails as keyboard-operable preview controls', () => {
		const article = document.createElement('div')
		article.innerHTML = `<img src="${thumbnail}" alt="流程图"><img src="/external.png" alt="外站图片">`

		Blog.methods.prepareManagedImages(article)

		const managed = article.querySelector('img')
		expect(managed.tabIndex).toBe(0)
		expect(managed.getAttribute('role')).toBe('button')
		expect(managed.getAttribute('aria-label')).toBe('查看大图：流程图')
		expect(article.querySelectorAll('[data-managed-preview]')).toHaveLength(1)
	})

	it('opens a managed thumbnail with Enter but ignores unrelated keys', () => {
		const image = document.createElement('img')
		image.src = thumbnail
		image.alt = '流程图'
		const open = vi.fn()
		const context = {$refs: {managedImageViewer: {open}}}
		const enterEvent = {type: 'keydown', key: 'Enter', target: image, preventDefault: vi.fn(), stopImmediatePropagation: vi.fn()}

		Blog.methods.openManagedImage.call(context, enterEvent)
		expect(open).toHaveBeenCalledWith(thumbnail, '/api/image/assets/123e4567-e89b-12d3-a456-426614174000/original.jpg', '流程图')

		Blog.methods.openManagedImage.call(context, {...enterEvent, key: 'ArrowDown'})
		expect(open).toHaveBeenCalledOnce()
	})
})
