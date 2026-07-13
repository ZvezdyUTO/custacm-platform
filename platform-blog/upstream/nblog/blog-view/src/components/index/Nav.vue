<template>
	<div ref="nav" class="ui fixed inverted stackable pointing menu" :class="{'transparent':$route.name==='home' && clientSize.clientWidth>768}">
		<div class="ui container">
			<router-link to="/" class="item nav-brand" aria-label="CUSTACM 首页">
				<span class="nav-brand-plate">
					<img src="/img/custacm-wordmark.png" alt="CUSTACM">
				</span>
			</router-link>
			<router-link to="/home" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='home'}">
				<i class="home icon"></i>首页
			</router-link>
			<el-dropdown trigger="click" @command="categoryRoute">
				<span class="el-dropdown-link item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='category'}">
					<i class="idea icon"></i>分类<i class="caret down icon"></i>
				</span>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item :command="category.name" v-for="(category,index) in categoryList" :key="index">{{ category.name }}</el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
			<el-dropdown class="nav-training-dropdown" trigger="click" :class="{'m-mobile-hide': mobileHide}" @command="trainingRoute">
				<button type="button" class="el-dropdown-link item nav-training-trigger" :class="{'active':trainingNavigationActive}">
					<i class="chart bar icon"></i>训练中心<i class="caret down icon"></i>
				</button>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item command="multiple">多人统计</el-dropdown-item>
						<el-dropdown-item command="single">单人查询</el-dropdown-item>
						<el-dropdown-item command="problem">题目查询</el-dropdown-item>
					</el-dropdown-menu>
				</template>
				</el-dropdown>
				<router-link v-if="authUser" to="/write" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='write'}">
					<i class="pencil alternate icon"></i>发布文章
				</router-link>
			<div class="right item m-search" :class="{'m-mobile-hide': mobileHide}">
				<el-input v-model="queryString" placeholder="搜索文章" aria-label="搜索文章"
				          @input="handleSearchInput" @keyup.enter="submitSearch" @blur="closeSearchResults">
					<template #suffix>
						<i :class="[searchLoading ? 'spinner loading' : 'search', 'icon', 'el-input__icon']"></i>
					</template>
				</el-input>
				<div v-if="searchOpen" class="m-search-item m-search-panel">
					<button v-for="item in queryResult" :key="item.id || item.title" type="button"
					        :disabled="!item.id" @mousedown.prevent="handleSelect(item)">
						<span class="title">{{ item.title }}</span>
						<span v-if="item.description" class="description">{{ item.description }}</span>
					</button>
				</div>
			</div>
			<router-link v-if="!authUser" :to="loginTarget" class="item" :class="{'m-mobile-hide': mobileHide}">
				<i class="user outline icon"></i>登录
			</router-link>
			<el-dropdown v-else trigger="click" :class="{'m-mobile-hide': mobileHide}" @command="accountCommand">
				<button type="button" class="item nav-auth-trigger">
					<i class="user circle icon"></i>
					<span>{{ authUser.nickname || authUser.username }}</span>
					<i class="caret down icon"></i>
				</button>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item
							v-for="item in accountItems"
							:key="item.command"
							:command="item.command"
							:divided="item.divided"
						>
							<i :class="`${item.icon} icon`"></i>{{ item.label }}
						</el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
			<button class="ui menu black icon button m-right-top m-mobile-show" @click="toggle">
				<i class="sidebar icon"></i>
			</button>
		</div>
	</div>
</template>

