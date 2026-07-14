// Author: huangbingrui.awa
import {flushPromises, shallowMount} from '@vue/test-utils'
import {beforeEach, describe, expect, it, vi} from 'vitest'
import BlogItem, {countVisibleTags} from '@/components/blog/BlogItem.vue'
import BlogList from '@/components/blog/BlogList.vue'
import ArticleCatalog from '@/views/category/Category.vue'
import {getSearchBlogList} from '@/api/blog'
import {getBlogList as getAllBlogs} from '@/api/home'
import {getBlogListByCategoryName} from '@/api/category'
import {getBlogListByTagName} from '@/api/tag'
import navSource from '@/components/index/Nav.vue?raw'
import routerSource from '@/router/index.js?raw'

vi.mock('@/api/home', () => ({getBlogList: vi.fn()}))
vi.mock('@/api/blog', () => ({getSearchBlogList: vi.fn()}))
vi.mock('@/api/category', () => ({getBlogListByCategoryName: vi.fn()}))
vi.mock('@/api/tag', () => ({getBlogListByTagName: vi.fn()}))

const blogs = [
	{
		id: 1,
		title: '有首图的文章',
		description: '<p>文章简介</p>',
		firstPicture: '/img/article-cover.jpg',
		createTime: '2026-07-14',
		words: 1024,
		authorNickname: '夏依冰',
		authorUsername: 'huangbingrui',
		authorAvatar: '/img/avatar.jpg',
		category: {name: '训练经验', color: '#315b75'},
		tags: [{name: 'dfs', color: '#4f6b50'}],
	},
	{
		id: 2,
		title: '没有首图的文章',
		description: '<p>另一篇文章简介</p>',
		firstPicture: '',
		createTime: '2026-07-13',
		words: 512,
		authorNickname: 'Administrator',
		authorUsername: 'root',
		category: {name: '训练经验', color: '#315b75'},
		tags: [],
	},
]

const categories = [
	{name: '训练经验', color: '#315b75'},
	{name: '算法笔记', color: '#8b5a3c'},
]

const tags = Array.from({length: 16}, (_, index) => ({name: `标签${index + 1}`, color: '#4f6b50'}))

beforeEach(() => {
	getAllBlogs.mockReset().mockResolvedValue({code: 200, data: {list: blogs, totalPage: 2}})
	getSearchBlogList.mockReset().mockResolvedValue({code: 200, data: []})
	getBlogListByCategoryName.mockReset().mockResolvedValue({code: 200, data: {list: [blogs[0]], totalPage: 1}})
	getBlogListByTagName.mockReset().mockResolvedValue({code: 200, data: {list: [blogs[1]], totalPage: 1}})
	globalThis.Prism = {highlightAll: vi.fn()}
})

function mountCatalog(route = {name: 'articles', params: {}, fullPath: '/articles'}) {
	return shallowMount(ArticleCatalog, {
		props: {categoryList: categories, tagList: tags},
		global: {
			mocks: {$route: route},
			stubs: {
				AppIcon: true,
				BlogList: true,
				'router-link': {props: ['to'], template: '<a :data-to="to"><slot /></a>'},
			},
		},
	})
}

