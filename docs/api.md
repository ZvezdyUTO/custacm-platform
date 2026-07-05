# custacm-platform API 文档

本文档记录当前已经实现并可以对外调用的后端接口。

当前阶段有两个可运行后端模块：

- `platform-auth/auth-web`：默认端口 `8081`，负责本地账号、登录、RSA JWT 签发和用户管理。
- `platform-training-data/training-data-web`：默认端口 `8082`，负责训练数据 ODS 批量写入入口。

当前后端没有自定义统一响应包裹，接口直接返回业务 JSON。错误响应使用稳定 `code` 和 `message`。

## 基础信息

默认本地地址：

```text
auth-web:          http://localhost:8081
training-data-web: http://localhost:8082
```

部署后端口由对应模块配置控制。当前 Compose 部署覆盖 `auth-web` 和它的 MySQL `auth-db`；`training-data-web` 的容器化部署还未接入 `deploy/`。

## 鉴权规则

HTTP 接口按 URL 分为三层，完整规则见 [authorization.md](authorization.md)。

```text
/admin/**   -> 必须携带平台 Bearer Token，且 role=admin
/player/**  -> 必须携带平台 Bearer Token，role=player 或 admin
其它路径     -> 游客接口，不需要 JWT，也不解析 JWT
```

各模块应把受保护接口放在自己的模块 API 前缀下：

```text
/api/<module>/admin/**
/api/<module>/player/**
```

游客接口即使收到 `Authorization` 请求头，也按未登录游客请求处理。凡是需要当前登录用户身份的接口，都必须放在 `/player/**` 或 `/admin/**` 下。

请求头格式：

```http
Authorization: Bearer <access_token>
```

JWT 由 `auth-web` 使用 RSA 私钥签发，其它后端使用同一对密钥的公钥验证。JWT 业务字段为：

- `sub`：平台业务用户 ID，对外字段名为 `studentIdentity`。
- `role`：单个 JWT 业务角色，当前只会是 `admin` 或 `player`。

平台角色：

```text
admin
player
```

账号管理接口中的账号 `role` 可为 `admin`、`player` 或 `disable`。`disable` 账号不能登录，因此不会拿到 JWT。未登录访问者没有 `role` 值；公开接口不需要 JWT，也不解析 JWT。

`admin` 包含 `player` 能力，因此管理员可以访问 `/player/**` 接口。

`studentIdentity` 是一个不可变字符串，通常格式为：

```text
固定位数学号 + 姓名
例：230511213黄炳睿
```

平台业务代码只使用 `studentIdentity` 作为用户 ID，不再另建用户 ID。

## 接口列表

| 服务 | 方法 | 路径 | 鉴权 | 说明 |
| --- | --- | --- | --- | --- |
| `auth-web` | `GET` | `/health` | 否 | 鉴权模块健康检查 |
| `auth-web` | `GET` | `/module-info` | 否 | 鉴权模块信息 |
| `auth-web` | `POST` | `/api/auth/login` | 否 | 使用 `studentIdentity` + 密码登录 |
| `auth-web` | `GET` | `/api/auth/player/me` | `player` 或 `admin` | 查询当前登录用户的平台身份 |
| `auth-web` | `PATCH` | `/api/auth/player/me/password` | `player` 或 `admin` | 当前用户修改自己的密码 |
| `auth-web` | `POST` | `/api/auth/admin/users` | `admin` | 管理员创建单个用户 |
| `auth-web` | `POST` | `/api/auth/admin/users:batch-create` | `admin` | 管理员批量创建用户 |
| `auth-web` | `GET` | `/api/auth/admin/users` | `admin` | 管理员查看用户列表 |
| `auth-web` | `PATCH` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员更新用户角色或重置密码 |
| `auth-web` | `DELETE` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员真实删除用户 |
| `training-data-web` | `GET` | `/health` | 否 | 训练数据模块健康检查 |
| `training-data-web` | `GET` | `/module-info` | 否 | 训练数据模块信息 |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/ods/codeforces/submissions:batch-upsert` | `admin` | 批量写入 Codeforces submission ODS |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/codeforces/handles` | `admin` | 管理员创建 `studentIdentity + Codeforces handle` 绑定 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/handles?studentIdentity=...` | 否 | 游客按 `studentIdentity` 查询 Codeforces handle |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/accepted-summary` | 否 | 游客按 `studentIdentity` 查询 Codeforces 区间 rating AC 汇总 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/submissions/by-student` | 否 | 游客按 `studentIdentity` 查询 Codeforces 提交明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/submissions/by-problem` | 否 | 游客按 `problemKey` 查询 Codeforces 提交明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/first-accepted/by-student` | 否 | 游客按 `studentIdentity` 查询 Codeforces 首 AC 题目明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/first-accepted/by-problem` | 否 | 游客按 `problemKey` 查询 Codeforces 首 AC handle 列表 |
| `training-data-codeforces` | `PATCH` | `/api/training-data/admin/codeforces/handles:change-identity` | `admin` | 管理员迁移 CF handle 绑定的 `studentIdentity`，不修改 handle |

