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
			<router-link to="/archives" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='archives'}">
				<i class="clone icon"></i>归档
			</router-link>
			<router-link to="/moments" class="item" :class="{'m-mobile-hide': mobileHide,'active':$route.name==='moments'}">
				<i class="comment alternate outline icon"></i>动态
			</router-link>
			<el-dropdown trigger="hover" :class="{'m-mobile-hide': mobileHide}">
				<router-link to="/training/multiple" class="el-dropdown-link item" :class="{'active':$route.name==='training'}">
					<i class="chart bar icon"></i>训练中心<i class="caret down icon"></i>
				</router-link>
				<template #dropdown>
					<el-dropdown-menu>
						<el-dropdown-item><router-link to="/training/multiple">多人统计</router-link></el-dropdown-item>
						<el-dropdown-item><router-link to="/training/single">单人查询</router-link></el-dropdown-item>
						<el-dropdown-item><router-link to="/training/problem">题目查询</router-link></el-dropdown-item>
					</el-dropdown-menu>
				</template>
			</el-dropdown>
			<el-autocomplete v-model="queryString" :fetch-suggestions="debounceQuery" placeholder="Search..."
			                 class="right item m-search" :class="{'m-mobile-hide': mobileHide}"
			                 popper-class="m-search-item" @select="handleSelect">
				<template #suffix>
					<i class="search icon el-input__icon"></i>
				</template>
				<template #default="{ item }">
					<div class="title">{{ item.title }}</div>
					<span class="content">{{ item.content }}</span>
				</template>
			</el-autocomplete>
			<router-link v-if="!authUser" to="/training/login" class="item" :class="{'m-mobile-hide': mobileHide}">
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
				timer: null
			}
		},
		computed: {
			...mapState(['clientSize']),
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
			debounceQuery(queryString, callback) {
				this.timer && clearTimeout(this.timer)
				this.timer = setTimeout(() => this.querySearchAsync(queryString, callback), 1000)
			},
			querySearchAsync(queryString, callback) {
				if (queryString == null
						|| queryString.trim() === ''
						|| queryString.indexOf('%') !== -1
						|| queryString.indexOf('_') !== -1
						|| queryString.indexOf('[') !== -1
						|| queryString.indexOf('#') !== -1
						|| queryString.indexOf('*') !== -1
						|| queryString.trim().length > 20) {
					return
				}
				getSearchBlogList(queryString).then(res => {
					if (res.code === 200) {
						this.queryResult = res.data
						if (this.queryResult.length === 0) {
							this.queryResult.push({title: '无相关搜索结果'})
						}
						callback(this.queryResult)
					}
				}).catch(() => {
					this.msgError("请求失败")
				})
			},
			handleSelect(item) {
				if (item.id) {
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
		width: 220px;
		min-width: 220px;
		margin-left: auto !important;
		padding: 0 !important;
	}

	.m-search input {
		color: rgba(255, 255, 255, .9);;
		border: 0px !important;
		background-color: inherit;
		padding: .67857143em 2.1em .67857143em 1em;
	}

	.m-search i {
		color: rgba(255, 255, 255, .9) !important;
	}

	.m-search-item {
		min-width: 350px !important;
	}

	.m-search-item li {
		line-height: normal !important;
		padding: 8px 10px !important;
	}

	.m-search-item li .title {
		text-overflow: ellipsis;
		overflow: hidden;
		color: rgba(0, 0, 0, 0.87);
	}

	.m-search-item li .content {
		text-overflow: ellipsis;
		font-size: 12px;
		color: rgba(0, 0, 0, .70);
	}

	.nav-auth-trigger {
		display: flex;
		max-width: 200px;
		align-items: center;
		border: 0;
		background: transparent;
		color: rgba(255, 255, 255, .9);
		cursor: pointer;
		font: inherit;
	}

	.nav-auth-trigger > span {
		min-width: 0;
		max-width: 180px;
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
			max-width: 120px;
		}
	}
</style>
