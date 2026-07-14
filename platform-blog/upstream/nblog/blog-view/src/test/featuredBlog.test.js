// Author: huangbingrui.awa
import {shallowMount} from '@vue/test-utils'
import {describe, expect, it, vi} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'
import FeaturedBlog from '@/components/sidebar/FeaturedBlog.vue'

const featuredSource = readFileSync(resolve(process.cwd(), 'src/components/sidebar/FeaturedBlog.vue'), 'utf8')

function article(id, overrides = {}) {
	return {
		id,
		title: `文章 ${id}`,
		description: `<p>简介 ${id}</p>`,
		firstPicture: `/img/article-${id}.jpg`,
		createTime: '2026-07-14',
		categoryName: '训练经验',
		tags: [],
		authorNickname: `作者 ${id}`,
		authorUsername: `player-${id}`,
		authorAvatar: `/img/avatar-${id}.jpg`,
		...overrides,
	}
}

function mountFeatured(featuredGroups, dispatch = vi.fn()) {
	return {
		dispatch,
		wrapper: shallowMount(FeaturedBlog, {
			props: {featuredGroups},
			global: {
				mocks: {
					$filters: {dateFormat: value => value},
					$store: {dispatch},
				},
			},
		}),
	}
}

describe('home featured article groups', () => {
	it('shows at most three groups and three articles in each group', () => {
		const featuredGroups = Array.from({length: 4}, (_, groupIndex) => ({
			id: groupIndex + 1,
			title: `分组 ${groupIndex + 1}`,
			articles: Array.from({length: 4}, (_, articleIndex) => article(groupIndex * 10 + articleIndex + 1)),
		}))
		featuredGroups[0].articles[1].description = ''
		const {wrapper} = mountFeatured(featuredGroups)

		expect(wrapper.findAll('.featured-release-section')).toHaveLength(3)
		expect(wrapper.findAll('.featured-release-card')).toHaveLength(9)
		expect(wrapper.findAll('.featured-release-media')).toHaveLength(9)
		expect(wrapper.text()).toContain('分组 3')
		expect(wrapper.text()).not.toContain('分组 4')
		expect(wrapper.text()).not.toContain('文章 4')
		expect(wrapper.text()).toContain('这篇文章暂时没有填写简介。')
		expect(wrapper.text()).toContain('作者 1')
		expect(wrapper.text()).toContain('@player-1')
		expect(wrapper.text()).toContain('训练经验')
	})

	it('uses a unique heading id for every group and opens the selected article', async () => {
		const selected = article(12)
		const {wrapper, dispatch} = mountFeatured([
			{id: 7, title: '第一组', articles: [article(1), article(2), article(3)]},
			{id: 'summer camp', title: '第二组', articles: [article(11), selected, article(13)]},
		])
		const sections = wrapper.findAll('.featured-release-section')

		expect(sections[0].attributes('aria-labelledby')).toBe('featured-release-title-7')
		expect(sections[0].get('h2').attributes('id')).toBe('featured-release-title-7')
		expect(sections[1].attributes('aria-labelledby')).toBe('featured-release-title-summer-camp')
		expect(new Set(sections.map(section => section.attributes('aria-labelledby'))).size).toBe(2)

		await wrapper.findAll('.featured-release-card')[4].trigger('click')
		expect(dispatch).toHaveBeenCalledWith('goBlogPage', selected)
	})

	it('falls back to the default avatar when author imagery is empty or fails', async () => {
		const {wrapper} = mountFeatured([{
			id: 1,
			title: '精选文章',
			articles: [article(1, {authorAvatar: ''}), article(2), article(3)],
		}])
		const avatars = wrapper.findAll('.featured-release-avatar')

		expect(avatars[0].attributes('src')).toBe('/img/default-avatar.jpg')
		Object.defineProperty(avatars[1].element, 'src', {value: 'https://example.com/broken.jpg', writable: true})
		await avatars[1].trigger('error')
		expect(avatars[1].element.src).toBe('/img/default-avatar.jpg')
	})

	it('shows persisted tag colors and an ellipsis when a card has more tags than its visual budget', () => {
		const tags = Array.from({length: 6}, (_, index) => ({
			id: index + 1,
			name: `标签 ${index + 1}`,
			color: index === 0 ? '#7c3aed' : '#0f766e',
		}))
		const {wrapper} = mountFeatured([{
			id: 1,
			title: '精选文章',
			articles: [article(1, {tags}), article(2), article(3)],
		}])
		const leadCard = wrapper.findAll('.featured-release-card')[0]

		expect(leadCard.findAll('.featured-release-tag')).toHaveLength(5)
		expect(leadCard.find('.featured-release-tag').attributes('style')).toContain('background-color: rgb(124, 58, 237)')
		expect(leadCard.find('.featured-release-tags-more').text()).toBe('…')
	})

	it('keeps every cover at 16:9 without cropping and provides restrained motion fallbacks', () => {
		expect(featuredSource).toContain('aspect-ratio: 16 / 9;')
		expect(featuredSource).toContain('.featured-release-media img { display: block; width: 100%; height: 100%; object-fit: contain; }')
		expect(featuredSource).not.toContain('.featured-release-media img { display: block; width: 100%; height: 100%; object-fit: cover; }')
		expect(featuredSource).not.toContain('height: 22rem;')
		expect(featuredSource).not.toContain('height: 24rem;')
		expect(featuredSource).toContain('@keyframes featured-release-enter')
		expect(featuredSource).toContain('@media (prefers-reduced-motion: reduce)')
		expect(featuredSource).toContain('.featured-release-card:hover .featured-release-arrow')
	})

	it('clamps the lead title and every summary to three lines with ellipses', () => {
		expect(featuredSource).toContain('.featured-release-card:first-child h3 { -webkit-line-clamp: 3; line-clamp: 3;')
		expect(featuredSource).toMatch(/\.featured-release-description \{[^}]*-webkit-line-clamp: 3;[^}]*line-clamp: 3;/)
		expect(featuredSource).toMatch(/\.featured-release-card h3 \{[^}]*overflow-wrap: anywhere;[^}]*text-overflow: ellipsis;/)
		expect(featuredSource).toMatch(/\.featured-release-description \{[^}]*overflow-wrap: anywhere;[^}]*text-overflow: ellipsis;/)
		expect(featuredSource).toContain('.featured-release-description :deep(*) { display: inline;')
	})

	it('uses the fixed Notion neutral card palette without a global mode override', () => {
		expect(featuredSource).toContain('background: var(--home-surface, #f7f7f5);')
		expect(featuredSource).toContain('border: 1px solid var(--home-border, #e9e9e7);')
		expect(featuredSource).toContain('outline: 2px solid var(--home-action, #2383e2);')
		expect(featuredSource).toContain('color: var(--home-text-soft, #37352f);')
		expect(featuredSource).toContain('color: var(--home-muted, #787774);')
		expect(featuredSource).toContain('box-shadow: none;')
		expect(featuredSource).toContain('margin: auto 0 0;')
		expect(featuredSource).not.toContain('data-theme="dark"')
		expect(featuredSource).not.toContain('.featured-release-card:nth-child(2) { background:')
	})
})
