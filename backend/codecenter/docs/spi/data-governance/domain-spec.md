| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L2 |
| 引入条件 | domains contains 'data-governance' |
| 适用架构 | 数据管理领域（架构无关） |
| 依赖规范 | naming-conventions, api-design, logging-standards |
| 互斥规范 | 无 |

# 数据管理领域规范

> 版本: 1.0 | 状态: Active | 最后更新: 2026-06-17

---

## 一、概述

本规范定义数据管理（Data Governance）领域的通用编码规则，适用于所有涉及数据采集、存储、加工、消费链路的项目。规范条目按优先级分为：

- **MUST（强制）**：违反将导致代码审查不通过，且 Skill 引擎会主动拦截
- **SHOULD（推荐）**：强烈建议遵守，违反需在 PR 描述中说明理由
- **MAY（可选）**：根据项目实际情况选择，不影响合规评分

---

## 二、数据质量校验规则

### 规则 DG-001：数据写入前必须执行完整性校验

**优先级：MUST**

任何向持久化存储写入业务数据的操作，必须在写入前对关键字段执行完整性校验。完整性校验至少覆盖以下三个维度：

- **非空性**：业务必填字段不得为 null 或空字符串
- **格式合法性**：字段值必须符合预定义的数据格式（如日期、手机号、身份证号等）
- **值域合法性**：枚举类字段必须在有效值范围内

```java
// 正例：写入前执行完整性校验
public class UserOrderValidator implements DataValidator<UserOrder> {

    @Override
    public ValidationResult validate(UserOrder order) {
        ValidationResult result = new ValidationResult();

        // 非空性校验
        result.requireNotNull(order.getUserId(), "userId 不能为空");
        result.requireNotNull(order.getAmount(), "amount 不能为空");

        // 格式合法性校验
        result.requirePattern(order.getPhone(), "^1[3-9]\\d{9}$", "手机号格式不合法");

        // 值域合法性校验
        result.requireIn(order.getStatus(),
            List.of("PENDING", "PAID", "SHIPPED", "COMPLETED"),
            "订单状态值不在有效范围内");

        return result;
    }
}

@Service
public class OrderService {

    private final UserOrderValidator validator;
    private final OrderRepository repository;

    public void createOrder(UserOrder order) {
        ValidationResult result = validator.validate(order);
        if (!result.isValid()) {
            throw new DataQualityException("数据完整性校验失败: " + result.getErrors());
        }
        repository.save(order);
    }
}
```

```java
// 反例：直接写入，无任何校验
@Service
public class OrderService {

    @Autowired
    private OrderRepository repository;

    public void createOrder(UserOrder order) {
        // 缺少完整性校验，脏数据直接入库
        repository.save(order);
    }
}
```

---

### 规则 DG-002：数据质量指标必须注册到质量监控平台

**优先级：MUST**

每个数据模型（表、主题域）必须注册至少一个质量指标，覆盖以下维度之一：

- **完整性（Completeness）**：非空率、缺失率
- **一致性（Consistency）**：跨表关联一致性、枚举值一致性
- **及时性（Timeliness）**：数据延迟、更新频率偏差

```java
// 正例：通过注解声明质量指标，由框架自动注册
@DataQualityMetric(
    domain = "order",
    entity = "UserOrder",
    dimensions = {
        @Metric(name = "completeness", field = "userId", threshold = 0.99),
        @Metric(name = "consistency",  field = "status", refTable = "order_status_dict"),
        @Metric(name = "timeliness",   field = "createTime", maxDelayMinutes = 5)
    }
)
@Entity
public class UserOrder {
    // ...
}
```

```java
// 反例：实体类无任何质量指标声明，质量问题无法被监控发现
@Entity
public class UserOrder {
    // 字段定义...
}
```

---

### 规则 DG-003：校验失败必须记录结构化质量事件

**优先级：MUST**

数据质量校验失败时，必须记录结构化的质量事件日志，包含：失败规则、字段名、期望值、实际值、时间戳、数据源标识。

```java
// 正例：结构化质量事件记录
public class DataQualityLogger {

    private static final Logger QUALITY_LOG = LoggerFactory.getLogger("data.quality");

    public static void logViolation(String rule, String field,
                                     Object expected, Object actual, String source) {
        QualityEvent event = QualityEvent.builder()
            .ruleId(rule)
            .fieldName(field)
            .expectedValue(String.valueOf(expected))
            .actualValue(String.valueOf(actual))
            .timestamp(Instant.now())
            .sourceId(source)
            .build();

        QUALITY_LOG.info("{}", event.toJson());
    }
}
```

---

## 三、元数据标注规范