describe('category article grid', () => {
	it('replaces the category dropdown with a direct article route', () => {
		expect(navSource).toContain('to="/articles"')
		expect(navSource).toContain('<AppIcon name="book" />文章')
		expect(navSource).not.toContain('@command="categoryRoute"')
		expect(routerSource).toContain("path: '/articles'")
		expect(routerSource).toContain("name: 'articles'")
	})

	it('loads the article catalog and renders category plus random-tag filters', async () => {
		const wrapper = mountCatalog()
		await flushPromises()

		expect(getAllBlogs).toHaveBeenCalledWith(undefined)
		expect(wrapper.find('.catalog-section-label').exists()).toBe(false)
		expect(wrapper.get('#article-catalog-title').text()).toBe('全部文章')
		expect(wrapper.get('.catalog-hero p').text()).toBe('记录思考、经验与发现，也分享简单的日常。')
		expect(wrapper.findAll('.catalog-filter-link')).toHaveLength(categories.length + 1)
		expect(wrapper.findAll('.catalog-tag-cloud a')).toHaveLength(12)
		expect(wrapper.get('.catalog-results').classes()).toContain('is-grid-view')
		expect(wrapper.getComponent(BlogList).props('layout')).toBe('grid')

		await wrapper.findAll('.catalog-view-switch button')[1].trigger('click')
		expect(wrapper.get('.catalog-results').classes()).toContain('is-list-view')
		expect(wrapper.getComponent(BlogList).props('layout')).toBe('list')
	})

	it('selects the matching public API for category and tag routes', async () => {
		const categoryWrapper = mountCatalog({name: 'category', params: {name: '训练经验'}, fullPath: '/category/训练经验'})
		await flushPromises()
		expect(getBlogListByCategoryName).toHaveBeenCalledWith('训练经验', undefined)
		expect(categoryWrapper.get('#article-catalog-title').text()).toBe('训练经验')

		const tagWrapper = mountCatalog({name: 'tag', params: {name: 'dfs'}, fullPath: '/tag/dfs'})
		await flushPromises()
		expect(getBlogListByTagName).toHaveBeenCalledWith('dfs', undefined)
		expect(tagWrapper.get('#article-catalog-title').text()).toBe('dfs')
	})

	it('moves global title search into the catalog and requests only after form submission', async () => {
		const results = [{id: 7, title: '训练复盘', description: '<p>一段搜索结果简介</p>'}]
		getSearchBlogList.mockResolvedValue({code: 200, data: results})
		const wrapper = mountCatalog({name: 'category', params: {name: '训练经验'}, fullPath: '/category/训练经验'})
		await flushPromises()

		const searchForm = wrapper.get('.catalog-search')
		const searchButton = wrapper.get('.catalog-search-submit')
		expect(searchForm.element.firstElementChild).toBe(searchButton.element)
		expect(searchButton.attributes('type')).toBe('submit')
		expect(wrapper.get('#article-catalog-search').attributes('placeholder')).toBe('搜索全部文章')

		await wrapper.get('#article-catalog-search').setValue('  训练  ')
		expect(getSearchBlogList).not.toHaveBeenCalled()

		await searchForm.trigger('submit')
		await flushPromises()

		expect(getSearchBlogList).toHaveBeenCalledTimes(1)
		expect(getSearchBlogList).toHaveBeenCalledWith('训练')
		expect(wrapper.get('.catalog-search-state').text()).toContain('找到 1 篇文章')
		expect(wrapper.get('.catalog-search-hit h2').text()).toBe('训练复盘')
		expect(wrapper.get('.catalog-search-hit p').text()).toBe('一段搜索结果简介')
		expect(wrapper.get('.catalog-search-hit a').attributes('data-to')).toBe('/blog/7')
		expect(wrapper.findComponent(BlogList).exists()).toBe(false)
	})

	it('restores the current catalog without a request when an empty search is submitted', async () => {
		const wrapper = mountCatalog()
		await flushPromises()
		wrapper.vm.searchSubmitted = true
		wrapper.vm.submittedQuery = '旧关键词'

		await wrapper.get('.catalog-search').trigger('submit')

		expect(getSearchBlogList).not.toHaveBeenCalled()
		expect(wrapper.vm.searchSubmitted).toBe(false)
		expect(wrapper.getComponent(BlogList).props('blogList')).toEqual(blogs)
	})

	it('rejects unsupported search characters without calling the API', async () => {
		const wrapper = mountCatalog()
		await flushPromises()

		await wrapper.get('#article-catalog-search').setValue('100%')
		await wrapper.get('.catalog-search').trigger('submit')

		expect(getSearchBlogList).not.toHaveBeenCalled()
		expect(wrapper.get('.catalog-search-state').text()).toContain('不能包含')
	})

	it('forwards the category-only grid layout through the paginated list', () => {
		const wrapper = shallowMount(BlogList, {
			props: {layout: 'grid', blogList: blogs, totalPage: 1, getBlogList: vi.fn()},
		})

		expect(wrapper.findComponent(BlogItem).props('layout')).toBe('grid')
	})

	it('renders clickable cards, keeps 16:9 media and moves tags into the author row', async () => {
		const dispatch = vi.fn()
		const wrapper = shallowMount(BlogItem, {
			props: {layout: 'grid', blogList: blogs},
			global: {
				mocks: {
					$filters: {dateFormat: value => value},
					$store: {dispatch},
				},
				stubs: {AppIcon: true},
			},
		})

		expect(wrapper.classes()).toContain('is-grid')
		expect(wrapper.findAll('.blog-list-card')).toHaveLength(2)
		expect(wrapper.findAll('.list-card-cover')).toHaveLength(2)
		expect(wrapper.find('.list-card-cover-empty').text()).toContain('暂无首图')
		expect(wrapper.find('.blog-list-card').attributes('style')).toContain('--category-color: #315b75')
		expect(wrapper.find('.list-card-action').exists()).toBe(false)
		expect(wrapper.find('.list-author-tags').text()).toContain('dfs')
		expect(wrapper.find('.list-card-tags').exists()).toBe(false)

		const cardLink = wrapper.find('.list-card-hit-area')
		expect(cardLink.attributes('href')).toBe('/blog/1')
		await cardLink.trigger('click')
		expect(dispatch).toHaveBeenCalledWith('goBlogPage', blogs[0])
	})

	it('keeps whole tags until the available row width is exhausted', () => {
		expect(countVisibleTags([42, 56, 38], 140, 24, 6)).toEqual({count: 2, overflow: true})
		expect(countVisibleTags([42, 56, 38], 148, 24, 6)).toEqual({count: 3, overflow: false})
	})

	it('keeps the original read-more and tag footer in the default list layout', () => {
		const wrapper = shallowMount(BlogItem, {
			props: {blogList: [blogs[0]]},
			global: {
				mocks: {
					$filters: {dateFormat: value => value},
					$store: {dispatch: vi.fn()},
				},
				stubs: {AppIcon: true},
			},
		})

		expect(wrapper.find('.list-card-hit-area').exists()).toBe(false)
		expect(wrapper.find('.read-more-button').exists()).toBe(true)
		expect(wrapper.find('.list-card-tags').text()).toContain('dfs')
	})
})
