// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {describe, expect, it, vi} from 'vitest'
import Tags from '@/components/sidebar/Tags.vue'

describe('tag cloud colors', () => {
	it('keeps stored random colors and derives stable deep colors when legacy data has none', () => {
		const wrapper = shallowMount(Tags, {props: {tagList: []}})

		expect(wrapper.vm.tagColor({name: '动态规划', color: '#245A73'})).toBe('#245A73')
		const fallback = wrapper.vm.tagColor({name: '图论'})
		expect(fallback).toMatch(/^hsl\(\d+ \d+% 2\d%\)$/)
		expect(wrapper.vm.tagColor({name: '图论'})).toBe(fallback)
	})

	it('randomly samples at most 30 tags without changing the source list', () => {
		const tags = Array.from({length: 35}, (_, index) => ({name: `标签 ${index}`}))
		const random = vi.spyOn(Math, 'random').mockReturnValue(0)
		const wrapper = shallowMount(Tags, {props: {tagList: tags}})

		expect(wrapper.vm.displayedTagList).toHaveLength(30)
		expect(wrapper.vm.displayedTagList.map(tag => tag.name)).not.toEqual(tags.slice(0, 30).map(tag => tag.name))
		expect(tags.map(tag => tag.name)).toEqual(Array.from({length: 35}, (_, index) => `标签 ${index}`))
		random.mockRestore()
	})
})
