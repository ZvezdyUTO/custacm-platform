# Contributing

本项目按公开 Pull Request 协作流程维护：任何人都可以 fork 仓库后提交 Pull Request，`main` 是稳定主线。非项目负责人发起的变更必须由项目负责人确认后合并；项目负责人本人发起的 PR 在负责人明确要求合并时无需额外审核确认。

## Branches

- `main`：稳定主线，只接受通过审核的 PR 合并。
- `feature/<short-name>`：新功能分支。
- `fix/<short-name>`：缺陷修复分支。
- `docs/<short-name>`：文档调整分支。
- `chore/<short-name>`：构建、脚本、依赖和仓库维护分支。

有仓库写权限时，可以直接从本仓库创建分支：

```bash
git checkout main
git pull
git checkout -b feature/training-records
```

没有仓库写权限时，不需要额外申请 collaborator 权限，直接 fork 仓库，在自己的 fork 里创建分支并提交 Pull Request。

## Pull Requests

- PR 目标分支固定为 `main`。
- 任何人都可以从 fork 提交 PR。
- PR 标题和描述使用中文。
- PR 描述需要说明做了什么、怎么验证、是否影响部署或配置。
- 每个 PR 需要同步更新 [CHANGELOG.md](CHANGELOG.md)，用面向人类的语言记录本次 MR 成果。
- 改代码、脚本、CI、部署配置或模块边界时，需要同步更新 `docs/doc-sync-map.tsv` 指向的文档。
- 不要把 `deploy/.env`、日志、构建产物、IDE 配置提交到仓库。
- 不要在 PR 中引入本地密码登录、自签 JWT、demo token 或拆分 `student_identity` 的实现。

## Review

- 所有 PR 都可以自由提交。
- 非项目负责人发起的 PR 必须经过项目负责人明确确认后才能合并。
- 项目负责人本人发起的 PR，在负责人明确要求合并时可以直接合并，不需要额外审核确认。
- 推荐使用 squash merge，让 `main` 历史保持清晰。
- 不要 force push `main`，不要直接删除 `main`。

## Verification

Java 代码变更提交 PR 前执行：

```bash
./scripts/check-doc-sync.sh origin/main WORKTREE
mvn clean test
```

`check-doc-sync.sh` 会检查代码/配置变更是否同步更新了对应文档。`mvn clean test` 会编译 Maven reactor 并运行仓库中已有的全部单元测试。历史代码不强制补齐单测或达到统一覆盖率；新增或实质修改的业务逻辑应同步增加有针对性的单测。已有测试不得为了让 MR 通过而被删除、跳过或弱化。JaCoCo 可作为本地质量参考，但覆盖率不是 MR 合并门禁。

部署配置变更提交 PR 前执行对应配置检查，例如：

```bash
docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config
```

文档-only 变更不要求 Maven 校验。
