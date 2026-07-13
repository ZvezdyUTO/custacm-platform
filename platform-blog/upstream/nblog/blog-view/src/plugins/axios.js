import axios from "axios";
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

const request = axios.create({
	baseURL: '/api/',
	timeout: 10000,
})

// 请求拦截
request.interceptors.request.use(
	config => {
		NProgress.start()
		return config
	}
)

// 响应拦截
request.interceptors.response.use(
	config => {
		NProgress.done()
		return config.data
	}
)

export default request
