<!-- Author: huangbingrui.awa -->
<template>
	<div class="competition-page competition-detail-page" :aria-busy="loading ? 'true' : 'false'">
		<router-link class="archive-back" :to="backTo">
			<span aria-hidden="true">←</span> 返回赛事荣誉
		</router-link>

		<section v-if="loading" class="detail-state" aria-live="polite">
			<AppIcon name="loader" :size="23" spin />
			<div>
				<strong>正在展开赛事档案</strong>
				<p>参赛成员与荣誉记录正在载入。</p>
			</div>
		</section>

		<section v-else-if="notFound" class="detail-state detail-missing" role="alert">
			<p class="archive-kicker">404 · RECORD NOT FOUND</p>
			<h1>这份赛事档案不存在</h1>
			<p>记录可能已移入回收站，或链接中的赛事编号不正确。</p>
			<router-link class="state-action" to="/competitions">查阅其他赛事</router-link>
		</section>

		<section v-else-if="errorMessage" class="detail-state detail-error" role="alert">
			<p class="archive-kicker">RECORD UNAVAILABLE</p>
			<h1>档案暂时无法读取</h1>
			<p>{{ errorMessage }}</p>
			<button class="state-action" type="button" @click="loadCompetition">重新调阅</button>
		</section>

		<template v-else-if="competition">
			<header class="detail-masthead">
				<div class="detail-title-block">
					<p class="archive-kicker">CUSTACM · OFFICIAL COMPETITION RECORD</p>
					<div class="detail-types">
						<span v-for="category in visibleCategories(competition)" :key="category.code">{{ category.label }}</span>
					</div>
					<h1>{{ competition.fullName }}</h1>
					<p class="detail-subtitle">{{ competition.year }} 年 · {{ competition.participationModeLabel || competition.participationMode }}</p>
				</div>
				<div class="archive-number" aria-label="档案编号">
					<span>ARCHIVE NO.</span>
					<strong>{{ competition.id }}</strong>
					<em>{{ competition.year }}</em>
				</div>
			</header>

			<dl class="detail-overview">
				<div>
					<dt>赛事年份</dt>
					<dd>{{ competition.year }}</dd>
				</div>
				<div>
					<dt>参赛形态</dt>
					<dd>{{ competition.participationModeLabel || competition.participationMode }}</dd>
				</div>
				<div>
					<dt>参赛成员</dt>
					<dd>{{ participants.length }} 人</dd>
				</div>
				<div>
					<dt>荣誉记录</dt>
					<dd>{{ awards.length }} 项</dd>
				</div>
			</dl>

			<div class="detail-ledger">
				<section class="ledger-section participant-section" aria-labelledby="participant-title">
					<header class="ledger-heading">
						<div>
							<p>PARTICIPANTS</p>
							<h2 id="participant-title">参赛成员</h2>
						</div>
						<span>{{ participants.length }}</span>
					</header>

					<p v-if="participants.length === 0" class="ledger-empty">暂无参赛成员记录。</p>
					<ol v-else class="participant-list">
						<li v-for="(participant, index) in participants" :key="participant.id">
							<span class="participant-order">{{ String(index + 1).padStart(2, '0') }}</span>
							<div class="participant-identity">
								<strong>{{ personName(participant) }}</strong>
								<span v-if="participant.username">@{{ participant.username }}</span>
								<span v-else class="retired-mark">账号已注销 · 历史记录保留</span>
							</div>
							<div class="participant-articles">
								<span v-if="safeList(participant.articles).length === 0">暂无公开文章</span>
								<router-link
									v-for="article in safeList(participant.articles)"
									:key="article.id"
									:to="{name: 'blog', params: {id: article.id}}"
								>
									<AppIcon name="file" :size="13" />{{ article.title }}
								</router-link>
							</div>
						</li>
					</ol>
				</section>

				<section class="ledger-section award-section" aria-labelledby="award-title">
					<header class="ledger-heading">
						<div>
							<p>HONOURS</p>
							<h2 id="award-title">荣誉记录</h2>
						</div>
						<span>{{ awards.length }}</span>
					</header>

					<p v-if="awards.length === 0" class="ledger-empty">暂无获奖记录。</p>
					<ol v-else class="award-list">
						<li v-for="(award, index) in awards" :key="award.id" class="award-record">
							<div class="award-spine">
								<span>{{ String(index + 1).padStart(2, '0') }}</span>
								<strong v-if="awardPresentation(award).showRank">{{ award.rank || formattedRank(award) }}</strong>
							</div>
							<div class="award-content">
								<div class="award-heading">
								<div>
									<p>{{ award.awardModeLabel || award.awardMode }}荣誉<span v-if="award.teamName"> · {{ award.teamName }}</span></p>
									<h3>{{ awardPresentation(award).awardTierLabel }}</h3>
									</div>
									<span v-if="award.awardScopeLabel" class="award-scope">{{ award.awardScopeLabel }}</span>
								</div>
								<dl class="award-facts">
									<div>
										<dt>归属形态</dt>
										<dd>{{ award.awardModeLabel || award.awardMode }}</dd>
									</div>
									<div v-if="award.teamName">
										<dt>队伍名称</dt>
										<dd>{{ award.teamName }}</dd>
									</div>
									<div v-if="award.awardScopeLabel">
										<dt>奖项范围</dt>
										<dd>{{ award.awardScopeLabel }}</dd>
									</div>
									<div>
										<dt>奖项档位</dt>
										<dd>{{ awardPresentation(award).awardTierLabel }}</dd>
									</div>
									<div v-if="awardPresentation(award).showRank">
										<dt>赛事排名</dt>
										<dd>{{ award.rank || formattedRank(award) }}</dd>
									</div>
								</dl>
								<div class="recipient-line">
									<span>获奖成员</span>
									<ul>
										<li v-for="recipient in safeList(award.recipients)" :key="recipient.participantId">
											<strong>{{ personName(recipient) }}</strong>
											<small v-if="recipient.username">@{{ recipient.username }}</small>
											<small v-else>历史成员</small>
										</li>
									</ul>
								</div>
							</div>
						</li>
					</ol>
				</section>
			</div>
		</template>
	</div>
