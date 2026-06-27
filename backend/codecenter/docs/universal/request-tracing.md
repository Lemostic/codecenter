# 请求链路与 AOP 规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-LS(日志规范)、UNI-EH(异常处理规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-RT-001: traceId 生成与传播

**规则:**

1. 每个进入系统的请求 MUST 被分配全局唯一的 `traceId`。
2. `traceId` 生成规则:
   - 方式一(推荐):使用 UUID v4(如 `a1b2c3d4-e5f6-7890-abcd-ef1234567890`)。
   - 方式二:使用 Snowflake 算法生成 64 位唯一 ID,格式化为十六进制字符串。
3. `traceId` MUST 通过 HTTP 请求头 `X-Trace-Id` 传播:
   - 若请求已包含 `X-Trace-Id` 头,MUST 复用该值(上游服务已生成)。
   - 若请求不包含 `X-Trace-Id` 头,MUST 生成新的 traceId 并注入响应头。
4. 响应 MUST 在 `X-Trace-Id` 响应头中返回 traceId,便于前端和调用方追踪。
5. `traceId` MUST 注入到 MDC(Mapped Diagnostic Context),使所有日志自动携带该字段。

**示例:**

```java
// Spring Filter: traceId 注入
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceFilter extends OncePerRequestFilter {

    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }

        // 注入 MDC,所有日志自动携带 traceId
        MDC.put("traceId", traceId);
        // 注入响应头
        response.setHeader(TRACE_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("traceId");
        }
    }
}
```

```typescript
// Express: traceId 中间件
import { v4 as uuidv4 } from 'uuid';
import asyncHooks from 'async_hooks';

const TRACE_HEADER = 'x-trace-id';

function traceMiddleware(req: Request, res: Response, next: NextFunction): void {
  const traceId = (req.headers[TRACE_HEADER] as string) || uuidv4().replace(/-/g, '');

  // 注入请求对象
  (req as any).traceId = traceId;
  // 注入响应头
  res.setHeader(TRACE_HEADER, traceId);
  // 注入 AsyncLocalStorage (类似 MDC)
  asyncLocalStorage.run({ traceId }, next);
}
```

---

## 规范 UNI-RT-002: spanId 子调用标识

**规则:**

1. 每次子调用(数据库查询、外部 API 调用、消息发送)MUST 生成独立的 `spanId`。
2. `spanId` MUST 为短唯一标识,建议使用 8 位随机十六进制字符串(如 `a1b2c3d4`)。
3. `spanId` MUST 注入 MDC,且在子调用完成后 MUST 从 MDC 中移除。
4. 嵌套子调用 MUST 保持 `spanId` 的独立性,子调用结束 MUST 恢复父调用的 `spanId`。

**示例:**

```java
public class SpanContext {

    private static final String SPAN_ID_KEY = "spanId";

    public static <T> T withSpan(String spanName, Supplier<T> action) {
        String spanId = generateSpanId();
        MDC.put(SPAN_ID_KEY, spanId);
        try {
            return action.get();
        } finally {
            MDC.remove(SPAN_ID_KEY);
        }
    }

    private static String generateSpanId() {
        return Long.toHexString(ThreadLocalRandom.current().nextLong())
            .substring(0, 8);
    }
}

// 使用示例
public UserResponse getUserWithOrders(Long userId) {
    User user = SpanContext.withSpan("db-query-user",
        () -> userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException("A00004", "用户不存在")));

    List<Order> orders = SpanContext.withSpan("db-query-orders",
        () -> orderRepository.findByUserId(userId));

    return UserResponse.from(user, orders);
}
```

---

## 规范 UNI-RT-003: MDC 集成

**规则:**

1. 所有服务 MUST 在请求入口处将 `traceId` 注入 MDC。
2. 对于异步操作(线程池、CompletableFuture),MUST 在提交任务时传递 MDC 上下文,因为 MDC 基于 `ThreadLocal`,不会自动传播到新线程。
3. MDC 中 MUST 包含以下字段:
   - `traceId` — 请求链路标识
   - `spanId` — 当前子调用标识(可选)
   - `userId` — 当前操作用户(脱敏后,认证后注入)
4. 请求结束时 MUST 清理 MDC,防止线程复用导致的数据污染。
5. 日志框架配置 MUST 输出 MDC 字段(详见 UNI-LS-001)。

**示例:**

```java
// 异步任务 MDC 传递
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 在主线程中捕获 MDC 上下文
        Map<String, String> contextMap = MDC.getCopyOfContextMap();
        return () -> {
            try {
                // 在工作线程中恢复 MDC 上下文
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}

// 配置线程池使用 MDC 装饰器
@Configuration
public class AsyncConfig {

    @Bean
    public TaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(200);
        executor.setTaskDecorator(new MdcTaskDecorator());
        executor.initialize();
        return executor;
    }
}
```

