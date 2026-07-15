# 后端日志规则

修改后端日志前必须阅读本文件。项目使用 Spring Boot 默认 SLF4J/Logback，不引入自定义日志系统或重型日志平台。

## 当前输出

```text
logs/combined.log  全部后端日志
logs/error.log     ERROR 日志
```

Compose 将日志目录挂载到应用容器；服务器路径由部署环境决定，仓库不提供公开日志查询端口。

## 写法

```java
private static final Logger log = LoggerFactory.getLogger(CurrentClass.class);

log.info("Collection completed, ojName={}, batchId={}", ojName, batchId);
log.warn("Request rejected, errorCode={}, reason={}", errorCode, reason);
log.error("Collection failed, errorCode={}", errorCode, exception);
```

- 使用参数占位符，不拼接字符串。
- 每条 `error` 日志包含稳定 `errorCode`；有异常时把 `Throwable` 作为最后参数。
- 请求追踪存在后通过 MDC 统一携带 `traceId`；业务代码不自行生成 trace ID。
- `info` 只记录重要生命周期或状态变化；预期拒绝/降级用 `warn`；需要调查的意外失败用 `error`；排障细节用 `debug`。
- 后台任务在记录任务最终状态的边界记录未处理异常，日志与任务状态使用同一稳定错误码。

## 禁止记录

- 密码、重置码、token、cookie、session、Authorization header；
- JWT 签名材料、数据库密码、`.env` 值；
- 含敏感个人信息的完整请求/响应或未经脱敏的个人数据。

需要关联用户时优先记录不可逆 hash。新增 runnable backend 时复用 `LOG_DIR`、`combined.log`、`error.log` 与 MDC 格式。
