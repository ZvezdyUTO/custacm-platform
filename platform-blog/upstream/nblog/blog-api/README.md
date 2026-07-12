# blog-api

`blog-api` 是项目唯一可运行的 Spring Boot 后端。Java 21、Spring Boot 3.5.16、MyBatis、JDBC、Flyway、MySQL、Redis、Quartz、BCrypt 与 HS512 JWT 在一个进程中运行。

浏览器通过 Nginx 的 `/api/**` 访问，网关会去掉 `/api`；直接访问 Blog API 时使用 `/login`、`/player/**`、`/admin/**` 等原始路径。

## 模块职责

- 提供 Blog 内容、评论与原有公开读取 API；公开评论在关联账号仍存在时返回只读 `username`，游客和已注销账号为空。
- 顶栏公开搜索只按标题子串匹配公开文章，并按最近更新时间返回最多十条候选；内部文章和草稿不进入公开聚合接口。
- 普通用户只能管理本人文章；管理员可管理全部文章，新建文章的作者固定为当前认证管理员。
- 普通用户与管理员文章写接口统一限制标题 100 字、简介 255 字、正文 200000 字，不能依赖前端输入限制保证数据合法。
- Markdown 输出在服务端按 HTML 白名单清洗，移除脚本、事件属性和危险 URL 协议，避免文章内容形成存储型 XSS。
- 提供首页横幅图片公开列表，以及管理员上传、排序和删除 API；上传图固定为 1920×1080 JPEG。
- 分类接受自定义十六进制颜色；新增标签由服务端生成并持久化非预设的深色随机颜色。公开 Blog DTO 返回颜色供分类丝带、文章标签和标签云以白字渲染。
- 统一负责登录、密码、JWT、账号、角色与 OJ handle。
- 已有 OJ handle 只能通过管理员高危接口更换；服务在同一事务内先清理该用户对应 OJ 的全部训练事实与汇总数据和旧采集状态，再完成换绑。
- 使用配置的 bootstrap 密码幂等创建固定 `root` 管理员，并禁止该账号删除、改名、降权或绑定 OJ handle。
- 将本人裁剪后的头像纳入托管图片资产，生成 512×512 高清版与 96×96 缩略图并更新 `user.avatar`。
- 接收本人文章首图和正文图片，使用 Thumbnailator 生成归一化高清版与缩略图；正文上传最大 15MB。
- 文章删除、图片移除、首图/头像替换后立即删除失效文件；后台每日重试失败删除并清理超过 24 小时的临时/孤儿资产。
- 管理员用户创建/修改接口不接受 avatar 字段或外部头像 URL；头像修改只有本人图片上传路径。
- 持久化本人 nickname、个性签名，以及最多八条按用户隔离和排序的 HTTP(S) 友情链接。
- 通过匿名只读的 `GET /profiles/{username}` 向文章详情名片提供作者头像、昵称、签名和有序友情链接，不暴露角色等账号管理字段。
- 组装训练模块，并在 `top.naccl` 内独占 player/admin 训练 Spring MVC adapter。
- 使用同一个 MySQL `DataSource`、事务管理器和 Flyway history 管理 Blog 与训练 schema。

## 目录结构

```text
src/main/java/top/naccl/          Blog、认证、账号/handle 管理和 HTTP adapter
src/main/resources/mapper/       MyBatis mapping
src/main/resources/db/migration/ Blog baseline 与整合迁移
src/test/java/top/naccl/          安全、账号、schema、service 与组装测试
```

`TrainingDataModuleConfiguration` 导入训练配置；已移除的训练 Web controller 不参与扫描。Blog API 可以依赖训练 application contract，训练模块不得依赖 `top.naccl` 类。

## 关键文件职责

