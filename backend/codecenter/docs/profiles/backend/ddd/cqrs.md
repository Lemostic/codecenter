| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | architecture-module |
| 引入条件 | fingerprint.profiles contains 'arch-ddd' |
| 所属架构包 | arch-ddd |
| 依赖规范 | arch-ddd (本包的父包) |

# 读写分离（CQRS）职责规范

## 核心原则

CQRS（Command Query Responsibility Segregation）将系统分为**命令端**（写）与**查询端**（读），各自独立演进。

```
┌──────────────────┐         ┌──────────────────┐
│   命令端 (Write)  │         │   查询端 (Read)   │
│                  │         │                  │
│  Controller      │         │  Controller      │
│       ↓          │         │       ↓          │
│  CommandAppService│        │  QueryAppService  │
│       ↓          │         │       ↓          │
│  Domain Model    │         │  直接查询DB       │
│  (聚合根+领域服务)│         │  (无领域层)       │
│       ↓          │         │       ↓          │
│  Repository      │  ──→   │  QueryRepository  │
│  (写模型持久化)   │  同步   │  (读模型查询)     │
└──────────────────┘         └──────────────────┘
```

## 命令端（Write Side）

**PROF-DDD-401** 命令端 MUST 基于聚合根进行写操作，通过领域模型保证业务一致性。 [MUST]

```java
// 命令端应用服务
@Service
@RequiredArgsConstructor
public class OrderCommandAppService {

    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateOrderCommand cmd) {
        // 通过聚合根工厂方法创建，执行业务规则校验
        Order order = Order.create(cmd.getUserId(), cmd.getItems());
        orderRepository.save(order);
        eventPublisher.publish(order.collectDomainEvents());
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long orderId) {
        Order order = orderRepository.findById(orderId);
        if (order == null) {
            throw new DomainException("订单不存在");
        }
        order.confirm();  // 聚合根方法内封装状态机逻辑
        orderRepository.save(order);
        eventPublisher.publish(order.collectDomainEvents());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancel(CancelOrderCommand cmd) {
        Order order = orderRepository.findById(cmd.getOrderId());
        if (order == null) {
            throw new DomainException("订单不存在");
        }
        order.cancel(cmd.getReason());
        orderRepository.save(order);
        eventPublisher.publish(order.collectDomainEvents());
    }
}
```

**PROF-DDD-402** 命令端 MUST 使用事务保证强一致性（同一聚合内）。 [MUST]

**PROF-DDD-403** 命令对象 MUST 包含完整操作意图，MUST NOT 包含查询结果数据。 [MUST]

```java
@Data
public class CreateOrderCommand {
    @NotNull
    private Long userId;

    @NotEmpty
    @Valid
    private List<OrderItemCommand> items;
}

@Data
public class OrderItemCommand {
    @NotBlank
    private String productId;

    @Min(1)
    private int quantity;

    @NotNull
    private BigDecimal unitPrice;
}
```

**PROF-DDD-404** 命令方法 MUST 返回聚合根ID或void，MUST NOT 返回聚合根完整数据（查询职责）。 [MUST]

```java
// 正确
public Long create(CreateOrderCommand cmd) { }    // 返回ID
public void confirm(Long orderId) { }             // 返回void
public void cancel(CancelOrderCommand cmd) { }    // 返回void

// 错误
public Order create(CreateOrderCommand cmd) { }   // 不应返回完整聚合
```

## 查询端（Read Side）

**PROF-DDD-405** 查询端 MUST 绕过领域层，直接查询数据库返回DTO，MUST NOT 加载聚合根。 [MUST]

```java
// 查询端应用服务
@Service
@RequiredArgsConstructor
public class OrderQueryAppService {

    private final OrderQueryRepository orderQueryRepository;  // 专用查询仓储

    public OrderDTO getById(Long id) {
        return orderQueryRepository.findOrderDTOById(id);
    }

    public PageResult<OrderDTO> page(OrderPageQuery query) {
        return orderQueryRepository.pageOrderDTO(query);
    }

    public List<OrderStatisticDTO> statistics(String startDate, String endDate) {
        return orderQueryRepository.orderStatistics(startDate, endDate);
    }
}
```

**PROF-DDD-406** 查询端 MUST NOT 包含任何业务逻辑，仅负责数据检索与格式化。 [MUST]

```java
// 正确：查询层直接返回DTO
public class OrderQueryRepositoryImpl implements OrderQueryRepository {

    @Override
    public OrderDTO findOrderDTOById(Long id) {
        // 直接SQL查询，JOIN多张表，返回扁平化DTO
        return orderMapper.selectOrderDTOById(id);
    }

    @Override
    public PageResult<OrderDTO> pageOrderDTO(OrderPageQuery query) {
        Page<OrderDTO> page = new Page<>(query.getPageNum(), query.getPageSize());
        return PageResult.of(orderMapper.selectOrderDTOPage(page, query));
    }
}

// 错误：查询层包含业务逻辑
public OrderDTO getById(Long id) {
    Order order = orderRepository.findById(id);   // 不应加载聚合根
    if (order.canBeViewed()) {                     // 不应执行业务规则
        return convertToDTO(order);
    }
    throw new DomainException("无权限查看");
}
```

