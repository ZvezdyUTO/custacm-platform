<template>
	<section
		class="achievements-panel"
		:class="{'is-compact': compact}"
		:aria-label="title"
		:aria-busy="loading ? 'true' : 'false'"
	>
		<header v-if="!compact" class="achievements-heading">
			<div>
				<p class="achievements-eyebrow">ACHIEVEMENTS</p>
				<h2>{{ title }}</h2>
			</div>
			<div class="achievements-heading-meta">
				<small v-if="reordering" role="status">正在保存排序…</small>
				<span v-if="!loading && achievements.length">{{ achievements.length }} 项</span>
			</div>
		</header>

		<p v-if="loading" class="achievements-loading" role="status" aria-live="polite">正在读取获奖记录…</p>
		<template v-else>
			<div v-if="errorMessage" class="achievements-error" role="alert">
				<p>{{ errorMessage }}</p>
				<button v-if="retryable" type="button" @click="$emit('retry')">重新读取</button>
			</div>
			<div v-if="orderedAchievements.length" :id="listId" class="achievement-list" :class="{'is-public-strips': compact}">
				<template v-for="achievement in displayedAchievements" :key="achievementKey(achievement)">
					<router-link
						v-if="compact"
						class="achievement-strip"
						:class="presentation(achievement).classes"
						:to="{name: 'competition-detail', params: {id: achievement.competitionId}}"
						:aria-label="`${achievement.competitionFullName}，${presentation(achievement).awardTierLabel}，查看比赛详情`"
						:title="`${achievement.competitionFullName} · ${presentation(achievement).awardTierLabel}`"
					>
						<span class="achievement-strip-title">{{ achievement.competitionFullName }}</span>
						<strong class="achievement-strip-award">{{ presentation(achievement).awardTierLabel }}</strong>
					</router-link>
					<article v-else class="achievement-row" :class="presentation(achievement).classes">
						<time
							v-if="datePresentation(achievement).isKnown"
							class="achievement-date"
							:datetime="datePresentation(achievement).datetime || undefined"
							:aria-label="datePresentation(achievement).label"
						>
							<strong>{{ datePresentation(achievement).year }}</strong>
							<small v-if="datePresentation(achievement).hasExactDate">{{ datePresentation(achievement).monthDay }}</small>
						</time>
						<span v-else class="achievement-date" aria-label="日期待补充"><strong>—</strong></span>
						<div class="achievement-copy">
							<strong>{{ achievement.competitionFullName }}</strong>
							<div v-if="presentation(achievement).categoryLabel" class="achievement-types" aria-label="比赛类型">
								<span>{{ presentation(achievement).categoryLabel }}</span>
							</div>
							<p>{{ presentation(achievement).awardTierLabel }}</p>
							<small>{{ awardMeta(achievement) }}</small>
						</div>
						<div v-if="editable" class="achievement-controls">
							<div v-if="achievement.profileVisible" class="achievement-order-actions" aria-label="公开荣誉顺序">
								<button
									type="button"
									:aria-label="`${achievement.competitionFullName}上移`"
									:disabled="orderButtonDisabled(achievement, -1)"
									@click="moveAchievement(achievement, -1)"
								><AppIcon name="arrow-up" /></button>
								<button
									type="button"
									:aria-label="`${achievement.competitionFullName}下移`"
									:disabled="orderButtonDisabled(achievement, 1)"
									@click="moveAchievement(achievement, 1)"
								><AppIcon name="arrow-down" /></button>
							</div>
							<button
								class="achievement-visibility"
								:class="{'is-visible': achievement.profileVisible}"
								type="button"
								role="switch"
								:aria-checked="achievement.profileVisible ? 'true' : 'false'"
								:aria-label="visibilityLabel(achievement)"
								:disabled="reordering || Boolean(updating[achievementKey(achievement)])"
								@click="$emit('visibility-change', achievement, !achievement.profileVisible)"
							>
								<span aria-hidden="true"><span class="achievement-visibility-thumb"></span></span>
								{{ updating[achievementKey(achievement)] ? '更新中…' : (achievement.profileVisible ? '公开展示' : '仅自己可见') }}
							</button>
						</div>
					</article>
				</template>
			</div>
			<button
				v-if="compact && orderedAchievements.length > compactLimit"
				class="achievement-expand-toggle"
				type="button"
				:aria-expanded="expanded ? 'true' : 'false'"
				:aria-controls="listId"
				@click="expanded = !expanded"
			>
				{{ expanded ? '收起荣誉' : `展开其余 ${orderedAchievements.length - compactLimit} 项` }}
				<AppIcon name="chevron-down" :class="{'is-open': expanded}" />
			</button>
			<p v-if="showEmpty && !errorMessage && orderedAchievements.length === 0" class="achievements-empty">还没有获奖记录。</p>
		</template>
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {achievementPresentation, achievementsInProfileOrder} from '@/utils/achievementPresentation'
	import {competitionDatePresentation} from '@/utils/competitionDatePresentation'

	let nextPanelId = 1

	export default {
		name: 'AchievementsPanel',
		emits: ['order-change', 'retry', 'visibility-change'],
		props: {
			achievements: {type: Array, default: () => []},
			compact: {type: Boolean, default: false},
			compactLimit: {type: Number, default: 3},
			editable: {type: Boolean, default: false},
			errorMessage: {type: String, default: ''},
			loading: {type: Boolean, default: false},
			retryable: {type: Boolean, default: false},
			reordering: {type: Boolean, default: false},
			showEmpty: {type: Boolean, default: false},
			title: {type: String, default: '获奖记录'},
			updating: {type: Object, default: () => ({})},
		},
		data() {
			return {expanded: false, listId: `achievement-list-${nextPanelId++}`}
		},
	computed: {
		orderedAchievements() {
			const achievements = this.compact
				? this.achievements.filter(achievement => achievement?.profileVisible !== false)
				: this.achievements
			return achievementsInProfileOrder(achievements)
			},
			displayedAchievements() {
				if (!this.compact || this.expanded) return this.orderedAchievements
				return this.orderedAchievements.slice(0, Math.max(1, this.compactLimit))
			},
			visibleAchievements() {
				return this.orderedAchievements.filter(achievement => achievement?.profileVisible)
			},
			hasPendingVisibility() {
				return Object.keys(this.updating || {}).length > 0
			},
		},
		watch: {
			achievements(value) {
				if (!Array.isArray(value) || value.length <= this.compactLimit) this.expanded = false
			},
		},
		methods: {
			achievementKey(achievement) {
				return `${achievement.competitionId}:${achievement.awardId}`
			},
			datePresentation(achievement) {
				return competitionDatePresentation(achievement)
			},
			presentation(achievement) {
				return achievementPresentation(achievement)
			},
			awardMeta(achievement) {
				const presentation = this.presentation(achievement)
				return [
					achievement.awardModeLabel,
					achievement.teamName,
					achievement.awardScopeLabel,
					presentation.showRank && achievement.rank ? `排名 ${achievement.rank}` : '',
				].filter(Boolean).join(' · ')
			},
			visibilityLabel(achievement) {
				const action = achievement.profileVisible ? '从公开名片隐藏' : '在公开名片展示'
				return `${action}：${achievement.competitionFullName} ${this.presentation(achievement).awardTierLabel}`
			},
			orderButtonDisabled(achievement, offset) {
				const index = this.visibleAchievements.findIndex(item => this.achievementKey(item) === this.achievementKey(achievement))
				const target = index + offset
				return this.reordering || this.hasPendingVisibility || index < 0 || target < 0 || target >= this.visibleAchievements.length
			},
			moveAchievement(achievement, offset) {
				if (this.orderButtonDisabled(achievement, offset)) return
				const ordered = [...this.visibleAchievements]
				const index = ordered.findIndex(item => this.achievementKey(item) === this.achievementKey(achievement))
				const target = index + offset
				const moved = ordered[index]
				ordered[index] = ordered[target]
				ordered[target] = moved
				this.$emit('order-change', ordered.map(item => item.awardId))
			},
		},
	}
