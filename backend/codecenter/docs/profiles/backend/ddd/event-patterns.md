| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | architecture-module |
| 引入条件 | fingerprint.profiles contains 'arch-ddd' |
| 所属架构包 | arch-ddd |
| 依赖规范 | arch-ddd (本包的父包) |

# 领域事件模式

## 领域事件设计原则

**PROF-DDD-301** 领域事件 MUST 表达"已发生的业务事实"，MUST NOT 表达"需要执行的命令"。 [MUST]

```java
// 正确：描述已发生的事实
public class OrderCreatedEvent extends DomainEvent {
    private final Long orderId;
    private final Long userId;
    private final Money totalAmount;
    private final LocalDateTime createdAt;
}

// 错误：这是命令或通知
public class CreateOrderEvent extends DomainEvent { }  // 命令式命名
public class SendOrderEmailEvent extends DomainEvent { } // 这是动作，不是事实
```

**PROF-DDD-302** 领域事件 MUST 是不可变的，包含事件消费者所需的全部信息，MUST NOT 依赖消费者回查数据库。 [MUST]

```java
// 正确：事件包含足够信息
public class UserCreatedEvent extends DomainEvent {
    private final Long userId;
    private final String username;
    private final String email;
    private final LocalDateTime createdAt;
    // 消费者可直接使用，无需回查
}

// 错误：事件信息不足
public class UserCreatedEvent extends DomainEvent {
    private final Long userId;
    // 消费者需要回查数据库获取用户名、邮箱等
}
```

**PROF-DDD-303** 领域事件 MUST NOT 包含领域对象的直接引用，仅包含ID和必要的数据快照。 [MUST]

## 事件发布策略

**PROF-DDD-304** 聚合根 SHOULD 在内部收集领域事件，由应用层在事务提交后统一发布。 [SHOULD]

```java
// 聚合根收集事件
public abstract class AggregateRoot {
    private final List<DomainEvent> domainEvents = new ArrayList<>();

    protected void registerEvent(DomainEvent event) {
        domainEvents.add(event);
    }

    public List<DomainEvent> collectDomainEvents() {
        List<DomainEvent> events = new ArrayList<>(domainEvents);
        domainEvents.clear();
        return events;
    }
}

// 在聚合根业务方法中注册事件
public class User extends AggregateRoot {
    public static User create(String username, String email) {
        User user = new User();
        user.id = IdGenerator.nextId();
        user.username = username;
        user.email = email;
        user.registerEvent(new UserCreatedEvent(user.id, username, email));
        return user;
    }
}

// 应用层发布事件
@Service
public class UserCommandAppService {
    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateUserCommand cmd) {
        User user = User.create(cmd.getUsername(), cmd.getEmail());
        userRepository.save(user);
        eventPublisher.publish(user.collectDomainEvents());
        return user.getId();
    }
}
```

**PROF-DDD-305** 同步事件（进程内）SHOULD 使用 Spring `ApplicationEventPublisher`，在事务提交后触发。 [SHOULD]

```java
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publish(List<DomainEvent> events) {
        events.forEach(eventPublisher::publishEvent);
    }
}
```

**PROF-DDD-306** 需要跨服务传播的事件 MUST 使用消息队列（MQ），MUST NOT 依赖HTTP同步调用。 [MUST]

**PROF-DDD-307** MQ事件发布 MUST 使用事务消息或Outbox模式，保证事件与数据变更的最终一致性。 [MUST]

```java
// Outbox模式实现
@Component
public class OutboxEventPublisher implements DomainEventPublisher {

    private final OutboxRepository outboxRepository;

    @Override
    public void publish(List<DomainEvent> events) {
        for (DomainEvent event : events) {
            OutboxMessage message = new OutboxMessage();
            message.setEventType(event.getClass().getSimpleName());
            message.setPayload(JsonUtils.toJson(event));
            message.setAggregateId(event.getAggregateId());
            message.setOccurredOn(event.getOccurredOn());
            outboxRepository.save(message);
            // 由定时任务扫描outbox表发送到MQ
        }
    }
}
```

