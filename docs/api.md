# custacm-platform API 文档

本文档记录当前已经实现并可以对外调用的后端接口。

当前阶段有两个可运行后端模块：

- `platform-auth/auth-web`：默认端口 `8081`，负责本地账号、登录、RSA JWT 签发和用户管理。
- `platform-training-data/training-data-web`：默认端口 `8082`，负责 Codeforces/AtCoder 训练数据写入、采集、清理、刷新和查询入口。

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
| `auth-web` | `POST` | `/api/auth/admin/users:batch-create` | `admin` | 管理员批量创建用户 |
| `auth-web` | `GET` | `/api/auth/users` | 否 | 游客查看用户列表 |
| `auth-web` | `PATCH` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员更新用户角色或重置密码 |
| `auth-web` | `DELETE` | `/api/auth/admin/users/{studentIdentity}` | `admin` | 管理员真实删除用户 |
| `training-data-web` | `GET` | `/health` | 否 | 训练数据模块健康检查 |
| `training-data-web` | `GET` | `/module-info` | 否 | 训练数据模块信息 |
| `training-data-common` | `POST` | `/api/training-data/admin/codeforces/submissions:collect` | `admin` | 管理员按 `studentIdentity` 和可选 `ojName` 采集最近 N 小时 OJ submission 并写入 ODS |
| `training-data-common` | `POST` | `/api/training-data/admin/codeforces/submissions:collect-batch-jobs` | `admin` | 管理员启动带可选 `ojName` 的批量采集后台任务；任务按 common 配置的 `platform.training-data.collector.job-item-interval` 逐个处理身份 |
| `training-data-common` | `GET` | `/api/training-data/admin/codeforces/submissions/collect-batch-jobs` | `admin` | 管理员查看后端保留的 OJ 采集任务列表 |
| `training-data-common` | `POST` | `/api/training-data/admin/oj-handles` | `admin` | 管理员创建 `studentIdentity + handles` OJ 绑定 |
| `training-data-common` | `DELETE` | `/api/training-data/admin/students/{studentIdentity}/oj-data` | `admin` | 管理员按 `studentIdentity + ojName` 删除指定 OJ 的 ODS/DWD/DWM/DWS 数据 |
| `training-data-common` | `GET` | `/api/training-data/oj-handles` | 否 | 游客一次性查询全量 OJ handle 绑定和 per-OJ 采集状态，响应是 `studentIdentity -> account` map |
| `training-data-common` | `GET` | `/api/training-data/codeforces/accepted-summary` | 否 | 游客按 `studentIdentity` 和可选 `ojName` 查询指定 OJ 区间 rating AC 汇总 |
| `training-data-common` | `GET` | `/api/training-data/codeforces/submissions/by-student` | 否 | 游客按 `studentIdentity` 和可选 `ojName` 分页查询指定 OJ 提交明细 |
| `training-data-common` | `GET` | `/api/training-data/codeforces/submissions/by-problem` | 否 | 游客按 `problemKey` 和可选 `ojName` 分页查询指定 OJ 提交明细 |
| `training-data-common` | `GET` | `/api/training-data/codeforces/first-accepted/by-student` | 否 | 游客按 `studentIdentity` 和可选 `ojName` 分页查询指定 OJ 首 AC 题目明细 |
| `training-data-common` | `GET` | `/api/training-data/codeforces/first-accepted/by-problem` | 否 | 游客按 `problemKey` 和可选 `ojName` 分页查询指定 OJ 首 AC handle 列表 |
| `training-data-common` | `PATCH` | `/api/training-data/admin/oj-handles:change-identity` | `admin` | 管理员迁移 OJ handle 绑定的 `studentIdentity`，并可更新是否参与自动采集 |

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
    "atcoder-ods-submission",
    "atcoder-ods-problem",
    "atcoder-problem-model",
    "atcoder-warehouse-tables",
    "atcoder-warehouse-refresh",
    "codeforces-handle-account",
    "codeforces-warehouse-refresh",
    "codeforces-submission-collector",
    "atcoder-submission-collector",
    "atcoder-problem-list-collector"
  ]
}
```

`training-data-web` 启动时也会应用内部 Codeforces ODS/DWD/DWM/DWS 表迁移、AtCoder ODS 落地表迁移、common 模块里的 AtCoder DWD/DWM/DWS 建表迁移和 `oj_handle_account` 表迁移；AtCoder ODS 迁移、Kenkoooo source client、ODS parser/writer、submission collector、problem metadata collector 和 DWD/DWM/DWS SQL task refresh 由 `training-data-atcoder` 提供，AtCoder DWD/DWM/DWS 表迁移由 `training-data-common` 提供。Codeforces 采集按单个 `user.status?handle=...` 页面执行，团队提交归因给本次采集的目标 handle，并以 `submission + handle` 粒度写入 ODS/DWD。AtCoder 题目元数据包含 `problems.json` 与 `problem-models.json`，DWD 会把 Kenkoooo 难度映射成 AtCoder 独立分段桶。DWD/DWM/DWS 转换以各 OJ 模块内的 SQL 任务资源表示，并由采集任务内部 refresh handler 触发；当前刷新任务覆盖 Codeforces 和 AtCoder。

## POST /api/auth/login

使用平台本地账号登录。当前没有公开注册入口；账号由管理员创建或批量导入。

### 请求

```http
POST /api/auth/login
Content-Type: application/json

