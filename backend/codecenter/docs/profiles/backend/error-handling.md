# 错误处理与日志规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | backend |
| 引入条件 | `fingerprint.profiles contains 'error-handling'` |
| 适用架构 | 后端服务（Spring Boot 为主） |
| 依赖规范 | `universal/exception-handling.md`、`universal/logging-standards.md`、`universal/request-tracing.md` |
| 互斥规范 | 无 |

> 本包是 L1 后端服务通用基线，定义全局异常处理、错误响应格式、链路追踪集成、日志规范、ErrorCode 体系。
> 配套 `universal/exception-handling.md`（基础异常分类）+ `universal/logging-standards.md`（日志格式）使用。

---

## 一、错误处理的核心原则

| 原则 | 说明 |
|------|------|
| **早抛晚捕** | 错误应在发现的第一时间抛出，由最外层统一捕获 |
| **分类处理** | 按错误类型（业务错误 / 系统错误 / 第三方错误）分类处理 |
| **统一响应** | 所有错误最终转换为统一的 ApiErrorResponse 格式 |
| **可追溯** | 每个错误必须有 `requestId` 和完整堆栈（开发环境） |
| **不泄露** | 生产环境不返回内部错误细节（堆栈、SQL 等） |

---

## 二、错误分类体系

### 2.1 错误分类

| 分类 | 范围 | HTTP 状态 | 业务码段位 | 处理策略 |
|------|------|-----------|------------|----------|
| 参数错误 | 客户端请求格式错误 | 400 | 1000-1099 | 立即返回，不重试 |
| 未认证 | token 缺失/无效 | 401 | 1100-1199 | 客户端重新登录 |
| 无权限 | 已认证但资源被禁 | 403 | 1200-1299 | 返回提示 |
| 资源不存在 | 资源 ID 不存在 | 404 | 1300-1399 | 立即返回 |
| 业务规则错误 | 业务校验失败 | 422 | 2000-8999 | 立即返回 |
| 限流触发 | 超过接口配额 | 429 | 1500-1599 | 客户端退避重试 |
| 系统错误 | 数据库/缓存/网络故障 | 500 | 9000-9099 | 记录 + 报警 + 重试 |
| 第三方错误 | 外部服务故障 | 502/503 | 9100-9199 | 降级 + 重试 |

**ERR-001** 所有异常 MUST 归类到上述 8 种之一，禁止使用裸 `Exception` 或 `RuntimeException` 直接抛出。 [MUST]

---

## 三、ErrorCode 体系

### 3.1 ErrorCode 枚举

```java
@Getter
public enum ErrorCode {
    // 0 - 成功
    SUCCESS(0, "成功"),

    // 1000-1099 参数错误
    PARAM_ERROR(1001, "参数错误"),
    PARAM_MISSING(1002, "必填参数缺失"),
    PARAM_INVALID_FORMAT(1003, "参数格式错误"),

    // 1100-1199 未认证
    UNAUTHORIZED(1100, "未认证"),
    TOKEN_EXPIRED(1101, "Token 已过期"),
    TOKEN_INVALID(1102, "Token 无效"),

    // 1200-1299 无权限
    FORBIDDEN(1200, "无权限"),
    PERMISSION_DENIED(1201, "权限不足"),

    // 1300-1399 资源不存在
    NOT_FOUND(1300, "资源不存在"),

    // 1500-1599 限流
    RATE_LIMIT_EXCEEDED(1500, "请求频率超限"),

    // 2000-8999 业务模块错误（按业务模块递增）
    USER_NOT_FOUND(2001, "用户不存在"),
    USER_ALREADY_EXISTS(2002, "用户已存在"),
    PASSWORD_ERROR(2003, "密码错误"),

    ORDER_NOT_FOUND(3001, "订单不存在"),
    ORDER_CANNOT_CANCEL(3002, "订单无法取消"),
    ORDER_PAID(3003, "订单已支付"),

    // 9000-9099 系统错误
    SYSTEM_ERROR(9001, "系统错误"),
    DATABASE_ERROR(9002, "数据库异常"),
    CACHE_ERROR(9003, "缓存异常"),

    // 9100-9199 第三方错误
    EXTERNAL_SERVICE_ERROR(9101, "外部服务异常"),
    EXTERNAL_SERVICE_TIMEOUT(9102, "外部服务超时");

    private final int code;
    private final String message;
}
```