| 文件/路径 | 职责 |
| --- | --- |
| `BlogApiApplication.java` | 唯一后端应用入口 |
| `config/SecurityConfig.java`、`JwtFilter.java` | URL 权限层级与基于数据库当前用户的 JWT 授权 |
| `config/BootstrapAdminInitializer.java` | 幂等创建固定用户名 `root` 的系统管理员 |
| `config/TrainingDataModuleConfiguration.java` | 进程内组装训练模块 |
| `aspect/ExceptionLogAspect.java` | 持久化 Controller 异常元数据与堆栈，不采集或保存请求参数 |
| `service/impl/AdminUserService.java` | 用户生命周期、`root` 与最后管理员保护、handle 绑定与训练数据清理 |
| `service/PlayerAvatarService.java` | 更新当前用户托管头像并触发旧头像即时回收 |
| `service/ImageProcessingService.java`、`service/ImageAssetService.java` | JPEG/PNG 校验、高清/缩略图生成、文章绑定、所有权与文件生命周期 |
| `controller/player/PlayerImageController.java` | 本人文章图片 multipart 上传和未绑定图片幂等删除 API |
| `service/PlayerProfileService.java`、`mapper/UserProfileLinkMapper.java` | 当前用户资料校验、友链整体替换与有序持久化 |
| `controller/PublicProfileController.java`、`model/vo/PublicProfile.java` | 匿名只读的文章作者公开名片接口与最小公开字段合同 |
| `controller/player/PlayerAccountController.java` | 当前用户资料、本人 OJ handle、昵称、签名、个人友链、密码与头像 API |
| `controller/admin/UserAdminController.java` | 管理员账号和 OJ handle API |
| `model/dto/OjHandleReplaceRequest.java` | 管理员高危更换单个 OJ handle 的目标 OJ 与新 handle 请求体 |
| `controller/player/PlayerBlogController.java`、`service/PlayerBlogService.java` | 本人文章 CRUD 与作者所有权校验；Player 文章允许空首图和空标签列表 |
| `util/BlogContentLimits.java` | Player 与管理员文章写入共用的标题、简介和正文长度约束 |
| `util/markdown/MarkdownUtils.java` | Markdown 转 HTML，并对白名单外标签、属性和危险 URL 协议做服务端清洗 |
| `model/vo/BlogDetail.java`、`model/vo/BlogInfo.java`、`mapper/BlogMapper.xml` | 公开/内部文章详情、公开列表与搜索；公开聚合排除内部文章，详情和列表返回 `authorUsername` |
| `model/vo/BlogInfo.java` | 首页文章缓存 DTO；读取 Redis 中旧版本缓存时忽略已移除字段，避免发布后的缓存兼容故障 |
| `controller/admin/BlogAdminController.java` | 全部文章管理；管理员新建文章时从当前 JWT 身份绑定作者 |
| `controller/HomepageBannerController.java`、`controller/admin/HomepageBannerAdminController.java` | 首页图片公开读取与管理员上传/排序/删除 API |
| `controller/admin/BlogAdminController.java`、`mapper/BlogMapper.xml` | 管理员文章列表与精选开关；公开侧栏按固定顺序查询已发布且精选的文章 |
| `service/HomepageBannerService.java`、`repository/HomepageBannerRepository.java` | 一至两张首页图片的数量/尺寸校验、同源文件存储与有序持久化 |
| `controller/admin/TrainingDataAdminController.java` | 训练采集、任务与数仓刷新 API；数仓刷新未传 batch 时选择最新有效批次，批次缺失返回 `BAD_REQUEST` |
| `controller/player/TrainingDataQueryController.java` | 认证后的训练查询与用户目录；注入无 MVC 注解的 `OjWarehouseQueryFacade`，不代理训练模块 Controller。 |
| `db/migration/` | Blog baseline、训练 schema、身份与外键整合、首页图片、用户资料、托管图片资产及文章可见性迁移 |
| `src/test/java/top/naccl/service/PlayerProfileServiceTest.java` | 覆盖资料修改、友链顺序/上限/安全协议和清空语义 |
| `src/test/java/top/naccl/controller/admin/TrainingDataAdminControllerTest.java` | 覆盖显式/最新 batch 刷新分流与稳定 HTTP 400 错误映射 |
| `src/test/java/top/naccl/controller/admin/UserAdminControllerTest.java` | 锁定 `/admin/users:batch-create` 无额外斜杠的批量创建路由合同 |
| `src/test/java/top/naccl/BlogAuthorContractTest.java` | 锁定公开文章详情和文章列表的 `authorUsername` MyBatis 映射 |
| `src/test/java/top/naccl/aspect/ExceptionLogAspectTest.java` | 锁定异常日志不持久化 Controller 参数的安全约束 |
| `src/test/java/top/naccl/controller/IntegratedControllerCoverageTest.java` | 覆盖公开资料、队员图片、训练查询与图片清理任务的委托及参数合同 |

## 训练用户目录

`GET /player/training-data/users` 要求 `ROLE_player` 或 `ROLE_admin`，只列出 `needCollect=true` 且至少绑定一个 OJ 账号的用户。每项字段严格为：

```json
{
  "username": "player1",
  "nickname": "队员一",
  "ojNames": ["CODEFORCES", "ATCODER"]
}
```

响应不暴露邮箱、角色、真实 OJ handle、采集状态或管理员私有字段，并按 `username` 排序。浏览器路径是 `/api/player/training-data/users`，Blog API 直接路径是 `/player/training-data/users`。

## 验证

Java 变更后从仓库根目录运行：

```bash
mvn clean test
```

该命令编译 reactor 并运行已有单测。历史 NBlog 代码不强制追补单测或覆盖率；新增或实质修改的业务逻辑应同步增加针对性单测。JaCoCo 报告仅用于本地诊断，不作为 MR 门禁。
