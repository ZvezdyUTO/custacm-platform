<template>
	<header ref="header" @mouseenter="handleMouseEnter" @mouseleave="handleMouseLeave" @mousemove="handleMouseMove">
		<div class="view" :style="viewStyle">
			<img class="image-preloader" :src="images[0]" alt="" @load="loaded = true">
			<div
				v-for="(image, index) in images"
				:key="`${image}-${index}`"
				class="background-layer"
				:style="layerStyle(image, index)"
			></div>
		</div>
		<div class="welcome-title" :aria-label="welcomeText">
			<div class="welcome-wordmark" aria-hidden="true">
				<span
					v-for="character in welcomeLeadCharacters"
					:key="character.key"
					:class="{'welcome-letter': !character.isSpace, 'welcome-space': character.isSpace}"
					:style="character.style"
				>{{ character.value }}</span>
			</div>
			<div class="welcome-brand-mark" aria-hidden="true">CUSTACM</div>
		</div>
		<div class="wrapper">
			<button type="button" class="header-scroll-button" aria-label="向下滚动到文章列表" @click="scrollToMain"><AppIcon name="arrow-down" :size="30" /></button>
		</div>
		<div class="wave1"></div>
		<div class="wave2"></div>
	</header>
</template>

<script>
	import {mapState} from 'vuex'
	import defaultSettings from '@/settings'
	import {getHomepageBanners} from '@/api/index'
	import {homepageBannerHeight, homepageBannerOpacity, homepageBannerPointerRatio} from '@/util/homepageBanner'

	export default {
		name: "Header",
	data() {
			const welcomeText = 'Welcome to CUSTACM'
			let letterIndex = 0
			const welcomeLeadCharacters = [...'WELCOME TO'].map((value, index) => {
				const isSpace = value === ' '
				const currentLetterIndex = letterIndex
				if (!isSpace) letterIndex += 1
				return {
					key: `${value}-${index}`,
					value: isSpace ? '\u00a0' : value,
					isSpace,
					style: isSpace ? undefined : {
						'--delay': `${currentLetterIndex * -0.24}s`,
						'--height': `${10 + (currentLetterIndex % 5) * 2}px`
					}
				}
			})
			return {
				loaded: false,
				images: [defaultSettings.homepageBanner],
				pointerRatio: 0,
				defaultSettings,
				welcomeText,
				welcomeLeadCharacters
			}
		},
		computed: {
			...mapState(['clientSize']),
			viewStyle() {
				return {transform: `translateX(${(this.pointerRatio - 0.5) * 100}px)`}
			}
		},
		watch: {
			'clientSize.clientHeight'() {
				this.setHeaderHeight()
			}
		},
		created() {
			this.loadHomepageBanners()
		},
		mounted() {
			this.setHeaderHeight()
		},
		methods: {
			async loadHomepageBanners() {
				try {
					const res = await getHomepageBanners()
					const urls = res.code === 200 ? res.data.map(item => item.imageUrl).filter(Boolean) : []
					if (urls.length) {
						this.loaded = false
						this.images = urls
					}
				} catch {
					// 后端不可用时继续使用构建内置的默认图片，保证首页仍可展示。
				}
			},
			layerStyle(image, index) {
				return {
					backgroundImage: `url(${image})`,
					opacity: homepageBannerOpacity(this.images.length, this.pointerRatio, index),
					zIndex: 10 + index
				}
			},
			handleMouseEnter() {
				this.$refs.header.classList.add('moving')
			},
			handleMouseLeave() {
				this.pointerRatio = 0
				this.$refs.header.classList.remove('moving')
			},
			handleMouseMove(event) {
				if (this.images.length <= 1) return
				const bounds = this.$refs.header.getBoundingClientRect()
				this.pointerRatio = homepageBannerPointerRatio(event.clientX, bounds.left, bounds.width)
				this.$refs.header.classList.add('moving')
			},
			//根据可视窗口高度，动态改变首图大小
			setHeaderHeight() {
				this.$refs.header.style.height = homepageBannerHeight(
					this.clientSize.clientHeight,
					this.clientSize.clientWidth
				) + 'px'
			},
			//平滑滚动至正文部分
			scrollToMain() {
				window.scrollTo({top: this.clientSize.clientHeight, behavior: 'smooth'})
			}
		},
	}
</script>

