<!-- Author: huangbingrui.awa -->
<template>
	<div class="competition-page competition-list-page" :aria-busy="loading ? 'true' : 'false'">
		<header class="archive-masthead">
			<div class="archive-copy">
				<p class="archive-kicker">CUSTACM · COMPETITION ARCHIVE</p>
				<h1>赛事荣誉</h1>
				<p class="archive-intro">勇攀高峰</p>
			</div>
			<div class="archive-scene" aria-hidden="true">
				<img src="/img/competition-archive-contest-hall.jpg" alt="">
			</div>
		</header>

		<form class="archive-filter" aria-label="赛事档案筛选" @submit.prevent="applyFilters">
			<label>
				<span>起始年份</span>
				<input v-model.trim="filters.startYear" type="number" inputmode="numeric" min="1900" max="9999" placeholder="不限">
			</label>
			<span class="range-rule" aria-hidden="true"></span>
			<label>
				<span>结束年份</span>
				<input v-model.trim="filters.endYear" type="number" inputmode="numeric" min="1900" max="9999" placeholder="不限">
			</label>
			<label class="type-filter">
				<span>赛事类型</span>
				<el-select
					v-model="filters.category"
					class="competition-type-select"
					popper-class="competition-type-dropdown"
					placeholder="全部类型"
					aria-label="赛事类型"
				>
					<el-option label="全部类型" value="" />
					<el-option v-for="option in categoryOptions" :key="option.value" :label="option.label" :value="option.value" />
				</el-select>
			</label>
			<div class="filter-actions">
				<button class="filter-submit" type="submit">查阅档案</button>
				<button class="filter-reset" type="button" @click="resetFilters">清除</button>
			</div>
		</form>
		<p v-if="filterError" class="filter-error" role="alert">{{ filterError }}</p>

		<section v-if="loading" class="archive-state archive-loading" aria-live="polite">
			<AppIcon name="loader" :size="22" spin />
			<div>
				<strong>正在调阅赛事档案</strong>
				<span>比赛、成员与荣誉记录将一并载入。</span>
			</div>
		</section>

		<section v-else-if="errorMessage" class="archive-state archive-error" role="alert">
			<p class="state-code">RECORD UNAVAILABLE</p>
			<h2>档案暂时无法读取</h2>
			<p>{{ errorMessage }}</p>
			<button type="button" @click="loadCompetitions">重新调阅</button>
		</section>

		<section v-else-if="records.length === 0" class="archive-state archive-empty">
			<p class="state-code">NO MATCHED RECORD</p>
			<h2>当前范围内暂无赛事记录</h2>
			<p>可以放宽年份区间或切换赛事类型后再查阅。</p>
			<button type="button" @click="resetFilters">查看全部赛事</button>
		</section>

		<template v-else>
			<div ref="archiveSummary" class="archive-summary" tabindex="-1" aria-live="polite">
				<span>共收录 <strong>{{ page.total }}</strong> 场赛事</span>
				<span>第 {{ page.pageNum }} / {{ Math.max(page.totalPages, 1) }} 页</span>
			</div>
			<section class="archive-timeline" aria-label="赛事时间线">
				<article v-for="(competition, index) in records" :key="competition.id" class="archive-record">
					<div class="record-index">
						<time :datetime="String(competition.year)">{{ competition.year }}</time>
						<span>NO. {{ serialNumber(index) }}</span>
					</div>
					<div class="record-body">
						<div class="record-heading">
							<div>
								<div class="type-tags">
									<span v-for="category in visibleCategories(competition)" :key="category.code">{{ category.label }}</span>
								</div>
								<h2>
									<router-link :to="detailTarget(competition.id)">{{ competition.fullName }}</router-link>
								</h2>
							</div>
							<router-link class="record-open" :to="detailTarget(competition.id)" :aria-label="`查看 ${competition.fullName} 完整档案`">
								查看档案 <span aria-hidden="true">↗</span>
							</router-link>
						</div>
						<dl class="record-metrics">
							<div>
								<dt>参赛形态</dt>
								<dd>{{ competition.participationModeLabel || competition.participationMode }}</dd>
							</div>
							<div>
								<dt>参赛成员</dt>
								<dd>{{ safeList(competition.participants).length }} 人</dd>
							</div>
							<div>
								<dt>荣誉记录</dt>
								<dd>{{ safeList(competition.awards).length }} 项</dd>
							</div>
							<div>
								<dt>公开文章</dt>
								<dd>{{ articleCount(competition) }} 篇</dd>
							</div>
						</dl>
						<div v-if="safeList(competition.awards).length" class="record-honours">
							<span v-for="award in safeList(competition.awards).slice(0, 3)" :key="award.id">
								{{ awardSummary(award, competition) }}
							</span>
							<em v-if="safeList(competition.awards).length > 3">另有 {{ safeList(competition.awards).length - 3 }} 项</em>
						</div>
					</div>
				</article>
			</section>

			<nav v-if="page.totalPages > 1" class="archive-pagination" aria-label="赛事档案分页">
				<el-pagination
					background
					layout="prev, pager, next"
					:current-page="page.pageNum"
					:page-size="page.pageSize"
					:total="page.total"
					@current-change="goToPage"
				/>
			</nav>
		</template>
	</div>