<script>
	import {getSearchBlogList} from "@/api/blog";
	import {accountMenuItems} from "@/auth/account-menu";
	import {clearSession, readUser, SESSION_CHANGE_EVENT} from "@/auth/session";
	import {mapState} from 'vuex'

	export default {
		name: "Nav",
		props: {
			categoryList: {
				type: Array,
				required: true
			},
		},
		data() {
			return {
				authUser: readUser(),
				mobileHide: true,
				queryString: '',
				queryResult: [],
				searchOpen: false,
				searchLoading: false,
				searchRequestId: 0
			}
		},
		computed: {
			...mapState(['clientSize']),
			trainingNavigationActive() {
				return this.$route.name === 'training'
					&& /^\/training\/(?:multiple|single|problem)\/?$/.test(this.$route.path)
			},
			loginTarget() {
				return {path: '/training/login', query: {returnTo: this.$route.fullPath || '/home'}}
			},
			accountItems() {
				return accountMenuItems(this.authUser)
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
			//监听页面滚动位置，改变导航栏的显示
			window.addEventListener('scroll', () => {
				//首页且不是移动端
				if (this.$route.name === 'home' && this.clientSize.clientWidth > 768) {
					if (window.scrollY > this.clientSize.clientHeight / 2) {
						this.$refs.nav.classList.remove('transparent')
					} else {
						this.$refs.nav.classList.add('transparent')
					}
				}
			})
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
		},
		methods: {
			accountCommand(command) {
				if (command === 'profile') {
					this.$router.push('/about')
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
			categoryRoute(name) {
				this.$router.push(`/category/${name}`)
			},
			trainingRoute(path) {
				this.$router.push(`/training/${path}`)
			},
			handleSearchInput() {
				this.searchRequestId += 1
				this.searchLoading = false
				this.searchOpen = false
			},
			closeSearchResults() {
				this.searchOpen = false
			},
			submitSearch() {
				const query = this.queryString.trim()
				this.searchOpen = false
				if (query === ''
						|| query.indexOf('%') !== -1
						|| query.indexOf('_') !== -1
						|| query.indexOf('[') !== -1
						|| query.indexOf('#') !== -1
						|| query.indexOf('*') !== -1
						|| query.length > 20) {
					return Promise.resolve()
				}
				const requestId = ++this.searchRequestId
				this.searchLoading = true
				return getSearchBlogList(query).then(res => {
					if (requestId !== this.searchRequestId) return
					if (res.code === 200) {
						this.queryResult = Array.isArray(res.data) ? res.data : []
						if (this.queryResult.length === 0) {
							this.queryResult.push({title: '无相关搜索结果'})
						}
						this.searchOpen = true
					}
				}).catch(() => {
					if (requestId !== this.searchRequestId) return
					this.msgError("请求失败")
				}).finally(() => {
					if (requestId === this.searchRequestId) this.searchLoading = false
				})
			},
			handleSelect(item) {
				if (item.id) {
					this.searchOpen = false
					this.$router.push(`/blog/${item.id}`)
				}
			}
		}
	}
</script>

<style>
	.ui.fixed.menu .container {
		box-sizing: border-box;
		width: 100% !important;
		max-width: 1400px !important;
		padding: 0 16px;
		margin-left: auto !important;
		margin-right: auto !important;
	}

	.ui.fixed.menu {
		transition: .3s ease-out;
	}

	.ui.menu .nav-brand.item {
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
		outline: 2px solid #48dbfb;
		outline-offset: 2px;
	}

	.ui.inverted.pointing.menu.transparent {
		background: transparent !important;
	}

	.ui.inverted.pointing.menu.transparent .ui.container > .item:not(.nav-brand):not(.m-search),
	.ui.inverted.pointing.menu.transparent .el-dropdown-link.item,
	.ui.inverted.pointing.menu.transparent .nav-auth-trigger {
		-webkit-text-stroke: .35px rgba(13, 20, 26, .9);
		paint-order: stroke fill;
		text-shadow: 0 1px 2px rgba(13, 20, 26, .72), 0 0 1px rgba(13, 20, 26, .9);
	}

	.ui.inverted.pointing.menu.transparent .active.item:after {
		background: transparent !important;
		transition: .3s ease-out;
	}

	.ui.inverted.pointing.menu.transparent .active.item:hover:after {
		background: transparent !important;
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

	.el-dropdown-menu__item > i.icon {
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

	.el-dropdown-menu__item > i.icon::before {
		display: block;
		line-height: 1 !important;
	}

	.el-dropdown-menu__item:hover > i.icon {
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

	.m-search {
		position: relative;
		width: 220px;
		min-width: 220px;
		margin-left: auto !important;
		padding: 0 !important;
	}

	.m-search input {
		color: #1f2933 !important;
		border: 0px !important;
		background-color: #fff;
		padding: .67857143em .25em .67857143em .85em;
	}

	.m-search input::placeholder {
		color: #909399;
		font-size: 13px;
	}

	.m-search i {
		color: #606266 !important;
	}

	.m-search-item {
		min-width: 350px !important;
	}

	.m-search-panel {
		position: absolute;
		top: calc(100% + 8px);
		right: 0;
		z-index: 2100;
		overflow: hidden;
		border: 1px solid #e4e7ed;
		border-radius: 4px;
		background: #fff;
		box-shadow: 0 6px 18px rgba(0, 0, 0, .14);
	}

	.m-search-panel button {
		display: block;
		width: 100%;
		border: 0;
		background: #fff;
		padding: 9px 12px;
		text-align: left;
		cursor: pointer;
	}

	.m-search-panel button:hover:not(:disabled) {
		background: #f5f7fa;
	}

	.m-search-panel button:disabled {
		cursor: default;
	}

	.m-search-panel .title {
		display: block;
		text-overflow: ellipsis;
		overflow: hidden;
		color: rgba(0, 0, 0, 0.87);
	}

	.m-search-panel .description {
		display: block;
		text-overflow: ellipsis;
		overflow: hidden;
		font-size: 12px;
		color: rgba(0, 0, 0, .70);
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
		.ui.fixed.menu .container {
			padding: 0 8px;
		}

		.ui.fixed.menu .container > .item:not(.m-search),
		.ui.fixed.menu .container > a > .item,
		.ui.fixed.menu .container .el-dropdown-link,
		.ui.fixed.menu .container .nav-auth-trigger {
			padding-left: .7em !important;
			padding-right: .7em !important;
		}

		.m-search {
			width: 160px;
			min-width: 160px;
		}

			.nav-auth-trigger,
			.nav-auth-trigger > span {
				max-width: 180px;
		}
	}
</style>