{
  "studentIdentity": "230511213黄炳睿",
  "password": "playerPass123",
  "rememberMe": false
}
```

`rememberMe` 可省略，默认按 `false` 处理。`false` 时签发普通 access token，当前默认有效期为 2 小时；`true` 时签发“记住我”access token，当前默认有效期为 30 天。

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

管理员通过批量创建接口创建用户、禁用、启用、调整其它账号角色、重置密码和真实删除用户。禁用通过把账号 `role` 设置为 `disable` 完成；重新启用时需要把 `role` 设置回 `admin` 或 `player`。角色更新和密码重置统一通过 `PATCH /api/auth/admin/users/{studentIdentity}` 完成。管理员不能把自己降级为 `player` 或 `disable`，也不能删除自己。

创建用户时 `password` 为空字符串、空白字符串或未传时，后端会生成 16 位随机密码。`POST /api/auth/admin/users:batch-create` 传入一个用户时就是单用户创建；不再提供单独的单用户创建接口。更新用户时 `newPassword` 为空字符串或空白字符串表示生成随机新密码；`newPassword` 未传或为 `null` 表示不修改密码。创建和修改密码成功时，响应会在 `plainPassword` 返回本次生效的明文密码；该明文只在本次响应中返回，数据库仍只保存哈希。

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

## POST /api/training-data/admin/codeforces/submissions:collect

管理员按 `studentIdentity` 和可选 `ojName` 采集最近 N 小时 submission，并写入对应 OJ 的 ODS。接口不接受外部传入的 handle 或结束时间；服务先用 `studentIdentity` 查 `oj_handle_account.handles_json` 中目标 OJ 的 handle，再以当前 instant 作为右边界，按 `lookbackHours` 回推左边界。`ojName` 为空使用 dispatcher 默认 OJ，当前默认是 `CODEFORCES`；传 `ATCODER` 时采集 Kenkoooo AtCoder submissions。每个成功采集的 handle 会写回 `oj_handle_account.collection_states_json[OJ_NAME]`，其中 `lastCollectedAt` 是采集器执行时间，不是最新提交时间；`historyStartReached` 表示已确认采到该 OJ handle 的历史左端。

后台自动任务从 `oj_handle_account` 读取 `need_collect=true` 且存在 schedule `ojName` 对应 handle 的绑定，并在采集前按该 handle 去重。该过滤在采集用例里完成，HTTP controller 不直接处理自动采集名单。

每次 Codeforces 分页请求默认使用 `connect-timeout=10s`、`read-timeout=30s`、`request-interval=4s`，并最多尝试 `max-request-attempts=3` 次；每次 AtCoder Kenkoooo 请求默认使用 `connect-timeout=10s`、`read-timeout=30s`、`request-interval=2s`，并最多尝试 `max-request-attempts=3` 次。实现上，HTTP controller、lookback 窗口计算、handle 解析契约、限速/重试、窗口过滤和结果聚合使用 `training-data-common` 的公共实现；Codeforces adapter 独立负责 `user.status` 分页、`creationTimeSeconds` 提交时间读取、源站错误码映射和 ODS 写入；AtCoder adapter 独立负责 Kenkoooo `from_second` 分页、`epoch_second` 提交时间读取、源站错误码映射和 ODS 写入。HTTP 单 identity 采集只解析出一个 handle；如果该 handle 的分页请求重试耗尽，本次采集返回 `FAILED`，记录带稳定 `errorCode` 的日志，并在响应的 `handles` 中返回失败状态。定时批量采集遇到单个 handle 失败时会继续处理后续 handle。

后台自动任务由 `application.yml` 的 `platform.training-data.collector.schedules` 列表驱动，每个调度项包含 `oj-name`、`enabled`、`cron`、`zone` 和 `lookback`。默认配置打开状态的 `daily-recent-submissions`（`CODEFORCES`，每天 `Asia/Shanghai` 12:00）和 `atcoder-daily-recent-submissions`（`ATCODER`，每天 `Asia/Shanghai` 12:15），都采集执行时刻往前 `120h` 的滚动窗口。自动采集写入 ODS 后，如果返回了 `batchId`，会调用对应 OJ 的仓库刷新 handler 刷新 DWD/DWM/DWS；没有 batch 的空采集跳过刷新。

### 请求

```http
POST /api/training-data/admin/codeforces/submissions:collect
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "112487张三",
  "lookbackHours": 120,
  "ojName": null
}
```

`studentIdentity` 必须是已在 OJ handle map 中绑定目标 OJ handle 的平台业务身份，`lookbackHours` 必须是正整数。实际窗口由服务计算为 `[now - lookbackHours, now)`，并和 OJ submission 的提交时间比较；Codeforces 使用 `creationTimeSeconds`，AtCoder 使用 Kenkoooo `epoch_second`。响应中的 `windowStartInclusive` 和 `windowEndExclusive` 是本次运行计算出的窗口。

### 响应

```json
{
  "ojName": "CODEFORCES",
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

HTTP 单 identity 采集的整体 `status` 可能是 `SUCCESS`、`FAILED` 或 `SKIPPED`。当没有命中 submission 或本 JVM 内已有采集正在运行时，`batchId` 为 `null` 且 `writtenRows` 为 `0`，`message` 说明原因。没有命中 submission 但 handle 采集成功时也会更新该 OJ 的 `lastCollectedAt`；ODS 写入失败或 handle 采集失败时不会标记成功采集。Codeforces 只有分页拿到源站最后一页时才把 `historyStartReached` 置为 true；AtCoder 的 Kenkoooo 采集只有从 `from_second=0` 开始时才会置为 true。`studentIdentity` 未绑定目标 OJ handle 时返回 handle-account 的 `404` 错误；请求尚未实现采集 adapter 的 OJ 时返回 `400`。AtCoder 成功写入时 `tableName` 为 `ods_atcoder__submission`，`batchId` 使用 `collector-atcoder-*` 前缀。

## Codeforces 后台采集任务

前端批量采集使用后台任务接口，避免长时间请求被网关切断。任务状态保存在当前 `training-data-web` 进程内，浏览器刷新和页面切换后通过任务列表继续查看；后端重启后不会恢复，最多保留最近 50 个任务快照。

### 启动任务

```http
POST /api/training-data/admin/codeforces/submissions:collect-batch-jobs
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentities": ["112487张三", "112488李四"],
  "lookbackHours": 120,
  "refreshWarehouse": true,
  "ojName": null
}
```

如果已有采集任务正在运行，接口返回该运行中任务快照，不再启动第二个并发采集任务。`ojName` 为空使用 dispatcher 默认 OJ，当前默认是 `CODEFORCES`。后台任务会在相邻两个 `studentIdentity` 的采集之间复用配置的 `request-interval`，Codeforces 默认等待 `4s`，AtCoder 默认等待 `2s`。`refreshWarehouse=true` 时，每个采集成功且产生 `batchId` 的用户会在同一个后台任务中继续触发该 OJ 的 warehouse refresh；当前 Codeforces 和 AtCoder 都实现了 refresh handler。这只是采集任务的收尾步骤，不提供单独 warehouse refresh HTTP 接口。

响应状态码为 `202 Accepted`，响应体是任务快照：

```json
{
  "jobId": "4f2e7bb1-7f7a-4d73-8c09-2fd7a1e8f4b2",
  "ojName": null,
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
      "ojName": "CODEFORCES",
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

## DELETE /api/training-data/admin/students/{studentIdentity}/oj-data

管理员删除一个平台身份当前绑定的指定 OJ 训练数据。接口先按 `studentIdentity` 查询 `oj_handle_account`，再按必填 query 参数 `ojName` 解析该 OJ 绑定的 handle。单个 OJ 删除会先按通用同层仓库表名删除：

- `dws_{oj}__handle_daily_rating_accepted_summary`
- `dwm_{oj}__handle_problem_first_accepted`
- `dwd_{oj}__submission`

然后调用该 OJ 自己的 ODS purge adapter 删除 ODS 原始表，例如 Codeforces 删除 `ods_codeforces__submission.author_handle` 对应成员行，AtCoder 删除 `ods_atcoder__submission.user_id`。整个 use case 包在一个事务里；任意一层删除失败都会回滚本次训练数据删除。没有 OJ handle 绑定或目标 OJ 未绑定时，接口返回 `200` 且各删除计数为 `0`。缺少 `ojName` 或传入空白值会返回 `400` 和 `OJ_STUDENT_DATA_PURGE_INVALID_REQUEST`。该接口不删除 `oj_handle_account` 绑定，也不删除 auth 登录账号；彻底删除用户时，调用方需要按当前绑定的 OJ 逐个清理训练数据，再调用 `DELETE /api/auth/admin/users/{studentIdentity}` 删除账号。

### 请求

```http
DELETE /api/training-data/admin/students/112487张三/oj-data?ojName=CODEFORCES
Authorization: Bearer <admin_access_token>
```

必填 query 参数：

- `ojName`：OJ 名，只删除该 OJ 的训练数据。

### 响应

```json
{
  "studentIdentity": "112487张三",
  "ojName": "CODEFORCES",
  "handle": "tourist",
  "handles": {
    "CODEFORCES": "tourist",
    "ATCODER": "tourist_atcoder"
  },
  "ojResults": [
    {
      "ojName": "CODEFORCES",
      "handle": "tourist",
      "odsSubmissionRows": 128,
      "dwdSubmissionRows": 128,
      "dwmFirstAcceptedRows": 90,
      "dwsAcceptedSummaryRows": 35,
      "totalDeletedRows": 381
    }
  ],
  "handleAccountRows": 0,
  "odsSubmissionRows": 128,
  "dwdSubmissionRows": 128,
  "dwmFirstAcceptedRows": 90,
  "dwsAcceptedSummaryRows": 35,
  "totalDeletedRows": 381
}
```

## POST /api/training-data/admin/oj-handles

管理员创建一个平台 `studentIdentity` 到多个 OJ handle 的绑定。一个 `studentIdentity` 只有一行 OJ handle account；`handles` 是 OJ 名到 handle 的 map，OJ 名统一使用大写常量，例如 `CODEFORCES`、`ATCODER`。同一个 OJ 内的 handle 只能绑定给一个 `studentIdentity`。

该接口调用 `training-data-common` 的 OJ handle-account 用例维护 `oj_handle_account`，不会创建或修改 auth 登录账号。基础物理表迁移由 Codeforces OJ 模块提供，通用采集状态列由 common 迁移提供。新绑定默认 `needCollect=true`；后台自动采集还会按 schedule 的 `ojName` 要求存在对应 OJ handle。

### 请求

```http
POST /api/training-data/admin/oj-handles
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "studentIdentity": "112487张三",
  "handles": {
    "CODEFORCES": "tourist",
    "ATCODER": "tourist_atcoder"
  }
}
```

### 响应

```json
{
  "studentIdentity": "112487张三",
  "handles": {
    "CODEFORCES": "tourist",
    "ATCODER": "tourist_atcoder"
  },
  "needCollect": true,
  "collectionStates": {
    "CODEFORCES": {
      "historyStartReached": false,
      "lastCollectedAt": null
    },
    "ATCODER": {
      "historyStartReached": false,
      "lastCollectedAt": null
    }
  }
}
```

## GET /api/training-data/oj-handles

游客一次性查询全量 OJ handle 绑定和采集状态。该接口是 guest endpoint，不需要 JWT，也不会解析请求里的 `Authorization` header。接口不提供按 `studentIdentity` 查单条的参数；调用方应从响应 map 中按 `studentIdentity` 取账号，再从账号的 `handles` map 中按大写 OJ 名取 handle，从 `collectionStates` map 中按同一个 OJ 名取采集状态。

### 请求

```http
GET /api/training-data/oj-handles
```

### 响应

```json
{
  "112487张三": {
    "studentIdentity": "112487张三",
    "handles": {
      "CODEFORCES": "tourist",
      "ATCODER": "tourist_atcoder"
    },
    "needCollect": true,
    "collectionStates": {
      "CODEFORCES": {
        "historyStartReached": true,
        "lastCollectedAt": "2026-07-05T04:00:00Z"
      },
      "ATCODER": {
        "historyStartReached": false,
        "lastCollectedAt": null
      }
    }
  }
}
```

## GET /api/training-data/codeforces/accepted-summary

游客按平台 `studentIdentity` 查询指定 OJ 的 DWS 区间 rating AC 汇总。接口不需要 JWT，也不会解析 `Authorization` header。`ojName` 作为 query 参数沿老请求链路透传，默认 `CODEFORCES`；app 层用它从 `oj_handle_account.handles` map 取对应 handle，infra 按同层多表命名规则查询 `dws_{oj}__handle_daily_rating_accepted_summary`。

可选参数：

- `ojName`：OJ 名，默认 `CODEFORCES`。
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

游客按平台 `studentIdentity` 查询指定 OJ 的 DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。`ojName` 会决定从 `oj_handle_account.handles` 取哪个 handle，并按同层多表命名规则查询 `dwd_{oj}__submission`。
结果按 `submittedAtUtcPlus8` 倒序分页；时间相同的记录按 `submissionId` 倒序排列。

可选参数：

- `ojName`：OJ 名，默认 `CODEFORCES`。
- `submittedFromUtcPlus8` / `submittedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。
- `page`：第几页，1-based，默认 `1`。
- `limit`：每页条数，默认 `15`，最大 `2000`。

