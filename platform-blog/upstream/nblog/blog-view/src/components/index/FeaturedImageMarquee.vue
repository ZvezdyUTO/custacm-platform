<template>
	<section
		v-if="visibleImages.length"
		class="featured-image-marquee"
		:class="{'is-dragging': dragging}"
		aria-label="精选图片"
		@mouseenter="hovered = true"
		@mouseleave="hovered = false"
		@focusin="focused = true"
		@focusout="focused = false"
	>
		<h2 class="featured-image-marquee-title">精选图片</h2>
		<div
			ref="scroller"
			class="featured-image-scroller"
			role="region"
			aria-label="精选图片横向滚动区域，悬停时暂停，可拖动、使用触控板或方向键滚动"
			tabindex="0"
			@keydown.left.prevent="nudge(-1)"
			@keydown.right.prevent="nudge(1)"
			@pointerdown="startDrag"
			@pointermove="drag"
			@pointerup="endDrag"
			@pointercancel="endDrag"
			@wheel="handleWheel"
			@scroll.passive="handleScroll"
		>
			<div ref="track" class="featured-image-track">
				<div v-for="copyIndex in copyCount" :key="copyIndex" class="featured-image-set">
					<figure
						v-for="(image, index) in visibleImages"
						:key="`${copyIndex}-${image.id}`"
						class="featured-image-frame"
						:aria-hidden="copyIndex !== interactiveCopyIndex"
					>
						<button
							type="button"
							class="featured-image-open"
							:tabindex="copyIndex === interactiveCopyIndex ? 0 : -1"
							:aria-label="copyIndex === interactiveCopyIndex ? `预览精选图片 ${index + 1}` : undefined"
							@click="openPreview(image, index)"
						>
							<img
								:src="image.thumbnailUrl || image.imageUrl"
								:alt="copyIndex === 2 ? `精选图片 ${index + 1}` : ''"
								:loading="copyIndex === 2 ? 'eager' : 'lazy'"
								decoding="async"
								:draggable="false"
								@error="handleImageError($event, image)"
							>
						</button>
					</figure>
				</div>
			</div>
		</div>
		<ManagedImageViewer ref="imageViewer" />
	</section>
</template>

