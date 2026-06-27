| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | architecture-module |
| 引入条件 | fingerprint.profiles contains 'arch-ddd' |
| 所属架构包 | arch-ddd |
| 依赖规范 | arch-ddd (本包的父包) |

# DDD 领域模型规范

## 1. 共享基类设计

**PROF-DDD-601** 项目 MUST 提供统一的聚合根基类，封装领域事件注册与收集能力。 [MUST]

```java
public abstract class AggregateRoot<ID> {
    @Transient
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> pollEvents() {
        var events = List.copyOf(domainEvents);
        domainEvents.clear();
        return events;
    }
}
```

> 聚合根基类是所有聚合的公共祖先，统一事件收集机制避免各聚合各自实现导致的不一致。`pollEvents()` 采用读取后清空策略，确保事件不会被重复发布。

**PROF-DDD-602** 项目 MUST 定义值对象标记接口和领域事件基类。 [MUST]

```java
public interface ValueObject { }

public interface DomainEvent {
    String getEventType();
    LocalDateTime occurredAt();
}
```

> `ValueObject` 为标记接口，用于在类型层面区分值对象与实体。`DomainEvent` 定义事件的最小契约，所有领域事件 MUST 实现该接口以保证事件基础设施的统一处理。

> **关于异常体系**：基础异常处理机制（全局异常处理器、统一响应体等）由 spring-boot-base 架构包提供。DDD 特有的分层异常类型（DomainException / BusinessException / InfrastructureException / ValidationException）定义见 PROF-DDD-605 ~ PROF-DDD-608，确保异常类型与 DDD 架构层级一一对应。

## 2. 聚合根设计规则

**PROF-DDD-201** 聚合根 MUST 具有全局唯一标识（ID），作为聚合的入口点。 [MUST]

```java
public class User extends AggregateRoot {
    private Long id;               // 全局唯一标识
    private String username;
    private String email;
    private UserStatus status;
    private List<UserRole> roles;  // 聚合内实体

    // 工厂方法创建，封装创建逻辑
    public static User create(String username, String email) {
        if (username == null || username.isBlank()) {
            throw new DomainException("用户名不能为空");
        }
        User user = new User();
        user.id = IdGenerator.nextId();
        user.username = username;
        user.email = email;
        user.status = UserStatus.ACTIVE;
        user.roles = new ArrayList<>();
        user.registerEvent(new UserCreatedEvent(user.id, username, email));
        return user;
    }
}
```

**PROF-DDD-202** 聚合根 MUST 作为不变量（Invariant）的一致性边界，保证聚合内所有业务规则在操作后仍然成立。 [MUST]

**PROF-DDD-203** 聚合根 MUST 控制对其内部实体的修改，外部 MUST NOT 直接操作聚合内实体。 [MUST]

```java
public class Order extends AggregateRoot {
    private Long id;
    private OrderStatus status;
    private List<OrderItem> items;      // 聚合内实体
    private Money totalAmount;

    // 正确：通过聚合根方法修改内部实体
    public void addItem(String productId, int quantity, Money unitPrice) {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("仅草稿状态可添加商品");
        }
        if (quantity <= 0) {
            throw new DomainException("数量必须大于0");
        }
        OrderItem item = new OrderItem(productId, quantity, unitPrice);
        items.add(item);
        recalculateTotal();  // 维护不变量：总金额 = 所有商品小计之和
    }

    public void removeItem(Long itemId) {
        if (status != OrderStatus.DRAFT) {
            throw new DomainException("仅草稿状态可移除商品");
        }
        items.removeIf(item -> item.getId().equals(itemId));
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalAmount = items.stream()
            .map(OrderItem::subtotal)
            .reduce(Money.ZERO, Money::add);
    }
}
```

**PROF-DDD-204** 聚合根 SHOULD 尽量小，仅包含必须保持一致性的对象；过大聚合 MUST 考虑拆分。 [SHOULD/MUST]

判断标准：
- 聚合内对象是否必须保持事务一致性？
- 聚合根是否能在合理时间内加载（行数 < 1000行数据）？
- 是否存在独立生命周期？

## 3. 实体设计规则

