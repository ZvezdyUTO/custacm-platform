## 变更内容

- 

## 更新日志

- [ ] 已按 `docs/agent/changelog.md` 更新 `CHANGELOG.md`，记录本次 MR 成果

## 验证方式

- [ ] `./scripts/check-doc-sync.sh origin/main WORKTREE`
- [ ] `mvn clean verify`
- [ ] `./scripts/check-test-policy.sh`
- [ ] `docker compose --env-file deploy/.env.example -f deploy/docker-compose.yml config`
- [ ] 文档-only 变更，无需运行上述命令

## 影响范围

- 

## 备注

- 
