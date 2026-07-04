# custacm-platform

集训队综合平台后端骨架。

## Current Scope

- `platform-common`：公共模块；当前保留基础公共能力，不承载业务身份模型。
- `platform-auth`：Keycloak-backed 鉴权模块，包含 JWT 解析、角色归一和当前学生身份接口。
- `platform-training-data`：训练数据模块第一版，包含 Codeforces 垂直 OJ 数仓模块、submission ODS 存储、DWD 明细、DWM 中间事实和 DWS 汇总 SQL 任务。
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

登录、注册、密码重置和 token 签发由 Keycloak 负责；后端不实现本地密码登录。

学生身份使用单个不可变字符串：

```text
student_identity = 固定位数学号 + 姓名
例：112487张三
```

Keycloak 用户属性和 JWT claim 均使用 `student_identity`。
平台业务代码里的用户 ID 就是 `studentIdentity`，不再另建用户 ID。

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
ODS 写入接口需要 Keycloak JWT 中带平台 `admin` 角色。`training-data-web` 默认连接 MySQL，并通过 Flyway 应用 OJ 模块里的 ODS/DWD/DWM/DWS 建表脚本；DWD/DWM/DWS 转换目前以幂等 SQL 任务文件形式提供，Java SQL-task 执行器和 HTTP refresh 入口尚未实现。
