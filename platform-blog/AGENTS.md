# platform-blog Agent Notes

- `upstream/nblog/blog-api` 是根 Maven reactor 中唯一可运行后端，负责 Blog、首页图片存储、BCrypt 账号、HS512 JWT、`username`、`ROLE_admin`/`ROLE_player`、OJ handle 和训练数据 HTTP adapter。
- `upstream/nblog/blog-view` 是生产环境 `/` 下的 Vue 3 + Vite Blog，并持有 `/training/**` 外层路由和唯一 `Nav.vue`；独立训练运行时挂载在内部 `/training-app/**`。两者由 `frontend/Dockerfile` 构建并交给同一个 Nginx 服务。
- `upstream/nblog/blog-cms` 仅保留上游源码，当前 Compose 不构建、不部署，也不要把它写成现行管理员入口。
- 浏览器 API 统一从 `/api/**` 进入 Nginx，Blog API 的直接路径不带 `/api`。不要在 Vue Axios 全局拦截器附加共享 JWT。
- Vue Blog 可读取 `custacm.accessToken` 与 `custacm.user` 展示账号摘要；左侧名片纯展示头像、nickname、username、签名和个人友情链接，修改入口统一位于右侧个人资料面板。受保护评论提交，以及本人头像、nickname、个性签名和个人友情链接更新显式发送 Bearer JWT。个人友情链接仅允许 HTTP(S) 绝对地址且每人最多 8 条；公开 Blog 请求和密码文章 token 必须与登录 JWT 分离。
- 训练采集、ODS/DWD/DWM/DWS 和数仓逻辑继续留在 `platform-training-data`；Blog API 负责身份、授权、用户/OJ handle 管理及向外暴露 HTTP。
- Java 变更后在仓库根目录运行 `mvn clean verify` 和 `./scripts/check-test-policy.sh`。Vue 变更在 `blog-view` 运行 `npm ci`、`npm test` 和 `npm run build`。
- 职责、路径、构建或权限改变时同步更新本文件、`README.md`、`upstream/nblog/blog-api/README.md`、`upstream/nblog/blog-view/README.md`、`../frontend/README.md`、`../docs/architecture.md` 和 `../docs/agent/context-map.md`。