</template>

<script>
	import {getCompetitions} from '@/api/competition'
	import {achievementPresentation} from '@/utils/achievementPresentation'
	import {
		legacyRouteCategory,
		PUBLIC_COMPETITION_CATEGORIES,
		PUBLIC_COMPETITION_CATEGORY_VALUES,
		publicCompetitionCategories,
	} from '@/utils/competitionTypes'

	const PAGE_SIZE = 10
	const MIN_YEAR = 1900
	const MAX_YEAR = 9999
	export default {
		name: 'CompetitionList',
		data() {
			return {
				categoryOptions: PUBLIC_COMPETITION_CATEGORIES,
				filters: {startYear: '', endYear: '', category: ''},
				page: {pageNum: 1, pageSize: PAGE_SIZE, total: 0, totalPages: 0, list: []},
				loading: false,
				errorMessage: '',
				filterError: '',
				requestId: 0,
				focusResultsAfterLoad: false,
			}
		},
		computed: {
			records() {
				return this.safeList(this.page.list)
			},
		},
		watch: {
			'$route.fullPath'() {
				if (this.$route.name !== 'competitions') return
				this.readRouteState()
				this.loadCompetitions()
			},
		},
		created() {
			this.readRouteState()
			this.loadCompetitions()
		},
		methods: {
			safeList(value) {
				return Array.isArray(value) ? value : []
			},
			firstQueryValue(value) {
				return Array.isArray(value) ? value[0] : value
			},
			validYearValue(value) {
				const normalized = this.firstQueryValue(value)
				if (normalized === undefined || normalized === null || normalized === '') return ''
				const year = Number(normalized)
				return Number.isInteger(year) && year >= MIN_YEAR && year <= MAX_YEAR ? String(year) : ''
			},
			readRouteState() {
				const query = this.$route.query || {}
				const requestedCategory = String(this.firstQueryValue(query.category) || '').toUpperCase()
				const category = PUBLIC_COMPETITION_CATEGORY_VALUES.has(requestedCategory)
					? requestedCategory
					: legacyRouteCategory(this.firstQueryValue(query.type))
				const pageNum = Number(this.firstQueryValue(query.pageNum))
				this.filters = {
					startYear: this.validYearValue(query.startYear),
					endYear: this.validYearValue(query.endYear),
					category,
				}
				this.page.pageNum = Number.isInteger(pageNum) && pageNum > 0 ? pageNum : 1
			},
			requestQuery(pageNum = this.page.pageNum) {
				const query = {pageNum, pageSize: PAGE_SIZE}
				if (this.filters.startYear) query.startYear = Number(this.filters.startYear)
				if (this.filters.endYear) query.endYear = Number(this.filters.endYear)
				if (this.filters.category) query.category = this.filters.category
				return query
			},
			routeQuery(pageNum = this.page.pageNum) {
				const query = {}
				if (this.filters.startYear) query.startYear = this.filters.startYear
				if (this.filters.endYear) query.endYear = this.filters.endYear
				if (this.filters.category) query.category = this.filters.category
				if (pageNum > 1) query.pageNum = String(pageNum)
				return query
			},
			querySignature(query) {
				return JSON.stringify(Object.keys(query || {}).sort().map(key => [key, String(query[key])]))
			},
			visibleCategories(competition) {
				return publicCompetitionCategories(competition)
			},
			navigate(pageNum) {
				const query = this.routeQuery(pageNum)
				if (this.querySignature(query) === this.querySignature(this.$route.query || {})) {
					this.page.pageNum = pageNum
					this.loadCompetitions()
					return
				}
				this.$router.push({name: 'competitions', query})
			},
			applyFilters() {
				this.filterError = ''
				const start = this.filters.startYear === '' ? null : Number(this.filters.startYear)
				const end = this.filters.endYear === '' ? null : Number(this.filters.endYear)
				if ((start !== null && (!Number.isInteger(start) || start < MIN_YEAR || start > MAX_YEAR))
					|| (end !== null && (!Number.isInteger(end) || end < MIN_YEAR || end > MAX_YEAR))) {
					this.filterError = '年份必须在 1900 到 9999 之间。'
					return
				}
				if (start !== null && end !== null && start > end) {
					this.filterError = '起始年份不能晚于结束年份。'
					return
				}
				this.navigate(1)
			},
			resetFilters() {
				this.filters = {startYear: '', endYear: '', category: ''}
				this.filterError = ''
				this.navigate(1)
			},
			goToPage(pageNum) {
				this.focusResultsAfterLoad = true
				this.navigate(pageNum)
			},
			async loadCompetitions() {
				const requestId = ++this.requestId
				this.loading = true
				this.errorMessage = ''
				try {
					const result = await getCompetitions(this.requestQuery())
					if (requestId !== this.requestId) return
					this.page = {
						pageNum: Number(result?.pageNum) || this.page.pageNum,
						pageSize: Number(result?.pageSize) || PAGE_SIZE,
						total: Number(result?.total) || 0,
						totalPages: Number(result?.totalPages) || 0,
						list: this.safeList(result?.list),
					}
					if (this.focusResultsAfterLoad) {
						this.focusResultsAfterLoad = false
						this.$nextTick(() => {
							const summary = this.$refs.archiveSummary
							if (!summary) return
							summary.focus({preventScroll: true})
							summary.scrollIntoView({
								behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'auto' : 'smooth',
								block: 'start',
							})
						})
					}
				} catch (error) {
					if (requestId !== this.requestId) return
					this.errorMessage = error?.response?.data?.msg || error?.message || '网络连接异常，请稍后重试。'
				} finally {
					if (requestId === this.requestId) this.loading = false
				}
			},
			detailTarget(id) {
				return {name: 'competition-detail', params: {id}, query: {from: this.$route.fullPath}}
			},
			serialNumber(index) {
				return String((this.page.pageNum - 1) * this.page.pageSize + index + 1).padStart(2, '0')
			},
			articleCount(competition) {
				return this.safeList(competition?.participants)
					.reduce((count, participant) => count + this.safeList(participant?.articles).length, 0)
			},
			awardSummary(award, competition) {
				return achievementPresentation({...competition, ...award, awards: undefined}).awardLabel
			},
		},
	}