### 3.2 业务异常类

```java
@Getter
public class BizException extends RuntimeException {
    private final ErrorCode errorCode;

    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + ": " + detail);
        this.errorCode = errorCode;
    }

    public BizException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
```

**ERR-010** 业务异常 MUST 携带 `ErrorCode`，禁止直接抛 `RuntimeException("xxx 错误")`。 [MUST]

---

## 四、全局异常处理器

### 4.1 Spring Boot 全局异常处理器

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BizException.class)
    public ApiResponse<Void> handleBizException(BizException e, HttpServletRequest request) {
        log.warn("业务异常: code={}, message={}, requestId={}",
            e.getErrorCode().getCode(), e.getMessage(), getRequestId(request));
        return ApiResponse.fail(e.getErrorCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
            .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", message);
        return ApiResponse.fail(ErrorCode.PARAM_ERROR, message);
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnknownException(Exception e, HttpServletRequest request) {
        log.error("系统异常: requestId={}", getRequestId(request), e);
        return ApiResponse.fail(ErrorCode.SYSTEM_ERROR, "系统繁忙，请稍后重试");
    }

    private String getRequestId(HttpServletRequest request) {
        return (String) request.getAttribute("traceId");
    }
}
```

**ERR-020** 全局异常处理器 MUST 覆盖所有受检 + 运行时异常，未捕获异常视为系统漏洞。 [MUST]

**ERR-021** 系统异常（`Exception.class`）MUST 记录完整堆栈（ERROR 级别），但响应 MUST 仅返回通用错误消息。 [MUST]

---

## 五、统一响应工具类

### 5.1 ApiResponse 工具

```java
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private String timestamp;
    private String requestId;

    public static <T> ApiResponse<T> ok(T data) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = ErrorCode.SUCCESS.getCode();
        resp.message = "success";
        resp.data = data;
        resp.timestamp = Instant.now().toString();
        resp.requestId = MDC.get("traceId");
        return resp;
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String detail) {
        ApiResponse<T> resp = new ApiResponse<>();
        resp.code = errorCode.getCode();
        resp.message = detail != null ? detail : errorCode.getMessage();
        resp.timestamp = Instant.now().toString();
        resp.requestId = MDC.get("traceId");
        return resp;
    }
}
```

**ERR-030** 成功响应 MUST 自动注入 `requestId`（从 MDC 取），无需业务代码手动传入。 [MUST]

---

## 六、链路追踪集成

### 6.1 TraceId 注入

```java
@Component
public class TraceIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String traceId = request.getHeader("X-Trace-Id");
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("traceId", traceId);
        response.setHeader("X-Trace-Id", traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
```

**ERR-040** 每个请求 MUST 在入口注入唯一 `traceId`，并在响应头回传 `X-Trace-Id`。 [MUST]

**ERR-041** MDC 中的 traceId MUST 在日志格式 `pattern` 中输出（`%X{traceId}`）。 [MUST]

### 6.2 异步 / 跨进程传递

```java
// 异步任务装饰器
TaskDecorator mdcTaskDecorator = runnable -> {
    String traceId = MDC.get("traceId");
    return () -> {
        try {
            MDC.put("traceId", traceId);
            runnable.run();
        } finally {
            MDC.remove("traceId");
        }
    };
};

// MQ 消息头传递
Message message = MessageBuilder.withPayload(body)
    .setHeader("X-Trace-Id", MDC.get("traceId"))
    .build();
```

**ERR-042** 异步任务 / MQ 消费 MUST 透传 traceId，确保全链路追踪不中断。 [MUST]

---

## 七、日志规范

### 7.1 日志级别

| 级别 | 使用场景 |
|------|----------|
| ERROR | 系统异常、未捕获错误、关键失败 |
| WARN | 业务校验失败、可重试错误、降级触发 |
| INFO | 关键业务节点（请求开始/结束、状态变更、定时任务） |
| DEBUG | 调试详情（生产环境默认关闭） |
| TRACE | 极详细调用链（生产环境默认关闭） |

**ERR-050** 日志 MUST 按业务严重程度选择级别，业务校验失败用 WARN，系统异常用 ERROR。 [MUST]

### 7.2 结构化日志格式

```json
{
  "timestamp": "2026-06-26T10:30:00.000Z",
  "level": "INFO",
  "logger": "com.example.UserService",
  "thread": "http-nio-8080-exec-1",
  "traceId": "abc123def456",
  "userId": "usr_001",
  "message": "用户创建成功",
  "operation": "createUser",
  "duration": 45,
  "status": "success"
}
```

**ERR-051** 日志 MUST 输出 JSON 格式（生产环境），便于日志聚合系统（Loki/ELK）解析。 [MUST]

### 7.3 敏感信息脱敏

| 字段类型 | 处理 |
|----------|------|
| 密码 | MUST 脱敏为 `***` |
| 身份证 | MUST 保留前 6 后 4 |
| 手机号 | MUST 保留前 3 后 4 |
| 邮箱 | MUST 保留 @ 前 1 位 + @ + 完整域名 |
| 银行卡 | MUST 保留后 4 位 |
| Token | MUST 脱敏为前 4 + `***` |

**ERR-060** 敏感字段 MUST 走脱敏工具（如 Hutool `DesensitizedUtil`），禁止在日志中明文打印。 [MUST]

---

## 八、Feign 调用异常还原

### 8.1 跨服务异常处理

```java
@Component
public class FeignErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() >= 400 && response.body() != null) {
            try {
                String body = Util.toString(response.body().asReader());
                ApiResponse<?> apiResp = objectMapper.readValue(body, ApiResponse.class);
                ErrorCode remoteCode = ErrorCode.fromCode(apiResp.getCode());
                return new BizException(remoteCode, "远程调用失败: " + methodKey);
            } catch (Exception e) {
                return new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR);
            }
        }
        return new BizException(ErrorCode.EXTERNAL_SERVICE_ERROR);
    }
}
```

**ERR-070** Feign 调用方 MUST 实现 ErrorDecoder，将远端业务异常还原为本地 BizException，保留原始错误码。 [MUST]

---

## 九、检查清单

### 9.1 异常处理检查

- [ ] 所有异常是否归类到 8 种错误类型
- [ ] BizException 是否携带 ErrorCode
- [ ] 是否实现全局异常处理器
- [ ] 系统异常是否记录完整堆栈
- [ ] 生产环境是否屏蔽内部错误细节

### 9.2 响应格式检查

- [ ] ApiResponse 是否包含 `code` + `message` + `data` + `timestamp` + `requestId`
- [ ] requestId 是否自动注入
- [ ] 错误响应是否使用统一的 ErrorCode

### 9.3 链路追踪检查

- [ ] 是否注入 traceId 到 MDC
- [ ] 异步任务是否透传 traceId
- [ ] MQ 消息是否携带 traceId
- [ ] 响应头是否回传 `X-Trace-Id`

### 9.4 日志规范检查

- [ ] 日志级别是否与严重程度匹配
- [ ] 生产环境是否输出 JSON 格式
- [ ] 敏感字段是否脱敏
- [ ] 是否记录关键业务节点

---

*本规范与 `universal/exception-handling.md`（异常基础分类）+ `universal/logging-standards.md`（日志格式基础）+ `universal/request-tracing.md`（链路追踪基础）协同使用。*