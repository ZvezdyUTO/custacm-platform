// Author: huangbingrui.awa
import {mount} from '@vue/test-utils'
import {describe, expect, it} from 'vitest'

import AchievementsPanel from '@/components/profile/AchievementsPanel.vue'
import achievementsSource from '@/components/profile/AchievementsPanel.vue?raw'
import {achievementPresentation} from '@/utils/achievementPresentation'

const medalAchievement = {
	competitionId: 31,
	competitionFullName: '2026 ICPC Asia Shanghai Regional Contest',
	category: 'ICPC_ASIA_REGIONAL',
	categoryLabel: 'ICPC 亚洲区域赛',
	year: 2026,
	awardId: 71,
	awardMode: 'TEAM',
	awardModeLabel: '团队',
	teamName: 'CustACM',
	awardScopeLabel: '国家级',
	awardTier: 'MEDAL_GOLD',
	awardTierLabel: '金牌',
	rank: '(3/280)',
	profileVisible: false,
	profileOrder: null,
}

const RouterLink = {
	name: 'RouterLink',
	props: ['to'],
	template: '<a><slot /></a>',
}

describe('achievements panel', () => {
	it('renders canonical achievement facts and emits an explicit visibility choice', async () => {
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [medalAchievement], editable: true, showEmpty: true},
		})

		expect(wrapper.text()).toContain(medalAchievement.competitionFullName)
		expect(wrapper.text()).toContain('ICPC 亚洲区域赛')
		expect(wrapper.text()).toContain('金牌')
		expect(wrapper.text()).toContain('CustACM')
		expect(wrapper.text()).toContain('排名 (3/280)')
		expect(wrapper.text()).not.toContain('还没有获奖记录')
		const toggle = wrapper.get('[role="switch"]')
		expect(toggle.attributes('aria-checked')).toBe('false')

		await toggle.trigger('click')

		expect(wrapper.emitted('visibility-change')).toEqual([[medalAchievement, true]])
	})

	it('renders the article sidebar as a two-field link without medal rank', () => {
		const achievement = {...medalAchievement, profileVisible: true, profileOrder: 1}
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [achievement], compact: true},
			global: {stubs: {RouterLink}},
		})

		expect(wrapper.find('[role="switch"]').exists()).toBe(false)
		expect(wrapper.classes()).toContain('is-compact')
		expect(wrapper.find('.achievements-heading').exists()).toBe(false)
		expect(wrapper.find('.achievement-row').exists()).toBe(false)
		expect(wrapper.get('.achievement-strip-title').text()).toBe(achievement.competitionFullName)
		expect(wrapper.get('.achievement-strip-award').text()).toBe('金牌')
		expect(wrapper.text()).not.toContain('(3/280)')
		expect(wrapper.get('.achievement-strip').classes()).toEqual(expect.arrayContaining([
			'is-award-tone-gold',
			'is-category-regional',
		]))
		expect(wrapper.getComponent(RouterLink).props('to')).toEqual({
			name: 'competition-detail',
			params: {id: 31},
		})
		expect(wrapper.text()).not.toContain('CustACM')
		expect(wrapper.text()).not.toContain('ICPC 亚洲区域赛')
	})

	it('never appends rank to an ordinary award', () => {
		const ordinary = {
			...medalAchievement,
			category: 'BAIDU_STAR',
			categoryLabel: '百度之星',
			awardTier: 'BAIDU_NATIONAL_FIRST',
			awardTierLabel: '国赛一等奖',
			profileVisible: true,
			profileOrder: 1,
		}
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [ordinary], compact: true},
			global: {stubs: {RouterLink}},
		})

		expect(achievementPresentation(ordinary).showRank).toBe(false)
		expect(wrapper.get('.achievement-strip-award').text()).toBe('国赛一等奖')
		expect(wrapper.text()).not.toContain('(3/280)')
		expect(wrapper.get('.achievement-strip').classes()).toContain('is-category-baidu')
	})

	it('shows the first three public achievements by profileOrder and expands the rest accessibly', async () => {
		const achievements = [3, 1, 5, 2, 4].map(order => ({
			...medalAchievement,
			competitionId: order,
			awardId: order,
			competitionFullName: `比赛 ${order}`,
			profileVisible: true,
			profileOrder: order,
		}))
		achievements.push({...medalAchievement, competitionId: 99, awardId: 99, competitionFullName: '非公开比赛', profileVisible: false})
		const wrapper = mount(AchievementsPanel, {
			props: {achievements, compact: true},
			global: {stubs: {RouterLink}},
		})

		expect(wrapper.findAll('.achievement-strip-title').map(node => node.text())).toEqual(['比赛 1', '比赛 2', '比赛 3'])
		const expand = wrapper.get('.achievement-expand-toggle')
		expect(expand.attributes('aria-expanded')).toBe('false')
		expect(expand.attributes('aria-controls')).toBe(wrapper.get('.achievement-list').attributes('id'))
		expect(expand.text()).toContain('展开其余 2 项')

		await expand.trigger('click')

		expect(wrapper.findAll('.achievement-strip-title').map(node => node.text())).toEqual([
			'比赛 1', '比赛 2', '比赛 3', '比赛 4', '比赛 5',
		])
		expect(expand.attributes('aria-expanded')).toBe('true')
		expect(wrapper.text()).not.toContain('非公开比赛')
	})

	it.each([
		['PROVINCIAL', 'provincial'],
		['ICPC_NATIONAL_INVITATIONAL', 'invitational'],
		['CCPC_NATIONAL_INVITATIONAL', 'invitational'],
		['ICPC_ASIA_REGIONAL', 'regional'],
		['CCPC_REGIONAL', 'regional'],
		['EC_FINAL', 'final'],
		['CCPC_FINAL', 'final'],
		['BAIDU_STAR', 'baidu'],
		['GPLT_NATIONAL', 'gplt'],
		['LANQIAO_CUP_NATIONAL', 'lanqiao'],
	])('maps %s to its category texture family', (category, pattern) => {
		expect(achievementPresentation({...medalAchievement, category}).pattern).toBe(pattern)
	})

	it.each([
		['MEDAL_GOLD', 'gold'],
		['MEDAL_SILVER', 'silver'],
		['MEDAL_BRONZE', 'bronze'],
		['MEDAL_HONORABLE_MENTION', 'iron'],
		['FIRST_PRIZE', 'gold'],
		['SECOND_PRIZE', 'silver'],
		['THIRD_PRIZE', 'bronze'],
	])('maps award tier %s to its own metal tone', (awardTier, tone) => {
		expect(achievementPresentation({...medalAchievement, awardTier}).classes)
			.toContain(`is-award-tone-${tone}`)
	})

	it('allows only public achievements to be reordered and emits the complete award id order', async () => {
		const first = {...medalAchievement, awardId: 71, competitionFullName: '比赛 A', profileVisible: true, profileOrder: 1}
		const second = {...medalAchievement, awardId: 72, competitionFullName: '比赛 B', profileVisible: true, profileOrder: 2}
		const hidden = {...medalAchievement, awardId: 73, competitionFullName: '比赛 C', profileVisible: false, profileOrder: null}
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [hidden, second, first], editable: true},
		})

		expect(wrapper.findAll('.achievement-order-actions')).toHaveLength(2)
		expect(wrapper.get('[aria-label="比赛 A上移"]').attributes('disabled')).toBeDefined()
		expect(wrapper.get('[aria-label="比赛 B下移"]').attributes('disabled')).toBeDefined()
		await wrapper.get('[aria-label="比赛 B上移"]').trigger('click')

		expect(wrapper.emitted('order-change')).toEqual([[[72, 71]]])
	})

	it('blocks both ordering and visibility while an order mutation is pending', () => {
		const achievement = {...medalAchievement, profileVisible: true, profileOrder: 1}
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [achievement], editable: true, reordering: true},
		})

		expect(wrapper.get('[role="switch"]').attributes('disabled')).toBeDefined()
		expect(wrapper.text()).toContain('正在保存排序')
	})

	it('shows loading instead of a premature empty state', () => {
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [], loading: true, showEmpty: true},
		})

		expect(wrapper.attributes('aria-busy')).toBe('true')
		expect(wrapper.get('[role="status"]').text()).toContain('正在读取')
		expect(wrapper.text()).not.toContain('还没有获奖记录')
	})

	it('keeps a load error visible and emits retry without showing the empty state', async () => {
		const wrapper = mount(AchievementsPanel, {
			props: {achievements: [], errorMessage: '读取失败', retryable: true, showEmpty: true},
		})

		expect(wrapper.get('[role="alert"]').text()).toContain('读取失败')
		expect(wrapper.text()).not.toContain('还没有获奖记录')
		await wrapper.get('.achievements-error button').trigger('click')
		expect(wrapper.emitted('retry')).toHaveLength(1)
	})

	it('keeps award metal colors separate from category patterns', () => {
		expect(achievementsSource).toContain('.achievement-strip.is-award-tone-gold')
		expect(achievementsSource).toContain('.achievement-strip.is-award-tone-silver')
		expect(achievementsSource).toContain('.achievement-strip.is-award-tone-bronze')
		expect(achievementsSource).toContain('.achievement-strip.is-award-tone-iron')
		expect(achievementsSource).toContain('.achievement-strip.is-category-provincial')
		expect(achievementsSource).toContain('.achievement-strip.is-category-invitational')
		expect(achievementsSource).toContain('.achievement-strip.is-category-regional')
		expect(achievementsSource).toContain('.achievement-strip.is-category-final')
	})
})
