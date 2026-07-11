# blog-api

`blog-api` 是项目唯一可运行的 Spring Boot 后端。Java 21、Spring Boot 3.5.16、MyBatis、JDBC、Flyway、MySQL、Redis、Quartz、BCrypt 与 HS512 JWT 在一个进程中运行。

浏览器通过 Nginx 的 `/api/**` 访问，网关会去掉 `/api`；直接访问 Blog API 时使用 `/login`、`/player/**`、`/admin/**` 等原始路径。

## 模块职责

- 提供 Blog 内容、评论与原有公开读取 API。
- 提供首页横幅图片公开列表，以及管理员上传、排序和删除 API；上传图固定为 1920×1080 JPEG。
- 统一负责登录、密码、JWT、账号、角色与 OJ handle。
- 持久化本人裁剪后的头像，并更新 `user.avatar`。
- 持久化本人 nickname、个性签名，以及最多八条按用户隔离和排序的 HTTP(S) 友情链接。
- 组装训练模块并暴露 player/admin 训练 HTTP adapter。
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
| `config/BootstrapAdminInitializer.java` | 幂等创建首个管理员 |
| `config/TrainingDataModuleConfiguration.java` | 进程内组装训练模块 |
| `service/impl/AdminUserService.java` | 用户生命周期、最后管理员保护、handle 绑定与训练数据清理 |
| `service/PlayerAvatarService.java` | 校验 512×512 PNG 头像、保存到上传目录并更新当前用户头像 |
| `service/PlayerProfileService.java`、`mapper/UserProfileLinkMapper.java` | 当前用户资料校验、友链整体替换与有序持久化 |
| `controller/player/PlayerAccountController.java` | 当前用户资料、本人 OJ handle、昵称、签名、个人友链、密码与头像 API |
| `controller/admin/UserAdminController.java` | 管理员账号和 OJ handle API |
| `controller/HomepageBannerController.java`、`controller/admin/HomepageBannerAdminController.java` | 首页图片公开读取与管理员上传/排序/删除 API |
| `service/HomepageBannerService.java`、`repository/HomepageBannerRepository.java` | 一至两张首页图片的数量/尺寸校验、同源文件存储与有序持久化 |
| `controller/admin/TrainingDataAdminController.java` | 训练采集、任务与数仓刷新 API；数仓刷新未传 batch 时选择最新有效批次，批次缺失返回 `BAD_REQUEST` |
| `controller/player/TrainingDataQueryController.java` | 认证后的训练查询与用户目录 |
| `db/migration/` | Blog baseline、训练 schema、身份与外键整合、首页图片，以及用户签名和个人友链迁移 |
| `src/test/java/top/naccl/service/PlayerProfileServiceTest.java` | 覆盖资料修改、友链顺序/上限/安全协议和清空语义 |
| `src/test/java/top/naccl/controller/admin/TrainingDataAdminControllerTest.java` | 覆盖显式/最新 batch 刷新分流与稳定 HTTP 400 错误映射 |
| `src/test/java/top/naccl/controller/admin/UserAdminControllerTest.java` | 锁定 `/admin/users:batch-create` 无额外斜杠的批量创建路由合同 |

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
mvn clean verify
./scripts/check-test-policy.sh
```
