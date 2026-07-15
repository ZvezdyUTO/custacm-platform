# GitHub Automation Rules

- 保持 required workflow job 名为 `verify`；CI 实际命令以 `.github/workflows/` 为准。
- PR 模板与 [../CONTRIBUTING.md](../CONTRIBUTING.md) 保持一致：中文 PR，说明影响与实际验证。
- 非项目负责人 PR 需要负责人明确确认；负责人本人明确要求合并时无需额外确认。
- workflow 必须确定性执行，不读取或输出仓库 secret，除非功能明确需要且已获批准。
