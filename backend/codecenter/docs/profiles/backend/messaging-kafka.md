| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | messaging |
| 引入条件 | fingerprint.profiles contains 'messaging-kafka' |
| 适用架构 | Spring Boot + Kafka |
| 依赖规范 | UNI-NC(命名规范)、UNI-LS(日志规范) |
| 互斥规范 | messaging-rocketmq |

# Kafka 消息队列规范

本包定义 Kafka 在 Spring Boot 项目中的使用约定，涵盖 Topic 设计、消息格式、生产者与消费者规范、事务消息及监控运维。与 `arch-ddd/event-patterns` 互补：event-patterns 定义"事件是什么"，本包定义"事件如何传输"。

---

## 一、Topic 命名与设计

**MSG-KF-001** Topic 名称 MUST 遵循四段式命名：`{env}.{domain}.{aggregate}.{event-type}`，全小写，段间用点分隔。 [MUST]

```
prod.order.order.created        # 生产环境-订单域-订单聚合-已创建
prod.user.user.deactivated      # 生产环境-用户域-用户聚合-已停用
dev.payment.refund.completed     # 开发环境-支付域-退款-已完成
```

```java
// 正确：集中管理 Topic 常量
public final class KafkaTopics {
    public static final String ORDER_CREATED = "prod.order.order.created";
    public static final String USER_DEACTIVATED = "prod.user.user.deactivated";

    private KafkaTopics() {}
}

// 错误：Topic 名称散落在代码中，或不符合命名约定
@KafkaListener(topics = "order_topic")         // 缺少环境、域前缀
@KafkaListener(topics = "OrderCreatedEvent")    // 大驼峰，不符合命名约定
```

**MSG-KF-002** Partition 数量 MUST 根据预估吞吐量显式指定，MUST NOT 依赖 Broker 默认值。同一业务域内的关联 Topic SHOULD 使用相同 Partition 数以支持 Co-Partitioning。 [MUST/SHOULD]

```java
@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic orderCreatedTopic() {
        // 显式指定 Partition 数为 6，副本数为 3
        return TopicBuilder.name("prod.order.order.created")
                .partitions(6)
                .replicas(3)
                .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(7).toMillis()))
                .build();
    }

    @Bean
    public NewTopic orderConfirmedTopic() {
        // 同域 Topic 保持相同 Partition 数，支持 Co-Partitioning
        return TopicBuilder.name("prod.order.order.confirmed")
                .partitions(6)
                .replicas(3)
                .build();
    }
}
```

**MSG-KF-003** Retention 策略 MUST 根据业务场景显式配置。事件流类 Topic SHOULD 使用较长 Retention（7~30天），临时指令类 Topic MAY 使用较短 Retention（1~3天）。 [MUST/SHOULD/MAY]

| Topic 类型 | Retention 建议 | 说明 |
|-----------|--------------|------|
| 领域事件 | 7~30 天 | 需要支持回放与审计 |
| 状态变更通知 | 3~7 天 | 仅需保证消费完成 |
| 临时指令/信号 | 1~3 天 | 一次性消费，无需回放 |

```java
// 领域事件 Topic：保留 30 天
TopicBuilder.name("prod.order.order.created")
    .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(30).toMillis()))
    .build();

// 临时指令 Topic：保留 1 天
TopicBuilder.name("prod.order.cache.invalidate")
    .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(Duration.ofDays(1).toMillis()))
    .build();
```

---

## 二、消息格式规范

**MSG-KF-004** 所有 Kafka 消息 MUST 使用统一信封结构 `MessageEnvelope<T>`，MUST NOT 直接发送裸业务对象。 [MUST]

```java
@Data
@Builder
public class MessageEnvelope<T> {
    /** 全局唯一消息ID，用于幂等与追踪 */
    private String messageId;
    /** 消息产生时间（UTC） */
    private Instant timestamp;
    /** 消息来源服务标识 */
    private String source;
    /** 业务载荷 */
    private T payload;
    /** 载荷 Schema 版本号，用于向前兼容 */
    private int schemaVersion;
    /** 事件类型标识，如 order.order.created */
    private String eventType;

    public static <T> MessageEnvelope<T> wrap(String eventType, T payload, int schemaVersion) {
        return MessageEnvelope.<T>builder()
                .messageId(UUID.randomUUID().toString())
                .timestamp(Instant.now())
                .source(ServiceInfo.getName())
                .eventType(eventType)
                .payload(payload)
                .schemaVersion(schemaVersion)
                .build();
    }
}
```

