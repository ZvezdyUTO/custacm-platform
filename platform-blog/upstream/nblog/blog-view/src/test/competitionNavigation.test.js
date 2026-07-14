// Author: huangbingrui.awa
import {describe, expect, it} from 'vitest'
import Nav from '@/components/index/Nav.vue'
import appIconSource from '@/components/common/AppIcon.vue?raw'
import navSource from '@/components/index/Nav.vue?raw'
import routerSource from '@/router/index.js?raw'

describe('competition routes and navigation', () => {
	it('registers public list and numeric detail routes without an auth guard', () => {
		expect(routerSource).toContain("path: '/competitions'")
		expect(routerSource).toContain("name: 'competitions'")
		expect(routerSource).toContain("path: '/competitions/:id(\\\\d+)'")
		expect(routerSource).toContain("name: 'competition-detail'")
		expect(routerSource).toContain("@/views/competition/CompetitionList.vue")
		expect(routerSource).toContain("@/views/competition/CompetitionDetail.vue")
		expect(routerSource).not.toMatch(/name: 'competitions',[\s\S]{0,180}requiresAuth/)
	})

	it('keeps the honour navigation active on both archive routes', () => {
		const active = Nav.computed.competitionNavigationActive

		expect(active.call({$route: {name: 'competitions'}})).toBe(true)
		expect(active.call({$route: {name: 'competition-detail'}})).toBe(true)
		expect(active.call({$route: {name: 'home'}})).toBe(false)
		expect(navSource).toContain('class="nav-item nav-primary-item nav-competitions"')
		expect(navSource).toContain('<AppIcon name="trophy" />赛事荣誉')
	})

	it('maps the trophy navigation glyph through the shared icon component', () => {
		expect(appIconSource).toMatch(/\bTrophy\b/)
		expect(appIconSource).toContain('trophy: Trophy')
	})
})
