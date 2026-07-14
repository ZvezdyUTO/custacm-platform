<template>
	<header class="home-hero">
		<div class="home-hero-copy">
			<h1 aria-label="Welcome to the CUSTACM Platform">
				<span
					v-for="(line, lineIndex) in titleLines"
					:key="`title-line-${lineIndex}`"
					class="home-hero-title-line"
					aria-hidden="true"
				>
					<span
						v-for="character in line"
						:key="character.key"
						:class="character.isSpace ? 'home-hero-title-space' : 'home-hero-title-letter'"
						:style="character.style"
					>{{ character.value }}</span>
				</span>
			</h1>
		</div>
		<figure class="home-hero-media">
			<img src="/img/homepage-banner-default.png" alt="CUSTACM 平台首页首图" loading="eager" decoding="async">
			<ul class="home-hero-logos" aria-label="赛事与校队标志">
				<li>
					<a href="https://icpc.global/" target="_blank" rel="noopener noreferrer" aria-label="访问 ICPC 官网">
						<img class="home-hero-logo home-hero-logo--icpc" src="/img/home-logos/icpc-foundation.svg" alt="ICPC">
					</a>
				</li>
				<li>
					<a href="https://ccpc.io/" target="_blank" rel="noopener noreferrer" aria-label="访问 CCPC 官网">
						<img class="home-hero-logo home-hero-logo--ccpc" src="/img/home-logos/ccpc.png" alt="CCPC">
					</a>
				</li>
				<li>
					<img class="home-hero-logo home-hero-logo--custacm" src="/img/home-logos/custacm-round.png" alt="CUSTACM 校队标志">
				</li>
			</ul>
		</figure>
	</header>
</template>

<script>
	export default {
		name: "Header",
		data() {
			let letterIndex = 0
			const titleLines = ['Welcome to the', 'CUSTACM Platform'].map((line, lineIndex) => [...line].map((value, characterIndex) => {
				const isSpace = value === ' '
				const currentLetterIndex = letterIndex
				if (!isSpace) letterIndex += 1
				return {
					key: `${lineIndex}-${characterIndex}-${value}`,
					value: isSpace ? '\u00a0' : value,
					isSpace,
					style: isSpace ? undefined : {
						'--delay': `${currentLetterIndex * -.24}s`,
						'--height': `${4 + (currentLetterIndex % 5)}px`
					}
				}
			}))
			return {titleLines}
		}
	}
</script>

