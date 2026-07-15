<template>
	<div ref="nav" class="site-nav">
		<div class="site-container nav-container">
			<router-link to="/" class="nav-item nav-brand" aria-label="CUSTACM 首页">
				<span class="nav-brand-plate">
					<img src="/img/custacm-wordmark.png" alt="CUSTACM">
				</span>
			</router-link>
			<router-link to="/home" class="nav-item nav-primary-item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='home'}">
				<AppIcon name="home" />首页
			</router-link>
			<router-link to="/articles" class="nav-item nav-primary-item nav-articles"
			             :class="{'m-mobile-hide': mobileHide, 'active': articleNavigationActive}">
				<AppIcon name="book" />文章
			</router-link>
			<router-link to="/competitions" class="nav-item nav-primary-item nav-competitions"
			             :class="{'m-mobile-hide': mobileHide, 'active': competitionNavigationActive}">
				<AppIcon name="trophy" />赛事荣誉
			</router-link>
			<el-dropdown class="nav-training-dropdown" trigger="click" :class="{'m-mobile-hide': mobileHide}" @command="trainingRoute">
				<button type="button" class="el-dropdown-link nav-item nav-primary-item nav-training-trigger" :class="{'active':trainingNavigationActive}">
					<AppIcon name="bar-chart" />训练中心<AppIcon name="chevron-down" class="nav-menu-chevron" />
				</button>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item command="multiple">多人统计</el-dropdown-item>
						<el-dropdown-item command="single">单人查询</el-dropdown-item>
						<el-dropdown-item command="problem">题目查询</el-dropdown-item>
					</el-dropdown-menu>
				</template>
				</el-dropdown>
			<router-link v-if="authUser" to="/write" class="nav-item nav-primary-item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='write'}">
					<AppIcon name="edit" />发布文章
				</router-link>
			<button
				type="button"
				class="nav-item nav-theme-toggle"
				:class="{'m-mobile-hide': mobileHide}"
				role="switch"
				:aria-label="themeToggleLabel"
				:aria-checked="darkTheme ? 'true' : 'false'"
				:title="themeToggleLabel"
				@click.stop="switchTheme"
			>
				<span class="nav-theme-track" :class="{'is-dark': darkTheme}" aria-hidden="true">
					<span class="nav-theme-thumb"><AppIcon :name="darkTheme ? 'moon' : 'sun'" :size="13" /></span>
				</span>
			</button>
			<router-link v-if="!authUser" :to="loginTarget" class="nav-item nav-account" :class="{'m-mobile-hide': mobileHide}">
				<AppIcon name="user" />登录
			</router-link>
			<el-dropdown v-else trigger="click" class="nav-account" :class="{'m-mobile-hide': mobileHide}" @command="accountCommand">
				<button type="button" class="nav-item nav-auth-trigger">
					<AppIcon name="user-circle" />
					<span>{{ authUser.nickname || authUser.username }}</span>
					<AppIcon name="chevron-down" />
				</button>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item
							v-for="item in accountItems"
							:key="item.command"
							:command="item.command"
							:divided="item.divided"
						>
							<AppIcon :name="item.icon" />{{ item.label }}
						</el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
			<button class="mobile-menu-button m-right-top m-mobile-show" aria-label="展开或收起导航" @click="toggle">
				<AppIcon name="menu" />
			</button>
		</div>
	</div>
</template>