```java
// 正确：使用统一信封包装
OrderCreatedPayload payload = new OrderCreatedPayload(orderId, userId, totalAmount);
MessageEnvelope<OrderCreatedPayload> message = MessageEnvelope.wrap(
    "order.order.created", payload, 1
);
kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(orderId), message);

// 错误：直接发送裸对象，缺少元数据
kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(orderId), orderCreatedPayload);
```

**MSG-KF-005** 序列化方式 MUST 统一为 JSON（使用 `JsonSerializer` / `JsonDeserializer`）。当消息体积大或 Schema 变更频繁时 MAY 升级至 Avro + Schema Registry。 [MUST/MAY]

```yaml
spring:
  kafka:
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.example.*"
        spring.json.value.default.type: "com.example.common.messaging.MessageEnvelope"
```

```java
// Avro 方案示例（可选）
@Configuration
public class KafkaAvroConfig {
    @Bean
    public Map<String, Object> producerAvroConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put(AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://schema-registry:8081");
        return props;
    }
}
```

**MSG-KF-006** 消息 Key MUST 使用业务上有意义的字段（如聚合根 ID），MUST NOT 使用 null 或随机值（除非明确要求广播到随机分区）。 [MUST]

```java
// 正确：使用聚合根 ID 作为 Key，保证同一聚合的事件进入同一 Partition
kafkaTemplate.send(topic, String.valueOf(order.getId()), envelope);

// 错误：Key 为 null，消息随机分配 Partition，无法保证顺序性
kafkaTemplate.send(topic, null, envelope);
```

---

## 三、生产者规范

**MSG-KF-007** 生产环境 Producer MUST 配置 `acks=all` 和 `enable.idempotence=true`，保证消息可靠投递且不重复。 [MUST]

```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
        delivery.timeout.ms: 120000
        linger.ms: 5
        batch.size: 16384
```

```java
// 正确：通过配置统一设定，无需在代码中重复
// 见上方 application.yml

// 错误：代码中手动覆盖关键配置，绕过统一标准
@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.ACKS_CONFIG, "0");  // 危险：消息可能丢失
    props.put(ProducerConfig.RETRIES_CONFIG, 0);  // 危险：无重试
    return new DefaultKafkaProducerFactory<>(props);
}
```

**MSG-KF-008** 发送确认模式 SHOULD 使用异步回调（`ListenableFuture`），仅在需要严格顺序确认的场景使用同步发送。Fire-and-Forget 模式 MUST NOT 用于业务关键消息。 [SHOULD/MUST]

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final KafkaTemplate<String, MessageEnvelope<?>> kafkaTemplate;

    /**
     * 异步发送（推荐）：适合大多数业务场景
     */
    public void sendOrderCreated(OrderCreatedPayload payload) {
        MessageEnvelope<OrderCreatedPayload> envelope =
                MessageEnvelope.wrap("order.order.created", payload, 1);

        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(payload.getOrderId()), envelope)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("消息发送失败: messageId={}, topic={}",
                                envelope.getMessageId(), KafkaTopics.ORDER_CREATED, ex);
                    } else {
                        log.info("消息发送成功: messageId={}, partition={}, offset={}",
                                envelope.getMessageId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }

    /**
     * 同步发送（仅在需要严格确认时使用）
     */
    public void sendOrderConfirmedSync(OrderConfirmedPayload payload) {
        MessageEnvelope<OrderConfirmedPayload> envelope =
                MessageEnvelope.wrap("order.order.confirmed", payload, 1);
        try {
            kafkaTemplate.send(KafkaTopics.ORDER_CONFIRMED, String.valueOf(payload.getOrderId()), envelope)
                    .get(5, TimeUnit.SECONDS);
            log.info("同步发送确认成功: orderId={}", payload.getOrderId());
        } catch (Exception e) {
            log.error("同步发送失败: orderId={}", payload.getOrderId(), e);
            throw new MessageSendException("消息发送失败", e);
        }
    }
}
```

**MSG-KF-009** Producer 的 `batch.size` SHOULD 设置为 16KB~64KB，`linger.ms` SHOULD 设置为 5~50ms，在吞吐与延迟间取得平衡。 [SHOULD]

| 场景 | batch.size | linger.ms | 说明 |
|------|-----------|-----------|------|
| 低延迟优先 | 8192 | 1 | 牺牲吞吐换取低延迟 |
| 均衡（推荐） | 16384 | 5~10 | 适合大多数业务场景 |
| 高吞吐优先 | 65536 | 20~50 | 日志采集、数据管道 |

---

## 四、消费者规范

**MSG-KF-010** Consumer Group 名称 MUST 遵循 `{service-name}-{topic-short-name}` 格式，MUST NOT 使用默认 Group 或硬编码通用名称。 [MUST]

```yaml
spring:
  kafka:
    consumer:
      group-id: order-service-order-created
      auto-offset-reset: earliest
      enable-auto-commit: false  # 手动提交
      max-poll-records: 500
      properties:
        fetch.min.bytes: 1
        fetch.max.wait.ms: 500