</template>

<script>
	import {getCompetition} from '@/api/competition'
	import {achievementPresentation} from '@/utils/achievementPresentation'
	import {publicCompetitionCategories} from '@/utils/competitionTypes'

	export default {
		name: 'CompetitionDetail',
		data() {
			return {
				competition: null,
				loading: false,
				notFound: false,
				errorMessage: '',
				requestId: 0,
			}
		},
		computed: {
			participants() {
				return this.safeList(this.competition?.participants)
			},
			awards() {
				return this.safeList(this.competition?.awards)
			},
			backTo() {
				const raw = Array.isArray(this.$route.query?.from) ? this.$route.query.from[0] : this.$route.query?.from
				return typeof raw === 'string' && /^\/competitions(?:\?|$)/.test(raw) ? raw : '/competitions'
			},
		},
		watch: {
			'$route.params.id'() {
				if (this.$route.name === 'competition-detail') this.loadCompetition()
			},
		},
		created() {
			this.loadCompetition()
		},
		methods: {
			safeList(value) {
				return Array.isArray(value) ? value : []
			},
			visibleCategories(competition) {
				return publicCompetitionCategories(competition)
			},
			awardPresentation(award) {
				return achievementPresentation({...this.competition, ...award, awards: undefined})
			},
			personName(person) {
				return person?.displayName || person?.username || '已注销用户'
			},
			formattedRank(award) {
				if (!award?.rankPosition || !award?.rankTotal) return '排名未记录'
				return `(${award.rankPosition}/${award.rankTotal})`
			},
			async loadCompetition() {
				const requestId = ++this.requestId
				this.loading = true
				this.notFound = false
				this.errorMessage = ''
				this.competition = null
				try {
					const result = await getCompetition(this.$route.params.id)
					if (requestId === this.requestId) this.competition = result
				} catch (error) {
					if (requestId !== this.requestId) return
					const status = error?.response?.status || error?.response?.data?.code || error?.code
					if (Number(status) === 404) {
						this.notFound = true
					} else {
						this.errorMessage = error?.response?.data?.msg || error?.message || '网络连接异常，请稍后重试。'
					}
				} finally {
					if (requestId === this.requestId) this.loading = false
				}
			},
		},
	}
</script>