## GET /health

健康检查接口，用于本地部署、Compose 更新脚本和服务器探活。

### 请求

```http
GET /health
```

### auth-web 响应

```json
{
  "status": "UP",
  "service": "auth-web"
}
```

### training-data-web 响应

```json
{
  "status": "UP",
  "service": "training-data-web"
}
```

## GET /module-info

模块信息接口，用于确认当前后端容器实际运行的模块和基础能力。

### auth-web 响应

```json
{
  "module": "platform-auth",
  "service": "auth-web",
  "features": [
    "local-login",
    "rsa-jwt",
    "user-management",
    "current-user"
  ]
}
```

### training-data-web 响应

```json
{
  "module": "platform-training-data",
  "service": "training-data-web",
  "features": [
    "oj-warehouse-modules",
    "codeforces-ods-submission",
    "codeforces-handle-account"
  ]
}
```

`training-data-web` 启动时也会应用内部 Codeforces ODS/DWD/DWM/DWS 表迁移和 `codeforces_handle_account` 表迁移。DWD/DWM/DWS 转换目前以 `training-data-codeforces` 内的 SQL 任务资源表示，尚未实现 Java SQL-task 执行器或公开 HTTP refresh 入口。

## POST /api/auth/login

使用平台本地账号登录。当前没有公开注册入口；账号由管理员创建或批量导入。

### 请求

```http
POST /api/auth/login
Content-Type: application/json

{
  "studentIdentity": "230511213黄炳睿",
  "password": "playerPass123"
}
```

### 响应

```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt>",
  "expiresInSeconds": 7200,
  "user": {
    "studentIdentity": "230511213黄炳睿",
    "role": "player"
  }
}
```

登录失败会返回 `401` 和 `AUTH_INVALID_CREDENTIALS`。同一 `studentIdentity` 输错后 5 秒内再次登录会返回 `429` 和 `AUTH_LOGIN_RATE_LIMITED`。

## GET /api/auth/player/me

查询当前请求 token 对应的平台用户身份。

### 请求

```http
GET /api/auth/player/me
Authorization: Bearer <access_token>
```

### 响应

```json
{
  "studentIdentity": "230511213黄炳睿",
  "role": "player"
}
```

## PATCH /api/auth/player/me/password

当前登录用户修改自己的密码。

### 请求

```http
PATCH /api/auth/player/me/password
Authorization: Bearer <access_token>
Content-Type: application/json

{
  "oldPassword": "playerPass123",
  "newPassword": "newPlayerPass123",
  "confirmNewPassword": "newPlayerPass123"
}
```

`newPassword` 和 `confirmNewPassword` 必须一致，否则返回 `400` 和 `AUTH_INVALID_REQUEST`。

### 响应

```http
204 No Content
```

## 管理员用户管理接口

所有 `/api/auth/admin/**` 接口都要求当前 token 的 `role` 为 `admin`。

管理员可以创建、批量创建、禁用、启用、调整其它账号角色、重置密码和真实删除用户。禁用通过把账号 `role` 设置为 `disable` 完成；重新启用时需要把 `role` 设置回 `admin` 或 `player`。角色更新和密码重置统一通过 `PATCH /api/auth/admin/users/{studentIdentity}` 完成。管理员不能把自己降级为 `player` 或 `disable`，也不能删除自己。