**PROF-DDD-205** 实体 MUST 具有唯一标识（局部ID或聚合内ID），通过标识区分，而非属性值。 [MUST]

```java
public class OrderItem {
    private Long id;              // 聚合内唯一标识
    private String productId;
    private int quantity;
    private Money unitPrice;

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}
```

**PROF-DDD-206** 实体的生命周期 MUST 由聚合根管理，MUST NOT 独立于聚合根创建或销毁。 [MUST]

```java
// 正确：通过聚合根创建实体
Order order = orderRepository.findById(orderId);
order.addItem(productId, quantity, unitPrice);

// 错误：绕过聚合根直接操作实体
OrderItem item = new OrderItem(productId, quantity, unitPrice);
order.getItems().add(item);  // 绕过业务规则检查
```

**PROF-DDD-207** 实体 SHOULD 封装业务行为，MUST NOT 沦为纯数据载体（贫血模型）。 [SHOULD/MUST]

```java
// 正确：充血模型
public class OrderItem {
    private int quantity;
    private Money unitPrice;

    public void changeQuantity(int newQuantity) {
        if (newQuantity <= 0) {
            throw new DomainException("数量必须大于0");
        }
        if (newQuantity > 999) {
            throw new DomainException("单品数量不能超过999");
        }
        this.quantity = newQuantity;
    }

    public Money subtotal() {
        return unitPrice.multiply(quantity);
    }
}

// 错误：贫血模型
public class OrderItem {
    private int quantity;
    private Money unitPrice;
    // 仅有getter/setter，无业务行为
}
```

## 4. 值对象设计规则

**PROF-DDD-208** 值对象 MUST 是不可变的（Immutable），所有字段在构造后 MUST NOT 修改。 [MUST]

```java
@Value  // Lombok @Value 自动生成不可变类
public class Money {
    private final BigDecimal amount;
    private final String currency;

    public Money add(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new DomainException("不同币种不能相加");
        }
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money multiply(int quantity) {
        return new Money(this.amount.multiply(BigDecimal.valueOf(quantity)), this.currency);
    }

    public static Money ofCNY(BigDecimal amount) {
        return new Money(amount, "CNY");
    }
}
```

**PROF-DDD-209** 值对象 MUST 通过属性值判断相等性（equals），而非引用标识。 [MUST]

```java
@Value
@EqualsAndHashCode  // 基于所有字段比较
public class DateRange {
    private final LocalDate start;
    private final LocalDate end;

    public boolean overlaps(DateRange other) {
        return !this.end.isBefore(other.start) && !other.end.isBefore(this.start);
    }

    public long days() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }
}
```

**PROF-DDD-210** 值对象 MUST NOT 拥有唯一标识字段（id），MUST NOT 持久化为独立表。 [MUST]

**PROF-DDD-211** 值对象 SHOULD 包含领域行为方法（计算、比较、转换），而非仅作为数据容器。 [SHOULD]

```java
@Value
public class Coordinate {
    private final double latitude;
    private final double longitude;

    public double distanceTo(Coordinate other) {
        // Haversine公式计算距离
        double earthRadius = 6371.0;
        double dLat = Math.toRadians(other.latitude - this.latitude);
        double dLng = Math.toRadians(other.longitude - this.longitude);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(this.latitude))
                 * Math.cos(Math.toRadians(other.latitude))
                 * Math.sin(dLng/2) * Math.sin(dLng/2);
        return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    }
}
```

## 5. 聚合间引用规则

**PROF-DDD-212** 聚合之间 MUST 通过ID引用，MUST NOT 通过对象直接引用。 [MUST]

```java
// 正确：通过ID引用
public class Order extends AggregateRoot {
    private Long id;
    private Long userId;         // 通过ID引用User聚合
    private List<OrderItem> items;
}

// 错误：直接对象引用
public class Order extends AggregateRoot {
    private Long id;
    private User user;           // 跨聚合直接引用，导致加载边界模糊
    private List<OrderItem> items;
}
```

**PROF-DDD-213** 跨聚合操作 SHOULD 通过领域事件或领域服务协调，MUST NOT 在一个事务中直接修改多个聚合。 [SHOULD/MUST]