```java
// CompletableFuture MDC 传递
public CompletableFuture<OrderResponse> asyncCreateOrder(CreateOrderRequest request) {
    Map<String, String> mdcContext = MDC.getCopyOfContextMap();

    return CompletableFuture.supplyAsync(() -> {
        MDC.setContextMap(mdcContext);
        try {
            return orderService.createOrder(request);
        } finally {
            MDC.clear();
        }
    }, taskExecutor);
}
```

---

## 规范 UNI-RT-004: AOP 切面设计 — 请求日志

**规则:**

1. MUST 通过 AOP 切面自动记录所有 Controller 层方法的入口和出口日志,禁止在业务代码中手动编写。
2. 入口日志 MUST 包含:类名、方法名、请求参数(脱敏后)。
3. 出口日志 MUST 包含:类名、方法名、执行耗时(毫秒)、响应摘要。
4. 异常日志 MUST 包含:类名、方法名、执行耗时、异常类型和消息。
5. 参数和响应对象过大时 SHOULD 截断(默认上限 1024 字符),避免日志膨胀。
6. AOP 切面 MUST 不影响业务逻辑,切面异常 MUST 被捕获并记录,禁止传播到业务代码。

**示例:**

```java
@Aspect
@Component
@Slf4j
public class RequestLogAspect {

    private static final int MAX_LOG_LENGTH = 1024;

    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object logRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = truncate(toJson(joinPoint.getArgs()));

        log.info("[入口] {}.{}(),参数: {}", className, methodName, args);

        long startTime = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            String response = truncate(toJson(result));

            log.info("[出口] {}.{}(),耗时: {}ms,响应: {}",
                className, methodName, duration, response);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[异常] {}.{}(),耗时: {}ms,异常: {}",
                className, methodName, duration, ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }

    private String truncate(String text) {
        if (text == null) return "null";
        return text.length() > MAX_LOG_LENGTH
            ? text.substring(0, MAX_LOG_LENGTH) + "...(truncated)"
            : text;
    }

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
```

```typescript
// NestJS: 请求日志拦截器
@Injectable()
export class RequestLogInterceptor implements NestInterceptor {
  private readonly logger = new Logger('RequestLog');

  intercept(context: ExecutionContext, next: CallHandler): Observable<any> {
    const request = context.switchToHttp().getRequest();
    const { method, url, body } = request;
    const className = context.getClass().name;
    const handlerName = context.getHandler().name;

    this.logger.log(`[入口] ${className}.${handlerName}() ${method} ${url}`);

    const startTime = Date.now();
    return next.handle().pipe(
      tap((result) => {
        const duration = Date.now() - startTime;
        this.logger.log(
          `[出口] ${className}.${handlerName}() 耗时: ${duration}ms`
        );
      }),
      catchError((err) => {
        const duration = Date.now() - startTime;
        this.logger.error(
          `[异常] ${className}.${handlerName}() 耗时: ${duration}ms 异常: ${err.message}`
        );
        throw err;
      }),
    );
  }
}
```

---

## 规范 UNI-RT-005: 跨服务链路传播

**规则:**

1. 服务间调用 MUST 通过 HTTP 请求头传递 `traceId`(`X-Trace-Id`)和 `spanId`(`X-Span-Id`)。
2. 下游服务 MUST 从请求头中提取 `traceId`,复用而非重新生成。
3. HTTP 客户端(Feign、RestTemplate、Axios)MUST 配置拦截器自动注入链路头。
4. 消息队列(RabbitMQ、Kafka)MUST 在消息头中携带 `traceId`,消费端 MUST 从消息头中恢复 traceId 到 MDC。
5. gRPC 调用 MUST 通过 Metadata 传递链路头。

**示例:**

```java
// Feign 拦截器: 自动注入链路头
@Component
public class TraceFeignInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            template.header("X-Trace-Id", traceId);
        }
        String spanId = MDC.get("spanId");
        if (spanId != null) {
            template.header("X-Span-Id", spanId);
        }
    }
}
```

```java
// RestTemplate 拦截器
@Component
public class TraceRestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution)
            throws IOException {
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            request.getHeaders().set("X-Trace-Id", traceId);
        }
        return execution.execute(request, body);
    }
}
```

