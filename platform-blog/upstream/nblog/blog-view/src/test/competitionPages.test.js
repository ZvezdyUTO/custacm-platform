// Author: huangbingrui.awa
import {enableAutoUnmount, flushPromises, mount} from '@vue/test-utils'
import {afterEach, beforeEach, describe, expect, it, vi} from 'vitest'
import {readFileSync} from 'node:fs'
import {resolve} from 'node:path'

const api = vi.hoisted(() => ({
	getCompetition: vi.fn(),
	getCompetitions: vi.fn(),
}))

vi.mock('@/api/competition', () => api)

import CompetitionDetail from '@/views/competition/CompetitionDetail.vue'
import CompetitionList from '@/views/competition/CompetitionList.vue'
import {SESSION_CHANGE_EVENT} from '@/auth/session'

enableAutoUnmount(afterEach)

const competitionListSource = readFileSync(resolve(process.cwd(), 'src/views/competition/CompetitionList.vue'), 'utf8')

const competition = {
	id: 31,
	fullName: '2026 ICPC Asia Shanghai Regional Contest',
	competitionDate: '2026-10-25',
	year: 2026,
	category: 'ICPC_ASIA_REGIONAL',
	categoryLabel: 'ICPC 亚洲区域赛',
	participationMode: 'TEAM',
	participationModeLabel: '团队',
	// Kept to prove canonical category fields take precedence over legacy tags.
	types: [{code: 'CCPC', label: 'CCPC'}, {code: 'NATIONAL_SITE', label: 'CCPC 分站赛'}],
	participants: [
		{id: 51, username: 'alice', displayName: 'Alice', articles: [{id: 91, title: '上海站复盘'}]},
		{id: 52, username: null, displayName: '历史队员', articles: []},
	],
	awards: [
		{
			id: 71,
			awardMode: 'TEAM',
			awardModeLabel: '团队',
			teamName: 'CustACM',
			awardScopeLabel: '国家级',
			awardTier: 'MEDAL_GOLD',
			awardTierLabel: '金牌',
			rankPosition: 3,
			rankTotal: 280,
			rank: '(3/280)',
			requiresLogin: false,
			recipients: [
				{participantId: 51, username: 'alice', displayName: 'Alice'},
				{participantId: 52, username: null, displayName: '历史队员'},
			],
		},
		{
			id: 72,
			awardMode: 'INDIVIDUAL',
			awardModeLabel: '个人',
			teamName: null,
			awardScopeLabel: null,
			awardTier: 'MEDAL_SILVER',
			awardTierLabel: '银牌',
			rankPosition: 8,
			rankTotal: 120,
			rank: '(8/120)',
			requiresLogin: true,
			recipients: [{participantId: 51, username: 'alice', displayName: 'Alice'}],
		},
	],
}

function countedAward(id, awardTier, awardTierLabel, awardScopeLabel = '国家级') {
	return {
		...competition.awards[0],
		id,
		awardTier,
		awardTierLabel,
		awardScopeLabel,
		rank: null,
		rankPosition: null,
		rankTotal: null,
	}
}

const ordinaryPrizeAwards = [
	countedAward(81, 'FIRST_PRIZE', '一等奖'),
	countedAward(82, 'FIRST_PRIZE', '一等奖'),
	countedAward(83, 'SECOND_PRIZE', '二等奖'),
	countedAward(84, 'THIRD_PRIZE', '三等奖'),
]

const baiduPrizeAwards = [
	countedAward(91, 'BAIDU_NATIONAL_FIRST', '国赛一等奖'),
	countedAward(92, 'BAIDU_NATIONAL_FIRST', '国赛一等奖'),
	countedAward(93, 'BAIDU_NATIONAL_SECOND', '国赛二等奖'),
	countedAward(94, 'BAIDU_PROVINCIAL_FIRST', '省赛一等奖', '省级'),
]

const stubs = {
	AppIcon: {props: ['name'], template: '<span class="app-icon-stub" :data-icon="name"></span>'},
	RouterLink: {props: ['to'], template: '<a><slot /></a>'},
	'el-pagination': {template: '<nav class="pagination-stub"></nav>'},
}