## 6. 仓储与聚合的对应关系

**PROF-DDD-214** 每个聚合根 MUST 有且仅有一个Repository，负责整个聚合的持久化与加载。 [MUST]

**PROF-DDD-215** Repository MUST 加载完整聚合（聚合根 + 所有内部实体/值对象），MUST NOT 返回部分聚合。 [MUST]

```java
// Repository实现中
@Override
public Order findById(Long id) {
    OrderDO orderDO = orderMapper.selectById(id);
    List<OrderItemDO> itemDOs = orderItemMapper.selectByOrderId(id);
    return orderConverter.toDomain(orderDO, itemDOs);  // 返回完整聚合
}
```

## 7. 聚合设计黄金法则

**PROF-DDD-501** 聚合 MUST 遵循以下5条黄金法则，每条法则都是聚合边界设计的核心约束。 [MUST]

| # | 法则 | 说明 | 违反后果 |
|---|------|------|---------|
| 1 | 聚合尽可能小 | 仅包含必须保持一致性的对象，不将"有关联"作为纳入聚合的理由 | 聚合膨胀，加载性能劣化，并发冲突激增 |
| 2 | 通过ID引用外部聚合 | 不跨聚合直接持有对象引用，使用ID建立逻辑关联 | 聚合边界模糊，加载链路不可控，级联查询拖垮系统 |
| 3 | 最终一致性优先 | 跨聚合修改使用领域事件协调，不追求跨聚合强一致 | 事务范围过大，锁竞争加剧，系统可用性降低 |
| 4 | 一个事务只改一个聚合 | 严禁单个事务操作多个聚合的写操作 | 分布式事务复杂性爆炸，回滚链路不可控 |
| 5 | 聚合边界就是一致性边界 | 聚合内强一致，聚合间最终一致 | 不变量无法保证，业务规则在并发下被破坏 |

### 聚合实现规范

**PROF-DDD-504** 聚合根 MUST 通过方法封装内部修改，MUST NOT 暴露内部集合的setter或直接add方法。 [MUST]

```java
// 正确：通过方法封装
public class Order extends AggregateRoot {
    public void addItem(String productId, int quantity, Money price) {
        Assert.isTrue(status == OrderStatus.DRAFT, "仅草稿状态可添加");
        items.add(OrderItem.create(productId, quantity, price));
        recalculateTotal();
    }

    // 返回不可变视图，防止外部修改
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }
}

// 错误：暴露内部集合
public class Order extends AggregateRoot {
    public List<OrderItem> getItems() { return items; }
    // 外部直接 order.getItems().add(item) — 绕过业务规则
}
```

**PROF-DDD-505** 聚合根 MUST 支持幂等操作，对重复调用 MUST 产生相同结果而非副作用叠加。 [MUST]

```java
public class Order extends AggregateRoot {
    public void cancel(String reason) {
        if (status == OrderStatus.CANCELLED) {
            return; // 幂等：已取消则直接返回，不重复注册事件
        }
        if (status == OrderStatus.SHIPPED) {
            throw new DomainException("已发货订单不能取消");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
        registerEvent(new OrderCancelledEvent(this.id, reason));
    }
}
```

> 幂等性保证：无论 `cancel()` 被调用一次还是多次，聚合状态和事件列表保持一致。

**PROF-DDD-506** 聚合根 MUST 添加乐观锁机制，防止并发修改导致数据不一致。 [MUST]

```java
public class Order extends AggregateRoot {
    private Long id;
    private int version;  // 乐观锁版本号
    private OrderStatus status;
    // ...
}
```

乐观锁的实现方式取决于持久化框架，以下为常见实现选项：

```java
// 实现选项1：JPA @Version 注解（基础设施层使用）
@Version
private int version;

// 实现选项2：MyBatis XML 中通过 WHERE 条件
// UPDATE orders SET status = #{status}, version = version + 1
// WHERE id = #{id} AND version = #{version}
```