```http
GET /api/training-data/codeforces/submissions/by-student?studentIdentity=112487张三&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=15
```

响应：

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "page": 1,
  "limit": 15,
  "total": 12345,
  "totalPages": 823,
  "hasMore": true,
  "submissions": [
    {
      "submissionId": "379398914",
      "studentIdentity": "112487张三",
      "handle": "tourist",
      "submittedAtUtcPlus8": "2026-07-01T12:00:00",
      "submittedDateUtcPlus8": "2026-07-01",
      "problemKey": "2237:G",
      "problemIndex": "G",
      "problemName": "Problem G",
      "difficulty": "2900",
      "language": "C++23",
      "verdict": "OK",
      "accepted": true,
      "timeConsumedMillis": 46,
      "sourceUrl": "https://codeforces.com/contest/2237/submission/379398914"
    }
  ]
}
```

提交明细响应只返回公共提交字段，不再返回 contest、testset、内存、标签等 Codeforces 专属 DWD 字段。
`total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`submissions` 为空且 `hasMore=false`。

## GET /api/training-data/codeforces/submissions/by-problem

游客按指定 OJ 的 `problemKey` 查询 DWD 提交明细。接口不需要 JWT，也不会解析 `Authorization` header。
结果按 `submittedAtUtcPlus8` 倒序分页；时间相同的记录按 `submissionId` 倒序排列。

