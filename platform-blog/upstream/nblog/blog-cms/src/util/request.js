import axios from 'axios'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import {Message} from 'element-ui'

const request = axios.create({
	baseURL: 'http://localhost:8090/admin/',
	timeout: 5000
})

// 请求拦截
request.interceptors.request.use(config => {
	NProgress.start()
		const token = window.localStorage.getItem('token')
		if (token) {
			config.headers.Authorization = token
		}
		return config
	},
	error => {
		console.info(error)
		return Promise.reject(error)
	}
)

// 响应拦截
request.interceptors.response.use(response => {
		NProgress.done()
		const res = response.data
		if (res.code !== 200) {
			let msg = res.msg || 'Error'
			Message.error(msg)
			return Promise.reject(new Error(msg))
		}
		return res
	},
	error => {
		console.info(error)
		Message.error(error.message)
		return Promise.reject(error)
	}
)

export default request
