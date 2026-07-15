# 系统架构

## 运行拓扑

生产与本地稳定模式由 `deploy/docker-compose.yml` 定义四个服务：

| 服务 | 职责 |
| --- | --- |
| `blog-api` | 唯一 Spring Boot 后端，端口 8090 |
| `frontend` | 唯一 Nginx 网关，同时托管两份 Vue 3 产物 |
| `blog-db` | MySQL 8.4，保存 Blog、账号和训练数据 |
| `blog-redis` | 缓存、登录冷却和限流状态 |

浏览器路径：

```text
/                 Vue Blog
/training/**      Blog 外壳持有的训练公开路由
/training-app/**  内部 Vue Training 静态运行时
/api/**           去掉 /api 后转发 blog-api:8090
```

`frontend` 是一个服务，不代表两套前端源码被合并。Blog 与 Training 保持独立 Router；训练运行时通过同源 frame 嵌入 Blog 外壳并复用唯一导航。部署步骤见 [../deploy/README.md](../deploy/README.md)。

## 仓库模块

| 路径 | 职责与依赖方向 |
| --- | --- |
| `platform-blog/upstream/nblog/blog-api` | `top.naccl` 下的唯一 HTTP、安全和事务运行时；可依赖训练 application contract |
| `platform-blog/upstream/nblog/blog-view` | 公开 Blog、文章、赛事、个人主页和训练外壳 |
| `frontend` | 训练查询和管理员工作区；同时包含共享 Nginx 镜像配置 |
| `platform-training-data/training-data-common` | OJ 中立的账号绑定、查询、采集编排、调度、刷新和清理 contract |
| `platform-training-data/training-data-codeforces` | Codeforces source、ODS、清洗 SQL 与 adapter |
| `platform-training-data/training-data-atcoder` | AtCoder source、题目元数据、ODS、清洗 SQL 与 adapter |
| `platform-common/common-core` | 不含业务实体的通用 SQL task DAG 执行能力 |

训练模块没有 Spring Boot 入口、Controller、JWT 或账号管理，也不得依赖 `top.naccl`。Blog API 进程内组装这些库，并共享一个 MySQL `DataSource`、事务管理器和 Flyway history。

## 身份与数据所有权

- Blog API 拥有 BCrypt 密码、HS512 JWT、用户、角色、资料和 OJ handle；`username` 同时是 JWT `sub` 和训练业务身份。
- 角色只有 `ROLE_admin` 与 `ROLE_player`。受保护请求验证 token 后重新从 MySQL 读取用户和当前角色。
- 管理员替换或移除 handle 时，Blog API 在同一事务中先清理对应 OJ 的可再生训练数据，再替换绑定。
- OJ 提交采集以一次逻辑运行作为批次边界，源分页过滤后立即用同一 batch id 分块写入；内存不跨页累计提交。源失败只阻止对应 handle 推进游标，写入失败则在任何游标推进前终止本次运行。
- accepted 汇总在 JDBC repository 中按 `handle + difficulty` 聚合，application 层只负责 OJ 难度桶顺序、区间展示和未知难度归入 `UNRATED` 的领域规则。
- 评论 service 拥有 PageHelper 分页边界：每次只递归当前根评论页，并以 501 条数据库读取检测 500 条回复上限。该上限同时约束宽树内存和深链递归，Controller 只负责公开 `repliesTruncated` 契约。
- 分类名、标签名、文章标签对及其引用完整性由 MySQL 唯一约束、复合主键和外键保证。写服务把重复键、过期引用写入或被引用资源的删除失败映射为 409；Controller 不执行有竞争窗口且多一次往返的“先查后写/先计数再删”。`blog_tag(tag_id, blog_id)` 索引同时支撑按标签反查文章和外键校验。
- 全局异常层只把明确的请求绑定、校验和领域请求异常映射为 4xx；未分类的 `IllegalArgumentException` 按内部错误处理，避免把程序缺陷伪装成客户端错误。训练请求只在 HTTP/facade 输入边界完成 OJ 名称与采集参数转换。
- 训练身份使用 `training_member` 与 `oj_handle_binding`。旧 `oj_handle_account` 仍在 schema 中，但生产 repository 不再读写；不得改动 V034，删除旧表需要新的 migration 和运营确认。
- Blog 内容、比赛/奖项、首页编排和本地图片资产属于 Blog API。文章和比赛删除使用七天回收站，期间保留关联数据，到期后才物理清理；物理删除文章时 `blog_tag` 由数据库外键级联清理，service 不重复逐篇发送关联删除。
- 图片写入 `uploads/`；Blog API 读写，Nginx 只读提供 `/api/image/**`。数据库事务提交后再回收失效文件。

## 前端边界

- 两份 Vue 应用共享 `custacm.accessToken` 和 `custacm.user`，但摘要只用于展示；权限由后端决定。
- 公开 Blog 请求不全局附加 JWT；内部文章或受保护写入由具体 adapter 显式发送 Bearer token。
- 共享视觉 token 的唯一源是 `frontend-design-tokens/tokens.css`，两端本地文件是生成副本。
- 前端提供手动日间/夜间主题；选择保存到 `custacm.theme` 并在同源 Training frame 和跨标签页间同步，不跟随系统主题。

## 稳定决策

- 不恢复独立认证服务、训练 Web runtime、公开注册、demo token 或内存登录。
- 不把两个前端 Router 合并，也不增加第二个对外网关。
- 自动提交采集和 AtCoder 元数据 bootstrap/调度默认关闭，只由显式部署变量开启。
- 已应用的 Flyway migration 不修改；当前结构以迁移文件和代码为准，不在本文复制完整迁移历史。

HTTP 合同见 [api.md](api.md)，安全矩阵见 [authorization.md](authorization.md)。