可选参数：

- `ojName`：OJ 名，默认 `CODEFORCES`。
- `submittedFromUtcPlus8` / `submittedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `page`：第几页，1-based，默认 `1`。
- `limit`：每页条数，默认 `15`，最大 `2000`。

```http
GET /api/training-data/codeforces/submissions/by-problem?problemKey=2237:G&submittedFromUtcPlus8=2026-07-01T00:00:00&submittedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=15
```

响应结构：

```json
{
  "problemKey": "2237:G",
  "page": 1,
  "limit": 15,
  "total": 321,
  "totalPages": 22,
  "hasMore": true,
  "submissions": [
    {
      "submissionId": "379398914",
      "studentIdentity": "112487张三",
      "handle": "tourist",
      "problemKey": "2237:G",
      "difficulty": "2900",
      "accepted": true
    }
  ]
}
```

如果结果里存在未绑定 `studentIdentity` 的当前 OJ handle，返回 `404` 和 `OJ_HANDLE_ACCOUNT_NOT_FOUND`。
`total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`submissions` 为空且 `hasMore=false`。

## GET /api/training-data/codeforces/first-accepted/by-student

游客按平台 `studentIdentity` 查询指定 OJ 的 DWM 首 AC 题目明细。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数：

- `ojName`：OJ 名，默认 `CODEFORCES`。
- `firstAcceptedFromUtcPlus8` / `firstAcceptedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `minProblemRating` / `maxProblemRating`：整数 rating 边界。设置任一边界时不包含 unrated。
- `page`：页码，从 1 开始，默认 `1`。
- `limit`：每页数量，默认 `15`，最大 `2000`。

```http
GET /api/training-data/codeforces/first-accepted/by-student?studentIdentity=112487张三&firstAcceptedFromUtcPlus8=2026-07-01T00:00:00&firstAcceptedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=15
```

```json
{
  "studentIdentity": "112487张三",
  "authorHandle": "tourist",
  "totalAcceptedProblemCount": 1,
  "page": 1,
  "limit": 15,
  "total": 1,
  "totalPages": 1,
  "hasMore": false,
  "problems": [
    {
      "problemKey": "2237:G",
      "problemIndex": "G",
      "problemName": "Problem G",
      "difficulty": "2900",
      "firstAcceptedSubmissionId": "379398914",
      "firstAcceptedAtUtcPlus8": "2026-07-01T12:00:00",
      "firstAcceptedDateUtcPlus8": "2026-07-01",
      "firstAcceptedLanguage": "C++23",
      "firstAcceptedSourceUrl": "https://codeforces.com/contest/2237/submission/379398914"
    }
  ]
}
```

结果按 `firstAcceptedAtUtcPlus8` 倒序分页；时间相同的记录按 `handle`、`problemKey` 升序排列。`totalAcceptedProblemCount` 和 `total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`problems` 为空且 `hasMore=false`。

