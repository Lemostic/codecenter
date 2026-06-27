| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | framework-base |
| 引入条件 | fingerprint.profiles contains 'spring-boot-base' |
| 适用场景 | 所有 Spring Boot 项目（MVC / DDD 均适用） |
| 依赖规范 | L0 通用规范 |

# Spring Boot 基础规范

本包定义所有 Spring Boot 项目的通用基础约定，与架构风格（MVC/DDD）和持久化技术无关。

## 依赖注入规范

**PROF-BASE-001** 所有 Spring 托管组件 MUST 使用构造器注入（@RequiredArgsConstructor），MUST NOT 使用 @Autowired 字段注入。 [MUST]

| 原因 | 说明 |
|------|------|
| 不可变依赖 | final 字段，避免运行时 NPE |
| 测试友好 | 无需 Spring 容器即可 Mock |
| 代码即文档 | 依赖关系一目了然 |
| SOLID 原则 | 显式依赖，符合依赖注入最佳实践 |

```java
// 正确：构造器注入
@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final EventPublisher eventPublisher;
}

// 错误：字段注入
@Service
public class OrderService {
    @Autowired  // 禁止
    private OrderRepository orderRepository;
}
```

**PROF-BASE-002** Spring Bean 名称 MUST 遵循驼峰命名，与类名一致（首字母小写）。 [MUST]

**PROF-BASE-003** `@Configuration` 类名 MUST 以 `Config` 结尾，描述配置范围。 [MUST]

```java
@Configuration
public class WebMvcConfig { }

@Configuration
public class MybatisPlusConfig { }
```

**PROF-BASE-004** `application.yml` 中自定义配置项 SHOULD 使用 kebab-case（短横线分隔）。 [SHOULD]

```yaml
app:
  upload-dir: /data/uploads
  max-retry-count: 3
  jwt:
    secret-key: ${JWT_SECRET}
    expire-hours: 24
```

## 统一响应封装

**PROF-BASE-005** 所有接口 MUST 使用统一响应包装类 `Result<T>`，MUST NOT 直接返回裸对象。 [MUST]

```java
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    public static <T> Result<T> ok(T data) {
        Result<T> r = new Result<>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    public static Result<Void> ok() {
        return ok(null);
    }

    public static <T> Result<T> fail(int code, String message) {
        Result<T> r = new Result<>();
        r.code = code;
        r.message = message;
        return r;
    }
}
```

**PROF-BASE-006** 分页结果 MUST 使用专用 `PageResult<T>` 封装，包含总数与列表。 [MUST]

```java
@Data
public class PageResult<T> {
    private long total;
    private List<T> records;
    private int pageNum;
    private int pageSize;
}
```

## 全局异常处理

**PROF-BASE-007** 项目 MUST 提供 `@RestControllerAdvice` 全局异常处理器，统一错误响应格式。 [MUST]

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        log.warn("业务异常: {}", e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return Result.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统内部错误");
    }
}
```

**PROF-BASE-008** 错误码 MUST 遵循统一编码规则：`{层级}_{模块}_{具体错误}`，大写 + 下划线。 [MUST]

| 错误码 | 含义 | HTTP 状态码 |
|--------|------|-----------|
| SYS_ERR | 未知系统异常 | 500 |
| VAL_REQUIRED | 必填参数缺失 | 400 |
| BIZ_NOT_FOUND | 资源不存在 | 404 |
| BIZ_DUPLICATE | 资源重复 | 409 |
| BIZ_STATUS | 状态不合法 | 400 |
| BIZ_CONCURRENT | 并发冲突 | 409 |
| INFRA_DB | 数据库异常 | 500 |
| INFRA_RPC | 远程调用异常 | 502 |

**PROF-BASE-009** Controller MUST NOT 捕获异常后返回自定义错误，统一由全局异常处理器处理。 [MUST]

## 事务管理

**PROF-BASE-010** `@Transactional` MUST 加在 Service 方法上，MUST NOT 加在 Controller 或 Mapper/Repository 上。 [MUST]

**PROF-BASE-011** 写操作方法 MUST 显式声明 `rollbackFor = Exception.class`，MUST NOT 依赖默认回滚策略。 [MUST]

**PROF-BASE-012** 只读查询方法 SHOULD 使用 `@Transactional(readOnly = true)` 优化性能。 [SHOULD]

```java
@Service
public class UserServiceImpl implements UserService {

    @Override
    @Transactional(readOnly = true)
    public UserVO getById(Long id) {
        // ...
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(UserCreateDTO dto) {
        // 多表写入操作，需要事务保护
        userMapper.insert(entity);
        userRoleMapper.insertBatch(roles);
        return entity.getId();
    }
}
```

**PROF-BASE-013** 事务方法内部 MUST NOT 调用外部 HTTP 接口或 MQ 发送，避免长事务。 [MUST]

```java
// 错误：事务内调用远程服务
@Transactional(rollbackFor = Exception.class)
public void create(UserCreateDTO dto) {
    userMapper.insert(entity);
    httpService.notifyOtherSystem(entity);  // 禁止：远程调用延长事务时间
}

// 正确：事务提交后发送消息
@Transactional(rollbackFor = Exception.class)
public void create(UserCreateDTO dto) {
    userMapper.insert(entity);
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                httpService.notifyOtherSystem(entity);
            }
        });
}
```

## 测试结构

**PROF-BASE-014** 测试类 MUST 与主代码包结构一一对应。 [MUST]

**PROF-BASE-015** Service 层测试 SHOULD 使用 Mock 替代持久层依赖，Controller 层测试 SHOULD 使用 MockMvc。 [SHOULD]

**PROF-BASE-016** 测试 MUST 遵循 Given-When-Then 结构，测试方法名 SHOULD 使用 `should_{{行为}}_when_{{条件}}` 格式。 [MUST/SHOULD]

## 通用编码约定

**PROF-BASE-017** 查询条件 MUST 使用参数化查询，MUST NOT 拼接 SQL 字符串，防止注入。 [MUST]

**PROF-BASE-018** SQL MUST NOT 使用 `SELECT *`，MUST 显式列出所需字段。 [MUST]

**PROF-BASE-019** 批量操作 MUST 使用批量方法（如 `saveBatch` / `insertBatch`），MUST NOT 在循环中逐条执行。 [MUST]

**PROF-BASE-020** `common` / `utils` 包 MUST NOT 依赖任何业务包，仅包含通用工具类、枚举、常量。 [MUST]

**PROF-BASE-021** 无 `e.printStackTrace()`，全部使用日志框架。 [MUST]

**PROF-BASE-022** 可选返回值使用 `Optional<T>`。 [MUST]

**PROF-BASE-023** 金额使用 `BigDecimal`，禁止 `Double` / `Float`。 [MUST]

**PROF-BASE-024** 日期使用 `LocalDateTime` / `Instant`，禁止 `java.util.Date`。 [MUST]