创建用户时 `password` 为空字符串、空白字符串或未传时，后端会生成 16 位随机密码。更新用户时 `newPassword` 为空字符串或空白字符串表示生成随机新密码；`newPassword` 未传或为 `null` 表示不修改密码。创建和修改密码成功时，响应会在 `plainPassword` 返回本次生效的明文密码；该明文只在本次响应中返回，数据库仍只保存哈希。

管理员写操作返回操作结果对象：

```json
{
  "success": true,
  "studentIdentity": "230511213黄炳睿",
  "user": {
    "studentIdentity": "230511213黄炳睿",
    "role": "player",
    "createdAt": "2026-07-04T12:00:00Z",
    "updatedAt": "2026-07-04T12:00:00Z"
  },
  "plainPassword": "playerPass123",
  "errorCode": null,
  "message": "user created"
}
```

`plainPassword` 只在创建或重置密码时有值；只改角色、删除用户、失败结果中为 `null`。

### POST /api/auth/admin/users

```http
POST /api/auth/admin/users
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "230511213黄炳睿",
  "password": "playerPass123",
  "role": "player"
}
```

响应 `201`：

```json
{
  "success": true,
  "studentIdentity": "230511213黄炳睿",
  "user": {
    "studentIdentity": "230511213黄炳睿",
    "role": "player",
    "createdAt": "2026-07-04T12:00:00Z",
    "updatedAt": "2026-07-04T12:00:00Z"
  },
  "plainPassword": "playerPass123",
  "errorCode": null,
  "message": "user created"
}
```

### POST /api/auth/admin/users:batch-create

```http
POST /api/auth/admin/users:batch-create
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "users": [
    {
      "studentIdentity": "230511213黄炳睿",
      "password": "playerPass123",
      "role": "player"
    }
  ]
}
```

响应 `201`，返回每条创建命令的结果数组。批量创建允许部分成功；失败项不会影响后续项继续处理。

```json
[
  {
    "success": true,
    "studentIdentity": "230511213黄炳睿",
    "user": {
      "studentIdentity": "230511213黄炳睿",
      "role": "player",
      "createdAt": "2026-07-04T12:00:00Z",
      "updatedAt": "2026-07-04T12:00:00Z"
    },
    "plainPassword": "generatedOrProvidedPassword",
    "errorCode": null,
    "message": "user created"
  },
  {
    "success": false,
    "studentIdentity": "existing-user",
    "user": null,
    "plainPassword": null,
    "errorCode": "AUTH_USER_EXISTS",
    "message": "user already exists"
  }
]
```

### GET /api/auth/admin/users

响应 `200`，返回用户数组。

### PATCH /api/auth/admin/users/{studentIdentity}

请求体支持部分更新，`role` 和 `newPassword` 至少传一个：

```json
{
  "role": "disable",
  "newPassword": "newPlayerPass123"
}
```

`role` 当前只允许 `admin`、`player` 或 `disable`。`disable` 表示账号禁用，不能登录。
更新接口里的 `role` 为空或 `guest` 时表示不修改角色。管理员不能把自己降级为 `player` 或 `disable`。

响应 `200`，返回操作结果对象。若本次修改了密码，`plainPassword` 为本次生效的明文密码；若只改角色，`plainPassword` 为 `null`。

### DELETE /api/auth/admin/users/{studentIdentity}

真实删除用户。该操作不会清理其它业务模块里已经引用此 `studentIdentity` 的历史数据。

响应 `200`，返回删除操作结果对象，其中 `user` 是删除前的用户信息，`plainPassword` 为 `null`。

## POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert

批量写入 Codeforces submission ODS。请求体直接是 Codeforces 原始 submission 数组，不包 `{ data: [...] }`。

后端会生成 `batchId` / `fetchedAt`，按每条 submission 计算 `rawPayload` 和 `payloadHash`，然后幂等写入 `ods_codeforces__submission`。

下游 DWD/DWM/DWS 转换目前不是 HTTP API。它们以模块资源中的幂等 SQL 任务文件存在，后续 Java 调度只需要按固定顺序触发这些 SQL。

### 请求