## GET /api/training-data/codeforces/first-accepted/by-problem

游客按指定 OJ 的 `problemKey` 查询 DWM 首次通过该题的 handle 列表。接口不需要 JWT，也不会解析 `Authorization` header。

可选参数：

- `ojName`：OJ 名，默认 `CODEFORCES`。
- `firstAcceptedFromUtcPlus8` / `firstAcceptedToUtcPlus8`：ISO 本地时间，例如 `2026-07-01T00:00:00`。
- `page`：页码，从 1 开始，默认 `1`。
- `limit`：每页数量，默认 `15`，最大 `2000`。

```http
GET /api/training-data/codeforces/first-accepted/by-problem?problemKey=2237:G&firstAcceptedFromUtcPlus8=2026-07-01T00:00:00&firstAcceptedToUtcPlus8=2026-07-31T23:59:59&page=1&limit=15
```

```json
{
  "problemKey": "2237:G",
  "acceptedHandleCount": 1,
  "page": 1,
  "limit": 15,
  "total": 1,
  "totalPages": 1,
  "hasMore": false,
  "acceptedHandles": [
    {
      "studentIdentity": "112487张三",
      "handle": "tourist",
      "firstAcceptedAtUtcPlus8": "2026-07-01T12:00:00"
    }
  ]
}
```