```java
// Kafka 消息: 生产端注入 traceId
public void sendMessage(String topic, Object payload) {
    Headers headers = new RecordHeaders();
    String traceId = MDC.get("traceId");
    if (traceId != null) {
        headers.add("X-Trace-Id", traceId.getBytes(StandardCharsets.UTF_8));
    }
    kafkaTemplate.send(new ProducerMessage<>(topic, payload, headers));
}

// Kafka 消费端: 恢复 traceId
@KafkaListener(topics = "order-events")
public void onMessage(ConsumerRecord<String, String> record) {
    Header traceHeader = record.headers().lastHeader("X-Trace-Id");
    String traceId = traceHeader != null
        ? new String(traceHeader.value(), StandardCharsets.UTF_8)
        : UUID.randomUUID().toString();

    MDC.put("traceId", traceId);
    try {
        processMessage(record.value());
    } finally {
        MDC.remove("traceId");
    }
}
```

---

## 规范 UNI-RT-006: 请求上下文对象设计

**规则:**

1. 项目 MUST 定义线程安全的 `RequestContext` 对象,封装请求链路中的公共信息。
2. `RequestContext` MUST 包含以下字段:
   - `traceId`(String) — 链路标识
   - `userId`(Long) — 当前用户 ID
   - `userRoles`(Set<String>) — 当前用户角色
   - `clientIp`(String) — 客户端 IP
   - `requestUri`(String) — 请求 URI
   - `startTime`(long) — 请求开始时间戳
3. `RequestContext` MUST 基于 `ThreadLocal` 实现(或 `TransmittableThreadLocal` 以支持线程池传递)。
4. 请求结束后 MUST 清理 `RequestContext`,防止线程复用导致数据泄漏。
5. 禁止在 `RequestContext` 中存放大型对象或业务数据。

**示例:**

```java
public class RequestContext {

    private static final ThreadLocal<RequestContext> HOLDER =
        new TransmittableThreadLocal<>();

    private String traceId;
    private Long userId;
    private Set<String> userRoles;
    private String clientIp;
    private String requestUri;
    private long startTime;

    public static RequestContext current() {
        return HOLDER.get();
    }

    public static void set(RequestContext context) {
        HOLDER.set(context);
    }

    public static void clear() {
        HOLDER.remove();
    }

    // Getters and Setters
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Set<String> getUserRoles() { return userRoles; }
    public void setUserRoles(Set<String> userRoles) { this.userRoles = userRoles; }
    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
    public String getRequestUri() { return requestUri; }
    public void setRequestUri(String requestUri) { this.requestUri = requestUri; }
    public long getStartTime() { return startTime; }
    public void setStartTime(long startTime) { this.startTime = startTime; }
}
```

```java
// Filter 中初始化 RequestContext
@Component
public class RequestContextFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        RequestContext ctx = new RequestContext();
        ctx.setTraceId(MDC.get("traceId"));
        ctx.setClientIp(getClientIp(request));
        ctx.setRequestUri(request.getRequestURI());
        ctx.setStartTime(System.currentTimeMillis());
        RequestContext.set(ctx);

        try {
            filterChain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0].trim();
    }
}
```

---

## 规范 UNI-RT-007: AOP 切面设计 — Service 层与外部调用

**规则:**

1. Service 层核心方法 SHOULD 通过 AOP 切面记录方法执行耗时和关键参数。
2. 所有外部服务调用(HTTP/RPC)MUST 通过 AOP 切面自动记录:调用目标、请求参数、响应状态、执行耗时。
3. 外部调用切面 MUST 在超时时自动记录超时阈值和已耗时,并抛出 `SystemException`。
4. 切面 MUST 使用 `@Around` 通知,确保在方法执行前后均能注入逻辑。
5. 切点表达式 MUST 使用注解驱动(自定义注解),避免硬编码包路径。

**示例:**

```java
// 自定义注解: 标记需要链路监控的方法
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Traced {
    String value() default "";  // 操作描述
}

@Aspect
@Component
@Slf4j
public class ServiceTraceAspect {

    @Around("@annotation(traced)")
    public Object traceMethod(ProceedingJoinPoint joinPoint, Traced traced) throws Throwable {
        String operation = traced.value().isEmpty()
            ? joinPoint.getSignature().toShortString()
            : traced.value();

        long startTime = System.currentTimeMillis();
        log.info("[调用开始] {},参数: {}", operation, toJson(joinPoint.getArgs()));

        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            log.info("[调用完成] {},耗时: {}ms", operation, duration);
            return result;
        } catch (Throwable ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[调用失败] {},耗时: {}ms,异常: {}",
                operation, duration, ex.getClass().getSimpleName(), ex);
            throw ex;
        }
    }
}

// 使用自定义注解
@Service
public class OrderService {

    @Traced("创建订单")
    public OrderResponse createOrder(CreateOrderRequest request) {
        // 业务逻辑...
    }

    @Traced("调用支付服务")
    public PaymentResponse callPaymentService(PaymentRequest request) {
        // 外部服务调用...
    }
}
```