## 事件处理器设计

**PROF-DDD-308** 事件处理器 MUST 保证幂等性，同一事件多次处理结果一致。 [MUST]

```java
@Component
@EventListener
public class UserCreatedNotificationHandler {

    private final NotificationService notificationService;
    private final EventConsumedRecordRepository recordRepository;

    public void handle(UserCreatedEvent event) {
        // 幂等检查：是否已处理过该事件
        if (recordRepository.existsByEventId(event.getEventId())) {
            return;
        }

        notificationService.sendWelcomeEmail(event.getEmail(), event.getUsername());

        // 记录已处理
        recordRepository.save(new EventConsumedRecord(event.getEventId()));
    }
}
```

**PROF-DDD-309** 事件处理器 SHOULD 异步执行，MUST NOT 阻塞主事务流程。 [SHOULD/MUST]

```java
@Component
public class OrderEventHandler {

    @Async("eventTaskExecutor")
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        // 异步处理：发送通知、更新统计数据等
    }
}

@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean("eventTaskExecutor")
    public Executor eventTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("event-handler-");
        return executor;
    }
}
```

**PROF-DDD-310** 事件处理器 MUST 处理失败时记录日志并支持重试，MUST NOT 静默吞掉异常。 [MUST]

```java
@Component
@Slf4j
public class OrderEventHandler {

    @Retryable(
        retryFor = {TransientException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    @EventListener
    public void handleOrderConfirmed(OrderConfirmedEvent event) {
        log.info("处理订单确认事件: orderId={}", event.getOrderId());
        inventoryService.deductStock(event.getItems());
    }
}
```

## 事件溯源考量

**PROF-DDD-311** 事件溯源（Event Sourcing）MAY 用于需要完整审计轨迹的聚合根，MUST NOT 作为全局默认策略。 [MAY/MUST]

适用场景：
- 金融交易流水
- 审批流程状态变更
- 操作审计日志

**PROF-DDD-312** 使用事件溯源时 MUST 提供快照机制，避免重放过长的事件流。 [MUST]

```java
public class OrderSnapshotService {
    // 每N个事件生成一次快照
    public OrderSnapshot createSnapshot(Long orderId, int eventVersion) {
        Order order = orderRepository.loadFromEvents(orderId);
        return new OrderSnapshot(orderId, eventVersion, JsonUtils.toJson(order));
    }
}
```

## 事件命名与版本化

**PROF-DDD-313** 事件类名 MUST 遵循 `{{Aggregate}}{{PastTenseVerb}}Event` 格式。 [MUST]

| 聚合根 | 事件 |
|--------|------|
| User | `UserCreatedEvent`, `UserDeactivatedEvent` |
| Order | `OrderCreatedEvent`, `OrderConfirmedEvent`, `OrderCancelledEvent` |
| Config | `ConfigUpdatedEvent`, `ConfigPublishedEvent` |

**PROF-DDD-314** MQ事件 MUST 包含版本号，支持向后兼容的Schema演进。 [MUST]

```java
public class UserCreatedEvent extends DomainEvent {
    public static final String EVENT_TYPE = "user.created";
    public static final int VERSION = 1;

    private final Long userId;
    private final String username;
    private final String email;
    // 新增字段使用可选（nullable），保持向后兼容
    private final String phone;  // v2新增，v1消费者可忽略
}
```

**PROF-DDD-315** 事件类型标识 SHOULD 使用点分命名：`{{bounded_context}}.{{aggregate}}.{{action}}`。 [SHOULD]

```
user.user.created
user.user.deactivated
order.order.created
order.order.confirmed
order.order.cancelled
```

**PROF-DDD-316** 废弃事件 SHOULD 标记 `@Deprecated` 并设置迁移截止日期，MUST NOT 立即删除仍在消费的事件。 [SHOULD/MUST]