```java
// 仓储层统一处理乐观锁异常
@Override
public void save(Order order) {
    try {
        if (order.getId() == null) {
            orderMapper.insert(orderConverter.toDataObject(order));
        } else {
            int affected = orderMapper.updateById(orderConverter.toDataObject(order));
            if (affected == 0) {
                throw new ConcurrencyException("订单已被其他操作修改，请重试");
            }
        }
    } catch (ConcurrencyException e) {
        throw e; // 由应用层捕获并决定重试策略
    }
}
```

> 乐观锁是聚合级别的并发控制策略，领域层定义 `version` 字段，具体锁机制由基础设施层实现。

## 8. 聚合粒度决策矩阵

**PROF-DDD-502** 聚合粒度 MUST 根据以下决策矩阵判断，结合业务场景选择正确的聚合策略。 [MUST]

| 场景 | 推荐策略 | 示例 |
|------|---------|------|
| 父子对象必须同时修改 | 同一聚合 | 订单 + 订单项 |
| 父子对象独立修改 | 不同聚合，通过ID引用 | 用户 + 订单 |
| 父删除子也必须删除 | 同一聚合，级联删除 | 文章 + 文章评论 |
| 子有独立生命周期 | 不同聚合 | 商品 + 商品评价 |
| 统计数据/报表 | 独立读模型（CQRS） | 订单 + 订单统计视图 |

**PROF-DDD-503** 判断聚合边界时 MUST 回答以下三个问题，全部通过才应纳入同一聚合：[MUST]

- [ ] 聚合内对象是否必须保持事务一致性？（必须同时成功或同时失败）
- [ ] 聚合根是否能在合理时间内加载（行数 < 1000行数据）？
- [ ] 被考虑的对象是否存在独立生命周期？（若存在，则不应纳入同一聚合）

> 判断顺序：先问问题1，不满足则直接拆分；再问问题2，不满足则考虑延迟加载或拆分；最后问问题3，有独立生命周期的对象必须拆为不同聚合。

## 9. 聚合间协调

**PROF-DDD-507** 跨聚合的写操作 MUST 通过领域事件实现最终一致性，MUST NOT 在一个事务中直接修改多个聚合。 [MUST]

```java
// 应用层：一个事务只操作一个聚合
@Transactional(rollbackFor = Exception.class)
public void confirmOrder(Long orderId) {
    // 1. 修改订单聚合（唯一写操作）
    Order order = orderRepository.findById(orderId);
    order.confirm();
    orderRepository.save(order);

    // 2. 发布领域事件，由事件处理器异步更新其他聚合
    eventPublisher.publish(order.collectDomainEvents());
}

// 事件处理器：异步扣减库存（独立事务）
@Component
public class InventoryEventHandler {

    @EventListener
    @Async
    @Transactional(rollbackFor = Exception.class)
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        Inventory inventory = inventoryRepository.findByProductId(event.getProductId());
        inventory.deduct(event.getQuantity());
        inventoryRepository.save(inventory);
    }
}
```

**PROF-DDD-508** 跨聚合的数据读取 SHOULD 通过防腐层（ACL）或查询服务获取，MUST NOT 在聚合根中直接注入其他聚合的Repository。 [SHOULD/MUST]

```java
// 正确：领域服务协调跨聚合读取
public class OrderCreationService {

    private final UserRepository userRepository;
    private final ProductQueryService productQueryService;  // 防腐层

    public Order createOrder(Long userId, List<OrderItemCommand> items) {
        User user = userRepository.findById(userId);
        if (user == null || !user.isActive()) {
            throw new DomainException("用户不存在或已停用");
        }

        // 通过查询服务（防腐层）获取商品信息，而非注入ProductRepository
        List<ProductInfo> products = productQueryService.findByIds(
            items.stream().map(OrderItemCommand::getProductId).collect(toList())
        );

        return Order.create(user.getId(), items, products);
    }
}

// 错误：聚合根直接注入其他聚合的Repository
public class Order extends AggregateRoot {
    private UserRepository userRepository;    // 聚合根不应感知其他聚合的仓储
    private ProductRepository productRepository;
}
```

**PROF-DDD-509** 当业务场景确实需要跨聚合强一致性时，MUST 通过领域服务协调并在应用层声明事务边界。 [MUST]