```http
POST /api/training-data/admin/ods/codeforces/submissions:batch-upsert
Authorization: Bearer <admin_access_token>
Content-Type: application/json

[
  {
    "id": 379398914,
    "contestId": 2237,
    "creationTimeSeconds": 1781798091,
    "relativeTimeSeconds": 4791,
    "problem": {
      "contestId": 2237,
      "index": "G",
      "name": "Send GCDs",
      "type": "PROGRAMMING",
      "points": 2750.0,
      "rating": 2900,
      "tags": ["math"]
    },
    "author": {
      "members": [
        { "handle": "tourist" }
      ],
      "participantType": "CONTESTANT"
    },
    "programmingLanguage": "C++23",
    "verdict": "OK",
    "testset": "TESTS",
    "passedTestCount": 10,
    "timeConsumedMillis": 375,
    "memoryConsumedBytes": 4505600
  }
]
```

### 响应

```json
{
  "batchId": "external-codeforces-1782554400000-...",
  "tableName": "ods_codeforces__submission",
  "writtenRows": 1,
  "fetchedAt": "2026-06-27T10:00:00Z"
}
```

## POST /api/training-data/admin/codeforces/handles

管理员创建一个平台 `studentIdentity` 到 Codeforces handle 的绑定。一个 `studentIdentity` 只能绑定一个 Codeforces handle；一个 Codeforces handle 也只能绑定给一个 `studentIdentity`。

该接口只维护 `training-data-codeforces` 内的 `codeforces_handle_account`，不会创建或修改 auth 登录账号。

### 请求

```http
POST /api/training-data/admin/codeforces/handles
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "112487张三",
  "handle": "tourist"
}
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handle": "tourist"
}
```

## GET /api/training-data/codeforces/handles

游客按平台 `studentIdentity` 查询已绑定的 Codeforces handle。该接口是 guest endpoint，不需要 JWT，也不会解析请求里的 `Authorization` header。

### 请求

```http
GET /api/training-data/codeforces/handles?studentIdentity=112487张三
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handle": "tourist"
}
```

## GET /api/training-data/codeforces/accepted-summary

游客按平台 `studentIdentity` 查询 Codeforces DWS 区间 rating AC 汇总。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数：

- `acceptedFromDateUtcPlus8` / `acceptedToDateUtcPlus8`：ISO 日期，例如 `2026-07-01`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。

```http
GET /api/training-data/codeforces/accepted-summary?studentIdentity=112487张三&acceptedFromDateUtcPlus8=2026-07-01&acceptedToDateUtcPlus8=2026-07-31&minProblemRating=800&maxProblemRating=1600
```

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "totalAcceptedProblemCount": 3,
  "ratingCounts": [
    { "problemRating": "800", "acceptedProblemCount": 2 },
    { "problemRating": "1600", "acceptedProblemCount": 1 }
  ]
}
```

## GET /api/training-data/codeforces/submissions/by-student

游客按平台 `studentIdentity` 查询 Codeforces DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数：

- `submittedFromUtcPlus8` / `submittedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。

```http
GET /api/training-data/codeforces/submissions/by-student?studentIdentity=112487张三&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59
```

响应：

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "submissions": [
    {
      "codeforcesSubmissionId": 379398914,
      "studentIdentity": "112487张三",
      "authorHandle": "tourist",
      "submittedAtUtcPlus8": "2026-07-01T12:00:00",
      "submittedDateUtcPlus8": "2026-07-01",
      "problemKey": "2237:G",
      "problemRating": 2900,
      "verdict": "OK",
      "accepted": true
    }
  ]
}
```

提交明细响应还会包含 contest、problem、language、testset、耗时和内存等字段；上例只展示主要字段。

## GET /api/training-data/codeforces/submissions/by-problem

游客按 Codeforces `problemKey` 查询 DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。

```http
GET /api/training-data/codeforces/submissions/by-problem?problemKey=2237:G&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59
```

响应结构：

```json
{
  "problemKey": "2237:G",
  "submissions": [
    {
      "codeforcesSubmissionId": 379398914,
      "studentIdentity": "112487张三",
      "authorHandle": "tourist",
      "problemKey": "2237:G",
      "accepted": true
    }
  ]
}
```

如果结果里存在未绑定 `studentIdentity` 的 Codeforces handle，返回 `404` 和 `CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND`。

## GET /api/training-data/codeforces/first-accepted/by-student

游客按平台 `studentIdentity` 查询 Codeforces DWM 首 AC 题目明细。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数：

- `firstAcceptedFromUtcPlus8` / `firstAcceptedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。