```

```java
// 正确：Group 名称包含服务名与 Topic 简名，便于排查
@KafkaListener(
    topics = KafkaTopics.ORDER_CREATED,
    groupId = "order-service-order-created"
)
public void handleOrderCreated(ConsumerRecord<String, MessageEnvelope<OrderCreatedPayload>> record) {
    // ...
}

// 错误：Group 名称过于笼统或遗漏
@KafkaListener(topics = KafkaTopics.ORDER_CREATED)  // 使用默认 Group
@KafkaListener(topics = KafkaTopics.ORDER_CREATED, groupId = "my-group") // 通用名称
```

**MSG-KF-011** 消费者 MUST 关闭自动提交（`enable.auto.commit=false`），使用手动 ACK 模式，确保消息处理成功后才提交 offset。 [MUST]

```java
@KafkaListener(
    topics = KafkaTopics.ORDER_CREATED,
    groupId = "order-service-order-created"
)
public void handleOrderCreated(
        ConsumerRecord<String, MessageEnvelope<OrderCreatedPayload>> record,
        Acknowledgment ack) {
    try {
        processEvent(record.value().getPayload());
        // 处理成功后手动提交
        ack.acknowledge();
        log.info("消息处理完成: messageId={}, offset={}",
                record.value().getMessageId(), record.offset());
    } catch (Exception e) {
        log.error("消息处理失败: messageId={}, offset={}",
                record.value().getMessageId(), record.offset(), e);
        // 不提交，触发重试或进入死信队列
        throw e;
    }
}
```

```java
// 配置手动 ACK 模式
@Configuration
public class KafkaConsumerConfig {
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>> kafkaListenerContainerFactory(
            ConsumerFactory<String, MessageEnvelope<?>> consumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // 手动 ACK 模式
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        factory.setConcurrency(3);
        return factory;
    }
}
```

**MSG-KF-012** 消费者 MUST 保证幂等性，同一消息多次投递结果一致。SHOULD 使用去重表（数据库唯一约束）或 Redis `SETNX` 实现幂等校验。 [MUST/SHOULD]

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedEventHandler {

    private final MessageDeduplicationRepository deduplicationRepo;
    private final InventoryService inventoryService;

    @KafkaListener(
        topics = KafkaTopics.ORDER_CREATED,
        groupId = "inventory-service-order-created"
    )
    public void handle(ConsumerRecord<String, MessageEnvelope<OrderCreatedPayload>> record,
                       Acknowledgment ack) {
        MessageEnvelope<OrderCreatedPayload> envelope = record.value();
        String messageId = envelope.getMessageId();

        // 幂等校验：基于 messageId 去重
        if (!deduplicationRepo.tryMarkConsumed(messageId, envelope.getEventType())) {
            log.warn("重复消息已跳过: messageId={}", messageId);
            ack.acknowledge();
            return;
        }

        try {
            inventoryService.reserveStock(envelope.getPayload());
            ack.acknowledge();
        } catch (Exception e) {
            // 回滚去重标记，允许重试
            deduplicationRepo.unmarkConsumed(messageId);
            throw e;
        }
    }
}
```

```java
// 去重表实现
@Repository
@RequiredArgsConstructor
public class JdbcMessageDeduplicationRepository implements MessageDeduplicationRepository {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public boolean tryMarkConsumed(String messageId, String eventType) {
        try {
            jdbcTemplate.update(
                "INSERT INTO consumed_message (message_id, event_type, consumed_at) VALUES (?, ?, ?)",
                messageId, eventType, Instant.now()
            );
            return true;
        } catch (DuplicateKeyException e) {
            return false; // 已消费过
        }
    }
}
```

**MSG-KF-013** 消费者 MUST 配置死信队列（DLQ）策略。处理失败的消息 MUST NOT 无限重试阻塞 Partition，SHOULD 在有限次重试后转入 DLQ Topic。 [MUST/SHOULD]

```java
@Configuration
public class KafkaErrorHandlerConfig {

    @Bean
    public CommonErrorHandler errorHandler(KafkaTemplate<String, Object> kafkaTemplate) {
        // 死信队列发布器
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate,
                    (record, ex) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // 固定间隔重试 3 次，每次间隔 5 秒，失败后进入 DLQ
        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer,
                new FixedBackOff(5000L, 3L));

        // 不可重试异常直接进入 DLQ
        handler.addNotRetryableExceptions(
                DeserializationException.class,
                MessageConversionException.class
        );

        return handler;
    }
}
```