function listMount(query = {}) {
	return mount(CompetitionList, {
		global: {
			stubs,
			mocks: {
				$route: {name: 'competitions', path: '/competitions', fullPath: '/competitions', query},
				$router: {push: vi.fn()},
			},
		},
	})
}

function detailMount(id = '31', query = {}) {
	return mount(CompetitionDetail, {
		global: {
			stubs,
			mocks: {$route: {name: 'competition-detail', params: {id}, query}},
		},
	})
}

describe('public competition pages', () => {
	beforeEach(() => vi.clearAllMocks())

	it('forwards canonical category and pagination filters once and renders the returned tree', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 2, pageSize: 10, total: 11, totalPages: 2, list: [competition]})

		const wrapper = listMount({
			startYear: '2024',
			endYear: '2026',
			category: 'ICPC_ASIA_REGIONAL',
			pageNum: '2',
		})
		await flushPromises()

		expect(api.getCompetitions).toHaveBeenCalledTimes(1)
		expect(api.getCompetitions).toHaveBeenCalledWith({
			startYear: 2024,
			endYear: 2026,
			category: 'ICPC_ASIA_REGIONAL',
			pageNum: 2,
			pageSize: 10,
		})
		expect(api.getCompetition).not.toHaveBeenCalled()
		expect(wrapper.text()).toContain(competition.fullName)
		expect(wrapper.text()).toContain('ICPC 亚洲区域赛')
		expect(wrapper.text()).not.toContain('CCPC 分站赛')
		expect(wrapper.text()).toContain('2 人')
		expect(wrapper.text()).toContain('2 项')
		expect(wrapper.find('.record-honours').exists()).toBe(true)
		expect(wrapper.text()).toContain('金牌 · (3/280)')
		const recordDate = wrapper.get('.record-index time')
		expect(recordDate.attributes('datetime')).toBe('2026-10-25')
		expect(recordDate.attributes('aria-label')).toBe('2026年10月25日')
		expect(recordDate.get('strong').text()).toBe('2026')
		expect(recordDate.get('small').text()).toBe('10.25')
	})

	it.each([
		['GPLT_NATIONAL', 'GPLT 团体程序设计天梯赛（国赛）', ordinaryPrizeAwards,
			[['国一', '2 项'], ['国二', '1 项'], ['国三', '1 项']]],
		['BAIDU_STAR', '百度之星', baiduPrizeAwards,
			[['国一', '2 项'], ['国二', '1 项'], ['省一', '1 项']]],
		['LANQIAO_CUP_NATIONAL', '蓝桥杯程序设计竞赛（国奖）', ordinaryPrizeAwards,
			[['国一', '2 项'], ['国二', '1 项'], ['国三', '1 项']]],
	])('shows grouped award tier counts for %s on the list page', async (category, categoryLabel, awards, expected) => {
		api.getCompetitions.mockResolvedValue({
			pageNum: 1,
			pageSize: 10,
			total: 1,
			totalPages: 1,
			list: [{...competition, category, categoryLabel, awards}],
		})

		const wrapper = listMount()
		await flushPromises()

		expect(wrapper.text()).toContain('4 项')
		const counts = wrapper.findAll('.record-award-counts > div')
			.map(item => [item.get('dt').text(), item.get('dd').text()])
		expect(counts).toEqual(expected)
		expect(wrapper.find('.record-honours').exists()).toBe(false)
	})

	it('also groups award tier counts for a legacy GPLT category tag', async () => {
		api.getCompetitions.mockResolvedValue({
			pageNum: 1,
			pageSize: 10,
			total: 1,
			totalPages: 1,
			list: [{
				...competition,
				category: undefined,
				categoryLabel: undefined,
				types: [{code: 'GPLT', label: '团体程序设计天梯赛'}],
				awards: ordinaryPrizeAwards,
			}],
		})

		const wrapper = listMount()
		await flushPromises()

		expect(wrapper.text()).toContain('4 项')
		expect(wrapper.get('.record-award-counts').text()).toContain('国一2 项')
		expect(wrapper.find('.record-honours').exists()).toBe(false)
	})

	it('exposes exactly the ten canonical public categories', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: []})

		const wrapper = listMount()
		await flushPromises()

		expect(wrapper.vm.categoryOptions).toEqual([
			{value: 'PROVINCIAL', label: '省赛'},
			{value: 'ICPC_NATIONAL_INVITATIONAL', label: 'ICPC 全国邀请赛'},
			{value: 'CCPC_NATIONAL_INVITATIONAL', label: 'CCPC 全国邀请赛'},
			{value: 'ICPC_ASIA_REGIONAL', label: 'ICPC 亚洲区域赛'},
			{value: 'CCPC_REGIONAL', label: 'CCPC 区域赛'},
			{value: 'EC_FINAL', label: 'EC-Final'},
			{value: 'CCPC_FINAL', label: 'CCPC-Final'},
			{value: 'BAIDU_STAR', label: '百度之星'},
			{value: 'GPLT_NATIONAL', label: 'GPLT 团体程序设计天梯赛（国赛）'},
			{value: 'LANQIAO_CUP_NATIONAL', label: '蓝桥杯程序设计竞赛（国奖）'},
		])
		expect(wrapper.get('.competition-type-select').attributes('placeholder')).toBe('全部类型')
	})

	it('keeps the masthead concise and labels empty year bounds as unlimited', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: []})

		const wrapper = listMount()
		await flushPromises()
		const yearInputs = wrapper.findAll('.archive-filter input[type="number"]')

		expect(wrapper.get('.archive-kicker').text()).toBe('CUSTACM · COMPETITION ARCHIVE')
		expect(wrapper.find('.archive-intro').exists()).toBe(false)
		expect(competitionListSource).not.toContain('.archive-intro {')
		expect(yearInputs).toHaveLength(2)
		expect(yearInputs.map(input => input.attributes('placeholder'))).toEqual(['不限', '不限'])
		expect(wrapper.vm.routeQuery()).toEqual({})
	})

	it('keeps year-only records readable and keeps exact dates optional', async () => {
		const historical = {...competition, competitionDate: null, year: 2024}
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 1, totalPages: 1, list: [historical]})
		api.getCompetition.mockResolvedValue(historical)

		const list = listMount()
		const detail = detailMount()
		await flushPromises()

		const recordDate = list.get('.record-index time')
		expect(recordDate.attributes('datetime')).toBe('2024')
		expect(recordDate.attributes('aria-label')).toBe('2024年')
		expect(recordDate.get('strong').text()).toBe('2024')
		expect(recordDate.find('small').exists()).toBe(false)
		expect(detail.get('.detail-subtitle').text()).toContain('2024年 · 团队')
		expect(detail.get('.detail-overview dt').text()).toBe('赛事日期')
		expect(detail.get('.detail-overview dd').text()).toBe('2024年')
		expect(detail.get('.archive-number time').attributes('datetime')).toBe('2024')
	})

	it('labels records with neither an exact date nor a legacy year as pending', async () => {
		const undated = {...competition, competitionDate: null, year: null}
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 1, totalPages: 1, list: [undated]})
		api.getCompetition.mockResolvedValue(undated)

		const list = listMount()
		const detail = detailMount()
		await flushPromises()

		expect(list.find('.record-index time').exists()).toBe(false)
		const recordDate = list.get('.record-index .record-date')
		expect(recordDate.element.tagName).toBe('SPAN')
		expect(recordDate.attributes('aria-label')).toBe('日期待补充')
		expect(recordDate.get('strong').text()).toBe('—')
		expect(recordDate.get('small').text()).toBe('日期待补充')
		expect(detail.get('.detail-subtitle').text()).toContain('日期待补充 · 团队')
		expect(detail.get('.detail-overview dd').text()).toBe('日期待补充')
		expect(detail.find('.archive-number time').exists()).toBe(false)
		expect(detail.get('.archive-number .archive-date').element.tagName).toBe('SPAN')
	})

	it('uses the contest hall photograph as a decorative full-height masthead scene', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: []})

		const wrapper = listMount()
		await flushPromises()

		const scene = wrapper.get('.archive-scene')
		expect(scene.attributes('aria-hidden')).toBe('true')
		expect(scene.get('img').attributes()).toMatchObject({
			src: '/img/competition-archive-contest-hall.jpg',
			alt: '',
		})
		expect(wrapper.find('.archive-seal').exists()).toBe(false)
	})

	it('reads one legacy type route but emits only the canonical category request', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: []})

		const wrapper = listMount({type: 'ASIA_REGIONAL'})
		await flushPromises()

		expect(wrapper.vm.filters.category).toBe('ICPC_ASIA_REGIONAL')
		expect(api.getCompetitions).toHaveBeenCalledWith({
			category: 'ICPC_ASIA_REGIONAL',
			pageNum: 1,
			pageSize: 10,
		})
		expect(wrapper.vm.routeQuery()).toEqual({category: 'ICPC_ASIA_REGIONAL'})
	})

	it('falls back to legacy response types when canonical category fields are absent', async () => {
		const legacyCompetition = {
			...competition,
			category: undefined,
			categoryLabel: undefined,
			types: [{code: 'CCPC'}, {code: 'INVITATIONAL'}],
		}
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 1, totalPages: 1, list: [legacyCompetition]})

		const wrapper = listMount()
		await flushPromises()

		expect(wrapper.text()).toContain('CCPC 全国邀请赛')
	})

	it('shows an actionable empty state and retries a failed list request', async () => {
		api.getCompetitions
			.mockRejectedValueOnce(new Error('服务暂时不可用'))
			.mockResolvedValueOnce({pageNum: 1, pageSize: 10, total: 0, totalPages: 0, list: []})
		const wrapper = listMount()
		await flushPromises()

		expect(wrapper.text()).toContain('档案暂时无法读取')
		expect(wrapper.text()).toContain('服务暂时不可用')
		await wrapper.get('.archive-error button').trigger('click')
		await flushPromises()

		expect(api.getCompetitions).toHaveBeenCalledTimes(2)
		expect(wrapper.text()).toContain('当前范围内暂无赛事记录')
	})

	it('marks paginated results for focus restoration before changing the query', async () => {
		api.getCompetitions.mockResolvedValue({pageNum: 1, pageSize: 10, total: 11, totalPages: 2, list: [competition]})
		const wrapper = listMount()
		await flushPromises()

		wrapper.vm.goToPage(2)

		expect(wrapper.vm.focusResultsAfterLoad).toBe(true)
		expect(wrapper.vm.$router.push).toHaveBeenCalledWith({name: 'competitions', query: {pageNum: '2'}})
		expect(wrapper.get('.archive-summary').attributes('tabindex')).toBe('-1')
	})

	it('renders medal rank and complete nullable facts in one detail request', async () => {
		api.getCompetition.mockResolvedValue(competition)
		const wrapper = detailMount('31', {from: '/competitions?category=ICPC_ASIA_REGIONAL'})
		await flushPromises()

		expect(api.getCompetition).toHaveBeenCalledTimes(1)
		expect(api.getCompetition).toHaveBeenCalledWith('31')
		expect(wrapper.text()).toContain(competition.fullName)
		expect(wrapper.text()).toContain('上海站复盘')
		expect(wrapper.text()).toContain('账号已注销 · 历史记录保留')
		expect(wrapper.text()).toContain('CustACM')
		expect(wrapper.text()).toContain('国家级')
		expect(wrapper.text()).toContain('金牌')
		expect(wrapper.text()).toContain('(3/280)')
		expect(wrapper.text()).toContain('银牌')
		expect(wrapper.text()).not.toContain('undefined')
		expect(wrapper.text()).not.toContain('null')
		expect(wrapper.get('.detail-subtitle').text()).toContain('2026年10月25日 · 团队')
		expect(wrapper.get('.detail-overview dt').text()).toBe('赛事日期')
		expect(wrapper.get('.detail-overview dd').text()).toBe('2026年10月25日')
		const archiveDate = wrapper.get('.archive-number time')
		expect(archiveDate.attributes('datetime')).toBe('2026-10-25')
		expect(archiveDate.get('em').text()).toBe('2026')
		expect(archiveDate.get('small').text()).toBe('10.25')
		expect(wrapper.vm.backTo).toBe('/competitions?category=ICPC_ASIA_REGIONAL')
	})

	it('does not render rank for an ordinary award family', async () => {
		const baiduCompetition = {
			...competition,
			category: 'BAIDU_STAR',
			categoryLabel: '百度之星',
			awards: [{
				...competition.awards[0],
				awardTier: 'BAIDU_NATIONAL_FIRST',
				awardTierLabel: '国赛一等奖',
				rank: '(1/100)',
			}],
		}
		api.getCompetition.mockResolvedValue(baiduCompetition)
		const wrapper = detailMount()
		await flushPromises()

		expect(wrapper.text()).toContain('国赛一等奖')
		expect(wrapper.text()).not.toContain('(1/100)')
		expect(wrapper.text()).not.toContain('赛事排名')
	})

	it.each([
		['GPLT_NATIONAL', 'GPLT 团体程序设计天梯赛（国赛）'],
		['BAIDU_STAR', '百度之星'],
		['LANQIAO_CUP_NATIONAL', '蓝桥杯程序设计竞赛（国奖）'],
	])('keeps %s award records expanded on the detail page', async (category, categoryLabel) => {
		api.getCompetition.mockResolvedValue({...competition, category, categoryLabel})

		const wrapper = detailMount()
		await flushPromises()

		expect(wrapper.find('.award-list').exists()).toBe(true)
		expect(wrapper.findAll('.award-record')).toHaveLength(2)
		expect(wrapper.text()).toContain('金牌')
		expect(wrapper.text()).toContain('银牌')
	})

	it('distinguishes a missing public record from a retryable transport failure', async () => {
		api.getCompetition.mockRejectedValue({response: {status: 404, data: {msg: '比赛不存在'}}})
		const wrapper = detailMount('404')
		await flushPromises()

		expect(wrapper.text()).toContain('这份赛事档案不存在')
		expect(wrapper.text()).toContain('查阅其他赛事')
		expect(wrapper.text()).not.toContain('重新调阅')
	})

	it('reloads and removes stale login-required detail awards when the session changes', async () => {
		api.getCompetition
			.mockResolvedValueOnce(competition)
			.mockResolvedValueOnce({...competition, awards: [competition.awards[0]]})
		const wrapper = detailMount()
		await flushPromises()
		expect(wrapper.text()).toContain('银牌')

		window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
		await flushPromises()

		expect(api.getCompetition).toHaveBeenCalledTimes(2)
		expect(wrapper.text()).toContain('金牌')
		expect(wrapper.text()).not.toContain('银牌')
	})

	it('reloads list counts and summaries when the session changes', async () => {
		api.getCompetitions
			.mockResolvedValueOnce({pageNum: 1, pageSize: 10, total: 1, totalPages: 1, list: [competition]})
			.mockResolvedValueOnce({
				pageNum: 1,
				pageSize: 10,
				total: 1,
				totalPages: 1,
				list: [{...competition, awards: [competition.awards[0]]}],
			})
		const wrapper = listMount()
		await flushPromises()
		expect(wrapper.text()).toContain('2 项')

		window.dispatchEvent(new Event(SESSION_CHANGE_EVENT))
		await flushPromises()

		expect(api.getCompetitions).toHaveBeenCalledTimes(2)
		expect(wrapper.text()).toContain('1 项')
		expect(wrapper.text()).not.toContain('银牌')
	})

	it('uses a visibility-aware empty award state', async () => {
		api.getCompetition.mockResolvedValue({...competition, awards: []})
		const wrapper = detailMount()
		await flushPromises()

		expect(wrapper.text()).toContain('暂无可见获奖记录')
	})
})
