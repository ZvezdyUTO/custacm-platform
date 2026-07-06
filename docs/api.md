# custacm-platform API 文档

本文档记录当前已经实现并可以对外调用的后端接口。

当前阶段有两个可运行后端模块：

- `platform-auth/auth-web`：默认端口 `8081`，负责本地账号、登录、RSA JWT 签发和用户管理。
- `platform-training-data/training-data-web`：默认端口 `8082`，负责 Codeforces 训练数据写入、采集、清理、刷新和查询入口。

当前后端没有自定义统一响应包裹，接口直接返回业务 JSON。错误响应使用稳定 `code` 和 `message`。

## 基础信息

默认本地地址：

```text
auth-web:          http://localhost:8081
training-data-web: http://localhost:8082
```

部署后端口由对应模块配置控制。当前 Compose 部署覆盖 `auth-web`、`training-data-web`、各自 MySQL 和前端 Nginx 静态/proxy 容器。

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
| `auth-web` | `GET` | `/api/auth/users` | 否 | 游客查看用户列表 |
| `auth-web` | `PATCH` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员更新用户角色或重置密码 |
| `auth-web` | `DELETE` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员真实删除用户 |
| `training-data-web` | `GET` | `/health` | 否 | 训练数据模块健康检查 |
| `training-data-web` | `GET` | `/module-info` | 否 | 训练数据模块信息 |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/ods/codeforces/submissions:batch-upsert` | `admin` | 批量写入 Codeforces submission ODS |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/codeforces/submissions:collect` | `admin` | 管理员按 `studentIdentity` 采集最近 N 小时 Codeforces submission 并写入 ODS |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/codeforces/submissions:collect-batch-jobs` | `admin` | 管理员启动 Codeforces 批量采集后台任务 |
| `training-data-codeforces` | `GET` | `/api/training-data/admin/codeforces/submissions/collect-batch-jobs` | `admin` | 管理员查看后端保留的 Codeforces 采集任务列表 |
| `training-data-codeforces` | `GET` | `/api/training-data/admin/codeforces/submissions/collect-batch-jobs/{jobId}` | `admin` | 管理员查询一个 Codeforces 采集任务状态 |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/codeforces/handles` | `admin` | 管理员创建 `studentIdentity + Codeforces handle` 绑定 |
| `training-data-codeforces` | `DELETE` | `/api/training-data/admin/codeforces/users/{studentIdentity}/data` | `admin` | 管理员删除某个 `studentIdentity` 当前 Codeforces 绑定及其 ODS/DWD/DWM/DWS 数据 |
| `training-data-codeforces` | `POST` | `/api/training-data/admin/codeforces/warehouse:refresh` | `admin` | 管理员同步刷新 Codeforces DWD/DWM/DWS SQL task DAG |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/handles` | 否 | 游客按 `studentIdentity` 查询单个 Codeforces handle 绑定 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/accepted-summary` | 否 | 游客按 `studentIdentity` 查询 Codeforces 区间 rating AC 汇总 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/accepted-summary/auto-collect-users` | 否 | 游客查询所有自动采集 Codeforces 绑定的区间 rating AC 汇总列表 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/submissions/by-student` | 否 | 游客按 `studentIdentity` 分页查询 Codeforces 提交明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/submissions/by-problem` | 否 | 游客按 `problemKey` 分页查询 Codeforces 提交明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/first-accepted/by-student` | 否 | 游客按 `studentIdentity` 查询 Codeforces 首 AC 题目明细 |
| `training-data-codeforces` | `GET` | `/api/training-data/codeforces/first-accepted/by-problem` | 否 | 游客按 `problemKey` 查询 Codeforces 首 AC handle 列表 |
| `training-data-codeforces` | `PATCH` | `/api/training-data/admin/codeforces/handles:change-identity` | `admin` | 管理员迁移 CF handle 绑定的 `studentIdentity`，并可更新是否参与自动采集 |

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
    "codeforces-handle-account",
    "codeforces-warehouse-refresh",
    "codeforces-submission-collector"
  ]
}
```

`training-data-web` 启动时也会应用内部 Codeforces ODS/DWD/DWM/DWS 表迁移和 `codeforces_handle_account` 表迁移。DWD/DWM/DWS 转换以 `training-data-codeforces` 内的 SQL 任务资源表示，并由 admin refresh 接口同步触发。

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

所有 `/api/auth/admin/**` 接口都要求当前 token 的 `role` 为 `admin`。公开用户列表接口位于 `/api/auth/users`，不放在 admin 前缀下。

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

### GET /api/auth/users

公开读取用户数组；该接口不需要 JWT，也不会解析请求里的 `Authorization` header。

响应 `200`：

```json
[
  {
    "studentIdentity": "230511213黄炳睿",
    "role": "player",
    "createdAt": "2026-07-04T12:00:00Z",
    "updatedAt": "2026-07-04T12:00:00Z"
  }
]
```

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

下游 DWD/DWM/DWS 转换通过 admin refresh 接口触发。该接口按 YAML manifest 建 DAG，每次请求都会重新读取 manifest、重建图并检查 DAG，再按拓扑顺序执行 SQL。

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

## POST /api/training-data/admin/codeforces/submissions:collect

管理员按 `studentIdentity` 从 Codeforces `user.status` 采集最近 N 小时 submission，并写入 `ods_codeforces__submission`。接口不接受外部传入的 Codeforces handle 或结束时间；服务先用 `studentIdentity` 查 `codeforces_handle_account` 绑定的 handle，再以当前 instant 作为右边界，按 `lookbackHours` 回推左边界。

后台自动任务从 `codeforces_handle_account` 读取 `need_collect=true` 的已绑定 handle，并在采集前去重。该过滤在采集用例里完成，HTTP controller 不直接处理自动采集名单。

每次 Codeforces 分页请求默认使用 `connect-timeout=10s`、`read-timeout=30s`、`request-interval=4s`，并最多尝试 `max-request-attempts=3` 次。HTTP 单 identity 采集只解析出一个 handle；如果该 handle 的分页请求重试耗尽，本次采集返回 `FAILED`，记录带稳定 `errorCode` 的日志，并在响应的 `handles` 中返回失败状态。定时批量采集遇到单个 handle 失败时会继续处理后续 handle。ODS 写入后当前只保留后续调度调用的 TODO，不触发未完成的调度逻辑。

后台自动任务由 `application.yml` 的 `platform.training-data.codeforces.collector.schedules` 列表驱动，默认配置一个关闭状态的 `daily-recent-submissions`。将该调度项的 `enabled` 改为 `true` 后，默认每天 `Asia/Shanghai` 12:00 调用同一采集用例，采集执行时刻往前 `120h` 的滚动窗口。

### 请求

```http
POST /api/training-data/admin/codeforces/submissions:collect
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "112487张三",
  "lookbackHours": 120
}
```

`studentIdentity` 必须是已绑定 Codeforces handle 的平台业务身份，`lookbackHours` 必须是正整数。实际窗口由服务计算为 `[now - lookbackHours, now)`，并和 Codeforces submission 的 `creationTimeSeconds` 比较。响应中的 `windowStartInclusive` 和 `windowEndExclusive` 是本次运行计算出的窗口。

### 响应

```json
{
  "status": "SUCCESS",
  "windowStartInclusive": "2026-06-30T04:00:00Z",
  "windowEndExclusive": "2026-07-05T04:00:00Z",
  "requestedHandleCount": 1,
  "succeededHandleCount": 1,
  "failedHandleCount": 0,
  "fetchedSubmissionCount": 2,
  "matchedSubmissionCount": 1,
  "batchId": "collector-codeforces-1783252800000-...",
  "tableName": "ods_codeforces__submission",
  "writtenRows": 1,
  "fetchedAt": "2026-07-05T04:00:00Z",
  "message": null,
  "handles": [
    {
      "handle": "alice",
      "status": "SUCCESS",
      "fetchedSubmissionCount": 2,
      "matchedSubmissionCount": 1,
      "errorCode": null,
      "message": null
    }
  ]
}
```

HTTP 单 identity 采集的整体 `status` 可能是 `SUCCESS`、`FAILED` 或 `SKIPPED`。当没有命中 submission 或本 JVM 内已有采集正在运行时，`batchId` 为 `null` 且 `writtenRows` 为 `0`，`message` 说明原因。`studentIdentity` 未绑定 Codeforces handle 时返回 handle-account 的 `404` 错误。

## Codeforces 后台采集任务

前端批量采集使用后台任务接口，避免长时间请求被网关切断。后台任务只覆盖 Codeforces submission 采集；ODS 导入和手动 warehouse refresh 仍是同步请求。任务状态保存在当前 `training-data-web` 进程内，浏览器刷新和页面切换后可以继续查询同一个任务；后端重启后不会恢复，最多保留最近 50 个任务快照。

### 启动任务

```http
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentities": ["112487张三", "112488李四"],
  "lookbackHours": 120,
  "refreshWarehouse": true
}
```

如果已有采集任务正在运行，接口返回该运行中任务快照，不再启动第二个并发采集任务。后台任务会在相邻两个 `studentIdentity` 的采集之间复用 `request-interval`，默认等待 `4s`，避免批量采集连续触发 Codeforces。`refreshWarehouse=true` 时，每个采集成功且产生 `batchId` 的用户会在同一个后台任务中继续触发该批次的 warehouse refresh；这只是采集任务的收尾步骤，不代表 warehouse refresh 已经被改造成独立后台任务。

响应状态码为 `202 Accepted`，响应体是任务快照：

```json
{
  "jobId": "4f2e7bb1-7f7a-4d73-8c09-2fd7a1e8f4b2",
  "status": "RUNNING",
  "requestedCount": 2,
  "completedCount": 1,
  "collectedCount": 1,
  "failedCount": 0,
  "refreshedCount": 1,
  "writtenRows": 18,
  "batchIds": ["collector-codeforces-1783252800000-..."],
  "startedAt": "2026-07-06T04:00:00Z",
  "finishedAt": null,
  "message": "采集任务运行中",
  "items": [
    {
      "studentIdentity": "112487张三",
      "itemStatus": "SUCCESS",
      "collectionStatus": "SUCCESS",
      "handle": "alice",
      "batchId": "collector-codeforces-1783252800000-...",
      "tableName": "ods_codeforces__submission",
      "writtenRows": 18,
      "fetchedSubmissionCount": 30,
      "matchedSubmissionCount": 18,
      "fetchedAt": "2026-07-06T04:01:00Z",
      "message": null,
      "refreshStatus": "SUCCESS",
      "refreshMessage": "SUCCESS"
    }
  ]
}
```

任务 `status` 可为 `RUNNING`、`SUCCESS`、`PARTIAL_SUCCESS`、`FAILED`。每个 `items[].itemStatus` 可为 `PENDING`、`RUNNING`、`SUCCESS`、`FAILED`。`refreshStatus` 可为 `NOT_REQUESTED`、`NO_BATCH`、`SUCCESS` 或 `FAILED`。

### 查询任务列表

```http
GET /api/training-data/admin/codeforces/submissions/collect-batch-jobs
Authorization: Bearer <admin_access_token>
```

返回后端当前保留的任务快照数组，按 `startedAt` 倒序排列。前端数据同步页会轮询该接口展示“当前采集任务”列表，并允许展开查看每个用户的采集、写入、批次、刷新和错误信息。

### 查询单个任务

```http
GET /api/training-data/admin/codeforces/submissions/collect-batch-jobs/{jobId}
Authorization: Bearer <admin_access_token>
```

返回同样的任务快照对象。任务不存在时返回 `404`：

```json
{
  "code": "CODEFORCES_SUBMISSION_COLLECTION_JOB_NOT_FOUND",
  "message": "Codeforces collection job not found: <jobId>"
}
```

## DELETE /api/training-data/admin/codeforces/users/{studentIdentity}/data

管理员删除一个平台身份当前绑定的 Codeforces 数据。接口先按 `studentIdentity` 查询 `codeforces_handle_account`，如果存在绑定，则按该 Codeforces handle 删除：

- `dws_codeforces__handle_daily_rating_accepted_summary`
- `dwm_codeforces__handle_problem_first_accepted`
- `dwd_codeforces__submission`
- `ods_codeforces__submission`
- `codeforces_handle_account`

如果该 `studentIdentity` 没有 Codeforces 绑定，接口返回 `200` 且各删除计数为 `0`。该接口不删除 auth 登录账号；彻底删除用户时，调用方需要先清理训练数据，再调用 `DELETE /api/auth/admin/users/{studentIdentity}` 删除账号。

### 请求

```http
DELETE /api/training-data/admin/codeforces/users/112487张三/data
Authorization: Bearer <admin_access_token>
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handle": "tourist",
  "handleAccountRows": 1,
  "odsSubmissionRows": 128,
  "dwdSubmissionRows": 128,
  "dwmFirstAcceptedRows": 90,
  "dwsAcceptedSummaryRows": 35,
  "totalDeletedRows": 382
}
```

## POST /api/training-data/admin/codeforces/warehouse:refresh

管理员同步刷新 Codeforces DWD/DWM/DWS。请求传入 `batchId`；服务先用该 batch 覆盖的 UTC+8 日期闭区间刷新上层表。若首 AC 事实移动到原区间之外，服务会把有效刷新区间扩大到受影响首 AC 日期的最小/最大边界，并自动重跑同一 SQL task DAG，直到区间稳定。若 ODS 中没有匹配该 `batchId` 且带 `creationTimeSeconds` 的提交，服务返回 `400`，不会启动 SQL task。

执行器会读取 `sql/tasks/codeforces-warehouse-refresh.yml`，构建并校验 DAG。当前任务顺序是：

```text
codeforces.dwd.submission
 -> codeforces.dwm.handle_problem_first_accepted
 -> codeforces.dws.handle_daily_rating_accepted_summary
```

每个节点一个事务。节点失败后，DAG 立即停止，失败节点之后的执行计划节点标记为 `SKIPPED`，日志包含稳定 `errorCode=SQL_TASK_SQL_EXECUTION_FAILED`。

### 请求

```http
POST /api/training-data/admin/codeforces/warehouse:refresh
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "batchId": "external-codeforces-1782554400000-...",
  "startFromTaskId": "codeforces.dwm.handle_problem_first_accepted"
}
```

`startFromTaskId` 可选。用于失败后手动续跑时，只执行该节点及其下游节点；不传时从 DAG 根节点开始。自动扩大区间后的重跑沿用同一个 `startFromTaskId`。成功响应仍是最后一次 SQL task run 的执行结果。

### 成功响应

```json
{
  "runId": "6bf5d79d-5f3d-400c-a23c-abae2a92c9df",
  "status": "SUCCESS",
  "manifestLocation": "classpath:sql/tasks/codeforces-warehouse-refresh.yml",
  "startFromTaskId": null,
  "failedTaskId": null,
  "startedAt": "2026-07-05T11:29:09.728Z",
  "finishedAt": "2026-07-05T11:29:09.834Z",
  "durationMillis": 106,
  "tasks": [
    {
      "taskId": "codeforces.dwd.submission",
      "description": "Refresh Codeforces DWD submissions for the ODS batch date interval.",
      "sqlLocation": "classpath:sql/dwd/upsert_dwd_codeforces__submission.sql",
      "status": "SUCCESS",
      "startedAt": "2026-07-05T11:29:09.729Z",
      "finishedAt": "2026-07-05T11:29:09.775Z",
      "durationMillis": 45,
      "affectedRows": 1000,
      "errorCode": null,
      "message": null
    }
  ]
}
```

如果 SQL 节点执行失败，HTTP 仍返回执行结果对象，但 `status` 为 `FAILED`、`failedTaskId` 为失败节点 ID，失败节点的 `errorCode` 为 `SQL_TASK_SQL_EXECUTION_FAILED`。请求体非法、`batchId` 没有可刷新的 ODS 提交区间，或 `startFromTaskId` 不存在时返回 `400` 和稳定 `code`；manifest、DAG 或 SQL 文件配置错误返回 `500` 和稳定 `code`。

## POST /api/training-data/admin/codeforces/handles

管理员创建一个平台 `studentIdentity` 到 Codeforces handle 的绑定。一个 `studentIdentity` 只能绑定一个 Codeforces handle；一个 Codeforces handle 也只能绑定给一个 `studentIdentity`。

该接口只维护 `training-data-codeforces` 内的 `codeforces_handle_account`，不会创建或修改 auth 登录账号。新绑定默认 `needCollect=true`，会参与后台自动采集。

### 请求

```http
POST /api/training-data/admin/codeforces/handles
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "112487张三",
  "handle": "tourist",
  "needCollect": true
}
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handle": "tourist",
  "needCollect": true
}
```

## GET /api/training-data/codeforces/handles

游客按 `studentIdentity` 查询已绑定的 Codeforces handle。该接口是 guest endpoint，不需要 JWT，也不会解析请求里的 `Authorization` header。

### 请求

```http
GET /api/training-data/codeforces/handles?studentIdentity=112487张三
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handle": "tourist",
  "needCollect": true
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

## GET /api/training-data/codeforces/accepted-summary/auto-collect-users

游客查询所有 `needCollect=true` 的 Codeforces handle 绑定在指定区间内的 DWS rating AC 汇总。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数同单人汇总：

- `acceptedFromDateUtcPlus8` / `acceptedToDateUtcPlus8`：ISO 日期，例如 `2026-07-01`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。

返回数组按 `totalAcceptedProblemCount` 降序排列。

```http
GET /api/training-data/codeforces/accepted-summary/auto-collect-users?acceptedFromDateUtcPlus8=2026-07-01&acceptedToDateUtcPlus8=2026-07-31&minProblemRating=800&maxProblemRating=1600
```

```json
[
  {
    "studentIdentity": "112489王五",
    "authorHandle": "Benq",
    "totalAcceptedProblemCount": 4,
    "ratingCounts": [
      { "problemRating": "1200", "acceptedProblemCount": 4 }
    ]
  },
  {
    "studentIdentity": "112487张三",
    "authorHandle": "tourist",
    "totalAcceptedProblemCount": 2,
    "ratingCounts": [
      { "problemRating": "800", "acceptedProblemCount": 2 }
    ]
  }
]
```

## GET /api/training-data/codeforces/submissions/by-student

游客按平台 `studentIdentity` 查询 Codeforces DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。
结果按 `submittedAtUtcPlus8` 倒序分页；时间相同的记录按 `codeforcesSubmissionId` 倒序排列。

可选参数：

- `submittedFromUtcPlus8` / `submittedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。
- `page`：第几页，1-based，默认 `1`。
- `limit`：每页条数，默认 `100`，最大 `2000`。

```http
GET /api/training-data/codeforces/submissions/by-student?studentIdentity=112487张三&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=100
```

响应：

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "page": 1,
  "limit": 100,
  "total": 12345,
  "totalPages": 124,
  "hasMore": true,
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
`total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`submissions` 为空且 `hasMore=false`。

## GET /api/training-data/codeforces/submissions/by-problem

游客按 Codeforces `problemKey` 查询 DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。
结果按 `submittedAtUtcPlus8` 倒序分页；时间相同的记录按 `codeforcesSubmissionId` 倒序排列。

可选参数：

- `submittedFromUtcPlus8` / `submittedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `page`：第几页，1-based，默认 `1`。
- `limit`：每页条数，默认 `100`，最大 `2000`。

```http
GET /api/training-data/codeforces/submissions/by-problem?problemKey=2237:G&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=100
```

响应结构：

```json
{
  "problemKey": "2237:G",
  "page": 1,
  "limit": 100,
  "total": 321,
  "totalPages": 4,
  "hasMore": true,
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
`total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`submissions` 为空且 `hasMore=false`。

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

管理员更新 Codeforces handle 绑定信息。该操作可以更新 `codeforces_handle_account.student_identity` 和 `need_collect`，不会修改 handle，也不会修改 auth 登录账号。

如果 `newStudentIdentity` 已经绑定了 Codeforces handle，返回 `409`。`needCollect` 省略时保持原值；如果只想切换是否自动采集，可以让 `oldStudentIdentity` 和 `newStudentIdentity` 相同。

### 请求

```http
PATCH /api/training-data/admin/codeforces/handles:change-identity
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "oldStudentIdentity": "112487张三",
  "newStudentIdentity": "112488张三",
  "needCollect": false
}
```

### 响应

```json
{
  "studentIdentity": "112488张三",
  "handle": "tourist",
  "needCollect": false
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
| CF 学生数据删除参数不合法 | `400` | `CODEFORCES_STUDENT_DATA_PURGE_INVALID_REQUEST` |

Spring Security 自身拦截的未认证或 token 无效请求可能返回默认 `401` / `403` 响应体。