<style scoped>
	.home-hero {
		position: relative;
		box-sizing: border-box;
		overflow: hidden;
		background: var(--home-canvas, var(--color-canvas));
		padding: clamp(168px, 20vh, 220px) 32px 0;
		color: var(--home-text, var(--color-text));
		user-select: none;
	}

	.home-hero-copy {
		width: min(1280px, 100%);
		margin: 0 auto;
		text-align: center;
	}

	.home-hero h1 {
		margin: 0;
		color: var(--home-text, var(--color-text));
		font-family: ui-sans-serif, -apple-system, BlinkMacSystemFont, "SF Pro Display", "Helvetica Neue", "Segoe UI", Arial, sans-serif;
		font-size: clamp(68px, 7vw, 104px);
		font-weight: 800;
		letter-spacing: -.065em;
		line-height: .98;
		text-rendering: geometricPrecision;
		text-wrap: balance;
		word-spacing: .1em;
	}

	.home-hero-title-line {
		display: block;
		white-space: nowrap;
	}

	.home-hero-title-line:first-child {
		position: relative;
		top: -8px;
	}

	.home-hero-title-letter {
		display: inline-block;
		animation: titleLetterFloat 6.4s ease-in-out var(--delay) infinite;
	}

	.home-hero-title-space {
		display: inline-block;
		width: .36em;
	}

	@keyframes titleLetterFloat {
		0%, 100% { transform: translateY(calc(var(--height) * .35)); }
		50% { transform: translateY(calc(var(--height) * -.65)); }
	}

	.home-hero-media {
		position: relative;
		left: 50%;
		width: 100vw;
		aspect-ratio: 16 / 9;
		overflow: hidden;
		margin: clamp(56px, 6vw, 86px) 0 0;
		transform: translateX(-50%);
	}

	.home-hero-media img {
		display: block;
		width: 100%;
		height: 100%;
		object-fit: cover;
		object-position: center;
	}

	.home-hero-media::after {
		position: absolute;
		inset: auto 0 0;
		height: clamp(190px, 28%, 350px);
		background: linear-gradient(to bottom, transparent 0%, color-mix(in srgb, var(--home-canvas, var(--color-canvas)) 28%, transparent) 24%, color-mix(in srgb, var(--home-canvas, var(--color-canvas)) 72%, transparent) 68%, var(--home-canvas, var(--color-canvas)) 96%);
		backdrop-filter: blur(24px);
		-webkit-backdrop-filter: blur(24px);
		mask-image: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, .12) 12%, rgba(0, 0, 0, .52) 46%, rgba(0, 0, 0, .9) 76%, #000 92%);
		-webkit-mask-image: linear-gradient(to bottom, transparent 0%, rgba(0, 0, 0, .12) 12%, rgba(0, 0, 0, .52) 46%, rgba(0, 0, 0, .9) 76%, #000 92%);
		content: '';
		pointer-events: none;
	}

	.home-hero-logos {
		position: absolute;
		z-index: 2;
		top: 4%;
		left: clamp(24px, 2.5vw, 48px);
		display: flex;
		align-items: center;
		justify-content: flex-start;
		width: min(480px, 36vw);
		margin: 0;
		padding: 0;
		gap: clamp(24px, 2.25vw, 34px);
		list-style: none;
	}

	.home-hero-logos li,
	.home-hero-logos a {
		display: grid;
		place-items: center;
	}

	.home-hero-logos li:nth-child(1) { animation: homeLogoFloatA 6.2s ease-in-out -.8s infinite; }
	.home-hero-logos li:nth-child(2) { animation: homeLogoFloatB 7.1s ease-in-out -3.4s infinite; }
	.home-hero-logos li:nth-child(3) { animation: homeLogoFloatC 5.8s ease-in-out -1.9s infinite; }

	.home-hero-logos a {
		border-radius: 50%;
		transition: opacity 180ms ease, transform 180ms ease;
	}

	.home-hero-logos a:hover,
	.home-hero-logos a:focus-visible {
		opacity: .78;
		transform: translateY(-3px);
	}

	.home-hero-logo {
		display: block;
		width: auto;
		height: clamp(72px, 7vw, 104px);
		object-fit: contain;
	}

	.home-hero-logo--icpc { width: clamp(94px, 9vw, 132px); }
	.home-hero-logo--ccpc { width: clamp(72px, 7vw, 104px); }
	.home-hero-logo--custacm { width: clamp(72px, 7vw, 104px); }

	@keyframes homeLogoFloatA {
		0%, 100% { transform: translateY(1px) rotate(-.08deg); }
		38% { transform: translateY(-3px) rotate(.1deg); }
		72% { transform: translateY(2px) rotate(-.05deg); }
	}

	@keyframes homeLogoFloatB {
		0%, 100% { transform: translateY(-1px) rotate(.08deg); }
		31% { transform: translateY(2px) rotate(-.12deg); }
		68% { transform: translateY(-4px) rotate(.06deg); }
	}

	@keyframes homeLogoFloatC {
		0%, 100% { transform: translateY(1px) rotate(.1deg); }
		44% { transform: translateY(-3px) rotate(-.1deg); }
		76% { transform: translateY(.5px) rotate(.04deg); }
	}

	@media (prefers-reduced-motion: reduce) {
		.home-hero-title-letter,
		.home-hero-logos li {
			animation: none;
		}
	}

	@media screen and (max-width: 767px) {
		.home-hero {
			padding: 112px 16px 0;
		}

		.home-hero h1 {
			font-size: clamp(42px, 12vw, 64px);
			letter-spacing: -.045em;
			line-height: 1.02;
		}

		.home-hero-title-line {
			white-space: normal;
		}

		.home-hero-media { margin-top: 40px; }

		.home-hero-media::after {
			height: 144px;
			backdrop-filter: blur(16px);
			-webkit-backdrop-filter: blur(16px);
		}

		.home-hero-logos {
			top: 8%;
			left: 20px;
			width: calc(100% - 40px);
			gap: 20px;
		}

		.home-hero-logo { height: 64px; }
		.home-hero-logo--icpc { width: 84px; }
		.home-hero-logo--ccpc,
		.home-hero-logo--custacm { width: 64px; }
	}
</style>
