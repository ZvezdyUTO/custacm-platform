// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {describe, expect, it, vi} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'
import FeaturedBlog from '@/components/sidebar/FeaturedBlog.vue'

const featuredSource = readFileSync(resolve(process.cwd(), 'src/components/sidebar/FeaturedBlog.vue'), 'utf8')

describe('home featured articles', () => {
	it('shows at most three cards and opens the selected article', async () => {
		const dispatch = vi.fn()
		const blogs = Array.from({length: 4}, (_, index) => ({
			id: index + 1,
			title: `文章 ${index + 1}`,
			description: `<p>简介 ${index + 1}</p>`,
			firstPicture: `/img/article-${index + 1}.jpg`,
			createTime: '2026-07-14',
			categoryName: '训练经验',
			top: index === 0,
		}))
		blogs[1].description = ''
		const wrapper = shallowMount(FeaturedBlog, {
			props: {featuredBlogList: blogs},
			global: {
				mocks: {
					$filters: {dateFormat: value => value},
					$store: {dispatch},
				},
			},
		})

		expect(wrapper.findAll('.featured-release-card')).toHaveLength(3)
		expect(wrapper.findAll('.featured-release-media')).toHaveLength(3)
		expect(wrapper.findAll('.featured-release-meta')).toHaveLength(3)
		expect(wrapper.text()).toContain('训练经验')
		expect(wrapper.text()).toContain('这篇文章暂时没有填写简介。')
		expect(wrapper.text()).not.toContain('置顶')
		expect(wrapper.text()).not.toContain('阅读全文')
		expect(wrapper.text()).not.toContain('文章 4')

		await wrapper.find('.featured-release-card').trigger('click')
		expect(dispatch).toHaveBeenCalledWith('goBlogPage', blogs[0])
	})

	it('uses one warm card color without shadows and keeps metadata at the content bottom', () => {
		expect(featuredSource).toContain('background: #eadfce;')
		expect(featuredSource).toContain('outline: 2px solid #8a5f24;')
		expect(featuredSource).toContain('box-shadow: none;')
		expect(featuredSource).toContain('margin: auto 0 0;')
		expect(featuredSource).toContain(':global(html[data-theme="dark"] .featured-release-card)')
		expect(featuredSource).toContain('background: #2c2723;')
		expect(featuredSource).not.toContain('#ebe4d8')
		expect(featuredSource).not.toContain('#d2d2d7')
		expect(featuredSource).not.toContain('.featured-release-card:nth-child(2) { background:')
	})
})