```java
// 应用层声明事务边界
@Service
@RequiredArgsConstructor
public class FundTransferAppService {

    private final FundTransferService fundTransferService;  // 领域服务
    private final AccountRepository accountRepository;

    @Transactional(rollbackFor = Exception.class)
    public void transfer(Long fromAccountId, Long toAccountId, Money amount) {
        // 领域服务协调跨聚合的强一致性操作
        fundTransferService.transfer(
            accountRepository.findById(fromAccountId),
            accountRepository.findById(toAccountId),
            amount
        );
        // 注意：此场景为强一致性需求，属于黄金法则的例外情况
        // MUST 在代码注释中说明为何不能采用最终一致性
    }
}
```

> 跨聚合强一致性是例外而非常态。MUST 在设计文档中记录选择强一致性的理由，并在代码审查中重点关注。

## 10. 聚合规模管理

**PROF-DDD-510** 以下信号 SHOULD 触发聚合拆分评估，当出现两条及以上信号时 MUST 启动重构。 [SHOULD]

| 信号 | 阈值 | 说明 |
|------|------|------|
| N+1 查询 | 聚合加载时频繁触发 | 聚合内包含大量关联实体，每次加载产生级联查询 |
| 并发冲突频率 | > 5% | 乐观锁冲突频繁，说明聚合边界过大或热点字段集中 |
| 聚合内实体数 | > 7 个 | 单一聚合承载过多概念，职责可能不内聚 |
| 聚合根方法数 | > 15 个 | 聚合根行为过多，考虑是否包含不属于当前聚合的操作 |
| 仅读取不修改的内部实体 | 存在即评估 | 这些实体可能属于独立聚合或读模型 |

**PROF-DDD-511** 当聚合过大但不适合拆分时，SHOULD 采用延迟加载策略，按需加载内部集合。 [SHOULD]

```java
public class Article extends AggregateRoot {
    private Long id;
    private String title;
    private String content;
    // 评论集合延迟加载，仅在显式调用时加载
    private List<Comment> comments;  // lazy-load

    /**
     * 默认加载仅包含文章主体，不加载评论
     * 需要评论时通过 Repository 显式加载
     */
    public List<Comment> getComments(CommentRepository commentRepo) {
        if (comments == null) {
            comments = commentRepo.findByArticleId(this.id);
        }
        return Collections.unmodifiableList(comments);
    }
}
```

> 延迟加载是聚合拆分的折中方案。如果延迟加载仍无法满足性能要求，MUST 重新评估聚合边界（参考 PROF-DDD-510）。

**PROF-DDD-512** 聚合根 MUST NOT 感知缓存策略。缓存职责 MUST 归属应用层或基础设施层。 [MUST]

| 缓存层级 | 适用场景 | 失效策略 |
|----------|---------|---------|
| L1 本地缓存 | 热点数据、字典值、枚举映射 | 定时刷新 + 主动失效 |
| L2 分布式缓存 | 频繁查询结果、聚合快照 | TTL + 发布订阅失效 |

```java
// 正确：缓存在应用层/基础设施层处理
@Service
@RequiredArgsConstructor
public class UserQueryAppService {

    private final UserQueryRepository userQueryRepository;
    private final CacheService cacheService;  // 缓存职责在应用层

    public UserDTO getById(Long id) {
        String cacheKey = "user:" + id;
        UserDTO cached = cacheService.get(cacheKey, UserDTO.class);
        if (cached != null) {
            return cached;
        }
        UserDTO dto = userQueryRepository.findUserDTOById(id);
        cacheService.put(cacheKey, dto, Duration.ofMinutes(30));
        return dto;
    }
}

// 错误：领域层感知缓存
public class Order extends AggregateRoot {
    @Cacheable("order")  // 领域层不应关心缓存
    public Money getTotal() { ... }
}
```

> 缓存是技术关注点，不属于领域逻辑。聚合根保持纯净，缓存策略由{{cache_layer}}层统一管理。

## 11. 领域服务与应用服务的区分

**PROF-DDD-216** 应用服务（AppService）MUST 仅负责编排（事务、安全、事件发布），MUST NOT 包含业务规则。 [MUST]

