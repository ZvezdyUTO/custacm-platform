# 文档索引

文档按需读取。所有任务先遵守根 [AGENTS.md](../AGENTS.md)，修改模块时再读最近的模块 `AGENTS.md`；不要把本目录全部预加载。

| 任务 | 权威文档 |
| --- | --- |
| 服务、模块和数据边界 | [architecture.md](architecture.md) |
| HTTP 方法、路径和关键合同 | [api.md](api.md) |
| 角色、JWT、路由与资源所有权 | [authorization.md](authorization.md) |
| 后端日志与敏感信息 | [logging.md](logging.md) |
| 共享前端视觉规则 | [frontend-design-system.md](frontend-design-system.md) |
| 本地开发、部署和升级 | [../deploy/README.md](../deploy/README.md) |
| 模块目录、入口和验证 | 对应模块 `README.md` |

## 事实归属

- `AGENTS.md` 只保存不可违反的约束。
- README 只说明模块职责、顶层目录、依赖边界、关键入口和验证方法。
- API、权限、视觉、部署分别只在上表对应文档展开；不要在多个模块文档复制完整合同。
- 迁移历史以 Flyway 文件和 Git 为准，当前架构文档不维护第二份逐版本清单。
- 只有公开合同、安全规则、模块职责、运行拓扑、部署步骤或用户可见行为变化时才更新文档；内部重构与测试变化通常不需要文档改动。

无法从当前仓库证明的外部状态应写成 TODO，不要声称已发布、已开启或已配置。