```http
GET /api/training-data/codeforces/first-accepted/by-student?studentIdentity=112487张三&firstAcceptedFromUtcPlus8=2026-07-01T00:00:00&firstAcceptedToUtcPlus8=2026-07-31T23:59:59
```

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "totalAcceptedProblemCount": 1,
  "problems": [
    {
      "problemKey": "2237:G",
      "problemRating": 2900,
      "firstAcceptedSubmissionId": 379398914,
      "firstAcceptedAtUtcPlus8": "2026-07-01T12:00:00",
      "firstAcceptedDateUtcPlus8": "2026-07-01",
      "firstAcceptedLanguage": "C++23"
    }
  ]
}
```

## GET /api/training-data/codeforces/first-accepted/by-problem

游客按 Codeforces `problemKey` 查询 DWM 首次通过该题的 handle 列表。接口不需要 JWT，也不会解析 `Authorization` header。

```http
GET /api/training-data/codeforces/first-accepted/by-problem?problemKey=2237:G&firstAcceptedFromUtcPlus8=2026-07-01T00:00:00&firstAcceptedToUtcPlus8=2026-07-31T23:59:59
```

```json
{
  "problemKey": "2237:G",
  "acceptedHandleCount": 1,
  "acceptedHandles": [
    {
      "studentIdentity": "112487张三",
      "authorHandle": "tourist",
      "firstAcceptedAtUtcPlus8": "2026-07-01T12:00:00"
    }
  ]
}
```

如果结果里存在未绑定 `studentIdentity` 的 Codeforces handle，返回 `404` 和 `CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND`。

## PATCH /api/training-data/admin/codeforces/handles:change-identity

管理员迁移 Codeforces handle 绑定的 `studentIdentity`。该操作只更新 `codeforces_handle_account.student_identity`，不会修改 handle，也不会修改 auth 登录账号。

如果 `newStudentIdentity` 已经绑定了 Codeforces handle，返回 `409`。

### 请求

```http
PATCH /api/training-data/admin/codeforces/handles:change-identity
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "oldStudentIdentity": "112487张三",
  "newStudentIdentity": "112488张三"
}
```

### 响应

```json
{
  "studentIdentity": "112488张三",
  "handle": "tourist"
}
```

## 错误响应

当前 auth 模块和 Codeforces handle-account 接口的业务错误响应：

```json
{
  "code": "AUTH_INVALID_CREDENTIALS",
  "message": "invalid student identity or password"
}
```

常见错误：

| 场景 | HTTP 状态 | code |
| --- | --- | --- |
| 请求参数不合法 | `400` | `AUTH_INVALID_REQUEST` |
| 登录名或密码错误 | `401` | `AUTH_INVALID_CREDENTIALS` |
| 登录重试冷却中 | `429` | `AUTH_LOGIN_RATE_LIMITED` |
| 账号已禁用 | `403` | `AUTH_USER_DISABLED` |
| 权限不足或管理员自我降级 | `403` | `AUTH_FORBIDDEN` |
| 用户不存在 | `404` | `AUTH_USER_NOT_FOUND` |
| 用户已存在 | `409` | `AUTH_USER_EXISTS` |
| CF handle 绑定参数不合法 | `400` | `CODEFORCES_HANDLE_ACCOUNT_INVALID_REQUEST` |
| CF handle 绑定不存在 | `404` | `CODEFORCES_HANDLE_ACCOUNT_NOT_FOUND` |
| `studentIdentity` 已绑定 CF handle | `409` | `CODEFORCES_HANDLE_ACCOUNT_IDENTITY_EXISTS` |
| CF handle 已绑定给其它 `studentIdentity` | `409` | `CODEFORCES_HANDLE_ACCOUNT_HANDLE_EXISTS` |

Spring Security 自身拦截的未认证或 token 无效请求可能返回默认 `401` / `403` 响应体。