<style scoped>
	.competition-page {
		--archive-navy: #173149;
		--archive-navy-soft: #31536a;
		--archive-copper: #8d5c35;
		--archive-copper-soft: #d7b593;
		--archive-paper: #f7f4ee;
		--archive-paper-deep: #ede7dc;
		--archive-line: #cfd5d9;
		--archive-ink: #1b2934;
		--archive-muted: #65727b;
		min-height: 620px;
		color: var(--archive-ink);
	}

	.archive-back {
		display: inline-flex;
		align-items: center;
		gap: 9px;
		margin-bottom: 18px;
		border-bottom: 1px solid transparent;
		padding: 4px 0;
		color: var(--archive-navy-soft);
		font-size: 13px;
		font-weight: 600;
	}

	.archive-back:hover,
	.archive-back:focus-visible {
		border-color: var(--archive-copper);
		color: var(--archive-copper);
	}

	.detail-masthead {
		display: grid;
		grid-template-columns: minmax(0, 1fr) 180px;
		gap: 48px;
		border-top: 4px solid var(--archive-navy);
		border-bottom: 1px solid var(--archive-line);
		background: linear-gradient(105deg, var(--archive-paper), rgba(247, 244, 238, .3));
		padding: 42px 48px 38px;
	}

	.archive-kicker,
	.ledger-heading p,
	.award-heading p {
		margin: 0;
		color: var(--archive-copper);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 11px;
		font-weight: 700;
		letter-spacing: .13em;
	}

	.detail-types {
		display: flex;
		flex-wrap: wrap;
		gap: 7px 16px;
		margin: 18px 0 12px;
	}

	.detail-types span {
		color: var(--archive-navy-soft);
		font-size: 12px;
		font-weight: 700;
	}

	.detail-types span + span::before {
		margin-right: 16px;
		color: var(--archive-copper-soft);
		content: "/";
	}

	.detail-masthead h1,
	.detail-state h1 {
		margin: 0;
		color: var(--archive-navy);
		font-family: "Songti SC", "STSong", "Noto Serif CJK SC", serif;
		font-size: clamp(34px, 4vw, 54px);
		font-weight: 700;
		line-height: 1.18;
	}

	.detail-subtitle {
		margin: 20px 0 0;
		color: var(--archive-muted);
		font-size: 15px;
		letter-spacing: .04em;
	}

	.archive-number {
		display: grid;
		align-content: center;
		border-left: 1px solid var(--archive-copper-soft);
		padding-left: 34px;
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
	}

	.archive-number span {
		color: var(--archive-muted);
		font-size: 10px;
		letter-spacing: .14em;
	}

	.archive-number strong {
		overflow: hidden;
		margin: 8px 0 18px;
		color: var(--archive-copper);
		font-size: 30px;
		font-variant-numeric: tabular-nums;
		text-overflow: ellipsis;
	}

	.archive-number em {
		color: var(--archive-navy);
		font-family: Georgia, "Times New Roman", serif;
		font-size: 44px;
		font-style: normal;
		font-weight: 700;
	}

	.detail-overview {
		display: grid;
		grid-template-columns: repeat(4, 1fr);
		gap: 1px;
		background: var(--archive-line);
		margin: 0;
		border-bottom: 1px solid var(--archive-line);
	}

	.detail-overview div {
		background: rgba(255, 255, 255, .66);
		padding: 18px 26px;
	}

	.detail-overview dt,
	.award-facts dt {
		color: var(--archive-muted);
		font-size: 11px;
	}

	.detail-overview dd {
		margin: 6px 0 0;
		color: var(--archive-navy);
		font-family: "Songti SC", serif;
		font-size: 20px;
		font-weight: 700;
	}

	.detail-ledger {
		display: grid;
		grid-template-columns: minmax(310px, .8fr) minmax(0, 1.35fr);
		gap: 0;
		border-bottom: 1px solid var(--archive-line);
		background: rgba(255, 255, 255, .38);
	}

	.ledger-section {
		min-width: 0;
		padding: 34px 38px 42px;
	}

	.participant-section {
		border-right: 1px solid var(--archive-line);
	}

	.ledger-heading {
		display: flex;
		align-items: end;
		justify-content: space-between;
		gap: 18px;
		border-bottom: 2px solid var(--archive-navy);
		padding-bottom: 14px;
	}

	.ledger-heading h2 {
		margin: 6px 0 0;
		color: var(--archive-navy);
		font-family: "Songti SC", serif;
		font-size: 25px;
	}

	.ledger-heading > span {
		color: var(--archive-copper);
		font-family: Georgia, serif;
		font-size: 30px;
		font-weight: 700;
	}

	.ledger-empty {
		border-bottom: 1px solid var(--archive-line);
		margin: 0;
		padding: 28px 0;
		color: var(--archive-muted);
	}

	.participant-list,
	.award-list,
	.recipient-line ul {
		margin: 0;
		padding: 0;
		list-style: none;
	}

	.participant-list > li {
		display: grid;
		grid-template-columns: 28px minmax(100px, .75fr) minmax(130px, 1fr);
		gap: 12px;
		border-bottom: 1px solid var(--archive-line);
		padding: 18px 0;
	}

	.participant-order {
		color: var(--archive-copper);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 11px;
	}

	.participant-identity {
		display: grid;
		align-content: start;
		gap: 4px;
		min-width: 0;
	}

	.participant-identity strong {
		overflow: hidden;
		color: var(--archive-navy);
		text-overflow: ellipsis;
	}

	.participant-identity span,
	.participant-articles > span {
		color: var(--archive-muted);
		font-size: 11px;
	}

	.retired-mark {
		color: var(--archive-copper) !important;
	}

	.participant-articles {
		display: grid;
		align-content: start;
		gap: 7px;
	}

	.participant-articles a {
		display: flex;
		min-width: 0;
		align-items: center;
		gap: 6px;
		color: var(--archive-navy-soft);
		font-size: 12px;
	}

	.participant-articles a:hover,
	.participant-articles a:focus-visible {
		color: var(--archive-copper);
		text-decoration: underline;
	}

	.award-record {
		display: grid;
		grid-template-columns: 94px minmax(0, 1fr);
		border-bottom: 1px solid var(--archive-line);
	}

	.award-spine {
		display: flex;
		align-items: flex-start;
		border-right: 1px solid var(--archive-copper-soft);
		padding: 24px 16px 24px 0;
		flex-direction: column;
	}

	.award-spine span {
		color: var(--archive-muted);
		font-family: ui-monospace, "SFMono-Regular", Consolas, monospace;
		font-size: 10px;
	}

	.award-spine strong {
		margin-top: 10px;
		color: var(--archive-copper);
		font-family: Georgia, serif;
		font-size: 19px;
		font-variant-numeric: tabular-nums;
	}

	.award-content {
		min-width: 0;
		padding: 24px 0 26px 24px;
	}

	.award-heading {
		display: flex;
		align-items: flex-start;
		justify-content: space-between;
		gap: 16px;
	}

	.award-heading h3 {
		margin: 7px 0 0;
		color: var(--archive-navy);
		font-family: "Songti SC", serif;
		font-size: 21px;
	}

	.award-scope {
		flex: 0 0 auto;
		border: 1px solid var(--archive-copper);
		padding: 4px 7px;
		color: var(--archive-copper);
		font-size: 11px;
		font-weight: 700;
	}

	.award-facts {
		display: flex;
		flex-wrap: wrap;
		gap: 12px 24px;
		margin: 20px 0 0;
	}

	.award-facts div {
		min-width: 82px;
	}

	.award-facts dd {
		margin: 4px 0 0;
		color: var(--archive-ink);
		font-size: 13px;
		font-weight: 600;
	}

	.recipient-line {
		display: grid;
		grid-template-columns: 76px minmax(0, 1fr);
		gap: 12px;
		border-top: 1px dashed var(--archive-line);
		margin-top: 20px;
		padding-top: 14px;
	}

	.recipient-line > span {
		color: var(--archive-muted);
		font-size: 11px;
	}

	.recipient-line ul {
		display: flex;
		flex-wrap: wrap;
		gap: 7px;
	}

	.recipient-line li {
		display: inline-flex;
		align-items: baseline;
		gap: 5px;
		border-left: 2px solid var(--archive-copper);
		background: var(--archive-paper-deep);
		padding: 5px 8px;
	}

	.recipient-line strong {
		color: var(--archive-navy);
		font-size: 12px;
	}

	.recipient-line small {
		color: var(--archive-muted);
		font-size: 10px;
	}

	.detail-state {
		display: grid;
		min-height: 480px;
		place-items: center;
		align-content: center;
		gap: 14px;
		border-top: 4px solid var(--archive-navy);
		border-bottom: 1px solid var(--archive-line);
		background: rgba(255, 255, 255, .52);
		padding: 52px;
		text-align: center;
	}

	.detail-state > div {
		display: grid;
		gap: 4px;
	}

	.detail-state p {
		max-width: 560px;
		margin: 8px 0 0;
		color: var(--archive-muted);
	}

	.detail-state .archive-kicker {
		margin-bottom: 4px;
		color: var(--archive-copper);
	}

	.state-action {
		display: inline-flex;
		min-height: 42px;
		align-items: center;
		justify-content: center;
		border: 1px solid var(--archive-navy);
		border-radius: 2px;
		background: var(--archive-navy);
		margin-top: 18px;
		padding: 0 18px;
		color: #fff;
		font-weight: 600;
		cursor: pointer;
	}

	@media (max-width: 1000px) {
		.detail-ledger {
			grid-template-columns: 1fr;
		}

		.participant-section {
			border-right: 0;
			border-bottom: 1px solid var(--archive-line);
		}
	}

	@media (max-width: 767px) {
		.detail-masthead {
			grid-template-columns: 1fr;
			padding: 30px 22px;
		}

		.archive-number {
			grid-template-columns: auto 1fr auto;
			align-items: center;
			gap: 10px;
			border-top: 1px solid var(--archive-copper-soft);
			border-left: 0;
			padding: 18px 0 0;
		}

		.archive-number strong {
			margin: 0;
			font-size: 22px;
		}

		.archive-number em {
			font-size: 30px;
		}

		.detail-overview {
			grid-template-columns: repeat(2, 1fr);
		}

		.ledger-section {
			padding: 28px 22px 34px;
		}

		.participant-list > li {
			grid-template-columns: 28px minmax(0, 1fr);
		}

		.participant-articles {
			grid-column: 2;
		}

		.award-record {
			grid-template-columns: 70px minmax(0, 1fr);
		}

		.award-content {
			padding-left: 18px;
		}
	}
</style>