</script>

<style scoped>
	.competition-page {
		--archive-navy: #173149;
		--archive-navy-soft: #29485f;
		--archive-copper: #8d5c35;
		--archive-copper-soft: #d8b896;
		--archive-paper: #f7f4ee;
		--archive-paper-deep: #ede7dc;
		--archive-line: #cfd5d9;
		--archive-ink: #1b2934;
		--archive-muted: #65727b;
		min-height: 620px;
		color: var(--archive-ink);
	}

	.archive-masthead {
		position: relative;
		display: flex;
		min-height: 284px;
		align-items: center;
		overflow: hidden;
		border-top: 4px solid var(--archive-navy);
		border-bottom: 1px solid var(--archive-line);
		background: var(--archive-paper);
		padding: 38px 42px 34px;
	}

	.archive-copy {
		position: relative;
		z-index: 1;
		max-width: 64%;
	}

	.archive-kicker,
	.state-code {
		margin: 0 0 10px;
		color: var(--archive-copper);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 12px;
		font-weight: 700;
		letter-spacing: .16em;
	}

	.archive-masthead h1 {
		margin: 0;
		color: var(--archive-navy);
		font-family: "Songti SC", "STSong", "Noto Serif CJK SC", serif;
		font-size: clamp(38px, 4vw, 58px);
		font-weight: 700;
		letter-spacing: .08em;
		line-height: 1.05;
	}

	.archive-intro {
		max-width: 680px;
		margin: 18px 0 0 clamp(32px, 3vw, 52px);
		color: var(--archive-muted);
		font-size: 15px;
		line-height: 1.8;
	}

	.archive-scene {
		position: absolute;
		top: 0;
		right: 0;
		bottom: 0;
		width: 40%;
		background: var(--archive-paper);
		pointer-events: none;
	}

	.archive-scene img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
		object-position: right 48%;
		opacity: .3;
		filter: saturate(.58) contrast(.82) brightness(1.08);
		mix-blend-mode: multiply;
	}

	.archive-scene::after {
		position: absolute;
		inset: 0;
		background: linear-gradient(90deg, var(--archive-paper) 0%, rgba(247, 244, 238, .7) 17%, rgba(247, 244, 238, .2) 100%);
		content: "";
	}

	.archive-filter {
		display: grid;
		grid-template-columns: minmax(128px, 160px) 34px minmax(128px, 160px) minmax(220px, 1fr) auto;
		align-items: end;
		gap: 18px;
		border-bottom: 1px solid var(--archive-line);
		background: rgba(255, 255, 255, .66);
		padding: 24px 42px;
	}

	.archive-filter label {
		display: grid;
		gap: 8px;
		color: var(--archive-muted);
		font-size: 12px;
		font-weight: 600;
		letter-spacing: .06em;
	}

	.archive-filter input {
		width: 100%;
		height: 42px;
		border: 1px solid var(--archive-line);
		border-radius: 2px;
		outline: 0;
		background: transparent;
		padding: 0 12px;
		color: var(--archive-ink);
	}

	.archive-filter input:focus {
		border-color: var(--archive-copper);
		box-shadow: 0 0 0 3px color-mix(in srgb, var(--archive-copper) 18%, transparent);
	}

	.competition-type-select {
		width: 100%;
	}

	.competition-type-select :deep(.el-select__wrapper) {
		min-height: 42px;
		border: 1px solid var(--archive-line);
		border-radius: 2px;
		background: transparent;
		box-shadow: none;
		padding: 0 12px;
	}

	.competition-type-select :deep(.el-select__wrapper.is-focused) {
		border-color: var(--archive-copper);
		box-shadow: 0 0 0 3px color-mix(in srgb, var(--archive-copper) 18%, transparent);
	}

	:global(.competition-type-dropdown.el-popper) {
		border: 1px solid rgba(23, 49, 73, .14);
		border-radius: 4px;
		background: rgba(255, 255, 255, .98);
		box-shadow: 0 14px 34px rgba(19, 35, 49, .16);
		backdrop-filter: blur(14px);
	}

	:global(.competition-type-dropdown .el-select-dropdown__list) {
		padding: 5px 0;
	}

	:global(.competition-type-dropdown .el-select-dropdown__item) {
		height: 32px;
		padding: 0 14px;
		color: #34424e;
		font-size: 13px;
		line-height: 32px;
	}

	:global(.competition-type-dropdown .el-select-dropdown__item.is-hovering),
	:global(.competition-type-dropdown .el-select-dropdown__item:hover) {
		background: #f3f6f8;
		color: #25a9c4;
	}

	:global(.competition-type-dropdown .el-select-dropdown__item.is-selected) {
		color: #173149;
		font-weight: 700;
	}

	.range-rule {
		height: 1px;
		margin-bottom: 20px;
		background: var(--archive-copper-soft);
	}

	.filter-actions {
		display: flex;
		gap: 8px;
	}

	.filter-actions button,
	.archive-state button {
		min-height: 42px;
		border: 1px solid var(--archive-navy);
		border-radius: 2px;
		padding: 0 18px;
		font-weight: 600;
		cursor: pointer;
		transition: background-color var(--duration-fast), color var(--duration-fast), transform var(--duration-fast);
	}

	.filter-submit,
	.archive-state button {
		background: var(--archive-navy);
		color: #fff;
	}

	.filter-reset {
		background: transparent;
		color: var(--archive-navy);
	}

	.filter-actions button:hover,
	.archive-state button:hover {
		transform: translateY(-1px);
	}

	.filter-error {
		border-bottom: 1px solid #e6c9c7;
		background: #fff5f4;
		margin: 0;
		padding: 10px 42px;
		color: #9c3430;
	}

	.archive-state {
		display: grid;
		min-height: 320px;
		place-items: center;
		align-content: center;
		border-bottom: 1px solid var(--archive-line);
		background: rgba(255, 255, 255, .48);
		padding: 48px;
		text-align: center;
	}

	.archive-loading {
		grid-template-columns: auto auto;
		gap: 16px;
		text-align: left;
	}

	.archive-loading div {
		display: grid;
		gap: 4px;
	}

	.archive-loading span,
	.archive-state > p:not(.state-code) {
		color: var(--archive-muted);
	}

	.archive-state h2 {
		margin: 0 0 8px;
		color: var(--archive-navy);
		font-family: "Songti SC", serif;
		font-size: 26px;
	}

	.archive-state button {
		margin-top: 18px;
	}

	.archive-summary {
		display: flex;
		justify-content: space-between;
		scroll-margin-top: 88px;
		border-bottom: 1px solid var(--archive-line);
		padding: 16px 42px;
		color: var(--archive-muted);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 12px;
		letter-spacing: .05em;
	}

	.archive-summary:focus-visible {
		outline: 2px solid var(--archive-copper);
		outline-offset: -2px;
	}

	.archive-summary strong {
		color: var(--archive-copper);
		font-size: 15px;
	}

	.archive-timeline {
		position: relative;
		background: rgba(255, 255, 255, .38);
		padding: 0 42px;
	}

	.archive-timeline::before {
		position: absolute;
		top: 0;
		bottom: 0;
		left: 119px;
		width: 1px;
		background: var(--archive-copper-soft);
		content: "";
	}

	.archive-record {
		position: relative;
		display: grid;
		grid-template-columns: 78px minmax(0, 1fr);
		gap: 36px;
	}

	.record-index {
		position: relative;
		z-index: 1;
		display: flex;
		align-items: flex-end;
		flex-direction: column;
		padding-top: 34px;
		text-align: right;
	}

	.record-index::after {
		position: absolute;
		top: 42px;
		right: -42px;
		width: 11px;
		height: 11px;
		border: 3px solid var(--archive-paper);
		border-radius: 50%;
		background: var(--archive-copper);
		box-shadow: 0 0 0 1px var(--archive-copper);
		content: "";
	}

	.record-index time {
		color: var(--archive-navy);
		font-family: Georgia, "Times New Roman", serif;
		font-size: 24px;
		font-variant-numeric: tabular-nums;
		font-weight: 700;
		line-height: 1;
	}

	.record-index span {
		margin-top: 8px;
		color: var(--archive-muted);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 10px;
		letter-spacing: .08em;
	}

	.record-body {
		border-bottom: 1px solid var(--archive-line);
		padding: 30px 0 32px;
		transition: border-color var(--duration-fast), transform var(--duration-fast);
	}

	.archive-record:hover .record-body {
		border-bottom-color: var(--archive-copper);
		transform: translateX(4px);
	}

	.record-heading {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 24px;
	}

	.type-tags {
		display: flex;
		flex-wrap: wrap;
		gap: 6px 14px;
		margin-bottom: 10px;
	}

	.type-tags span {
		color: var(--archive-copper);
		font-size: 11px;
		font-weight: 700;
		letter-spacing: .06em;
		text-transform: uppercase;
	}

	.type-tags span + span::before {
		margin-right: 14px;
		color: var(--archive-line);
		content: "/";
	}

	.record-heading h2 {
		margin: 0;
		color: var(--archive-navy);
		font-family: "Songti SC", "STSong", serif;
		font-size: clamp(21px, 2vw, 28px);
		line-height: 1.35;
	}

	.record-heading h2 a:hover,
	.record-heading h2 a:focus-visible {
		color: var(--archive-copper);
	}

	.record-open {
		flex: 0 0 auto;
		border-bottom: 1px solid var(--archive-copper);
		padding: 5px 0;
		color: var(--archive-navy-soft);
		font-size: 12px;
		font-weight: 600;
	}

	.record-metrics {
		display: grid;
		grid-template-columns: repeat(4, minmax(100px, 1fr));
		gap: 1px;
		background: var(--archive-line);
		margin: 24px 0 0;
	}

	.record-metrics div {
		background: var(--archive-paper);
		padding: 12px 14px;
	}

	.record-metrics dt {
		color: var(--archive-muted);
		font-size: 11px;
	}

	.record-metrics dd {
		margin: 5px 0 0;
		color: var(--archive-navy);
		font-weight: 700;
	}

	.record-honours {
		display: flex;
		flex-wrap: wrap;
		gap: 8px;
		margin-top: 14px;
	}

	.record-honours span,
	.record-honours em {
		border-left: 2px solid var(--archive-copper);
		background: color-mix(in srgb, var(--archive-paper-deep) 74%, transparent);
		padding: 5px 9px;
		color: var(--archive-muted);
		font-size: 12px;
		font-style: normal;
	}

	.archive-pagination {
		display: flex;
		justify-content: center;
		border-bottom: 1px solid var(--archive-line);
		padding: 30px 0 8px;
	}

	.archive-pagination :deep(.el-pager li.is-active),
	.archive-pagination :deep(.btn-prev:hover),
	.archive-pagination :deep(.btn-next:hover),
	.archive-pagination :deep(.el-pager li:hover) {
		background: var(--archive-navy) !important;
		color: #fff !important;
	}

	@media (max-width: 1000px) {
		.archive-filter {
			grid-template-columns: 1fr 22px 1fr 1.5fr;
		}

		.filter-actions {
			grid-column: 1 / -1;
		}
	}

	@media (max-width: 767px) {
		.archive-masthead {
			min-height: 250px;
			padding: 28px 22px;
		}

		.archive-copy {
			max-width: 78%;
		}

		.archive-scene {
			width: 56%;
		}

		.archive-scene img {
			opacity: .1;
		}

		.archive-filter {
			grid-template-columns: 1fr 16px 1fr;
			padding: 20px 22px;
		}

		.type-filter,
		.filter-actions {
			grid-column: 1 / -1;
		}

		.archive-summary,
		.archive-timeline {
			padding-right: 22px;
			padding-left: 22px;
		}

		.archive-timeline::before {
			left: 35px;
		}

		.archive-record {
			grid-template-columns: 1fr;
			gap: 0;
			padding-left: 28px;
		}

		.record-index {
			align-items: flex-start;
			padding-top: 26px;
			text-align: left;
		}

		.record-index::after {
			top: 33px;
			right: auto;
			left: -15px;
		}

		.record-body {
			padding-top: 16px;
		}

		.record-heading,
		.archive-summary {
			align-items: flex-start;
			flex-direction: column;
		}

		.record-metrics {
			grid-template-columns: repeat(2, 1fr);
		}
	}

	@media (prefers-reduced-motion: reduce) {
		.record-body,
		.filter-actions button,
		.archive-state button {
			transition: none;
		}

		.archive-record:hover .record-body,
		.filter-actions button:hover,
		.archive-state button:hover {
			transform: none;
		}
	}
</style>