结果按 `firstAcceptedAtUtcPlus8` 倒序分页；时间相同的记录按 `handle` 升序排列。`acceptedHandleCount` 和 `total` 是同一组筛选条件下的精确总数，`totalPages` 按 `limit` 计算。请求页超过最后一页时，`acceptedHandles` 为空且 `hasMore=false`。

如果结果里存在未绑定 `studentIdentity` 的 Codeforces handle，返回 `404` 和 `OJ_HANDLE_ACCOUNT_NOT_FOUND`。

## PATCH /api/training-data/admin/oj-handles:change-identity

管理员更新 OJ handle 绑定信息。该操作可以更新 `oj_handle_account.student_identity`、`need_collect` 和可选 `handles` map，不会修改 auth 登录账号。`handles` 省略时保持原绑定；传入时按 OJ 名合并到已有 map，不会删除未出现在请求体里的其它 OJ handle。未改变的 OJ handle 会保留已有采集状态；某个 OJ handle 值被替换时，该 OJ 的采集状态会重置。

如果 `newStudentIdentity` 已经存在 OJ handle account，返回 `409`。`needCollect` 省略时保持原值；如果只想切换是否自动采集，可以让 `oldStudentIdentity` 和 `newStudentIdentity` 相同。

### 请求

```http
PATCH /api/training-data/admin/oj-handles:change-identity
Authorization: Bearer <admin_access_token>
Content-Type: application/json

{
  "oldStudentIdentity": "112487张三",
  "newStudentIdentity": "112488张三",
  "needCollect": false,
  "handles": {
    "ATCODER": "tourist_atcoder"
  }
}
```