</script>

<style scoped>
	.achievements-panel { --achievement-meta-color: color-mix(in srgb, var(--color-text) 68%, var(--color-surface)); margin-top: 28px; border: 1px solid var(--color-border); background: var(--color-surface); }
	.achievements-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 16px; border-bottom: 1px solid var(--color-border); padding: 18px 20px; }
	.achievements-heading h2 { margin: 0; color: var(--color-text); }
	.achievements-heading h2 { font-size: 20px; }
	.achievements-heading-meta { display: grid; justify-items: end; gap: 3px; }
	.achievements-heading-meta span { color: var(--achievement-meta-color); font-size: 12px; font-weight: 700; }
	.achievements-heading-meta small { color: var(--color-action); font-size: 10px; font-weight: 700; }
	.achievements-eyebrow { margin: 0 0 6px; color: var(--achievement-meta-color); font-size: 10px; font-weight: 800; letter-spacing: .16em; }
	.achievement-list { display: grid; }
	.achievement-row { display: grid; grid-template-columns: 58px minmax(0, 1fr) auto; align-items: center; gap: 16px; padding: 17px 20px; }
	.achievement-row + .achievement-row { border-top: 1px solid var(--color-border); }
	.achievement-row { --achievement-row-tone: var(--color-action); }
	.achievement-row .achievement-date { display: grid; width: 52px; min-height: 42px; place-content: center; justify-items: center; gap: 2px; border-left: 3px solid var(--achievement-row-tone); background: var(--color-surface-subtle); color: var(--color-text); font-variant-numeric: tabular-nums; }
	.achievement-row .achievement-date strong { font-size: 13px; font-weight: 800; }
	.achievement-row .achievement-date small { color: var(--achievement-meta-color); font-size: 9px; font-weight: 800; letter-spacing: .05em; }
	.achievement-copy { display: grid; min-width: 0; gap: 6px; }
	.achievement-copy strong { overflow: hidden; color: var(--color-text); font-size: 14px; line-height: 1.45; text-overflow: ellipsis; white-space: nowrap; }
	.achievement-copy p { margin: 0; color: var(--color-text-muted); font-size: 13px; font-weight: 700; }
	.achievement-copy small { color: var(--achievement-meta-color); font-size: 11px; }
	.achievement-types { display: flex; flex-wrap: wrap; gap: 5px; }
	.achievement-types span { border: 1px solid var(--color-border); border-radius: var(--radius-pill); background: var(--color-surface-subtle); padding: 2px 7px; color: var(--color-text-muted); font-size: 10px; font-weight: 700; }
	.achievement-controls { display: grid; min-width: 120px; justify-items: end; gap: 2px; }
	.achievement-order-actions { display: inline-flex; gap: 2px; }
	.achievement-order-actions button { display: grid; width: 26px; height: 24px; place-items: center; border: 0; border-radius: var(--radius-control); background: transparent; color: var(--achievement-meta-color); cursor: pointer; }
	.achievement-order-actions button:hover:not(:disabled), .achievement-order-actions button:focus-visible { background: var(--color-surface-subtle); color: var(--color-text); }
	.achievement-order-actions button:focus-visible { outline: 2px solid var(--color-focus-ring); outline-offset: 1px; }
	.achievement-order-actions button:disabled { cursor: default; opacity: .3; }
	.achievement-order-actions .app-icon { width: 13px; height: 13px; }
	.achievement-visibility { display: inline-flex; min-width: 120px; align-items: center; justify-content: flex-end; gap: 8px; border: 0; background: transparent; color: var(--achievement-meta-color); padding: 6px 0 6px 12px; font: inherit; font-size: 11px; font-weight: 700; cursor: pointer; }
	.achievement-visibility > span { position: relative; width: 32px; height: 18px; border: 1px solid var(--color-border-strong); border-radius: var(--radius-pill); background: var(--color-surface-subtle); transition: background var(--duration-fast) var(--ease-standard), border-color var(--duration-fast) var(--ease-standard); }
	.achievement-visibility-thumb { position: absolute; top: 3px; left: 3px; width: 10px; height: 10px; border-radius: 50%; background: var(--color-text-faint); transition: transform var(--duration-fast) var(--ease-standard), background var(--duration-fast) var(--ease-standard); }
	.achievement-visibility.is-visible { color: var(--color-success); }
	.achievement-visibility.is-visible > span { border-color: var(--color-success); background: color-mix(in srgb, var(--color-success) 18%, var(--color-surface)); }
	.achievement-visibility.is-visible .achievement-visibility-thumb { background: var(--color-success); transform: translateX(14px); }
	.achievement-visibility:focus-visible { border-radius: var(--radius-control); outline: 2px solid var(--color-focus-ring); outline-offset: 2px; }
	.achievement-visibility:disabled { cursor: wait; opacity: .58; }
	.achievements-loading { margin: 0; padding: 24px 20px; color: var(--achievement-meta-color); font-size: 13px; text-align: center; }
	.achievements-error { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin: 14px 20px 0; border-left: 3px solid var(--color-danger); background: color-mix(in srgb, var(--color-danger) 9%, var(--color-surface)); padding: 9px 11px; color: var(--color-danger); font-size: 12px; }
	.achievements-error p { margin: 0; }
	.achievements-error button { flex: 0 0 auto; border: 1px solid currentColor; border-radius: var(--radius-control); background: transparent; padding: 6px 9px; color: inherit; font: inherit; font-weight: 700; cursor: pointer; }
	.achievements-empty { margin: 0; padding: 24px 20px; color: var(--achievement-meta-color); font-size: 13px; text-align: center; }

	.achievements-panel.is-compact { margin-top: 0; border: 0; background: transparent; }
	.is-public-strips { display: grid; gap: 2px; }
	.achievement-strip {
		--strip-surface: #e4dfda;
		--strip-title: #282b2e;
		--strip-award: #52463e;
		--strip-border: rgba(18, 21, 24, .15);
		--strip-pattern: rgba(17, 21, 24, .065);
		display: grid;
		grid-template-columns: minmax(0, 1fr) auto;
		min-height: 23px;
		align-items: center;
		gap: 3px;
		overflow: hidden;
		border: 1px solid var(--strip-border);
		border-radius: 4px;
		background-color: var(--strip-surface);
		color: var(--strip-title);
		padding: 1px 6px;
		text-decoration: none;
		transition: border-color var(--duration-fast) var(--ease-standard), box-shadow var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard);
	}
	.achievement-strip-title {
		overflow: hidden;
		color: var(--strip-title);
		font-size: 10px;
		font-weight: 650;
		letter-spacing: -.01em;
		line-height: 1.2;
		text-overflow: ellipsis;
		white-space: nowrap;
	}
	.achievement-strip-award {
		color: var(--strip-award);
		font-size: 10px;
		font-weight: 800;
		line-height: 1.2;
		white-space: nowrap;
	}
	.achievement-strip:hover {
		border-color: color-mix(in srgb, var(--strip-award) 42%, transparent);
		box-shadow: 0 4px 12px rgba(15, 19, 22, .08);
		transform: translateY(-1px);
	}
	.achievement-strip:focus-visible {
		outline: 2px solid var(--color-focus-ring);
		outline-offset: 2px;
	}

	.achievement-strip.is-award-tone-gold {
		--strip-surface: #f6edcf;
		--strip-award: #725512;
		--strip-border: rgba(66, 47, 7, .18);
	}
	.achievement-strip.is-award-tone-silver {
		--strip-surface: #eceff1;
		--strip-award: #4a555e;
		--strip-border: rgba(34, 43, 50, .16);
	}
	.achievement-strip.is-award-tone-bronze {
		--strip-surface: #f0dfd1;
		--strip-award: #75462b;
		--strip-border: rgba(66, 37, 20, .17);
	}
	.achievement-strip.is-award-tone-iron,
	.achievement-strip.is-award-tone-neutral {
		--strip-surface: #e4dfda;
		--strip-award: #594b42;
		--strip-border: rgba(43, 34, 29, .16);
	}

	.achievement-row.is-award-tone-gold { --achievement-row-tone: #a77a1d; }
	.achievement-row.is-award-tone-silver { --achievement-row-tone: #79848d; }
	.achievement-row.is-award-tone-bronze { --achievement-row-tone: #a0623d; }
	.achievement-row.is-award-tone-iron { --achievement-row-tone: #6f625a; }

	.achievement-strip.is-category-provincial {
		background-image: radial-gradient(circle, var(--strip-pattern) .75px, transparent .9px);
		background-size: 9px 9px;
	}
	.achievement-strip.is-category-invitational {
		background-image: repeating-linear-gradient(135deg, transparent 0 15px, var(--strip-pattern) 15px 16px);
	}
	.achievement-strip.is-category-regional {
		background-image:
			repeating-linear-gradient(135deg, transparent 0 18px, var(--strip-pattern) 18px 19px),
			repeating-linear-gradient(45deg, transparent 0 18px, var(--strip-pattern) 18px 19px);
	}
	.achievement-strip.is-category-final {
		background-image:
			repeating-linear-gradient(135deg, transparent 0 11px, var(--strip-pattern) 11px 12px),
			repeating-linear-gradient(45deg, transparent 0 11px, var(--strip-pattern) 11px 12px),
			radial-gradient(circle, var(--strip-pattern) .8px, transparent 1px);
		background-size: auto, auto, 12px 12px;
	}
	.achievement-strip.is-category-baidu {
		background-image: radial-gradient(circle at 2px 2px, var(--strip-pattern) 1px, transparent 1.2px);
		background-size: 10px 10px;
	}
	.achievement-strip.is-category-gplt {
		background-image: repeating-linear-gradient(90deg, transparent 0 9px, var(--strip-pattern) 9px 10px, transparent 10px 14px, var(--strip-pattern) 14px 15px);
	}
	.achievement-strip.is-category-lanqiao {
		background-image: repeating-linear-gradient(135deg, transparent 0 10px, var(--strip-pattern) 10px 11px, transparent 11px 20px);
	}
	.achievement-expand-toggle {
		display: flex;
		width: 100%;
		min-height: 19px;
		align-items: center;
		justify-content: center;
		gap: 3px;
		margin-top: 3px;
		border: 0;
		border-top: 1px solid var(--color-border);
		background: transparent;
		color: var(--color-text-muted);
		padding: 1px 2px 0;
		font: inherit;
		font-size: 9px;
		font-weight: 700;
		cursor: pointer;
	}
	.achievement-expand-toggle:hover, .achievement-expand-toggle:focus-visible { color: var(--color-text); }
	.achievement-expand-toggle:focus-visible { outline: 2px solid var(--color-focus-ring); outline-offset: 2px; }
	.achievement-expand-toggle .app-icon { width: 10px; height: 10px; transition: transform var(--duration-fast) var(--ease-standard); }
	.achievement-expand-toggle .app-icon.is-open { transform: rotate(180deg); }

	.achievement-strip.is-category-plain {
		background-image: none;
	}

	@media (prefers-reduced-motion: reduce) {
		.achievement-strip, .achievement-expand-toggle .app-icon { transition: none; }
		.achievement-strip:hover { transform: none; }
	}

	@media (max-width: 960px) {
		.achievement-row { grid-template-columns: 52px minmax(0, 1fr); }
		.achievement-controls { grid-column: 2; justify-items: start; }
		.achievement-visibility { justify-content: flex-start; padding-left: 0; }
	}
</style>