### 规则 DG-004：所有数据字段必须附带业务描述注解

**优先级：MUST**

每个持久化数据模型的字段必须通过 `@FieldMeta` 注解声明业务含义，包含：中文名称、业务定义、数据所有者、敏感等级。

```java
// 正例：字段元数据完整标注
@Entity
public class CustomerProfile {

    @FieldMeta(
        cnName = "客户姓名",
        description = "客户在系统中登记的法定姓名",
        owner = "customer-team",
        sensitivity = SensitivityLevel.PII
    )
    private String customerName;

    @FieldMeta(
        cnName = "客户等级",
        description = "根据近12个月消费金额计算的等级，取值范围 A/B/C/D",
        owner = "customer-team",
        sensitivity = SensitivityLevel.INTERNAL
    )
    private String grade;
}
```

```java
// 反例：字段无注解，元数据缺失
@Entity
public class CustomerProfile {
    private String customerName;  // 含义不明，无所有者信息
    private String grade;
}
```

---

### 规则 DG-005：数据血缘关系必须通过注解声明

**优先级：SHOULD**

当某字段的数据来源于其他表或外部系统时，应使用 `@DataLineage` 注解声明血缘关系，便于数据影响分析和变更评估。

```java
// 正例：声明字段血缘来源
@Entity
public class AggregatedSalesReport {

    @DataLineage(
        sourceTable = "fact_order",
        sourceField = "order_amount",
        transformType = TransformType.SUM,
        transformDesc = "按日汇总订单金额"
    )
    private BigDecimal dailyTotalAmount;

    @DataLineage(
        sourceTable = "dim_customer",
        sourceField = "customer_region",
        transformType = TransformType.DIRECT_COPY,
        transformDesc = "直接引用客户区域维度"
    )
    private String region;
}
```

---

## 四、数据血缘追踪 API 规范

### 规则 DG-006：血缘查询接口必须遵循统一契约

**优先级：MUST**

对外暴露的数据血缘查询 API 必须实现 `LineageQueryable` 接口，返回标准血缘结构（包含上游节点、下游节点、变换规则）。

```java
// 正例：统一血缘查询接口
public interface LineageQueryable {

    /**
     * 查询指定字段的上游血缘链路
     * @param tableName  表名
     * @param fieldName  字段名
     * @param depth      追踪深度（1=直接上游，N=N 层上游）
     * @return 标准血缘图结构
     */
    LineageGraph queryUpstream(String tableName, String fieldName, int depth);

    /**
     * 查询指定字段的下游影响范围
     * @param tableName  表名
     * @param fieldName  字段名
     * @param depth      追踪深度
     * @return 标准血缘图结构
     */
    LineageGraph queryDownstream(String tableName, String fieldName, int depth);
}

// 标准返回结构
public class LineageGraph {
    private LineageNode rootNode;
    private List<LineageEdge> edges;
    private List<LineageNode> nodes;
}

public class LineageNode {
    private String nodeId;       // 格式: {systemId}.{tableName}.{fieldName}
    private String systemId;
    private String tableName;
    private String fieldName;
    private String nodeType;     // TABLE, FIELD, REPORT, API
}

public class LineageEdge {
    private String fromNodeId;
    private String toNodeId;
    private String transformType;  // DIRECT_COPY, AGGREGATE, DERIVED, JOIN
    private String transformDesc;
}
```

---

### 规则 DG-007：血缘变更必须触发通知

**优先级：SHOULD**

当数据模型的字段血缘关系发生变化（新增、删除、修改转换规则）时，应通过事件总线发布 `LineageChangeEvent`，供下游消费方感知变更。

```java
// 正例：血缘变更事件发布
@Service
public class SchemaChangeNotifier {

    private final EventBus eventBus;

    public void notifyLineageChange(String tableName, String fieldName,
                                     LineageChangeType changeType) {
        LineageChangeEvent event = LineageChangeEvent.builder()
            .tableName(tableName)
            .fieldName(fieldName)
            .changeType(changeType)
            .timestamp(Instant.now())
            .build();
        eventBus.publish("lineage.change", event);
    }
}
```

---

## 五、数据分类与敏感等级

### 规则 DG-008：数据必须按敏感等级分类并执行对应保护策略

**优先级：MUST**

所有持久化数据必须按以下四级分类，并在 `@FieldMeta` 注解的 `sensitivity` 属性中声明：

| 等级 | 标识 | 说明 | 保护策略 |
|------|------|------|----------|
| L1 | `PUBLIC` | 可对外公开的数据 | 无特殊限制 |
| L2 | `INTERNAL` | 仅限内部使用 | 接口不对外暴露 |
| L3 | `CONFIDENTIAL` | 敏感业务数据 | 传输加密，访问鉴权 |
| L4 | `PII` | 个人身份信息 | 存储脱敏，最小化采集，GDPR/个保法合规 |