<style scoped>
	@import url('https://fonts.googleapis.com/css2?family=Bowlby+One+SC&display=swap');

	header {
		position: relative;
		overflow: hidden;
		user-select: none;
	}

	.view {
		position: absolute;
		top: 0;
		right: 0;
		bottom: 0;
		left: 0;
		display: flex;
		justify-content: center;
		transition: transform .2s ease-in;
	}

	.view .background-layer {
		background-position: center center;
		background-size: cover;
		position: absolute;
		width: 110%;
		height: 100%;
	}

	.view .background-layer {
		transition: opacity .2s ease-in, filter var(--theme-image-duration, 260ms) ease;
	}

	header.moving .view {
		transition: none;
	}

	header.moving .background-layer {
		transition: filter var(--theme-image-duration, 260ms) ease;
	}

	.image-preloader {
		display: none;
	}

	.welcome-title {
		position: absolute;
		z-index: 60;
		top: 33%;
		left: 50%;
		display: flex;
		flex-direction: column;
		align-items: center;
		gap: clamp(18px, 2.4vh, 30px);
		justify-content: center;
		width: min(94vw, 1460px);
		opacity: .8;
		transform: translate(-50%, -50%);
		filter: drop-shadow(0 10px 22px rgb(3 25 48 / 30%));
		animation: welcomeFade 1.2s ease-out both;
	}

	.welcome-wordmark {
		display: flex;
		align-items: center;
		justify-content: center;
		width: min(88vw, 1120px);
		font-family: 'Bowlby One SC', sans-serif;
		font-size: min(9.28vw, 118.4px);
		line-height: 1;
		white-space: nowrap;
		color: #f3f7f8;
		-webkit-text-stroke: min(.18vw, 3px) #111;
		paint-order: stroke fill;
		filter: drop-shadow(.035em .055em 0 rgb(0 0 0 / 56%)) drop-shadow(0 .12em .18em rgb(0 0 0 / 24%));
	}

	.welcome-letter {
		display: inline-block;
		animation: letterFloat 6.4s ease-in-out var(--delay) infinite;
	}

	.welcome-space {
		display: inline-block;
		width: .34em;
	}

	.welcome-brand-mark {
		width: min(88vw, 1120px);
		font-family: 'Bowlby One SC', sans-serif;
		font-size: min(12.56vw, 160px);
		line-height: .76;
		text-align: center;
		white-space: nowrap;
		color: #f3f7f8;
		-webkit-text-stroke: min(.18vw, 3px) #111;
		paint-order: stroke fill;
		filter: drop-shadow(.035em .055em 0 rgb(0 0 0 / 56%)) drop-shadow(0 .12em .18em rgb(0 0 0 / 24%));
		animation: brandFloat 6.4s ease-in-out -1.3s infinite;
	}

	@keyframes welcomeFade {
		from { opacity: 0; transform: translate(-50%, calc(-50% + 14px)); }
		to { opacity: .8; transform: translate(-50%, -50%); }
	}

	@keyframes brandFloat {
		0%, 100% { transform: translateY(5px); }
		50% { transform: translateY(-9px); }
	}

	@keyframes letterFloat {
		0%, 100% { transform: translateY(calc(var(--height) * .35)); }
		50% { transform: translateY(calc(var(--height) * -.65)); }
	}

	@media (prefers-reduced-motion: reduce) {
		.welcome-title,
		.welcome-letter,
		.welcome-brand-mark,
		.header-scroll-button {
			animation: none;
		}
	}

	.wrapper {
		position: absolute;
		width: 100px;
		bottom: 150px;
		left: 0;
		right: 0;
		margin: auto;
		font-size: 26px;
		z-index: 100;
	}

	.header-scroll-button {
		border: 0;
		background: transparent;
		color: #fff;
		padding: 0;
		opacity: 0.5;
		cursor: pointer;
		position: absolute;
		top: 55px;
		left: 20px;
		animation: opener .5s ease-in-out alternate infinite;
		transition: opacity .2s ease-in-out, transform .5s ease-in-out .2s;
	}

	.header-scroll-button:hover {
		opacity: 1;
	}

	@keyframes opener {
		100% {
			top: 65px
		}
	}

	.wave1, .wave2 {
		position: absolute;
		bottom: 0;
		background-image: none;
		-webkit-mask-repeat: repeat-x;
		-webkit-mask-position: left bottom;
		-webkit-mask-size: auto 100%;
		mask-repeat: repeat-x;
		mask-position: left bottom;
		mask-size: auto 100%;
		transition-duration: .4s, .4s;
		z-index: 80;
	}

	.wave1 {
		background-color: color-mix(in srgb, var(--home-canvas, var(--color-canvas-alternate)) 72%, transparent);
		-webkit-mask-image: url('/img/header/wave1.png');
		mask-image: url('/img/header/wave1.png');
		height: 75px;
		width: 100%;
	}

	.wave2 {
		background-color: var(--home-canvas, var(--color-canvas-alternate));
		-webkit-mask-image: url('/img/header/wave2.png');
		mask-image: url('/img/header/wave2.png');
		height: 90px;
		width: calc(100% + 100px);
		left: -100px;
	}
</style>
