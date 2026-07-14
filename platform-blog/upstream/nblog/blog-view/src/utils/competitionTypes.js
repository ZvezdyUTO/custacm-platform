// Author: huangbingrui.awa

export const PUBLIC_COMPETITION_CATEGORIES = Object.freeze([
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

export const PUBLIC_COMPETITION_CATEGORY_VALUES = new Set(PUBLIC_COMPETITION_CATEGORIES.map(item => item.value))

const CATEGORY_LABELS = new Map(PUBLIC_COMPETITION_CATEGORIES.map(item => [item.value, item.label]))

const LEGACY_ROUTE_CATEGORIES = Object.freeze({
	PROVINCIAL: 'PROVINCIAL',
	ICPC_INVITATIONAL: 'ICPC_NATIONAL_INVITATIONAL',
	CCPC_INVITATIONAL: 'CCPC_NATIONAL_INVITATIONAL',
	ASIA_REGIONAL: 'ICPC_ASIA_REGIONAL',
	NATIONAL_SITE: 'CCPC_REGIONAL',
	ASIA_EAST_CONTINENT_FINAL: 'EC_FINAL',
	BAIDU_STAR: 'BAIDU_STAR',
	GPLT: 'GPLT_NATIONAL',
	LANQIAO_CUP: 'LANQIAO_CUP_NATIONAL',
})

function normalizedCode(value) {
	return typeof value === 'string' ? value.trim().toUpperCase() : ''
}

function legacyTypeCodes(types) {
	return new Set((Array.isArray(types) ? types : []).map(type => normalizedCode(type?.code)).filter(Boolean))
}

export function legacyRouteCategory(value) {
	const code = normalizedCode(value)
	if (PUBLIC_COMPETITION_CATEGORY_VALUES.has(code)) return code
	return LEGACY_ROUTE_CATEGORIES[code] || ''
}

export function legacyCompetitionCategory(types) {
	const codes = legacyTypeCodes(types)
	if (codes.has('BAIDU_STAR')) return 'BAIDU_STAR'
	if (codes.has('GPLT')) return 'GPLT_NATIONAL'
	if (codes.has('LANQIAO_CUP')) return 'LANQIAO_CUP_NATIONAL'
	if (codes.has('CCPC') && codes.has('NATIONAL_FINAL')) return 'CCPC_FINAL'
	if (codes.has('ASIA_EAST_CONTINENT_FINAL')) return 'EC_FINAL'
	if (codes.has('CCPC') && codes.has('NATIONAL_SITE')) return 'CCPC_REGIONAL'
	if (codes.has('ICPC') && codes.has('ASIA_REGIONAL')) return 'ICPC_ASIA_REGIONAL'
	if (codes.has('CCPC') && codes.has('INVITATIONAL')) return 'CCPC_NATIONAL_INVITATIONAL'
	if (codes.has('ICPC') && codes.has('INVITATIONAL')) return 'ICPC_NATIONAL_INVITATIONAL'
	if (codes.has('PROVINCIAL')) return 'PROVINCIAL'
	return ''
}

export function competitionCategory(competition) {
	const responseCategory = normalizedCode(competition?.category)
	const code = PUBLIC_COMPETITION_CATEGORY_VALUES.has(responseCategory)
		? responseCategory
		: legacyCompetitionCategory(competition?.types)
	if (!code) return null
	return {
		code,
		label: responseCategory === code && typeof competition?.categoryLabel === 'string' && competition.categoryLabel.trim()
			? competition.categoryLabel.trim()
			: CATEGORY_LABELS.get(code),
	}
}

export function publicCompetitionCategories(competition) {
	const category = competitionCategory(competition)
	return category ? [category] : []
}
