# custacm-platform API 文档

本文档记录当前已经实现并可以对外调用的后端接口。

当前阶段只有 `platform-auth/auth-web` 是可运行后端模块，默认端口为 `8081`。登录、注册、密码重置、会话管理和 token 签发均由 Keycloak 负责，平台后端只校验 Keycloak JWT 并返回平台业务身份。

## 基础信息

默认本地地址：

```text
http://localhost:8081
```

部署后端口由 `BACKEND_PORT` 暴露，服务内部端口由 `AUTH_WEB_PORT` 控制。

当前后端没有自定义统一响应包裹，接口直接返回业务 JSON。

## 鉴权规则

除健康检查和模块信息接口外，`/api/**` 接口都需要 Keycloak Bearer Token。

请求头格式：

```http
Authorization: Bearer <access_token>
```

后端从 JWT 中读取：

- `student_identity`：平台业务用户 ID，对外字段名为 `studentIdentity`。
- `realm_access.roles`：Keycloak realm 角色。
- `resource_access.*.roles`：Keycloak client/resource 角色。

平台当前只识别两个业务角色：

```text
admin
student
```

如果同一个 token 同时包含 `admin` 和 `student`，后端按 `admin` 处理。

`student_identity` 是一个不可变字符串，格式为：

```text
固定位数学号 + 姓名
例：112487张三
```

平台业务代码只使用 `studentIdentity` 作为用户 ID，不再另建本地用户 ID。

## 接口列表

| 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/health` | 否 | 后端健康检查 |
| `GET` | `/module-info` | 否 | 当前模块信息 |
| `GET` | `/api/auth/me` | 是 | 查询当前登录用户的平台身份 |

## GET /health

健康检查接口，用于本地部署、Compose 更新脚本和服务器探活。

### 请求

```http
GET /health
```

### 响应

```json
{
  "status": "UP",
  "service": "auth-web"
}
```

### 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | `string` | 服务状态，当前正常时为 `UP` |
| `service` | `string` | 当前 Spring Boot 服务名 |

## GET /module-info

模块信息接口，用于确认当前后端容器实际运行的模块和基础能力。

### 请求

```http
GET /module-info
```

### 响应

```json
{
  "module": "platform-auth",
  "service": "auth-web",
  "features": [
    "keycloak-jwt",
    "current-user"
  ]
}
```

### 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `module` | `string` | 当前业务模块 |
| `service` | `string` | 当前 Spring Boot 服务名 |
| `features` | `string[]` | 当前模块已经提供的能力 |

## GET /api/auth/me

查询当前请求 token 对应的平台用户身份。

该接口不签发 token，也不查询本地用户表。它只把 Keycloak JWT 中的平台身份信息转换为业务响应。

### 请求

```http
GET /api/auth/me
Authorization: Bearer <access_token>
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "role": "student"
}
```

### 字段说明

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `studentIdentity` | `string` | 平台业务用户 ID，来自 JWT claim `student_identity` |
| `role` | `string` | 平台业务角色，当前只能是 `admin` 或 `student` |

### curl 示例

```bash
curl -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  http://localhost:8081/api/auth/me
```

## 错误行为

当前错误响应还没有平台统一错误格式，认证相关错误主要由 Spring Security 处理。

| 场景 | 当前行为 |
| --- | --- |
| 未携带 `Authorization` | 返回认证失败响应，通常为 `401` |
| Bearer token 无效或过期 | 返回认证失败响应，通常为 `401` |
| token 缺少 `student_identity` | 当前代码会拒绝解析该 token，后续统一错误格式时再固化响应体 |
| token 缺少 `admin` / `student` 平台角色 | 当前代码会拒绝解析该 token，后续统一错误格式时再固化响应体 |

## 非平台后端接口

以下能力不属于 `custacm-platform` 后端 API：

- 登录
- 注册
- 退出登录
- 密码重置
- token 签发和刷新
- Keycloak 用户管理

这些能力由 Keycloak 和后续前端 OIDC 流程处理。平台后端只接收并校验 Keycloak 已签发的 JWT。
