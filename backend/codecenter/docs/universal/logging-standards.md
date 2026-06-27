# 日志规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-RT(请求链路与 AOP 规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-LS-001: 结构化日志格式

**规则:**

1. 所有服务 MUST 输出 JSON 格式的结构化日志,禁止输出非结构化的纯文本日志。
2. 每条日志 MUST 包含以下基础字段:
   - `timestamp`(String) — ISO 8601 格式,精确到毫秒
   - `level`(String) — 日志级别:ERROR / WARN / INFO / DEBUG / TRACE
   - `service`(String) — 服务名称
   - `traceId`(String) — 请求链路标识(详见 UNI-RT-001)
   - `message`(String) — 日志摘要信息
3. 日志 SHOULD 包含以下扩展字段:
   - `spanId`(String) — 子调用标识
   - `userId`(String) — 操作人标识(脱敏后)
   - `className`(String) — 输出日志的类名
   - `method`(String) — 输出日志的方法名
   - `duration`(Long) — 操作耗时(毫秒)

**示例:**

```json
{
  "timestamp": "2026-06-17T10:30:45.123+08:00",
  "level": "INFO",
  "service": "user-service",
  "traceId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "spanId": "span-001",
  "userId": "1001",
  "className": "com.example.user.service.UserService",
  "method": "createUser",
  "message": "用户创建成功",
  "duration": 156
}
```

```java
// Logback 配置: logback-spring.xml
<configuration>
    <appender name="JSON" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp>
                    <pattern>yyyy-MM-dd'T'HH:mm:ss.SSSXXX</pattern>
                </timestamp>
                <logLevel/>
                <mdc>
                    <includeMdcKeyName>traceId</includeMdcKeyName>
                    <includeMdcKeyName>spanId</includeMdcKeyName>
                    <includeMdcKeyName>userId</includeMdcKeyName>
                </mdc>
                <loggerName/>
                <message/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON"/>
    </root>
</configuration>
```

---

## 规范 UNI-LS-002: 日志级别使用标准

**规则:**

1. **ERROR** — MUST 用于影响系统功能、需要人工介入的错误(服务不可用、数据不一致、未捕获异常)。ERROR 日志 MUST 触发告警通知。
2. **WARN** — MUST 用于可预期但不影响核心功能的异常情况(重试成功、降级处理、配置缺失使用默认值)。WARN 日志 SHOULD 纳入监控统计。
3. **INFO** — MUST 用于关键业务流程节点(用户登录、订单创建、支付完成、配置变更)。INFO 日志 SHOULD 记录操作的输入和输出摘要。
4. **DEBUG** — MUST 用于开发和排查问题的详细信息(方法参数、SQL 语句、中间计算结果)。DEBUG 日志 MUST 不在生产环境默认开启。
5. **TRACE** — MAY 用于极细粒度的诊断信息(循环迭代、逐行处理),仅在特定排查场景临时开启。
6. 生产环境默认日志级别 MUST 为 INFO,可通过配置中心动态调整。

**示例:**

```java
// ERROR: 系统异常,需要人工介入
log.error("数据库连接池耗尽,当前活跃连接数: {},最大连接数: {}",
    activeCount, maxCount, exception);

// WARN: 可预期的异常,已自动恢复
log.warn("Redis 连接失败,降级使用本地缓存,key: {}", cacheKey);

// INFO: 关键业务节点
log.info("用户创建成功,userId: {},userName: {}", userId, userName);
log.info("配置变更生效,configKey: {},oldValue: {},newValue: {}",
    configKey, oldValue, newValue);

// DEBUG: 调试信息
log.debug("查询用户列表,pageNumber: {},pageSize: {},查询条件: {}",
    pageNumber, pageSize, queryCondition);

// TRACE: 极细粒度
log.trace("遍历用户列表,index: {},userId: {}", index, user.getId());
```

---

## 规范 UNI-LS-003: 日志内容规则

**规则:**

1. 日志消息 MUST 使用参数化占位符(`{}`),禁止使用字符串拼接(`+` 或 `String.format`)。
2. 日志 MUST 包含足够的上下文信息(谁、做了什么、结果如何、关键参数),以便排查问题。
3. 日志内容 MUST 不包含敏感信息(详见 UNI-LS-004)。
4. 异常日志 MUST 包含完整的异常堆栈,禁止仅打印 `e.getMessage()`。
5. 循环或高频调用中的日志 SHOULD 使用 `isDebugEnabled()` 或条件判断避免性能开销。
6. 日志消息 SHOULD 使用中文或英文,同一服务内 MUST 保持一致。

**示例:**

```java
// BAD: 字符串拼接(性能差)
log.info("User " + userName + " created at " + LocalDateTime.now());

// BAD: 缺少上下文
log.info("操作成功");

// BAD: 丢失堆栈
log.error("系统异常: " + e.getMessage());

// GOOD: 参数化占位符 + 完整上下文
log.info("用户创建成功,userId: {},userName: {},email: {}",
    user.getId(), user.getUserName(), user.getEmail());

// GOOD: 异常包含完整堆栈
log.error("用户创建失败,userName: {},email: {}", userName, email, e);

// GOOD: 高频日志加条件判断
if (log.isDebugEnabled()) {
    log.debug("批量处理进度,current: {},total: {},batch: {}",
        currentIndex, totalCount, batchSize);
}
```

```typescript
// TypeScript: 使用结构化日志库
import pino from 'pino';

const logger = pino({
  level: process.env.LOG_LEVEL || 'info',
  formatters: {
    level: (label) => ({ level: label.toUpperCase() }),
  },
});

// GOOD: 参数化上下文
logger.info({ userId, userName }, '用户创建成功');

// GOOD: 异常包含堆栈
try {
  await userService.createUser(request);
} catch (error) {
  logger.error({ err: error, userName }, '用户创建失败');
}
```

