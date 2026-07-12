// Author: huangbingrui.awa
import {afterEach, describe, expect, it, vi} from 'vitest'
import {mount} from '@vue/test-utils'
import ArticleCoverUpload from '@/components/article/ArticleCoverUpload.vue'

vi.mock('cropperjs', () => ({default: vi.fn()}))

function croppedCanvas() {
	return {toBlob: callback => callback(new Blob(['cover'], {type: 'image/jpeg'}))}
}

describe('ArticleCoverUpload', () => {
	afterEach(() => {
		document.body.innerHTML = ''
		vi.restoreAllMocks()
	})

	it('closes the crop dialog after a successful upload', async () => {
		const asset = {id: 1, thumbnailUrl: '/thumbnail.jpg'}
		const wrapper = mount(ArticleCoverUpload, {
			attachTo: document.body,
			props: {uploadCover: vi.fn().mockResolvedValue(asset)},
		})
		wrapper.vm.cropSource = 'blob:cover'
		wrapper.vm.cropper = {getCroppedCanvas: croppedCanvas, destroy: vi.fn()}

		await wrapper.vm.uploadCrop()

		expect(wrapper.emitted('uploaded')).toEqual([[asset]])
		expect(wrapper.vm.cropSource).toBe('')
		expect(wrapper.vm.uploading).toBe(false)
		wrapper.unmount()
	})

	it('keeps the crop dialog open when upload fails', async () => {
		const wrapper = mount(ArticleCoverUpload, {
			attachTo: document.body,
			props: {uploadCover: vi.fn().mockRejectedValue(new Error('上传失败'))},
		})
		wrapper.vm.cropSource = 'blob:cover'
		wrapper.vm.cropper = {getCroppedCanvas: croppedCanvas, destroy: vi.fn()}

		await wrapper.vm.uploadCrop()

		expect(wrapper.emitted('uploaded')).toBeUndefined()
		expect(wrapper.vm.cropSource).toBe('blob:cover')
		expect(wrapper.vm.errorMessage).toBe('上传失败')
		wrapper.unmount()
	})
})
