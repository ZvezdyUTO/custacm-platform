# custacm-platform

集训队综合平台后端骨架。

## Current Scope

- `platform-common`：公共模块；当前保留基础公共能力，不承载业务身份模型。
- `platform-auth`：平台自有用户管理和鉴权模块，包含本地账号、BCrypt 密码哈希、RSA JWT 签发/解析和当前用户接口。
- `platform-training-data`：训练数据模块第一版，包含 Codeforces 垂直 OJ 数仓模块、submission ODS 存储、DWD 明细、DWM 中间事实、DWS 汇总 SQL 任务和 CF handle 绑定维护。
- `platform-blog`：Blog / 内容模块占位。
- `platform-editor`：编辑器接入模块占位。
- `platform-article-storage`：文章存储模块占位。

## Architecture

- Agent instructions: [AGENTS.md](AGENTS.md)
- Contributing guide: [CONTRIBUTING.md](CONTRIBUTING.md)
- Todo list: [TODO.md](TODO.md)
- Changelog: [CHANGELOG.md](CHANGELOG.md)
- Documentation index: [docs/README.md](docs/README.md)
- Architecture notes: [docs/architecture.md](docs/architecture.md)
- API docs: [docs/api.md](docs/api.md)

## Verify

```bash
mvn clean verify
```

`mvn clean verify` runs unit tests and the JaCoCo line coverage gate. Current minimum: `70%` for code-bearing modules.

## Run Auth

```bash
java -jar platform-auth/auth-web/target/auth-web-0.1.0-SNAPSHOT.jar
```

Default port: `8081`.

```bash
curl http://localhost:8081/health
curl http://localhost:8081/module-info
```

`auth-web` 负责登录、密码哈希、管理员用户管理和 token 签发；当前没有公开注册入口。

用户业务身份使用单个不可变字符串：

```text
studentIdentity = 固定位数学号 + 姓名
例：230511213黄炳睿
```

登录名就是 `studentIdentity`。账号表使用单个 `role` 字段，取值为 `admin`、`player` 或 `disable`；`disable` 账号不能登录。JWT 使用 `sub` 承载 `studentIdentity`，使用 `role` 承载 `admin` 或 `player`。未登录访问者是 `guest`，不需要 JWT；公共游客接口也不会解析 JWT。HTTP 接口按 `/admin/**`、`/player/**` 和游客公开路径分层，规则见 [docs/authorization.md](docs/authorization.md)。

## Run Training Data

```bash
java -jar platform-training-data/training-data-web/target/training-data-web-0.1.0-SNAPSHOT.jar
```

Default port: `8082`.

```bash
curl http://localhost:8082/health
curl http://localhost:8082/module-info
```

训练数据第一版实现 Codeforces 独立 OJ 数仓链路：ODS 原始提交、DWD 标准提交明细、DWM handle-题目首次通过和 DWS handle-日期-rating 汇总。
ODS 写入接口位于 `/api/training-data/admin/ods/codeforces/submissions:batch-upsert`，需要平台 JWT 中带 `admin` 角色。Codeforces handle 绑定维护支持管理员创建 `studentIdentity + handle`、迁移绑定的 `studentIdentity`，以及游客按 `studentIdentity` 查询 handle。`training-data-web` 默认连接 MySQL，并通过 Flyway 应用 OJ 模块里的建表脚本；DWD/DWM/DWS 转换目前以幂等 SQL 任务文件形式提供，Java SQL-task 执行器和 HTTP refresh 入口尚未实现。
