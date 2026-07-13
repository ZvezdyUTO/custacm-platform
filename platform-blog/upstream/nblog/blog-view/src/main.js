import {createApp} from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
//自定义css
import './assets/css/base.css'
//阿里icon
import './assets/css/icon/iconfont.css'
//typo.css
import "./assets/css/typo.css";
//semantic-ui
import 'semantic-ui-css/semantic.min.css'
//Element Plus
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
//moment
import {dateFilters} from './util/dateTimeFormatUtils.js'
//v-viewer
import 'viewerjs/dist/viewer.css'
import Viewer from 'v-viewer'
//directive
import directives from './util/directive'
//懒加载
import VueLazyload from 'vue-lazyload'
import loadingImage from './assets/img/loading.gif'
import {initializeTheme} from './theme'
// Load theme overrides after every vendor stylesheet.
import './assets/css/night.css'

console.log(
	'%c NBlog %c By Naccl %c https://github.com/Naccl/NBlog',
	'background:#35495e ; padding: 1px; border-radius: 3px 0 0 3px;  color: #fff',
	'background:#41b883 ; padding: 1px; border-radius: 0 3px 3px 0;  color: #000',
	'background:transparent'
)

initializeTheme()

const app = createApp(App)

app.use(ElementPlus)
app.use(Viewer)
app.use(VueLazyload, {
	preLoad: 1.2,
	loading: loadingImage,
})
app.use(directives)

app.config.globalProperties.msgSuccess = function (msg) {
	this.$message.success(msg)
}

app.config.globalProperties.msgError = function (msg) {
	this.$message.error(msg)
}

app.config.globalProperties.msgInfo = function (msg) {
	this.$message.info(msg);
}

app.config.globalProperties.$filters = dateFilters

const cubic = value => Math.pow(value, 3);
const easeInOutCubic = value => value < 0.5 ? cubic(value * 2) / 2 : 1 - cubic((1 - value) * 2) / 2;
//滚动至页面顶部，沿用 Element Plus 回到顶部组件的缓动算法
app.config.globalProperties.scrollToTop = function () {
	const el = document.documentElement
	const beginTime = Date.now()
	const beginValue = el.scrollTop
	const rAF = window.requestAnimationFrame || (func => setTimeout(func, 16))
	const frameFunc = () => {
		const progress = (Date.now() - beginTime) / 500;
		if (progress < 1) {
			el.scrollTop = beginValue * (1 - easeInOutCubic(progress))
			rAF(frameFunc)
		} else {
			el.scrollTop = 0
		}
	}
	rAF(frameFunc)
}


app.use(router)
app.use(store)
app.mount('#app')
