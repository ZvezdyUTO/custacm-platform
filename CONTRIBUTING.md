# Contributing

`main` 是稳定主线。任何人都可以通过 fork 或仓库分支提交 Pull Request；PR 目标分支为 `main`，标题和描述使用中文。

## 分支与提交

推荐使用 `feature/<name>`、`fix/<name>`、`docs/<name>` 或 `chore/<name>`。不要提交真实 `.env`、secret、日志、构建产物或 IDE 配置，也不要 force push `main`。

## PR 要求

- 说明做了什么、如何验证，以及是否影响 API、权限、数据迁移、部署或配置。
- 只有公开合同、安全规则、模块职责、运行拓扑、部署步骤或用户可见行为变化时，才更新对应文档；不要为内部实现变化批量触碰无关文档。
- 仅当变更影响用户、部署、数据或兼容性时更新 [CHANGELOG.md](CHANGELOG.md)。
- 新增或实质修改业务逻辑、安全解析、Controller 或 adapter 时增加聚焦测试；不得删除、跳过或弱化已有测试。
- 非项目负责人发起的 PR 必须经项目负责人明确确认后才能合并；项目负责人本人明确要求合并时无需额外确认。

## 验证

按改动范围选择检查：

```bash
mvn clean test
cd frontend && pnpm lint && pnpm test && pnpm typecheck && pnpm build
cd platform-blog/upstream/nblog/blog-view && npm install && npm test && npm run build
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
./scripts/sync-design-tokens.sh --check
git diff --check
```

文档-only 变更不要求 Maven 或前端构建。
