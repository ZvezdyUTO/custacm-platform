# Scripts Agent Notes

- 脚本必须兼容其声明的 shell，保持非交互、可失败退出，并且不得写入或输出真实 secret。
- 启动入口只有 `dev.sh` 和 `deploy.sh`；其行为以 [deploy/README.md](../deploy/README.md) 为准。不得新增第三种模式、模块级部署或 `deploy/` 下的包装入口。
- 启动和部署脚本不得隐式执行 Git 拉取、切换分支、提交或推送，也不得删除数据库或 Redis 命名卷。
- `dev.sh` 进入开发模式时停止生产 Nginx，退出时只停止两份 Vite 并保留 Docker 后端；`deploy.sh` 构建、启动并验证完整四服务栈。
- `check-test-policy.sh` 和 `sync-design-tokens.sh` 是校验/同步工具，不是启动入口；token 同步脚本不得访问网络。
- 脚本行为变化时更新自身 usage，并只在运行或升级方式确实变化时同步 `deploy/README.md`。
