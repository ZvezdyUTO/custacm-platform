// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {describe, expect, it} from 'vitest'
import Tags from '@/components/sidebar/Tags.vue'

describe('tag cloud colors', () => {
	it('keeps stored random colors and derives stable deep colors when legacy data has none', () => {
		const wrapper = shallowMount(Tags, {props: {tagList: []}})

		expect(wrapper.vm.tagColor({name: '动态规划', color: '#245A73'})).toBe('#245A73')
		const fallback = wrapper.vm.tagColor({name: '图论'})
		expect(fallback).toMatch(/^hsl\(\d+ \d+% 2\d%\)$/)
		expect(wrapper.vm.tagColor({name: '图论'})).toBe(fallback)
	})
})
