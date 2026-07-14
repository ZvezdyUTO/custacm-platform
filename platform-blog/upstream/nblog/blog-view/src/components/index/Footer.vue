<template>
	<footer class="site-footer" aria-label="站点信息与友情链接">
		<div class="site-container footer-container">
			<p class="footer-greeting">Welcome back to custacm-platform</p>
			<div class="footer-links" aria-label="项目与竞赛平台链接">
				<a
					v-for="item in footerLinks"
					:key="item.url"
					class="footer-link"
					rel="external nofollow noopener"
					:href="item.url"
					:style="{'--link-accent': item.accent}"
					target="_blank"
				>
					<span class="footer-link-icon" aria-hidden="true">
						<img v-if="item.icon" :src="item.icon" alt="">
						<span v-else>{{ item.monogram }}</span>
					</span>
					<span>{{ item.label }}</span>
				</a>
			</div>
		</div>
	</footer>
</template>

<script>
	export default {
		name: "Footer",
		data() {
			return {
				footerLinks: [
					{label: 'ZvezdyUTO/custacm-platform', url: 'https://github.com/ZvezdyUTO/custacm-platform', icon: 'https://github.githubassets.com/favicons/favicon-dark.svg', accent: '#f0f6fc'},
					{label: 'Naccl/NBlog', url: 'https://github.com/Naccl/NBlog', icon: 'https://github.githubassets.com/favicons/favicon-dark.svg', accent: '#f0f6fc'},
					{label: 'Codeforces', url: 'https://codeforces.com/', icon: 'https://codeforces.com/favicon.ico', accent: '#4a90c2'},
					{label: 'AtCoder', url: 'https://atcoder.jp/', icon: 'https://img.atcoder.jp/assets/favicon.png', accent: '#b8b8b8'},
					{label: '洛谷', url: 'https://www.luogu.com.cn/', icon: 'https://www.luogu.com.cn/favicon.ico', accent: '#52c41a'},
					{label: '牛客竞赛', url: 'https://ac.nowcoder.com/', icon: 'https://www.nowcoder.com/favicon.ico', accent: '#00bc9b'},
					{label: 'QOJ', url: 'https://qoj.ac/', monogram: 'Q', accent: '#9b8afb'},
				]
			}
		}
	}
</script>

<style scoped>
	.site-footer {
		margin: 0 !important;
		padding: 12px 0 14px !important;
		background: #17191b !important;
	}

	.footer-container {
		padding-right: 16px;
		padding-left: 16px;
		text-align: center;
	}

	.footer-greeting {
		margin: 0 0 9px;
		color: rgba(255, 255, 255, 0.72);
		font-size: 15px;
		font-weight: 500;
		letter-spacing: 0.04em;
		line-height: 20px;
	}

	.footer-links {
		display: flex;
		align-items: center;
		justify-content: center;
		gap: 8px;
		flex-wrap: wrap;
	}

	.footer-link {
		display: inline-flex;
		min-height: 36px;
		align-items: center;
		gap: 8px;
		border: 1px solid rgba(255, 255, 255, 0.14);
		border-radius: 9px;
		background: rgba(255, 255, 255, 0.055);
		color: rgba(255, 255, 255, 0.86);
		padding: 4px 11px 4px 5px;
		font-size: 13px;
		font-weight: 500;
		line-height: 1;
		text-decoration: none;
		transition: border-color 160ms ease, background 160ms ease, color 160ms ease, transform 160ms ease;
	}

	.footer-link-icon {
		display: inline-flex;
		width: 26px;
		height: 26px;
		align-items: center;
		justify-content: center;
		border-radius: 7px;
		background: rgba(255, 255, 255, 0.1);
		color: var(--link-accent);
		font-size: 14px;
		font-weight: 700;
	}

	.footer-link-icon img {
		width: 18px;
		height: 18px;
		object-fit: contain;
	}

	.footer-link:hover,
	.footer-link:focus-visible {
		border-color: color-mix(in srgb, var(--link-accent) 55%, transparent);
		background: rgba(255, 255, 255, 0.1);
		color: #ffffff;
		transform: translateY(-1px);
	}

	.footer-link:focus-visible {
		outline: 2px solid rgba(72, 219, 251, 0.45);
		outline-offset: 2px;
	}

	@media (prefers-reduced-motion: reduce) {
		.footer-link {
			transition: none;
		}
	}
</style>
