// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import {EditorState} from '@codemirror/state'
import {EditorView} from '@codemirror/view'
import {contentUsesAsset, deleteManagedImageBackward, markdownForImage, originalUrlForManagedThumbnail, validateArticleImage} from '@/util/articleImages'

describe('article images', () => {
	it('generates standard image markdown so live preview renders the thumbnail', () => {
		const asset = {publicId: 'asset-id', thumbnailUrl: '/thumb.jpg', originalUrl: '/original.jpg'}
		expect(markdownForImage(asset, '流程图')).toBe('![流程图](/thumb.jpg)')
		expect(contentUsesAsset('![x](/api/image/assets/asset-id/thumbnail.jpg)', asset)).toBe(true)
	})

	it('derives the original URL only for managed thumbnail URLs', () => {
		const thumbnail = '/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg'
		expect(originalUrlForManagedThumbnail(thumbnail)).toBe('/api/image/assets/123e4567-e89b-12d3-a456-426614174000/original.jpg')
		expect(originalUrlForManagedThumbnail('https://example.com/image.jpg')).toBe('')
	})

	it('deletes a managed image as one atomic Backspace operation', () => {
		const image = '![流程图](/api/image/assets/123e4567-e89b-12d3-a456-426614174000/thumbnail.jpg)'
		const view = new EditorView({state: EditorState.create({doc: `${image}\n正文`, selection: {anchor: image.length}})})

		expect(deleteManagedImageBackward(view)).toBe(true)
		expect(view.state.doc.toString()).toBe('\n正文')
		view.destroy()
	})

	it('accepts JPEG and PNG up to 15MB and rejects other formats', () => {
		expect(() => validateArticleImage({type: 'image/png', size: 15 * 1024 * 1024})).not.toThrow()
		expect(() => validateArticleImage({type: 'image/gif', size: 1})).toThrow('JPEG')
		expect(() => validateArticleImage({type: 'image/jpeg', size: 15 * 1024 * 1024 + 1})).toThrow('15MB')
	})
})
