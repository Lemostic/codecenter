# 异常处理规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-AD(API 设计规范)、UNI-LS(日志规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-EH-001: 全局异常处理器

**规则:**

1. 每个服务 MUST 实现全局异常处理器,统一捕获和转换所有未被局部处理的异常。
2. 全局异常处理器 MUST 位于框架层(如 Spring `@ControllerAdvice`、Express 中间件),业务代码禁止自行处理全局异常。
3. 全局异常处理器 MUST 输出符合 UNI-AD-003 的统一响应格式(`code`/`message`/`data`/`requestId`)。
4. 异常处理器 MUST 根据异常类型设置正确的 HTTP 状态码。
5. 全局异常处理器 MUST 记录日志:已知业务异常记录 WARN 级别,未知系统异常记录 ERROR 级别(含堆栈)。

**示例:**

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 业务异常 → 400
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {
        log.warn("业务异常,code: {},message: {},uri: {}",
            ex.getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    // 参数校验异常 → 400
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(
            ValidationException ex, HttpServletRequest request) {
        log.warn("参数校验失败,code: {},message: {},uri: {}",
            ex.getCode(), ex.getMessage(), request.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.fail(ex.getCode(), ex.getMessage()));
    }

    // 未授权 → 401
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedException(
            UnauthorizedException ex, HttpServletRequest request) {
        log.warn("未授权访问,uri: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.fail("A00002", "未授权,请先登录"));
    }

    // 权限不足 → 403
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("权限不足,uri: {}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.fail("A00003", "权限不足"));
    }

    // 未知系统异常 → 500
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) {
        log.error("系统未知异常,uri: {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail("B00001", "系统内部异常,请稍后重试"));
    }
}
```

```typescript
// Express: 全局错误处理中间件
import { Request, Response, NextFunction } from 'express';

interface AppError extends Error {
  code?: string;
  statusCode?: number;
  isOperational?: boolean;
}

function globalErrorHandler(
  err: AppError,
  req: Request,
  res: Response,
  _next: NextFunction
): void {
  const statusCode = err.statusCode || 500;
  const code = err.code || 'B00001';

  if (err.isOperational) {
    logger.warn({ code, message: err.message, uri: req.originalUrl }, '业务异常');
  } else {
    logger.error({ err, uri: req.originalUrl }, '系统未知异常');
  }

  res.status(statusCode).json({
    code,
    message: err.isOperational ? err.message : '系统内部异常,请稍后重试',
    data: null,
    requestId: req.headers['x-trace-id'] || generateTraceId(),
  });
}
```

---

## 规范 UNI-EH-002: 自定义异常层次结构

**规则:**

1. 项目 MUST 定义统一的异常基类 `BaseException`,所有自定义异常 MUST 继承该基类。
2. `BaseException` MUST 包含 `code`(错误码)和 `message`(错误描述)属性。
3. 异常层次 MUST 遵循以下结构:
   - `BaseException`
     - `BusinessException` — 业务逻辑异常(可预期的业务规则违反)
     - `ValidationException` — 参数校验异常(输入不合法)
     - `SystemException` — 系统级异常(不可预期的技术问题)
     - `UnauthorizedException` — 认证异常(未登录或 Token 失效)
     - `AccessDeniedException` — 权限异常(已登录但权限不足)
4. `BusinessException` 和 `ValidationException` MUST 标记为已知操作异常(`isOperational = true`)。
5. 新增异常类型 MUST 在全局异常处理器中注册对应的处理逻辑。

**示例:**

```java
// 异常基类
public abstract class BaseException extends RuntimeException {
    private final String code;

    protected BaseException(String code, String message) {
        super(message);
        this.code = code;
    }

    protected BaseException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

// 业务异常
public class BusinessException extends BaseException {
    public BusinessException(String code, String message) {
        super(code, message);
    }
}

// 参数校验异常
public class ValidationException extends BaseException {
    public ValidationException(String code, String message) {
        super(code, message);
    }
}

// 系统异常
public class SystemException extends BaseException {
    public SystemException(String code, String message) {
        super(code, message);
    }

    public SystemException(String code, String message, Throwable cause) {
        super(code, message, cause);
    }
}
```

```typescript
// TypeScript 异常层次
class BaseError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly isOperational: boolean = false,
    public readonly statusCode: number = 500,
  ) {
    super(message);
    this.name = this.constructor.name;
    Error.captureStackTrace(this, this.constructor);
  }
}

class BusinessError extends BaseError {
  constructor(code: string, message: string) {
    super(code, message, true, 400);
  }
}

class ValidationError extends BaseError {
  constructor(code: string, message: string) {
    super(code, message, true, 400);
  }
}

class SystemError extends BaseError {
  constructor(code: string, message: string, cause?: Error) {
    super(code, message, false, 500);
    if (cause) this.stack = cause.stack;
  }
}
```

---

## 规范 UNI-EH-003: 异常到 HTTP 状态码映射

**规则:**

1. 异常类型与 HTTP 状态码 MUST 遵循以下映射关系:

| 异常类型 | HTTP 状态码 | 说明 |
|---------|------------|------|
| `ValidationException` | `400 Bad Request` | 参数校验失败 |
| `BusinessException` | `400 Bad Request` | 业务规则违反 |
| `UnauthorizedException` | `401 Unauthorized` | 未认证 |
| `AccessDeniedException` | `403 Forbidden` | 权限不足 |
| 资源不存在 | `404 Not Found` | 请求的资源不存在 |
| 资源冲突 | `409 Conflict` | 重复创建等冲突 |
| `SystemException` | `500 Internal Server Error` | 系统内部错误 |
| 第三方调用失败 | `502 Bad Gateway` | 上游服务异常 |
| 服务不可用 | `503 Service Unavailable` | 服务维护或过载 |

2. 禁止将所有异常统一映射为 `200 OK` 或 `500`。
3. 自定义异常 MUST 在全局处理器中声明明确的 HTTP 状态码。

---

## 规范 UNI-EH-004: 错误响应格式一致性

**规则:**

1. 所有错误响应 MUST 使用 UNI-AD-003 定义的统一响应格式,包含 `code`、`message`、`data`(null)、`requestId`。
2. 参数校验失败时,MUST 在 `message` 中提供所有校验失败字段的详细信息。
3. 系统内部错误 MUST 对终端用户隐藏技术细节(如 SQL 语句、堆栈信息),仅返回通用提示。
4. 错误信息 MUST 对调用方友好,便于前端展示和排查。

**示例:**

```json
// 参数校验失败 — 多字段错误
{
  "code": "A00001",
  "message": "参数校验失败",
  "data": {
    "errors": [
      { "field": "userName", "message": "用户名不能为空" },
      { "field": "email", "message": "邮箱格式不正确" },
      { "field": "age", "message": "年龄须在 1-200 之间" }
    ]
  },
  "requestId": "req-abc123"
}

// 业务异常
{
  "code": "A01001",
  "message": "用户名已存在,请更换后重试",
  "data": null,
  "requestId": "req-abc123"
}

// 系统异常 — 隐藏技术细节
{
  "code": "B00001",
  "message": "系统内部异常,请稍后重试",
  "data": null,
  "requestId": "req-abc123"
}
```

```java
// Spring: 参数校验异常处理
@ExceptionHandler(MethodArgumentNotValidException.class)
public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(
        MethodArgumentNotValidException ex) {
    List<Map<String, String>> errors = ex.getBindingResult()
        .getFieldErrors()
        .stream()
        .map(fe -> Map.of(
            "field", fe.getField(),
            "message", fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "校验失败"
        ))
        .toList();

    Map<String, Object> data = Map.of("errors", errors);
    return ResponseEntity.badRequest()
        .body(ApiResponse.failWithData("A00001", "参数校验失败", data));
}
```

---

## 规范 UNI-EH-005: 异常日志规则

**规则:**

1. **已知业务异常**(`BusinessException`、`ValidationException`)MUST 记录 WARN 级别日志,包含错误码、错误信息和请求 URI。
2. **系统异常**(`SystemException`、未捕获 `Exception`)MUST 记录 ERROR 级别日志,包含完整堆栈信息。
3. **第三方调用异常** MUST 记录 ERROR 级别日志,包含调用的服务名、方法名、请求参数和耗时。
4. 异常日志 MUST 包含 `traceId`(通过 MDC 自动注入),便于链路追踪。
5. 禁止在循环中重复记录相同异常,SHOULD 在循环外统一处理。

**示例:**

```java
// 业务异常: WARN 级别,无需堆栈
catch (BusinessException e) {
    log.warn("业务处理失败,code: {},message: {},uri: {},traceId: {}",
        e.getCode(), e.getMessage(), requestUri, MDC.get("traceId"));
}

// 系统异常: ERROR 级别,包含堆栈
catch (Exception e) {
    log.error("系统未知异常,uri: {},traceId: {}",
        requestUri, MDC.get("traceId"), e);
    // e 作为最后一个参数,自动输出完整堆栈
}

// 第三方调用异常
catch (HttpClientErrorException e) {
    log.error("第三方服务调用失败,service: {},method: {},statusCode: {},duration: {}ms",
        serviceName, methodName, e.getStatusCode(), duration, e);
}
```

---

## 规范 UNI-EH-006: 异常处理反模式

**规则:**

1. **禁止捕获 `Exception` 或 `Throwable` 后不做任何处理**(吞掉异常):所有 `catch` 块 MUST 至少记录日志或重新抛出。
2. **禁止将异常用于正常的业务流程控制**:异常仅用于真正的异常情况,禁止用 `try-catch` 代替 `if` 判断。
3. **禁止在 `finally` 块中返回值**:`finally` 块仅用于资源清理。
4. **禁止使用 `e.printStackTrace()`**:MUST 使用日志框架输出异常信息。
5. **禁止在循环体内抛出异常作为流程控制**:异常创建和抛出有性能开销。
6. 捕获异常后 MUST 处理或转换为更明确的异常重新抛出,禁止仅仅打印日志后继续执行(除非是明确的容错场景)。

**示例:**

```java
// BAD: 吞掉异常
try {
    userService.createUser(request);
} catch (Exception e) {
    // 什么都不做 — 严重问题!
}

// BAD: 异常作为流程控制
try {
    User user = userService.getUserById(id);
} catch (ResourceNotFoundException e) {
    user = new User();  // 应该用 if 判断
}

// BAD: e.printStackTrace()
catch (Exception e) {
    e.printStackTrace();  // 禁止!使用 log.error
}

// BAD: 捕获后仅打印日志,继续执行(可能产生不可预期后果)
try {
    orderService.pay(orderId);
} catch (Exception e) {
    log.error("支付失败", e);
    // 继续执行后续逻辑 — 危险!
}

// GOOD: 捕获后转换为明确的业务异常重新抛出
try {
    thirdPartyClient.call(params);
} catch (TimeoutException e) {
    throw new SystemException("C00001", "第三方服务调用超时", e);
}

// GOOD: 明确的容错场景(记录日志 + 降级处理)
try {
    cacheService.refresh(key);
} catch (Exception e) {
    log.warn("缓存刷新失败,使用旧缓存,key: {}", key, e);
    // 容错:降级使用旧缓存,记录 WARN 日志
}
```
