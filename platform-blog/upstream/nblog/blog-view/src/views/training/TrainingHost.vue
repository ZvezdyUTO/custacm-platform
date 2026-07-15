<template>
	<section class="training-host" aria-label="训练中心">
		<iframe
			ref="frame"
			class="training-frame"
			:src="frameSource"
			title="训练中心"
			@load="syncThemeToFrame"
		></iframe>
	</section>
</template>

<script setup>
import {computed, onBeforeUnmount, onMounted, ref} from 'vue'
import {useRoute} from 'vue-router'
import {getCurrentTheme, THEME_CHANGE_EVENT} from '../../theme'
import {buildTrainingFrameSource, isAllowedTrainingRoutePath} from '../../utils/trainingRoute'

// Author: huangbingrui.awa
const route = useRoute()
const frame = ref(null)
const frameSource = computed(() => {
	const rawPath = Array.isArray(route.params.trainingPath) ? route.params.trainingPath.join('/') : route.params.trainingPath
	return buildTrainingFrameSource(rawPath, route.query)
})

function syncTrainingUrl(event) {
	if (event.origin !== window.location.origin || event.source !== frame.value?.contentWindow || event.data?.type !== 'custacm:training-route') return
	const path = typeof event.data.path === 'string' ? event.data.path : ''
	if (!isAllowedTrainingRoutePath(path)) return
	window.history.replaceState(window.history.state, '', `/training${path}`)
}

function syncThemeToFrame(event) {
	const theme = event?.detail?.theme === 'dark' || event?.detail?.theme === 'light'
		? event.detail.theme : getCurrentTheme()
	frame.value?.contentWindow?.postMessage({type: 'custacm:theme', theme}, window.location.origin)
}

onMounted(() => {
	document.documentElement.classList.add('training-host-active')
	window.addEventListener('message', syncTrainingUrl)
	window.addEventListener(THEME_CHANGE_EVENT, syncThemeToFrame)
})
onBeforeUnmount(() => {
	document.documentElement.classList.remove('training-host-active')
	window.removeEventListener('message', syncTrainingUrl)
	window.removeEventListener(THEME_CHANGE_EVENT, syncThemeToFrame)
})
</script>

<style scoped>
.training-host {
	box-sizing: border-box;
	width: 100%;
	height: 100vh;
	overflow: hidden;
	padding-top: 51px;
	background: var(--color-canvas-alternate);
}

.training-frame {
	display: block;
	width: 100%;
	height: calc(100vh - 51px);
	border: 0;
	background: var(--color-canvas-alternate);
}
</style>