**PROF-DDD-217** 领域服务（DomainService）MUST 封装跨聚合的复杂业务规则，由应用服务调用。 [MUST]

```java
// 应用服务：编排流程
@Service
public class OrderCommandAppService {

    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateOrderCommand cmd) {
        // 1. 调用领域服务处理跨聚合逻辑
        Order order = orderCreationService.createOrder(cmd.getUserId(), cmd.getItems());

        // 2. 持久化
        orderRepository.save(order);

        // 3. 发布事件
        eventPublisher.publish(order.collectDomainEvents());

        return order.getId();
    }
}

// 领域服务：跨聚合业务规则
public class OrderCreationService {

    public Order createOrder(Long userId, List<OrderItemCommand> items) {
        // 需要查询用户信息（跨聚合）判断是否允许下单
        // 需要检查库存（跨聚合）判断商品是否可购买
        // 这些规则不适合放在单一聚合根内
    }
}
```

**PROF-DDD-218** 领域服务 MUST NOT 依赖Spring注解或任何框架代码，MUST 保持领域纯净性。 [MUST]

### 领域服务 vs 应用服务辨析

**PROF-DDD-220** 团队 MUST 清晰区分领域服务与应用服务的职责边界：[MUST]

| 对比维度 | 领域服务（DomainService） | 应用服务（AppService） |
|---------|-------------------------|---------------------|
| 所属层级 | 领域层 | 应用层 |
| 业务性质 | 核心业务规则、计算 | 用例编排、事务协调 |
| 依赖方向 | 仅依赖领域对象 | 依赖仓储、外部服务等 |
| 事务感知 | 无 | @Transactional |
| 可独立测试 | 是（纯JUnit） | 需要Mock外部依赖 |
| 修改频率 | 业务规则变更时 | 用例流程变更时 |

**PROF-DDD-221** 领域服务 SHOULD 仅在以下场景使用：[SHOULD]

- 涉及多个聚合的业务规则（如检查用户信用额度 + 订单金额）
- 跨聚合的计算（如运费计算：地址 + 物流规则 + 商品重量）
- 外部服务在领域层的适配（如税率计算，领域定义接口，基础设施实现）
- 复杂业务规则组合（如折扣策略：满减 + 会员折扣 + 优惠券叠加）

**PROF-DDD-222** 领域服务 MUST NOT 包含以下应用层职责：[MUST]

- 调用Repository或基础设施接口
- 声明或管理事务
- 接收或返回DTO/Command/Query对象
- 处理HTTP/RPC/MQ协议细节

## 12. DDD 核心设计原则

**PROF-DDD-219** DDD项目 MUST 遵循以下四项核心设计原则：[MUST]

| 原则 | 含义 | 验证标准 |
|------|------|---------|
| **Domain First** | 领域层是核心资产，业务规则必须内聚于领域对象 | 领域层无框架污染 |
| **Clean Architecture** | 外层依赖内层，内层无框架污染 | 替换持久化框架，领域层零改动 |
| **Executable Documentation** | 代码即文档，注释仅解释"为什么" | 方法名体现业务意图 |
| **Testable by Design** | 设计即考虑可测试性，领域层可脱离容器单元测试 | 纯JUnit运行领域测试 |

## 13. DDD 命名规范

### 聚合根命名

**PROF-DDD-101** 聚合根 MUST 使用名词命名，表达业务概念本身，MUST NOT 使用 Manager/Handler/Processor 后缀。 [MUST]

```java
// 正确：名词命名
public class User { }
public class Order { }
public class Config { }

// 错误
public class UserManager { }      // Manager暗示贫血模型
public class OrderHandler { }     // Handler暗示过程式编程
public class ConfigProcessor { }  // Processor偏离领域概念
```

**PROF-DDD-102** 聚合根 MUST NOT 以 Entity/Model/Bean 结尾，聚合根本身就是领域实体。 [MUST]

```java
// 正确
public class User { }

// 错误
public class UserEntity { }   // 冗余后缀
public class UserModel { }    // 模糊命名
```

### 实体与值对象命名

**PROF-DDD-103** 实体（Entity）使用名词命名，体现其在聚合内的角色。 [MUST]