<script>
	import {accountMenuItems} from "@/auth/account-menu";
	import {clearSession, readUser, SESSION_CHANGE_EVENT} from "@/auth/session";
	import {getCurrentTheme, THEME_CHANGE_EVENT, toggleTheme} from '@/theme'

	export default {
		name: "Nav",
		data() {
			return {
				authUser: readUser(),
				darkTheme: getCurrentTheme() === 'dark',
				mobileHide: true,
			}
		},
		computed: {
			articleNavigationActive() {
				return ['articles', 'category', 'tag', 'blog'].includes(this.$route.name)
			},
			competitionNavigationActive() {
				return this.$route.name === 'competitions' || this.$route.name === 'competition-detail'
			},
			trainingNavigationActive() {
				return this.$route.name === 'training'
					&& /^\/training\/(?:multiple|single|problem)\/?$/.test(this.$route.path)
			},
			loginTarget() {
				return {path: '/training/login', query: {returnTo: this.$route.fullPath || '/home'}}
			},
			accountItems() {
				return accountMenuItems(this.authUser)
			},
			themeToggleLabel() {
				return this.darkTheme ? '当前夜间模式，切换到日间模式' : '当前日间模式，切换到夜间模式'
			}
		},
		watch: {
			//路由改变时，收起导航栏
			'$route.path'() {
				this.mobileHide = true
			}
		},
		mounted() {
			window.addEventListener('storage', this.handleStorage)
			window.addEventListener(SESSION_CHANGE_EVENT, this.refreshAuthUser)
			window.addEventListener(THEME_CHANGE_EVENT, this.refreshTheme)
			//监听点击事件，收起导航菜单
			document.addEventListener('click', (e) => {
				//遍历冒泡
				let flag = this.$refs.nav.contains(e.target)
				//如果导航栏是打开状态，且点击的元素不是Nav的子元素，则收起菜单
				if (!this.mobileHide && !flag) {
					this.mobileHide = true
				}
			})
		},
		beforeUnmount() {
			window.removeEventListener('storage', this.handleStorage)
			window.removeEventListener(SESSION_CHANGE_EVENT, this.refreshAuthUser)
			window.removeEventListener(THEME_CHANGE_EVENT, this.refreshTheme)
		},
		methods: {
			switchTheme() {
				this.darkTheme = toggleTheme() === 'dark'
			},
			refreshTheme(event) {
				this.darkTheme = (event?.detail?.theme || getCurrentTheme()) === 'dark'
			},
			accountCommand(command) {
				if (command === 'profile') {
					this.$router.push('/profile')
				} else if (command === 'admin' && this.authUser?.role === 'ROLE_admin') {
					this.$router.push('/training/admin')
				} else if (command === 'logout') {
					this.logout()
				}
			},
			refreshAuthUser() {
				this.authUser = readUser()
			},
			handleStorage(event) {
				if (
					event.key === null
					|| event.key === 'custacm.accessToken'
					|| event.key === 'custacm.user'
				) {
					this.refreshAuthUser()
				}
			},
			logout() {
				clearSession()
				this.authUser = null
			},
			toggle() {
				this.mobileHide = !this.mobileHide
			},
			trainingRoute(path) {
				this.$router.push(`/training/${path}`)
			}
		}
	}
</script>

