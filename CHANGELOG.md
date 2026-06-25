# 更新日志

给人类看的项目更新记录。每次 MR 合并前，由 agent 按 [docs/agent/changelog.md](docs/agent/changelog.md) 的格式在最上方追加本次交付成果。

## 未发布

### 2026-06-25 - 项目待办和更新日志

- 成果：新增根目录待办列表、面向人类的更新日志，以及 agent 写更新日志的固定格式。
- 影响：后续 MR 需要按“成果 / 影响 / 验证”记录交付结果，未来 agent 也能从仓库文档中读到规则。
- 验证：已运行 `./scripts/check-doc-sync.sh origin/main WORKTREE`、`./scripts/check-test-policy.sh`、`mvn clean verify` 和 `git diff --check`。