```java
// 聚合根Order内的实体
public class OrderItem { }       // 订单项
public class ShippingAddress { } // 收货地址（作为值对象亦可）
```

**PROF-DDD-104** 值对象（Value Object）使用名词或名词短语命名，表达不可变属性集合。 [MUST]

```java
public class Money {             // 金额值对象
    private final BigDecimal amount;
    private final String currency;
}

public class DateRange {         // 日期范围值对象
    private final LocalDate start;
    private final LocalDate end;
}

public class Coordinate {        // 坐标值对象
    private final double latitude;
    private final double longitude;
}
```

### 领域服务命名

**PROF-DDD-105** 领域服务 MUST 使用动词短语命名，描述跨聚合或复杂业务操作。 [MUST]

```java
// 正确：动词短语，描述职责
public class UserAuthorizationService { }   // 用户授权
public class OrderPricingService { }        // 订单定价
public class InventoryDeductionService { }  // 库存扣减

// 错误
public class UserService { }       // 过于模糊，与CRUD Service混淆
public class OrderHelper { }       // Helper不是领域服务命名
```

**PROF-DDD-106** 领域服务 SHOULD 仅在以下场景使用，MUST NOT 将聚合根内部逻辑外泄到领域服务：[SHOULD/MUST]

- 跨聚合根的业务操作
- 需要外部信息的复杂业务规则
- 不符合任何单一聚合根归属的逻辑

### 领域事件命名

**PROF-DDD-107** 领域事件 MUST 使用过去时态命名，表达"已发生的事实"。 [MUST]

```java
// 正确：过去时态
public class UserCreatedEvent { }
public class OrderConfirmedEvent { }
public class ConfigUpdatedEvent { }
public class UserDeactivatedEvent { }

// 错误
public class CreateUserEvent { }      // 这是命令，不是事件
public class OrderConfirmEvent { }    // 缺少过去时态
public class ConfigChangeEvent { }     // 时态不明确
```

**PROF-DDD-108** 领域事件 MUST 继承统一基类，包含事件元数据（发生时间、聚合根ID）。 [MUST]

```java
public abstract class DomainEvent {
    private final String eventId;
    private final LocalDateTime occurredOn;
    private final String aggregateType;
    private final Long aggregateId;
}

public class UserCreatedEvent extends DomainEvent {
    private final String username;
    private final String email;
}
```

### Repository 命名

**PROF-DDD-109** 仓储接口 MUST 定义在 domain 层，以 `Repository` 结尾，以聚合根名称为前缀。 [MUST]

```java
// domain/repository/UserRepository.java
public interface UserRepository {
    void save(User user);
    User findById(Long id);
    Optional<User> findByUsername(String username);
}
```

**PROF-DDD-110** 仓储实现 MUST 定义在 infrastructure 层，以 `RepositoryImpl` 结尾。 [MUST]

```java
// infrastructure/repository/impl/UserRepositoryImpl.java
@Repository
public class UserRepositoryImpl implements UserRepository {
    // ...
}
```

**PROF-DDD-111** 每个聚合根 MUST 对应一个Repository，MUST NOT 为聚合内子实体单独建Repository。 [MUST]

```java
// 正确：只为聚合根建Repository
public interface OrderRepository { }  // Order是聚合根

// 错误：为子实体建Repository
public interface OrderItemRepository { }  // OrderItem属于Order聚合内
```

### 应用服务命名

**PROF-DDD-112** 应用服务 SHOULD 按CQRS拆分为命令服务与查询服务。 [SHOULD]

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 命令服务 | `{{Aggregate}}CommandAppService` | `UserCommandAppService` |
| 查询服务 | `{{Aggregate}}QueryAppService` | `UserQueryAppService` |
| 通用服务 | `{{Aggregate}}AppService` | `UserAppService` |

```java
// CQRS模式
public class UserCommandAppService {
    public Long create(CreateUserCommand cmd) { }
    public void update(UpdateUserCommand cmd) { }
    public void delete(Long id) { }
}

public class UserQueryAppService {
    public UserDTO getById(Long id) { }
    public PageResult<UserDTO> page(UserPageQuery query) { }
}
```

