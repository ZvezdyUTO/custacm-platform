// Author: huangbingrui.awa
import {competitionCategory} from '@/utils/competitionTypes'

const MEDAL_CATEGORIES = new Set([
	'PROVINCIAL',
	'ICPC_NATIONAL_INVITATIONAL',
	'CCPC_NATIONAL_INVITATIONAL',
	'ICPC_ASIA_REGIONAL',
	'CCPC_REGIONAL',
	'EC_FINAL',
	'CCPC_FINAL',
])

const TIER_TONES = Object.freeze({
	MEDAL_GOLD: 'gold',
	MEDAL_SILVER: 'silver',
	MEDAL_BRONZE: 'bronze',
	MEDAL_HONORABLE_MENTION: 'iron',
	BAIDU_NATIONAL_FIRST: 'gold',
	BAIDU_NATIONAL_SECOND: 'silver',
	BAIDU_NATIONAL_THIRD: 'bronze',
	BAIDU_NATIONAL_FOURTH: 'iron',
	BAIDU_PROVINCIAL_FIRST: 'gold',
	BAIDU_PROVINCIAL_SECOND: 'silver',
	BAIDU_PROVINCIAL_THIRD: 'bronze',
	FIRST_PRIZE: 'gold',
	SECOND_PRIZE: 'silver',
	THIRD_PRIZE: 'bronze',
})

const LEGACY_LEVEL_TONES = Object.freeze({1: 'gold', 2: 'silver', 3: 'bronze', 4: 'iron'})

const CATEGORY_PATTERNS = Object.freeze({
	PROVINCIAL: 'provincial',
	ICPC_NATIONAL_INVITATIONAL: 'invitational',
	CCPC_NATIONAL_INVITATIONAL: 'invitational',
	ICPC_ASIA_REGIONAL: 'regional',
	CCPC_REGIONAL: 'regional',
	EC_FINAL: 'final',
	CCPC_FINAL: 'final',
	BAIDU_STAR: 'baidu',
	GPLT_NATIONAL: 'gplt',
	LANQIAO_CUP_NATIONAL: 'lanqiao',
})

function normalizedCode(value) {
	return typeof value === 'string' ? value.trim().toUpperCase() : ''
}

function awardTierLabel(achievement) {
	if (typeof achievement?.awardTierLabel === 'string' && achievement.awardTierLabel.trim()) {
		return achievement.awardTierLabel.trim()
	}
	if (typeof achievement?.awardName === 'string' && achievement.awardName.trim()) {
		return achievement.awardName.trim()
	}
	const level = Number(achievement?.awardLevel)
	return Number.isInteger(level) && level >= 1 && level <= 4 ? `${level} 级奖项` : '奖项'
}

function awardTone(achievement) {
	const tier = normalizedCode(achievement?.awardTier)
	return TIER_TONES[tier] || LEGACY_LEVEL_TONES[Number(achievement?.awardLevel)] || 'neutral'
}

function isMedalAchievement(achievement, categoryCode) {
	const tier = normalizedCode(achievement?.awardTier)
	if (tier) return tier.startsWith('MEDAL_')
	return MEDAL_CATEGORIES.has(categoryCode)
}

function labelWithRank(label, rank, showRank) {
	return showRank && typeof rank === 'string' && rank.trim() ? `${label} · ${rank.trim()}` : label
}

export function achievementsInProfileOrder(achievements) {
	const source = Array.isArray(achievements) ? achievements : []
	const visible = []
	const hidden = []
	source.forEach((achievement, index) => {
		const entry = {achievement, index}
		if (achievement?.profileVisible === false) hidden.push(entry)
		else visible.push(entry)
	})
	visible.sort((left, right) => {
		const leftOrder = Number.isInteger(Number(left.achievement?.profileOrder))
			&& left.achievement?.profileOrder !== null && left.achievement?.profileOrder !== ''
			? Number(left.achievement.profileOrder) : Number.POSITIVE_INFINITY
		const rightOrder = Number.isInteger(Number(right.achievement?.profileOrder))
			&& right.achievement?.profileOrder !== null && right.achievement?.profileOrder !== ''
			? Number(right.achievement.profileOrder) : Number.POSITIVE_INFINITY
		return leftOrder - rightOrder || left.index - right.index
	})
	return [...visible, ...hidden].map(entry => entry.achievement)
}

export function achievementPresentation(achievement) {
	const category = competitionCategory(achievement)
	const categoryCode = category?.code || ''
	const baseLabel = awardTierLabel(achievement)
	const showRank = isMedalAchievement(achievement, categoryCode)
	const tone = awardTone(achievement)
	const pattern = CATEGORY_PATTERNS[categoryCode] || 'plain'
	return {
		awardLabel: labelWithRank(baseLabel, achievement?.rank, showRank),
		awardTierLabel: baseLabel,
		categoryCode,
		categoryLabel: category?.label || '',
		classes: [`is-award-tone-${tone}`, `is-category-${pattern}`],
		pattern,
		showRank,
		tone,
	}
}