---

## 规范 UNI-LS-004: 日志脱敏规则

**规则:**

1. 日志输出 MUST 对以下敏感字段进行脱敏处理:
   - 密码类字段(`password`、`secret`、`credential`) — 全部替换为 `******`
   - Token 类字段(`token`、`accessToken`、`apiKey`) — 保留前 4 位 + `****`
   - 身份证号 — 保留前 3 位和后 4 位,中间替换为 `*`
   - 银行卡号 — 保留后 4 位,其余替换为 `*`
   - 手机号 — 保留前 3 位和后 4 位,中间替换为 `*`
   - 邮箱 — 用户名保留首字符,其余替换为 `*`
2. 脱敏 MUST 在日志序列化阶段通过自定义序列化器实现,禁止在业务代码中手动脱敏。
3. 新增敏感字段类型 MUST 同步更新脱敏规则。

**示例:**

```java
// 自定义脱敏注解
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveDataSerializer.class)
public @interface SensitiveData {
    SensitiveType value();
}

public enum SensitiveType {
    PASSWORD,     // ******
    TOKEN,        // abcd****
    ID_CARD,      // 110***********1234
    BANK_CARD,    // ************5678
    PHONE,        // 138****5678
    EMAIL         // a***@example.com
}

// 使用注解
public class UserLogDTO {
    private String userName;

    @SensitiveData(SensitiveType.PHONE)
    private String phone;

    @SensitiveData(SensitiveType.EMAIL)
    private String email;

    @SensitiveData(SensitiveType.PASSWORD)
    private String password;
}
```

```
// 脱敏前
log.info("用户登录,phone: {},email: {},password: {}", phone, email, password);
// 输出: 用户登录,phone: 13812345678,email: admin@example.com,password: MyP@ss123

// 脱敏后(自动处理)
// 输出: 用户登录,phone: 138****5678,email: a***@example.com,password: ******
```

---

## 规范 UNI-LS-005: 性能日志

**规则:**

1. 所有外部服务调用(HTTP/RPC)MUST 记录请求耗时,超过阈值 MUST 打印 WARN 日志。
2. 数据库慢查询 MUST 记录 SQL 语句、参数和执行耗时,阈值建议为 **200ms**。
3. 关键业务接口(创建、支付等)MUST 记录端到端耗时。
4. 性能日志 MUST 包含 `duration` 字段,且 SHOULD 包含请求的关键参数。
5. 性能阈值 SHOULD 可通过配置中心动态调整。

**示例:**

```java
// 外部调用耗时监控
long startTime = System.currentTimeMillis();
try {
    ThirdPartyResponse response = thirdPartyClient.queryData(request);
    long duration = System.currentTimeMillis() - startTime;

    if (duration > SLOW_CALL_THRESHOLD_MS) {
        log.warn("外部调用慢请求,service: {},method: {},duration: {}ms,requestId: {}",
            "payment-service", "queryOrder", duration, requestId);
    } else {
        log.info("外部调用完成,service: {},method: {},duration: {}ms",
            "payment-service", "queryOrder", duration);
    }
} catch (Exception e) {
    long duration = System.currentTimeMillis() - startTime;
    log.error("外部调用异常,service: {},method: {},duration: {}ms",
        "payment-service", "queryOrder", duration, e);
}
```

```yaml
# 数据库慢查询配置 (application.yml)
mybatis:
  configuration:
    log-impl: org.apache.ibatis.logging.slf4j.Slf4jImpl

logging:
  level:
    # MyBatis SQL 日志
    com.example.mapper: DEBUG

# 自定义慢查询阈值
slow-query:
  threshold-ms: 200
```

---

## 规范 UNI-LS-006: 日志保留与轮转

**规则:**

1. 日志文件 MUST 按日期和大小进行轮转:单文件不超过 200MB,按天切割。
2. 日志保留策略:
   - ERROR 日志 MUST 保留至少 **90 天**。
   - INFO/WARN 日志 MUST 保留至少 **30 天**。
   - DEBUG 日志 SHOULD 保留至少 **7 天**。
3. 历史日志文件 MUST 使用 gzip 压缩存储以节省磁盘空间。
4. 日志存储 MUST 使用异步写入模式,避免日志 I/O 阻塞业务线程。
5. 生产环境日志 MUST 同时输出到标准输出(容器场景)和文件系统。

**示例:**

```xml
<!-- logback-spring.xml: 日志轮转配置 -->
<appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
    <file>/var/log/app/application.log</file>
    <rollingPolicy class="ch.qos.logback.core.rolling.SizeAndTimeBasedRollingPolicy">
        <!-- 按天切割,单文件不超过 200MB -->
        <fileNamePattern>/var/log/app/application.%d{yyyy-MM-dd}.%i.log.gz</fileNamePattern>
        <maxFileSize>200MB</maxFileSize>
        <!-- 保留 30 天 -->
        <maxHistory>30</maxHistory>
        <!-- 总大小上限 10GB -->
        <totalSizeCap>10GB</totalSizeCap>
    </rollingPolicy>
    <encoder>
        <pattern>%d{yyyy-MM-dd'T'HH:mm:ss.SSSXXX} [%thread] %-5level %logger{36} - %msg%n</pattern>
    </encoder>
</appender>

<!-- 异步写入 -->
<appender name="ASYNC_FILE" class="ch.qos.logback.classic.AsyncAppender">
    <queueSize>1024</queueSize>
    <discardingThreshold>0</discardingThreshold>
    <appender-ref ref="FILE"/>
</appender>
```