```java
// DLQ 消费者：独立处理死信消息
@Component
@Slf4j
@RequiredArgsConstructor
public class DeadLetterConsumer {

    private final DlqMessageRepository dlqRepository;

    @KafkaListener(
        topics = "prod.order.order.created.DLT",
        groupId = "order-service-dlt-consumer"
    )
    public void handleDeadLetter(ConsumerRecord<String, MessageEnvelope<?>> record,
                                 Acknowledgment ack) {
        log.error("收到死信消息: topic={}, partition={}, offset={}, messageId={}",
                record.topic(), record.partition(), record.offset(),
                record.value() != null ? record.value().getMessageId() : "null");

        // 持久化死信消息，支持后续人工排查或重放
        dlqRepository.save(DlqMessage.builder()
                .originalTopic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .messageId(record.value() != null ? record.value().getMessageId() : null)
                .payload(JsonUtils.toJson(record.value()))
                .failedAt(Instant.now())
                .build());

        ack.acknowledge();
    }
}
```

**MSG-KF-014** 消费者 SHOULD 配置合理的 `max.poll.records` 和 `max.poll.interval.ms`，避免单次拉取过多消息导致 Rebalance。 [SHOULD]

```yaml
spring:
  kafka:
    consumer:
      max-poll-records: 200          # 单次拉取最大消息数
      properties:
        max.poll.interval.ms: 300000  # 5 分钟内必须完成处理并提交
        session.timeout.ms: 30000     # 心跳超时 30 秒
        heartbeat.interval.ms: 10000  # 心跳间隔 10 秒
```

---

## 五、事务消息

**MSG-KF-015** 需要保证"数据库写入 + 消息发送"原子性时，MUST 使用 Kafka 事务（`read_committed` 隔离级别）或 Outbox + CDC 模式，MUST NOT 在业务事务内直接调用 `kafkaTemplate.send()` 而不使用事务管理。 [MUST]

```java
// 方案一：Kafka 事务（适合消息量较小的场景）
@Configuration
public class KafkaTransactionConfig {

    @Bean
    public ProducerFactory<String, Object> transactionalProducerFactory() {
        DefaultKafkaProducerFactory<String, Object> factory =
                new DefaultKafkaProducerFactory<>(producerConfigs());
        factory.setTransactionIdPrefix("order-service-tx-");
        return factory;
    }

    @Bean
    public KafkaTransactionManager<String, Object> kafkaTransactionManager(
            ProducerFactory<String, Object> transactionalProducerFactory) {
        return new KafkaTransactionManager<>(transactionalProducerFactory);
    }
}

@Service
@RequiredArgsConstructor
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final KafkaTemplate<String, MessageEnvelope<?>> kafkaTemplate;

    @Transactional("kafkaTransactionManager")
    public Long createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(cmd);
        orderRepository.save(order);

        // 在 Kafka 事务内发送消息，与数据库操作共享事务边界
        MessageEnvelope<OrderCreatedPayload> envelope =
                MessageEnvelope.wrap("order.order.created", new OrderCreatedPayload(order), 1);
        kafkaTemplate.send(KafkaTopics.ORDER_CREATED, String.valueOf(order.getId()), envelope);

        return order.getId();
    }
}
```

```java
// 方案二：Outbox + Kafka Connect CDC（推荐，适合高吞吐场景）
// 业务代码仅写入 Outbox 表，由 Debezium/Kafka Connect 异步投递
@Service
@RequiredArgsConstructor
public class OrderCommandServiceOutbox {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;

    @Transactional(rollbackFor = Exception.class)
    public Long createOrder(CreateOrderCommand cmd) {
        Order order = Order.create(cmd);
        orderRepository.save(order);

        // 写入 Outbox 表，与业务数据在同一个数据库事务中
        outboxRepository.save(OutboxMessage.builder()
                .aggregateId(String.valueOf(order.getId()))
                .aggregateType("Order")
                .eventType("order.order.created")
                .payload(JsonUtils.toJson(new OrderCreatedPayload(order)))
                .schemaVersion(1)
                .createdAt(Instant.now())
                .build());

        return order.getId();
    }
}
```

**MSG-KF-016** 消费者端 MUST 配置 `isolation.level=read_committed`，确保只消费事务已提交的消息。 [MUST]

```yaml
spring:
  kafka:
    consumer:
      properties:
        isolation.level: read_committed
```