### 响应

```json
{
  "studentIdentity": "112488张三",
  "handles": {
    "CODEFORCES": "tourist",
    "ATCODER": "tourist_atcoder"
  },
  "needCollect": false,
  "collectionStates": {
    "CODEFORCES": {
      "historyStartReached": false,
      "lastCollectedAt": null
    },
    "ATCODER": {
      "historyStartReached": false,
      "lastCollectedAt": null
    }
  }
}
```

## 错误响应

当前 auth 模块和 OJ handle-account 接口的业务错误响应：

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
| OJ handle 绑定参数不合法 | `400` | `OJ_HANDLE_ACCOUNT_INVALID_REQUEST` |
| OJ handle 绑定不存在 | `404` | `OJ_HANDLE_ACCOUNT_NOT_FOUND` |
| `studentIdentity` 已有 OJ handle account | `409` | `OJ_HANDLE_ACCOUNT_IDENTITY_EXISTS` |
| 某个 OJ handle 已绑定给其它 `studentIdentity` | `409` | `OJ_HANDLE_ACCOUNT_HANDLE_EXISTS` |
| CF 学生数据删除参数不合法 | `400` | `OJ_STUDENT_DATA_PURGE_INVALID_REQUEST` |

Spring Security 自身拦截的未认证或 token 无效请求可能返回默认 `401` / `403` 响应体。
