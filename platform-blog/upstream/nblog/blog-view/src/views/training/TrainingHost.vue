<template>
	<section class="training-host" aria-label="训练中心">
		<iframe
			ref="frame"
			class="training-frame"
			:src="frameSource"
			title="训练中心"
		></iframe>
	</section>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted} from 'vue'
import {useRoute} from 'vue-router'

// Author: huangbingrui.awa
const route = useRoute()
const allowedPages = new Set(['login', 'multiple', 'single', 'problem', 'admin', 'admin/create-users', 'admin/users', 'admin/articles', 'admin/training', 'admin/appearance'])
const frameSource = computed(() => {
	const rawPath = Array.isArray(route.params.trainingPath) ? route.params.trainingPath.join('/') : route.params.trainingPath
	const page = allowedPages.has(rawPath) ? rawPath : 'multiple'
	const query = new URLSearchParams()
	for (const [key, value] of Object.entries(route.query)) {
		if (typeof value === 'string') query.set(key, value)
	}
	return `/training-app/${page}${query.size ? `?${query.toString()}` : ''}`
})

function syncTrainingUrl(event) {
	if (event.origin !== window.location.origin || event.data?.type !== 'custacm:training-route') return
	const path = typeof event.data.path === 'string' ? event.data.path : ''
	if (!/^\/(?:login|multiple|single|problem|admin(?:\/(?:create-users|users|articles|training|appearance))?)(?:\?|$)/.test(path)) return
	window.history.replaceState(window.history.state, '', `/training${path}`)
}

onMounted(() => window.addEventListener('message', syncTrainingUrl))
onBeforeUnmount(() => window.removeEventListener('message', syncTrainingUrl))
</script>

<style scoped>
.training-host {
	width: 100%;
	height: 100vh;
	overflow: hidden;
	padding-top: 51px;
	background: #f4f6f8;
}

.training-frame {
	display: block;
	width: 100%;
	height: calc(100vh - 51px);
	border: 0;
	background: #f4f6f8;
}
</style>
