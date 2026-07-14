<template>
  <footer class="login-footer" aria-label="站点信息与友情链接">
    <p class="login-footer-greeting">Welcome back to custacm-platform</p>
    <nav class="login-footer-links" aria-label="项目与竞赛平台链接">
      <a
        v-for="item in footerLinks"
        :key="item.url"
        class="login-footer-link"
        :href="item.url"
        :style="{ '--link-accent': item.accent }"
        rel="external nofollow noopener"
        target="_blank"
      >
        <span class="login-footer-icon" aria-hidden="true">
          <img v-if="item.icon" :src="item.icon" alt="" />
          <span v-else>{{ item.monogram }}</span>
        </span>
        <span>{{ item.label }}</span>
      </a>
    </nav>
  </footer>
</template>

<script setup lang="ts">
// Mirrors the Blog shell footer inside the Training iframe so the login route
// can reach it without reintroducing document-level scrolling.
type FooterLink = {
  label: string;
  url: string;
  accent: string;
  icon?: string;
  monogram?: string;
};

const footerLinks: readonly FooterLink[] = [
  { label: 'ZvezdyUTO/custacm-platform', url: 'https://github.com/ZvezdyUTO/custacm-platform', icon: 'https://github.githubassets.com/favicons/favicon-dark.svg', accent: '#f0f6fc' },
  { label: 'Naccl/NBlog', url: 'https://github.com/Naccl/NBlog', icon: 'https://github.githubassets.com/favicons/favicon-dark.svg', accent: '#f0f6fc' },
  { label: 'Codeforces', url: 'https://codeforces.com/', icon: 'https://codeforces.com/favicon.ico', accent: '#4a90c2' },
  { label: 'AtCoder', url: 'https://atcoder.jp/', icon: 'https://img.atcoder.jp/assets/favicon.png', accent: '#b8b8b8' },
  { label: '洛谷', url: 'https://www.luogu.com.cn/', icon: 'https://www.luogu.com.cn/favicon.ico', accent: '#52c41a' },
  { label: '牛客竞赛', url: 'https://ac.nowcoder.com/', icon: 'https://www.nowcoder.com/favicon.ico', accent: '#00bc9b' },
  { label: 'QOJ', url: 'https://qoj.ac/', monogram: 'Q', accent: '#9b8afb' },
];
</script>

<style scoped>
.login-footer {
  box-sizing: border-box;
  width: 100%;
  align-self: end;
  margin-top: 52px;
  border-radius: 18px 18px 0 0;
  background: #17191b;
  padding: 18px 24px 20px;
  text-align: center;
}

.login-footer-greeting {
  margin: 0 0 12px;
  color: rgb(255 255 255 / 72%);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.04em;
  line-height: 20px;
}

.login-footer-links {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-wrap: wrap;
}

.login-footer-link {
  display: inline-flex;
  min-height: 36px;
  align-items: center;
  gap: 8px;
  border: 1px solid rgb(255 255 255 / 14%);
  border-radius: 9px;
  background: rgb(255 255 255 / 5.5%);
  padding: 4px 11px 4px 5px;
  color: rgb(255 255 255 / 86%);
  font-size: 13px;
  font-weight: 600;
  line-height: 1;
  text-decoration: none;
  transition: border-color 160ms ease, background 160ms ease, color 160ms ease, transform 160ms ease;
}

.login-footer-icon {
  display: inline-flex;
  width: 26px;
  height: 26px;
  align-items: center;
  justify-content: center;
  border-radius: 7px;
  background: rgb(255 255 255 / 10%);
  color: var(--link-accent);
  font-size: 14px;
  font-weight: 700;
}

.login-footer-icon img {
  width: 18px;
  height: 18px;
  object-fit: contain;
}

.login-footer-link:hover,
.login-footer-link:focus-visible {
  border-color: color-mix(in srgb, var(--link-accent) 55%, transparent);
  background: rgb(255 255 255 / 10%);
  color: #fff;
  transform: translateY(-1px);
}

.login-footer-link:focus-visible {
  outline: 2px solid rgb(72 219 251 / 45%);
  outline-offset: 2px;
}

@media (prefers-reduced-motion: reduce) {
  .login-footer-link {
    transition: none;
  }
}
</style>