**PROF-DDD-407** 查询DTO SHOULD 针对前端展示优化，MAY 包含冗余字段、计算字段、展示枚举描述。 [SHOULD/MAY]

```java
@Data
public class OrderDTO {
    private Long id;
    private String orderNo;
    private Long userId;
    private String username;          // 冗余：关联查询用户名
    private String status;            // 枚举code
    private String statusDesc;        // 冗余：枚举中文描述
    private BigDecimal totalAmount;
    private int itemCount;            // 冗余：商品数量汇总
    private LocalDateTime createTime;
    private String createTimeStr;     // 冗余：格式化后的时间字符串
}
```

**PROF-DDD-408** 查询仓储 SHOULD 使用独立的MyBatis Mapper，MAY 与命令端共享Mapper但 SHOULD 使用独立SQL。 [SHOULD/MAY]

```java
// 查询端专用Mapper
@Mapper
public interface OrderQueryMapper {
    OrderDTO selectOrderDTOById(@Param("id") Long id);
    Page<OrderDTO> selectOrderDTOPage(Page<?> page, @Param("query") OrderPageQuery query);
    List<OrderStatisticDTO> selectOrderStatistics(
        @Param("startDate") String startDate,
        @Param("endDate") String endDate);
}
```

## 适用场景判断

**PROF-DDD-409** 以下场景 SHOULD 采用CQRS模式：[SHOULD]

- 读写比例严重不对称（读远大于写）
- 查询视图需要关联多张表，加载聚合根效率低
- 命令模型与查询模型差异大
- 需要独立的读写扩展能力

**PROF-DDD-410** 以下场景 MAY 不采用CQRS，使用简单CRUD即可：[MAY]

- 简单CRUD，读写模型完全一致
- 数据量小，无性能瓶颈
- 内部工具/管理后台，无复杂查询需求

## 数据同步机制

**PROF-DDD-411** 同一数据库的CQRS（共享存储），命令端与查询端 MUST 读写同一数据库实例。 [MUST]

```
命令端写入 → DB表 → 查询端直接读取（无需同步）
```

**PROF-DDD-412** 分离存储的CQRS（独立读库/搜索引擎），MUST 通过领域事件实现最终一致性同步。 [MUST]

```
命令端写入 → 主DB → 发布领域事件 → 事件处理器 → 更新读库/ES索引
```

```java
@Component
@Slf4j
public class OrderReadModelUpdater {

    private final OrderReadModelRepository readModelRepository;

    @EventListener
    @Async
    public void onOrderCreated(OrderCreatedEvent event) {
        OrderReadModel readModel = new OrderReadModel();
        readModel.setOrderId(event.getOrderId());
        readModel.setUserId(event.getUserId());
        readModel.setTotalAmount(event.getTotalAmount());
        readModel.setStatus("CREATED");
        readModel.setCreatedAt(event.getCreatedAt());
        readModelRepository.save(readModel);
    }

    @EventListener
    @Async
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        readModelRepository.updateStatus(event.getOrderId(), "CONFIRMED");
    }
}
```

**PROF-DDD-413** 读模型同步 MUST 保证幂等性，事件乱序或重复投递时结果一致。 [MUST]

## 反模式清单

**PROF-DDD-414** 以下做法 MUST 被视为CQRS反模式并禁止：[MUST]

| 反模式 | 说明 | 正确做法 |
|--------|------|---------|
| 命令端返回聚合根数据 | 混淆读写职责 | 命令端仅返回ID |
| 查询端加载聚合根 | 性能浪费 | 查询端直接SQL查DTO |
| 查询端包含业务规则 | 职责越界 | 业务规则仅在命令端 |
| 同一方法既读又写 | 违反分离原则 | 拆分为命令方法和查询方法 |
| 查询端修改数据 | 破坏不变量 | 查询端严格只读 |

**PROF-DDD-415** 查询端 MUST 使用 `@Transactional(readOnly = true)` 标记只读事务，优化数据库性能。 [MUST]

```java
@Service
@RequiredArgsConstructor
public class OrderQueryAppService {

    @Transactional(readOnly = true)
    public OrderDTO getById(Long id) {
        return orderQueryRepository.findOrderDTOById(id);
    }

    @Transactional(readOnly = true)
    public PageResult<OrderDTO> page(OrderPageQuery query) {
        return orderQueryRepository.pageOrderDTO(query);
    }
}
```

**PROF-DDD-416** Controller MUST 根据读写操作路由到不同的AppService，MUST NOT 在同一Controller方法中混合调用命令与查询。 [MUST]

```java
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandAppService commandAppService;
    private final OrderQueryAppService queryAppService;

    // 写操作 → 命令服务
    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateOrderCommand cmd) {
        return Result.ok(commandAppService.create(cmd));
    }

    // 读操作 → 查询服务
    @GetMapping("/{id}")
    public Result<OrderDTO> detail(@PathVariable Long id) {
        return Result.ok(queryAppService.getById(id));
    }

    @GetMapping
    public PageResult<OrderDTO> page(@Valid OrderPageQuery query) {
        return queryAppService.page(query);
    }
}
```