<script>
	// Author: huangbingrui.awa
	import {getHomepageFeaturedImages} from '@/api/index'
	import ManagedImageViewer from '@/components/article/ManagedImageViewer.vue'
	import {
		centeredLoopOffset,
		circularCopyCount,
		middleCopyIndex,
		normalizeLoopOffset,
	} from '@/util/circularMarquee'

	const AUTO_SCROLL_PIXELS_PER_SECOND = 22
	const KEYBOARD_NUDGE_PIXELS = 280

	export default {
		name: 'FeaturedImageMarquee',
		components: {ManagedImageViewer},
		data() {
			return {
				images: [],
				copyCount: 5,
				failedIds: new Set(),
				hovered: false,
				focused: false,
				dragging: false,
				reducedMotion: false,
				dragStartX: 0,
				dragStartScrollLeft: 0,
				dragMoved: false,
				suppressClick: false,
				animationFrame: null,
				lastFrameTime: null,
				autoScrollRemainder: 0,
				motionQuery: null,
				resizeObserver: null,
				normalizationFrame: null,
				normalizingScroll: false,
			}
		},
		computed: {
			visibleImages() {
				return this.images.filter(image => image?.imageUrl && !this.failedIds.has(image.id))
		},
			shouldAutoScroll() {
				return !this.reducedMotion && !this.hovered && !this.focused && !this.dragging
			},
			interactiveCopyIndex() {
				return middleCopyIndex(this.copyCount) + 1
			},
		},
		watch: {
			visibleImages() {
				this.$nextTick(this.refreshLoopGeometry)
			},
			shouldAutoScroll() {
				this.lastFrameTime = null
				this.autoScrollRemainder = 0
			}
		},
		async mounted() {
			this.installMotionPreference()
			await this.loadImages()
			this.$nextTick(() => {
				this.refreshLoopGeometry()
				this.installResizeObserver()
				this.animationFrame = window.requestAnimationFrame(this.tick)
			})
		},
		beforeUnmount() {
			if (this.animationFrame !== null) window.cancelAnimationFrame(this.animationFrame)
			if (this.normalizationFrame !== null) window.cancelAnimationFrame(this.normalizationFrame)
			this.resizeObserver?.disconnect()
			this.motionQuery?.removeEventListener?.('change', this.updateMotionPreference)
		},
		methods: {
			async loadImages() {
				try {
					const res = await getHomepageFeaturedImages()
					this.images = res.code === 200 && Array.isArray(res.data)
						? res.data
							.filter(image => image && typeof image.imageUrl === 'string' && image.imageUrl.trim())
							.sort((left, right) => (left.sortOrder ?? 0) - (right.sortOrder ?? 0))
						: []
				} catch {
					this.images = []
				}
			},
			installMotionPreference() {
				if (typeof window.matchMedia !== 'function') return
				this.motionQuery = window.matchMedia('(prefers-reduced-motion: reduce)')
				this.updateMotionPreference(this.motionQuery)
				this.motionQuery.addEventListener?.('change', this.updateMotionPreference)
			},
			updateMotionPreference(event) {
				this.reducedMotion = Boolean(event.matches)
			},
			installResizeObserver() {
				if (typeof ResizeObserver === 'undefined') return
				this.resizeObserver = new ResizeObserver(this.refreshLoopGeometry)
				if (this.$refs.scroller) this.resizeObserver.observe(this.$refs.scroller)
				if (this.$refs.track?.firstElementChild) this.resizeObserver.observe(this.$refs.track.firstElementChild)
			},
			setWidth() {
				return this.$refs.track?.firstElementChild?.getBoundingClientRect().width || 0
			},
			centerOnMiddleCopy() {
				const scroller = this.$refs.scroller
				const width = this.setWidth()
				if (!scroller || !width) return
				scroller.scrollLeft = centeredLoopOffset(this.copyCount, width)
			},
			refreshLoopGeometry() {
				const scroller = this.$refs.scroller
				const width = this.setWidth()
				if (!scroller || !width) return
				const nextCopyCount = circularCopyCount(scroller.clientWidth, width)
				if (nextCopyCount !== this.copyCount) {
					this.copyCount = nextCopyCount
					this.$nextTick(this.centerOnMiddleCopy)
					return
				}
				this.centerOnMiddleCopy()
			},
			normalizeLoopPosition() {
				const scroller = this.$refs.scroller
				const width = this.setWidth()
				if (!scroller || !width) return
				const currentOffset = scroller.scrollLeft
				const normalizedOffset = normalizeLoopOffset(currentOffset, width, this.copyCount)
				const shift = normalizedOffset - currentOffset
				if (Math.abs(shift) < .5) return
				this.normalizingScroll = true
				scroller.scrollLeft = normalizedOffset
				if (this.dragging) this.dragStartScrollLeft += shift
				if (this.normalizationFrame !== null) window.cancelAnimationFrame(this.normalizationFrame)
				this.normalizationFrame = window.requestAnimationFrame(() => {
					this.normalizingScroll = false
					this.normalizationFrame = null
				})
			},
			handleScroll() {
				if (!this.normalizingScroll) this.normalizeLoopPosition()
			},
			tick(timestamp) {
				if (this.lastFrameTime === null) this.lastFrameTime = timestamp
				const elapsed = Math.min(timestamp - this.lastFrameTime, 64)
				this.lastFrameTime = timestamp
				if (this.shouldAutoScroll && this.$refs.scroller) {
					this.autoScrollRemainder += AUTO_SCROLL_PIXELS_PER_SECOND * elapsed / 1000
					const pixels = Math.trunc(this.autoScrollRemainder)
					if (pixels > 0) {
						this.$refs.scroller.scrollLeft += pixels
						this.autoScrollRemainder -= pixels
						this.normalizeLoopPosition()
					}
				}
				this.animationFrame = window.requestAnimationFrame(this.tick)
			},
			startDrag(event) {
				if (event.button !== 0 || !this.$refs.scroller) return
				this.dragging = true
				this.dragMoved = false
				this.dragStartX = event.clientX
				this.dragStartScrollLeft = this.$refs.scroller.scrollLeft
			},
			drag(event) {
				if (!this.dragging || !this.$refs.scroller) return
				if (!this.dragMoved && Math.abs(event.clientX - this.dragStartX) > 6) {
					this.dragMoved = true
					this.$refs.scroller.setPointerCapture?.(event.pointerId)
				}
				this.$refs.scroller.scrollLeft = this.dragStartScrollLeft - (event.clientX - this.dragStartX)
				this.normalizeLoopPosition()
			},
			endDrag(event) {
				if (!this.dragging) return
				this.dragging = false
				this.suppressClick = this.dragMoved
				this.$refs.scroller?.releasePointerCapture?.(event.pointerId)
				this.normalizeLoopPosition()
				window.setTimeout(() => { this.suppressClick = false }, 0)
			},
			handleWheel(event) {
				if (!event.shiftKey || !this.$refs.scroller) return
				event.preventDefault()
				this.$refs.scroller.scrollLeft += event.deltaY || event.deltaX
				this.normalizeLoopPosition()
			},
			nudge(direction) {
				this.$refs.scroller?.scrollBy({
					left: direction * KEYBOARD_NUDGE_PIXELS,
					behavior: this.reducedMotion ? 'auto' : 'smooth'
				})
				window.setTimeout(this.normalizeLoopPosition, this.reducedMotion ? 0 : 320)
			},
			openPreview(image, index) {
				if (this.suppressClick) return
				const thumbnailUrl = image.thumbnailUrl || image.imageUrl
				this.$refs.imageViewer?.open(thumbnailUrl, image.imageUrl, `精选图片 ${index + 1}`)
			},
			handleImageError(event, image) {
				const element = event.currentTarget
				if (!element?.dataset?.originalFallback && image.imageUrl) {
					element.dataset.originalFallback = 'true'
					element.src = image.imageUrl
					return
				}
				this.markFailed(image.id)
			},
			markFailed(id) {
				this.failedIds = new Set([...this.failedIds, id])
			}
		}
	}
