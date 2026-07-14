import {createRouter, createWebHistory} from 'vue-router'
import getPageTitle from '@/util/get-page-title'
import {readToken} from '@/auth/session'

const routes = [
	{
		path: '/login',
		beforeEnter() {
			window.location.replace('/training/login')
		}
	},
	{
		path: '/',
		component: () => import('@/views/Index.vue'),
		redirect: '/home',
		children: [
			{
				path: '/training/:trainingPath(.*)*',
				name: 'training',
				component: () => import('@/views/training/TrainingHost.vue'),
				meta: {title: '训练中心'}
			},
			{
				path: '/home',
				name: 'home',
				component: () => import('@/views/home/Home.vue'),
				meta: {title: '首页'}
			},
				{
					path: '/blog/:id',
				name: 'blog',
				component: () => import('@/views/blog/Blog.vue'),
				meta: {title: '博客'}
				},
				{
					path: '/write/:id(\\d+)?',
					name: 'write',
					component: () => import('@/views/article/ArticleEditor.vue'),
					meta: {title: '发布文章', requiresAuth: true}
				},
			{
				path: '/articles',
				name: 'articles',
				component: () => import('@/views/category/Category.vue'),
				meta: {title: '文章'}
			},
			{
				path: '/tag/:name',
				name: 'tag',
				component: () => import('@/views/tag/Tag.vue'),
				meta: {title: '标签'}
			},
			{
				path: '/category/:name',
				name: 'category',
				component: () => import('@/views/category/Category.vue'),
				meta: {title: '分类'}
			},
			{
				path: '/competitions',
				name: 'competitions',
				component: () => import('@/views/competition/CompetitionList.vue'),
				meta: {title: '赛事荣誉'}
			},
			{
				path: '/competitions/:id(\\d+)',
				name: 'competition-detail',
				component: () => import('@/views/competition/CompetitionDetail.vue'),
				meta: {title: '赛事档案'}
			},
			{
				path: '/profile',
				name: 'profile',
				component: () => import('@/views/profile/Profile.vue'),
				meta: {title: '我的主页'}
			}
		]
	}
]

const router = createRouter({
	history: createWebHistory(import.meta.env.BASE_URL),
	routes
})

//挂载路由守卫
router.beforeEach(to => {
	document.title = getPageTitle(to.meta.title)
	if (to.meta.requiresAuth && !readToken()) {
		return {path: '/training/login', query: {returnTo: to.fullPath}}
	}
})

export default router