```java
// 正例：敏感等级声明与对应保护策略联动
public enum SensitivityLevel {
    PUBLIC(1, "公开数据", ProtectionStrategy.NONE),
    INTERNAL(2, "内部数据", ProtectionStrategy.ACCESS_CONTROL),
    CONFIDENTIAL(3, "敏感数据", ProtectionStrategy.ENCRYPT_IN_TRANSIT),
    PII(4, "个人身份信息", ProtectionStrategy.ENCRYPT_AT_REST_AND_AUDIT);

    private final int level;
    private final String label;
    private final ProtectionStrategy strategy;
}

// 框架层拦截器：根据敏感等级自动执行保护策略
@Aspect
@Component
public class DataProtectionInterceptor {

    @Around("@annotation(fieldMeta)")
    public Object enforceProtection(ProceedingJoinPoint pjp, FieldMeta fieldMeta) throws Throwable {
        SensitivityLevel level = fieldMeta.sensitivity();
        if (level == SensitivityLevel.PII) {
            AuditLogger.logAccess(fieldMeta.cnName(), getCurrentUser());
        }
        Object result = pjp.proceed();
        if (level.ordinal() >= SensitivityLevel.CONFIDENTIAL.ordinal()) {
            result = maskSensitiveData(result, level);
        }
        return result;
    }
}
```

---

### 规则 DG-009：禁止在日志中打印 L3 及以上敏感数据

**优先级：MUST**

日志输出（包括 DEBUG/INFO/ERROR 各级别）不得包含 `CONFIDENTIAL` 或 `PII` 级别字段的原始值。必须使用脱敏工具类处理后输出。

```java
// 正例：使用脱敏工具类处理后再记录日志
log.info("用户注册成功，手机号: {}", MaskUtils.maskPhone(user.getPhone()));
// 输出：用户注册成功，手机号: 138****5678

// 反例：直接打印原始值
log.info("用户注册成功，手机号: {}", user.getPhone());
// 输出：用户注册成功，手机号: 13812345678（敏感数据泄露）
```

---

## 六、数据画像与监控钩子

### 规则 DG-010：关键数据表必须注册画像采集钩子

**优先级：SHOULD**

核心业务表（被 2 个及以上下游系统消费的表）应注册数据画像（Data Profiling）采集钩子，定期自动统计并上报以下指标：

- 记录总数与增量变化趋势
- 各字段的空值率、唯一值数量、值分布
- 数值字段的均值、中位数、标准差、最大最小值

```java
// 正例：通过注解声明画像采集钩子
@DataProfileHook(
    tableName = "fact_order",
    schedule = "0 2 * * *",  // 每日凌晨 2 点采集
    fields = {
        @ProfileField(name = "order_amount", metrics = {MEAN, MEDIAN, STDDEV, MIN, MAX}),
        @ProfileField(name = "status",       metrics = {VALUE_DISTRIBUTION, NULL_RATE}),
        @ProfileField(name = "create_time",  metrics = {NULL_RATE, MIN, MAX})
    }
)
@Entity
public class FactOrder {
    // ...
}
```

---

### 规则 DG-011：监控指标必须配置告警阈值

**优先级：SHOULD**

与 DG-002（质量指标）和 DG-010（画像钩子）配合使用，所有已注册的质量指标和画像指标应配置告警阈值，当指标偏离基线超过阈值时触发告警。

```java
// 正例：为质量指标配置告警阈值
@DataQualityMetric(
    domain = "order",
    entity = "UserOrder",
    dimensions = {
        @Metric(
            name = "completeness",
            field = "userId",
            threshold = 0.99,
            alert = @Alert(
                condition = "below_threshold",
                severity = AlertSeverity.WARNING,
                notifyChannels = {"dingtalk:data-team", "email:data-quality@example.com"}
            )
        )
    }
)
```

---

## 七、验收标准

| 验收项 | 标准 |
|--------|------|
| 完整性校验覆盖 | 所有写入操作均有 Validator 实现 |
| 元数据注解覆盖 | 100% 持久化字段有 `@FieldMeta` |
| 敏感等级声明 | 所有字段有明确 sensitivity 级别 |
| 血缘关系声明 | 衍生字段均有 `@DataLineage` 注解 |
| 日志脱敏 | L3/L4 字段零原始值出现在日志中 |

---

## 八、变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-17 | 初始版本，包含 11 条核心规则 |