### Command / Query 对象命名

**PROF-DDD-113** 命令对象 MUST 使用祈使句命名，表达意图。 [MUST]

```java
public class CreateUserCommand { }
public class UpdateUserCommand { }
public class DeactivateUserCommand { }
public class ConfirmOrderCommand { }
```

**PROF-DDD-114** 查询对象 MUST 使用名词或描述性命名。 [MUST]

```java
public class UserPageQuery { }
public class UserDetailQuery { }
public class OrderSearchQuery { }
```

**PROF-DDD-115** DTO（返回给适配层）使用 `{{Aggregate}}DTO` 或 `{{Aggregate}}DetailDTO` 命名。 [MUST]

```java
public class UserDTO { }          // 列表场景
public class UserDetailDTO { }    // 详情场景
```

### Gateway 命名

**PROF-DDD-116** 网关接口 MUST 定义在 domain 层，以 `Gateway` 结尾，描述外部系统能力。 [MUST]

```java
// domain/gateway/NotificationGateway.java
public interface NotificationGateway {
    void sendEmail(String to, String subject, String body);
    void sendSms(String phone, String content);
}

// infrastructure/gateway/impl/NotificationGatewayImpl.java
@Component
public class NotificationGatewayImpl implements NotificationGateway {
    // 封装具体HTTP调用
}
```

## 14. DDD 单元测试规范

**PROF-DDD-610** 领域层单元测试 MUST 不依赖Spring容器，MUST 在纯JUnit环境运行。 [MUST]

```java
// 领域层纯单元测试 — 无需 @SpringBootTest
class OrderTest {

    @Test
    void shouldRejectConfirmWhenAlreadyCancelled() {
        Order order = Order.create(userId, items);
        order.cancel("买家取消");

        assertThrows(DomainException.class, () -> order.confirm());
    }
}
```

> 领域层测试不启动Spring容器，保证测试执行速度（毫秒级）。如果领域层测试需要Spring容器，说明领域对象混入了框架依赖，违反了 PROF-DDD-218。

**PROF-DDD-611** 领域层测试 SHOULD 使用Builder模式构建测试数据，SHOULD 遵循Given-When-Then结构。 [SHOULD]

```java
@DisplayName("订单聚合测试")
class OrderTest {

    @Test
    @DisplayName("添加订单项应重算总金额")
    void shouldRecalculateTotalWhenAddItem() {
        // Given
        Order order = OrderBuilder.defaultOrder().build();
        Money price = new Money(new BigDecimal("100.00"));

        // When
        order.addItem(new ProductId("P001"), "商品A", price, 2);

        // Then
        assertEquals(new BigDecimal("200.00"),
                    order.getTotalAmount().getAmount());
    }
}
```

```java
// 测试数据Builder — 封装默认有效数据
public class OrderBuilder {
    private Long userId = 1L;
    private List<OrderItem> items = new ArrayList<>();

    public static OrderBuilder defaultOrder() {
        return new OrderBuilder();
    }

    public OrderBuilder withUserId(Long userId) {
        this.userId = userId;
        return this;
    }

    public Order build() {
        return Order.create(userId, items);
    }
}
```

> Builder模式将测试数据构建与测试逻辑分离，`{{builder_class}}` 应放在 `src/test/java` 对应包下，避免污染生产代码。Given-When-Then结构使测试意图清晰可读。

**PROF-DDD-612** 测试策略 SHOULD 遵循DDD测试金字塔： [SHOULD]

| 层级 | 占比 | 覆盖目标 |
|------|------|---------|
| 单元测试 | 最多（>=80%覆盖率） | 领域层核心业务规则 |
| 集成测试 | 适中 | 仓储、应用服务，关键用例 |
| 契约测试 | 适量 | 外部服务接口 |
| E2E测试 | 最少 | 关键业务链，API级别 |

> 测试金字塔强调底层测试多而快、顶层测试少而精。领域层单元测试是投入产出比最高的测试类型，MUST 优先保证覆盖。集成测试聚焦于验证仓储实现与领域模型的映射正确性，E2E测试仅覆盖核心业务链路。