</script>

<style scoped>
	.featured-image-marquee {
		position: relative;
		isolation: isolate;
		z-index: 2;
		width: 100%;
		overflow: hidden;
		background: var(--home-canvas, var(--color-canvas));
		margin-top: clamp(-14px, -.8vw, -6px);
		padding: 0 0 clamp(42px, 5vw, 72px);
	}

	.featured-image-marquee::before,
	.featured-image-marquee::after {
		position: absolute;
		z-index: 3;
		top: 0;
		bottom: clamp(42px, 5vw, 72px);
		width: clamp(42px, 8vw, 144px);
		content: '';
		pointer-events: none;
		backdrop-filter: blur(7px);
		-webkit-backdrop-filter: blur(7px);
	}

	.featured-image-marquee::before {
		left: 0;
		background: linear-gradient(90deg, var(--home-canvas, var(--color-canvas)) 10%, color-mix(in srgb, var(--home-canvas, var(--color-canvas)) 70%, transparent) 52%, transparent);
		mask-image: linear-gradient(90deg, #000 8%, rgba(0, 0, 0, .52) 62%, transparent);
		-webkit-mask-image: linear-gradient(90deg, #000 8%, rgba(0, 0, 0, .52) 62%, transparent);
	}

	.featured-image-marquee::after {
		right: 0;
		background: linear-gradient(270deg, var(--home-canvas, var(--color-canvas)) 10%, color-mix(in srgb, var(--home-canvas, var(--color-canvas)) 70%, transparent) 52%, transparent);
		mask-image: linear-gradient(270deg, #000 8%, rgba(0, 0, 0, .52) 62%, transparent);
		-webkit-mask-image: linear-gradient(270deg, #000 8%, rgba(0, 0, 0, .52) 62%, transparent);
	}

	.featured-image-marquee-title {
		position: absolute;
		width: 1px;
		height: 1px;
		overflow: hidden;
		clip: rect(0 0 0 0);
		clip-path: inset(50%);
		white-space: nowrap;
	}

	.featured-image-scroller {
		width: 100%;
		overflow-x: auto;
		overflow-y: hidden;
		cursor: grab;
		overscroll-behavior-inline: contain;
		scrollbar-width: none;
		touch-action: pan-x pinch-zoom;
	}

	.featured-image-scroller::-webkit-scrollbar { display: none; }

	.featured-image-scroller:focus-visible {
		outline: 2px solid var(--color-action);
		outline-offset: -4px;
	}

	.featured-image-marquee.is-dragging .featured-image-scroller {
		cursor: grabbing;
		user-select: none;
	}

	.featured-image-track {
		display: flex;
		width: max-content;
	}

	.featured-image-set {
		display: flex;
		flex: none;
		gap: clamp(12px, 1.25vw, 20px);
		padding-right: clamp(12px, 1.25vw, 20px);
	}

	.featured-image-frame {
		position: relative;
		flex: 0 0 clamp(250px, 25vw, 390px);
		aspect-ratio: 3 / 2;
		overflow: hidden;
		margin: 0;
		border: 1px solid var(--home-border, var(--color-border));
		border-radius: clamp(14px, 1.4vw, 22px);
		background: var(--home-surface, var(--color-surface));
		box-shadow: 0 14px 34px rgb(15 15 15 / 10%);
		transform: translateZ(0);
	}

	.featured-image-open {
		display: block;
		width: 100%;
		height: 100%;
		border: 0;
		background: transparent;
		padding: 0;
		cursor: zoom-in;
	}

	.featured-image-open:focus-visible {
		outline: 3px solid var(--color-action);
		outline-offset: -4px;
	}

	.featured-image-marquee.is-dragging .featured-image-open { cursor: grabbing; }

	.featured-image-frame img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
		transition: transform 360ms ease;
	}

	.featured-image-open:hover img { transform: scale(1.018); }

	@media (prefers-reduced-motion: reduce) {
		.featured-image-frame img { transition: none; }
	}

	@media screen and (max-width: 767px) {
		.featured-image-marquee { padding-bottom: 42px; }
		.featured-image-marquee::before,
		.featured-image-marquee::after {
			bottom: 42px;
			width: 42px;
			backdrop-filter: blur(4px);
			-webkit-backdrop-filter: blur(4px);
		}
		.featured-image-frame { flex-basis: min(74vw, 310px); }
	}
</style>
