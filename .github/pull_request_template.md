## 变更内容

- 

## 更新日志

- [ ] 已按 `docs/agent/changelog.md` 更新 `CHANGELOG.md`，记录本次 MR 成果

## 审核确认

- [ ] 项目负责人本人发起的 PR，或非负责人 PR 已获得项目负责人明确确认

## 验证方式

- [ ] `./scripts/check-doc-sync.sh origin/main WORKTREE`
- [ ] `mvn clean test`
- [ ] `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`
- [ ] 文档-only 变更，无需运行上述命令

## 影响范围

- 

## 备注

- 