<style>
	.site-nav .nav-container {
		box-sizing: border-box;
		width: 100% !important;
		max-width: 1400px !important;
		padding: 0 16px;
		margin-left: auto !important;
		margin-right: auto !important;
	}

	.site-nav {
		position: fixed;
		top: 0;
		right: 0;
		left: 0;
		z-index: 2000;
		display: flex;
		min-height: 51px;
		border-bottom: 1px solid rgba(255, 255, 255, .12);
		background: #17191b;
		color: rgba(255, 255, 255, .9);
		transition: .3s ease-out;
	}

	.site-nav .nav-container {
		display: flex;
		min-height: 51px;
		align-items: stretch;
	}

	.site-nav .nav-item {
		position: relative;
		display: flex;
		min-height: 51px;
		align-items: center;
		gap: 7px;
		border: 0;
		background: transparent;
		padding: 0 14px;
		color: inherit;
		font: inherit;
		white-space: nowrap;
		cursor: pointer;
	}

	.site-nav .nav-primary-item {
		box-sizing: border-box;
		gap: 8px;
		font-family: var(--font-sans) !important;
		font-size: 16px !important;
		font-weight: 500 !important;
		line-height: 24px !important;
		letter-spacing: 0 !important;
		text-rendering: geometricPrecision;
	}

	.site-nav .nav-primary-item > .app-icon {
		width: 20px;
		height: 20px;
		flex-basis: 20px;
		stroke-width: 2;
	}

	.site-nav .nav-primary-item > .nav-menu-chevron {
		width: 16px;
		height: 16px;
		flex-basis: 16px;
		margin-left: -2px;
	}

	.site-nav .nav-item.active::after {
		position: absolute;
		right: 12px;
		bottom: 0;
		left: 12px;
		height: 3px;
		background: #48dbfb;
		content: "";
	}

	.site-nav .nav-brand {
		align-self: stretch;
		padding: 5px 8px 5px 0 !important;
		background: transparent !important;
	}

	.nav-brand-plate {
		display: inline-flex;
		height: 34px;
		align-items: center;
		border: 1px solid rgba(255, 255, 255, .32);
		border-radius: 3px;
		background: rgba(244, 246, 248, .96);
		box-shadow: 0 2px 8px rgba(7, 11, 14, .16);
		margin-left: -8px;
		padding: 0 8px;
	}

	.nav-brand-plate img {
		display: block;
		width: 130px;
		height: auto;
	}

	.nav-brand:focus-visible .nav-brand-plate {
		outline: none;
	}

	.el-dropdown-link {
		outline-style: none !important;
		outline-color: unset !important;
		height: 100%;
		cursor: pointer;
	}

	.nav-training-trigger {
		border: 0;
		background: transparent;
		color: inherit;
		font: inherit;
	}

	.mobile-menu-button {
		display: grid;
		width: 48px;
		height: 51px;
		place-items: center;
		border: 0;
		background: #17191b;
		color: #fff;
		cursor: pointer;
	}

	.el-dropdown-menu {
		margin: 0 !important;
		padding: 6px 0 !important;
		border: 0 !important;
		border-radius: 4px !important;
		background: #fff !important;
	}

	.el-dropdown-menu__item {
		display: flex;
		height: 40px;
		min-height: 40px;
		align-items: center;
		padding: 0 18px !important;
		color: #303133 !important;
		font-size: 14px;
		line-height: 1.2 !important;
		white-space: nowrap;
	}

	.el-dropdown-menu__item:hover {
		background: #f5f7fa !important;
		color: #25a9c4 !important;
	}

	.el-dropdown-menu__item > .app-icon {
		display: inline-flex !important;
		width: 20px;
		height: 20px;
		flex: 0 0 20px;
		align-items: center;
		justify-content: center;
		margin: 0 10px 0 0 !important;
		color: #606266;
		font-size: 16px;
		line-height: 20px !important;
		text-align: center;
	}

	.el-dropdown-menu__item:hover > .app-icon {
		color: #25a9c4;
	}

	.el-dropdown-menu__item > a {
		display: flex;
		width: 100%;
		min-height: 36px;
		align-items: center;
		color: inherit;
		text-decoration: none;
	}

	.el-dropdown__popper.el-popper {
		border-color: #e4e7ed !important;
		background: #fff !important;
		box-shadow: 0 6px 18px rgba(0, 0, 0, .14) !important;
		padding: 0 !important;
	}

	.el-dropdown__popper .el-popper__arrow::before {
		border-color: #e4e7ed !important;
		background: #fff !important;
	}

	.nav-theme-toggle {
		min-width: 58px !important;
		justify-content: center;
		margin-left: auto;
		padding-right: 8px !important;
		padding-left: 8px !important;
	}

	.nav-theme-track {
		position: relative;
		display: inline-flex;
		width: 42px;
		height: 24px;
		align-items: center;
		border: 1px solid var(--color-border-strong);
		border-radius: 999px;
		background: var(--color-surface-subtle);
		transition: border-color var(--duration-fast) var(--ease-standard), background var(--duration-fast) var(--ease-standard);
	}

	.nav-theme-track.is-dark {
		border-color: var(--anthropic-clay);
		background: color-mix(in srgb, var(--anthropic-clay) 24%, var(--color-surface));
	}

	.nav-theme-thumb {
		display: grid;
		width: 18px;
		height: 18px;
		place-items: center;
		border-radius: 50%;
		background: var(--color-surface);
		box-shadow: 0 1px 4px color-mix(in srgb, var(--color-text) 18%, transparent);
		color: var(--color-text-muted);
		transform: translateX(2px);
		transition: color var(--duration-fast) var(--ease-standard), transform var(--duration-fast) var(--ease-standard);
	}

	.nav-theme-track.is-dark .nav-theme-thumb {
		color: var(--anthropic-clay);
		transform: translateX(20px);
	}

	.nav-account {
		margin-left: 0;
	}

		.nav-auth-trigger {
			display: flex;
			max-width: 260px;
		align-items: center;
		border: 0;
		background: transparent;
		color: rgba(255, 255, 255, .9);
		cursor: pointer;
		font: inherit;
	}

		.nav-auth-trigger > span {
			min-width: 0;
			max-width: 240px;
		overflow: hidden;
		text-overflow: ellipsis;
		white-space: nowrap;
	}

	@media screen and (min-width: 769px) and (max-width: 1600px) {
		.site-nav .nav-container {
			padding: 0 8px;
		}

		.site-nav .nav-container > .nav-item,
		.site-nav .nav-container .el-dropdown-link,
		.site-nav .nav-container .nav-auth-trigger {
			padding-left: .7em !important;
			padding-right: .7em !important;
		}

			.nav-auth-trigger,
			.nav-auth-trigger > span {
				max-width: 180px;
		}
	}

	@media screen and (max-width: 767px) {
		.site-nav .nav-container {
			display: block;
			padding: 0 52px 0 8px;
		}

		.site-nav .nav-item:not(.nav-brand) {
			width: 100%;
			justify-content: flex-start;
		}

		.nav-account {
			margin-left: 0;
		}

		.nav-theme-toggle {
			justify-content: flex-start !important;
			margin-left: 0;
		}

		.site-nav .nav-brand {
			height: 51px;
		}
	}
</style>