**MSG-KF-017** 使用 Outbox 模式时，SHOULD 配合 Kafka Connect + Debezium CDC 自动投递，MAY 使用定时轮询 Outbox 表作为降级方案。Outbox 表 MUST 包含索引以支持高效扫描。 [SHOULD/MAY/MUST]

```java
// Outbox 表结构
@Entity
@Table(name = "outbox_message", indexes = {
    @Index(name = "idx_outbox_status_created", columnList = "status,created_at"),
    @Index(name = "idx_outbox_aggregate", columnList = "aggregate_type,aggregate_id")
})
@Data
@Builder
public class OutboxMessage {
    @Id
    private String id;
    private String aggregateId;
    private String aggregateType;
    private String eventType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    private int schemaVersion;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status; // PENDING, PUBLISHED, FAILED
}
```

---

## 六、监控与运维

**MSG-KF-018** 项目 MUST 暴露 Kafka 关键 Metrics 至监控系统（Prometheus / Micrometer），至少包含以下指标： [MUST]

| 指标 | 类型 | 说明 |
|------|------|------|
| `kafka.consumer.group.lag` | Gauge | Consumer Group 消费延迟（Lag） |
| `kafka.consumer.records.consumed.rate` | Gauge | 消费速率（条/秒） |
| `kafka.consumer.fetch.latency.avg` | Gauge | Fetch 请求平均延迟 |
| `kafka.consumer.rebalance.total` | Counter | Rebalance 发生次数 |
| `kafka.producer.records.send.rate` | Gauge | 生产速率（条/秒） |
| `kafka.producer.request.latency.avg` | Gauge | 生产请求平均延迟 |
| `kafka.producer.errors.total` | Counter | 生产失败总数 |

```java
@Configuration
public class KafkaMetricsConfig {

    @Bean
    public MetricsConsumerInterceptor<String, Object> metricsConsumerInterceptor(MeterRegistry meterRegistry) {
        return new MetricsConsumerInterceptor<>(meterRegistry, "order-service");
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>> kafkaListenerContainerFactory(
            ConsumerFactory<String, MessageEnvelope<?>> consumerFactory,
            MeterRegistry meterRegistry) {
        ConcurrentKafkaListenerContainerFactory<String, MessageEnvelope<?>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        // 注册 Micrometer 监控
        factory.getContainerProperties().setMicrometerEnabled(true);
        factory.getContainerProperties().setMicrometerTags(
                Map.of("service", "order-service"));
        return factory;
    }
}
```

**MSG-KF-019** Consumer Lag MUST 配置告警，告警阈值 SHOULD 根据业务 SLA 设定。以下推荐基线值： [MUST/SHOULD]

| 告警级别 | Lag 阈值 | 说明 |
|---------|---------|------|
| Warning | Lag > 1000 | 消费略有延迟，需关注 |
| Critical | Lag > 10000 | 消费严重滞后，需立即排查 |
| Emergency | Lag > 100000 或持续增长 > 5min | 消费者可能已停止工作 |

```java
// 自定义 Lag 告警检查（集成到定时任务或监控系统）
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsumerLagMonitor {

    private final AdminClient adminClient;
    private final AlertService alertService;

    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkConsumerLag() {
        Map<String, Long> lags = calculateLag("order-service-order-created");
        lags.forEach((topic, lag) -> {
            if (lag > 100000) {
                alertService.sendEmergency(
                    String.format("Consumer Lag 紧急告警: topic=%s, lag=%d", topic, lag));
            } else if (lag > 10000) {
                alertService.sendCritical(
                    String.format("Consumer Lag 严重告警: topic=%s, lag=%d", topic, lag));
            } else if (lag > 1000) {
                alertService.sendWarning(
                    String.format("Consumer Lag 预警: topic=%s, lag=%d", topic, lag));
            }
        });
    }
}
```

**MSG-KF-020** 日志 MUST 记录消息的关键元数据（messageId、topic、partition、offset），MUST NOT 打印完整消息载荷（避免日志膨胀）。异常场景 MUST 记录完整堆栈。 [MUST]

```java
// 正确：记录关键元数据，便于链路追踪
log.info("消费消息: messageId={}, topic={}, partition={}, offset={}",
        envelope.getMessageId(), record.topic(), record.partition(), record.offset());

// 错误：打印完整载荷，导致日志膨胀
log.info("消费消息: {}", JsonUtils.toJson(envelope));  // 禁止

// 异常场景：记录完整堆栈
log.error("消息处理失败: messageId={}, topic={}, partition={}, offset={}",
        envelope.getMessageId(), record.topic(), record.partition(), record.offset(), exception);
```
