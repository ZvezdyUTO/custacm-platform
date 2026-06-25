# deploy

本目录提供服务器 / 开发环境的一键部署编排。当前会启动：

- `keycloak-db`：Keycloak 使用的 PostgreSQL
- `keycloak`：官方 Keycloak 镜像，导入 `custacm` realm
- `custacm-backend`：当前 Spring Boot 后端

## Quick Start

在仓库根目录执行：

```bash
cp deploy/.env.example deploy/.env
```

编辑 `deploy/.env`，至少修改：

```env
KEYCLOAK_ADMIN_PASSWORD=change-me-admin-password
KEYCLOAK_DB_PASSWORD=change-me-keycloak-db-password
```

启动：

```bash
./scripts/deploy.sh
```

日常只更新某个业务模块时，见 [UPDATE.md](UPDATE.md)。
服务器部署和远端 AI 查日志方式见 [server-deployment.md](../docs/server-deployment.md)。

默认地址：

- Keycloak: http://localhost:8180
- Backend health: http://localhost:8081/health
- Realm: `custacm`
- Public client: `custacm-web`
- Realm roles: `admin`, `student`
- Student identity claim: `student_identity`
- Backend logs: `../logs/combined.log`, `../logs/error.log`

业务接口只暴露一个 `role` 字符串。正常用户只应分配一个平台角色；如果同时存在 `admin` 和 `student`，后端按 `admin` 处理。

## Student Identity

学生身份是一个不可变字符串：

```text
student_identity = 固定位数学号 + 姓名
例：112487张三
```

在 Keycloak 用户属性里设置：

```text
student_identity=112487张三
```

`custacm-web` client 已配置 protocol mapper，会把这个用户属性写入 access token、ID token 和 userinfo。

## JWT Settings

后端通过以下环境变量校验 Keycloak JWT：

```env
KEYCLOAK_ISSUER_URI=http://localhost:8180/realms/custacm
KEYCLOAK_JWK_SET_URI=http://keycloak:8080/realms/custacm/protocol/openid-connect/certs
```

`issuer-uri` 使用浏览器和 token 中可见的公网地址；`jwk-set-uri` 使用 Docker 内网地址，供后端容器拉取 Keycloak 公钥。

## Notes

- 不要提交 `deploy/.env`。
- 不要提交 `logs/` 下的运行时日志。
- 当前 Keycloak 使用 `start-dev --import-realm`，适合第一阶段开箱即用。
- 正式生产环境后续应切换到 `start --optimized`、HTTPS 反向代理、独立数据库备份和 Admin Console 访问隔离。
