# custacm-platform

集训队综合平台后端骨架。

## Current Scope

- `platform-common`：公共模块；当前保留基础公共能力，不承载业务身份模型。
- `platform-auth`：Keycloak-backed 鉴权模块，包含 JWT 解析、角色归一和当前学生身份接口。
- `platform-training-data`：训练数据模块占位。
- `platform-blog`：Blog / 内容模块占位。
- `platform-editor`：编辑器接入模块占位。
- `platform-article-storage`：文章存储模块占位。

## Architecture

- Agent instructions: [AGENTS.md](AGENTS.md)
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
