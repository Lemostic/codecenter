| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | messaging |
| 引入条件 | `fingerprint.profiles contains 'messaging-rocketmq'` |
| 适用架构 | Spring Boot + Apache RocketMQ |
| 依赖规范 | `universal/naming-conventions.md`、`universal/logging-standards.md` |
| 互斥规范 | `messaging-kafka`（同一项目二选一） |

# RocketMQ 消息队列规范

> 本包定义 Apache RocketMQ 在 Spring Boot 项目中的使用约定,涵盖 Topic/Tag 设计、消息格式、生产者/消费者规范、事务消息、顺序消息、延迟消息及监控运维。
>
> 与 `messaging-kafka` 互斥:同一项目二选一,切换需评审。`arch-ddd/event-patterns` 定义"事件是什么",本包定义"事件如何用 RocketMQ 传输"。

---

## 一、Topic 与 Tag 设计

### 规范 MSG-RMQ-001: Topic 命名 [MUST]

**规则:**

1. Topic 名 MUST 使用业务域前缀 + 业务实体 + 动作格式:`<业务域>_<实体>_<动作>`,全小写 + 下划线分隔。
2. 示例:`order_order_created`、`payment_payment_succeeded`、`user_user_registered`。
3. Topic 名长度 SHOULD ≤ 64 字符,RocketMQ 单 Topic 名上限 127 字符。
4. Topic 名 MUST NOT 含敏感信息(密码、token、个人身份信息)。
5. Topic 名 SHOULD 在团队 Wiki 集中登记,避免重复创建含义相近的 Topic。
6. 一个 Topic 对应一个业务实体的一个事件类型(创建/更新/删除分别建 Topic 或用 Tag 区分)。
7. 禁止使用驼峰或空格,RocketMQ 控制台对非小写 Topic 名兼容性差。
8. 跨服务的事件 SHOULD 用独立 Topic(`inventory_reserved`、`shipment_shipped`),禁止多服务共用一个 Topic。

**示例:**

```
# ✅ 正确
order_order_created
order_order_cancelled
payment_payment_succeeded
payment_refund_initiated

# ❌ 错误
OrderCreated                      # 驼峰
order-created                     # 横线
user_change                       # 含义模糊
user/change                       # 斜杠
```

---

### 规范 MSG-RMQ-002: Tag 设计 [MUST]

**规则:**

1. 当一个 Topic 下需要细分消息类型时,使用 Tag 区分,Tag 名 MUST 用全小写 + 下划线。
2. Tag 名 SHOULD 描述消息子类型(`vip`、`retry`、`urgent`),不描述消息内容(内容在 body)。
3. 单 Topic 下 Tag 数 SHOULD ≤ 10,过多时拆为独立 Topic。
4. 消费者订阅时 SHOULD 同时指定 Topic + Tag,避免拉到不需要的消息(`MessageSelector.byTag(...)`)。
5. Tag 用于消息过滤,不用于业务逻辑判断(业务判断由消费者解析 body 完成)。
6. 禁止用 Tag 传业务数据,Tag 只用于路由。

---

## 二、消息格式

### 规范 MSG-RMQ-003: 消息信封 [MUST]

**规则:**

1. 消息 body MUST 是 JSON 字符串,禁止二进制、Protobuf、纯文本(除非跨语言兼容要求)。
2. 消息 MUST 包含标准信封字段,统一规范:

```json
{
  "messageId": "uuid-xxx",                  // 消息唯一 ID,用于幂等去重
  "eventType": "order.order.created",        // 事件类型,匹配 Topic + Tag
  "eventVersion": "v1",                      // 事件 schema 版本
  "occurredAt": "2026-06-18T10:30:00Z",     // 事件发生时间(UTC ISO 8601)
  "producer": "order-service",               // 生产服务名
  "traceId": "trace-xxx",                   // 链路追踪 ID,与请求 traceId 一致
  "payload": { ... }                         // 业务数据
}
```

3. `messageId` MUST 用 UUID v4 或雪花算法,保证全局唯一(用于消费者幂等)。
4. `eventVersion` 字段 SHOULD 跟随业务 schema 演进,消费者按 version 决定是否兼容。
5. `occurredAt` MUST 用 UTC ISO 8601,禁止本地时间或时间戳(避免时区混乱)。
6. 消息体大小 SHOULD ≤ **128 KB**(RocketMQ 默认上限 4 MB,但小消息更利于集群稳定性)。

---

## 三、生产者规范

### 规范 MSG-RMQ-004: 同步发送与异步发送 [MUST]

**规则:**

1. 业务主流程 SHOULD 优先用**同步发送**(`rocketMQTemplate.syncSend`),保证消息发送成功后再返回业务结果。
2. 异步发送(`asyncSend`) SHOULD 仅用于"允许少量丢失"的非关键消息(如埋点、日志聚合)。
3. 禁止使用 `sendOneWay`(无返回值,无法保证送达),除非场景明确允许丢失。
4. 同步发送 MUST 设置超时时间(默认 3 秒),超时 MUST 记录告警并降级处理(本地持久化 + 定时重投)。
5. 发送失败 MUST 重试(最多 3 次,指数退避),重试后仍失败 MUST 写本地消息表 + 异步补偿。
6. 业务主流程与消息发送 SHOULD 走"事务消息"或"本地消息表"模式保证最终一致性,避免数据库事务成功但消息未发送(详见 MSG-RMQ-007)。

**示例:**

```java
@Service
public class OrderService {

    @Autowired private RocketMQTemplate rocketMQTemplate;

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        // 1. 业务落库
        Order order = orderRepository.save(new Order(request));

        // 2. 同步发送事件
        Map<String, Object> event = Map.of(
            "orderId", order.getId(),
            "amount", order.getAmount(),
            "userId", order.getUserId()
        );
        Message message = MessageBuilder.withBody(JsonUtils.toJson(event).getBytes())
            .setHeader(RocketMQHeaders.KEYS, order.getId())  // 业务主键
            .build();

        SendResult result = rocketMQTemplate.syncSend(
            "order_order_created:order",  // topic:tag
            message,
            3000  // 超时 3 秒
        );
        log.info("Order event sent, msgId={}", result.getMsgId());
        return order;
    }
}
```

---

### 规范 MSG-RMQ-005: 消息 Key [MUST]

**规则:**

1. 每条消息 MUST 设置 `keys`(业务主键),RocketMQ 内部按 Key 做 hash 路由到特定队列,保证同一业务实体的消息顺序。
2. `keys` 取值 SHOULD 是业务实体的唯一 ID(如 `orderId`、`userId`)。
3. 多个 key 之间用空格分隔(如 `"orderId itemId"`)。
4. 消费者 SHOULD 用 `keys` 做幂等去重(配合业务表唯一索引或 Redis 去重表)。

---

## 四、消费者规范

### 规范 MSG-RMQ-006: 消费模式与并发 [MUST]

**规则:**

1. 消费模式 MUST 用 **集群模式**(默认,`MessageModel.CLUSTERING`),同一条消息只被一个消费者实例消费(避免重复)。
2. 广播模式(`BROADCASTING`) MUST 仅用于"每个实例都需要消费"的场景(如本地缓存刷新),且禁止依赖消费结果做业务写入。
3. 消费者线程数(`consumeThreadMin` / `consumeThreadMax`) SHOULD 根据业务处理速度设置,默认 `min=20, max=64`。
4. 批量消费(`batchSize`) SHOULD ≤ 32,过大时单次失败回滚成本高。
5. 消费失败重试(`retryTimesWhenConsumeFailed`) SHOULD 设为 3 次,超过后转入死信队列(`%DLQ%Topic`)。
6. 消费模式选择 SHOULD 与 Kafka 保持一致语义,RocketMQ 集群模式 ≈ Kafka consumer group 模式。

**示例:**

```java
@RocketMQMessageListener(
    topic = "order_order_created",
    selectorExpression = "order",  // 订阅 Tag = order
    consumerGroup = "order-service-consumer",
    messageModel = MessageModel.CLUSTERING,
    consumeThreadMin = 20,
    consumeThreadMax = 64,
    maxReconsumeTimes = 3  // 失败重试 3 次后入死信
)
public class OrderCreatedListener implements RocketMQListener<OrderCreatedEvent> {

    @Override
    public void onMessage(OrderCreatedEvent event) {
        // 处理业务逻辑
    }
}
```

---

### 规范 MSG-RMQ-007: 幂等消费 [MUST]

**规则:**

1. 消费者 MUST 实现幂等(同一 messageId 多次消费结果一致),因为 RocketMQ 至少一次投递语义,网络异常时可能重复。
2. 幂等去重 SHOULD 用以下任一方式:
   - 业务表唯一索引(把 messageId 写入业务表,DB 唯一约束保证不重复)
   - Redis 去重表(用 `SETNX messageId` 记录 24 小时)
   - 数据库唯一日志表(独立 `consumed_messages(message_id, consumer_group, consumed_at)`)
3. 消费者 MUST 在完成业务逻辑后记录 messageId(入库或 Redis),失败前不要记录(否则丢消息)。
4. 死信队列消息处理 MUST 人工介入,禁止自动重投(避免毒消息反复消费)。

**示例:**

```java
@Override
public void onMessage(OrderCreatedEvent event) {
    // 幂等去重
    String dedupKey = "consumed:" + event.getMessageId();
    Boolean firstTime = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", 24, TimeUnit.HOURS);
    if (Boolean.FALSE.equals(firstTime)) {
        log.info("Duplicate message, skipped: {}", event.getMessageId());
        return;
    }

    // 业务处理
    orderProjectionService.upsertFromEvent(event);
}
```

---

### 规范 MSG-RMQ-008: 消费失败处理 [MUST]

**规则:**

1. 消费者抛出异常时,RocketMQ 自动重试,重试间隔默认 10s / 30s / 1min / 2min / 5min(可配置)。
2. 重试耗尽后消息进入 `%DLQ%<原 Topic>`,消费者 MUST 监控死信队列告警。
3. 死信消息 SHOULD 落库(`dlq_messages` 表)以便人工排查,禁止直接丢弃。
4. 业务异常 SHOULD 区分:
   - **可重试异常**(网络抖动、临时 DB 锁):抛出运行时异常,触发重试。
   - **不可重试异常**(消息格式错误、业务规则违反):捕获后记录日志 + 跳过 + 入死信(避免毒消息反复重试)。
5. 消费失败时 MUST 打印 messageId + topic + tag + body 摘要,便于排查。

---

## 五、事务消息

### 规范 MSG-RMQ-009: RocketMQ 事务消息 [SHOULD]

**规则:**

1. 业务主流程涉及"DB 写入 + 消息发送"两阶段时,优先用 RocketMQ 事务消息(`@RocketMQTransactionListener`)保证最终一致性。
2. 事务消息执行流程:
   - 发送 half 消息(对消费者不可见)
   - 执行本地事务(DB 写入)
   - 根据本地事务结果 Commit 或 Rollback half 消息
   - Commit 失败时 RocketMQ 自动回查(默认 1 分钟),通过 `checkLocalTransaction` 确认本地事务状态
3. 禁止在事务消息中处理耗时业务(> 5 秒),会触发回查超时。
4. `checkLocalTransaction` MUST 是幂等的(可重复执行,返回一致结果),可查询本地事务表。
5. 简单场景优先用**本地消息表**模式(详见 MSG-RMQ-010),事务消息仅用于"业务写入与消息发送强一致"。

**示例:**

```java
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Autowired private OrderTransactionLogRepository logRepository;

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        // 本地事务执行:写 DB + 写事务日志表(记录 messageId + 事务状态)
        try {
            OrderTransactionLog log = new OrderTransactionLog(messageId, "PENDING");
            logRepository.save(log);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        // 回查:根据 messageId 查事务日志表
        return logRepository.findById(messageId)
            .map(log -> "COMMITTED".equals(log.getStatus())
                ? RocketMQLocalTransactionState.COMMIT
                : RocketMQLocalTransactionState.ROLLBACK)
            .orElse(RocketMQLocalTransactionState.ROLLBACK);
    }
}
```

---

## 六、顺序消息

### 规范 MSG-RMQ-010: 顺序保证 [MUST]

**规则:**

1. 业务要求"同一业务实体的消息严格按发送顺序消费"时,使用**顺序消息**(`MessageQueueSelector`,按 key hash 选同一队列)。
2. 顺序消息的 key MUST 是业务主键(如 orderId),RocketMQ 通过 hash(key) % queueNum 固定到同一队列。
3. 顺序消费 MUST 用单线程(`consumeThreadMin = consumeThreadMax = 1`),否则顺序无法保证。
4. 顺序消息消费失败 MUST 暂停后续消费(阻塞当前队列),人工修复后才能继续。
5. 顺序消费性能 SHOULD 评估,单队列吞吐上限 1000-2000 QPS,业务量大时考虑分片。
6. 非必要不使用顺序消息(性能成本高),能用业务幂等解决的 SHOULD 用普通消息。

**示例:**

```java
SendResult result = rocketMQTemplate.syncSendOrderly(
    "order_order_status_changed",  // topic
    message,
    orderId,  // hash key,保证同一订单的消息固定到同一队列
    3000  // 超时
);
```

---

## 七、延迟消息

### 规范 MSG-RMQ-011: 延迟消息 [SHOULD]

**规则:**

1. 业务需要"延迟 N 秒后消费"(如订单 30 分钟未支付自动取消)时,使用 RocketMQ 延迟消息。
2. 延迟级别固定为:`1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h`,共 18 级。
3. 任意延迟时间 SHOULD 向上对齐到最近的延迟级别(如需要 7 分钟,选 5m 或 10m,严禁 7 分钟)。
4. 自定义延迟时间(非标准级别) MUST 升级 RocketMQ 版本 ≥ 5.x,并配置 broker `messageDelayLevel`。
5. 延迟消息 MUST 设置消息 Key,业务回查时定位具体订单。
6. 延迟消息消费失败时,重试 3 次后入死信,人工处理(避免"过期订单被错误取消"循环)。

---

## 八、生产环境配置

### 规范 MSG-RMQ-012: NameServer 与 Broker [MUST]

**规则:**

1. 生产环境 NameServer MUST 部署至少 2 个节点(避免单点故障),Broker 至少 2 主 2 从(主从不同机器)。
2. `rocketmq.name-srv-addr` MUST 配置为 NameServer 集群地址(`ip1:9876;ip2:9876`)。
3. Broker `brokerName` SHOULD 唯一标识,跨机房 SHOULD 用前缀区分(`broker_a_shanghai`、`broker_b_shanghai`)。
4. Broker SHOULD 开启 `brokerRole=ASYNC_MASTER`(异步主从同步)或 `SYNC_MASTER`(同步主从,数据强一致)。
5. 生产环境禁止使用 `brokerRole=ASYNC_MASTER` 与 SYNC_MASTER 混用,会引发数据不一致。
6. Broker 磁盘 MUST 用 SSD,IO 性能直接影响吞吐。
7. Topic 创建 SHOULD 在 RocketMQ 控制台预先创建,设置 `readQueueNums=8, writeQueueNums=8`(读写队列数)。

---

### 规范 MSG-RMQ-013: Spring Boot 集成 [MUST]

**规则:**

1. `pom.xml` MUST 引入 `org.apache.rocketmq:rocketmq-spring-boot-starter`,版本与 RocketMQ 服务端版本兼容。
2. `application.yml` MUST 配置 `rocketmq.name-server` 与 `rocketmq.producer.group`。
3. 生产者 `group` 名 SHOULD 用服务名(`order-service-producer`)。
4. 消费者 `group` 名 SHOULD 用服务名 + 业务域(`order-service-order-created-consumer`)。
5. 同一 group 下的消费者实例数 SHOULD ≤ Topic 队列数,否则多余实例无消息可消费(浪费资源)。
6. 生产环境 SHOULD 配置 `rocketmq.producer.send-message-timeout=3000`、`rocketmq.producer.max-message-size=131072`(128KB)。
7. `application-test.yml` SHOULD 用 embedded RocketMQ 或 TestContainers 启动本地 broker,禁止连生产环境测试。

---

## 九、监控与告警

### 规范 MSG-RMQ-014: 关键指标 [MUST]

**规则:**

1. 生产环境 MUST 监控以下指标:
   - 生产者发送成功率(`< 99.9%` 告警)
   - 生产者发送延迟 P99(`> 1s` 告警)
   - 消费者消费延迟 P99(`> 5s` 告警)
   - 消费者积压量(consumer offset 与 broker commit offset 差距,`> 10000` 告警)
   - 死信队列消息数(`> 0` 告警)
2. 监控 SHOULD 用 Prometheus + Grafana,RocketMQ Exporter 暴露 JMX 指标。
3. 关键告警 SHOULD 配置 PagerDuty / 钉钉机器人 / 飞书机器人,7×24 响应。
4. 消费者处理失败率 SHOULD 持续监控,失败率 > 1% 时人工介入。
5. 消息积压超过阈值 SHOULD 自动扩容消费者实例(若 Topic 队列数允许)。

---

### 规范 MSG-RMQ-015: 消息追踪与审计 [MUST]

**规则:**

1. 关键业务消息(支付、订单) MUST 启用 RocketMQ Trace(消息链路追踪,`rocketmq-trace-topic` 记录全链路)。
2. 消息体 MUST 包含 `traceId`(从 MDC 透传,跨服务传递),便于链路追踪。
3. 审计消息(合规要求) SHOULD 额外写入 `audit_messages` 表,记录 `messageId / topic / producer / consumer / occurredAt / status`。
4. 死信消息 MUST 持久化到 `dlq_messages` 表,保留 30 天以上,便于事后追溯。
5. 任何生产环境消息发送失败 MUST 记录 ERROR 级别日志,含 messageId + topic + 失败原因。

---

## 十、版本与兼容性

### 规范 MSG-RMQ-016: 客户端版本 [MUST]

**规则:**

1. RocketMQ 客户端版本 MUST 与服务端版本匹配(主版本号一致,如 5.x ↔ 5.x)。
2. 升级 RocketMQ 主版本 MUST 先在测试环境验证,再灰度到生产。
3. 禁止混用不同主版本的客户端(生产者 5.1 + 消费者 4.9)连接同一 Broker。
4. 升级前 MUST 备份 Topic 配置与消费位点,异常时回滚。
5. RocketMQ 4.x 升级到 5.x 需关注:
   - 新增 POP 消费模式(可选,默认仍是 PULL)
   - 移除部分旧 API(如 `MessageQueueSelector` 部分方法签名变化)
   - 事务消息 API 增强(支持异步 Commit)

---

## 十一、与 Kafka 的对比选择

### 规范 MSG-RMQ-017: 选型决策 [MUST]

**规则:**

1. 项目选型 RocketMQ vs Kafka MUST 按业务特征评估:
   - **RocketMQ 优势**:事务消息、顺序消息、延迟消息、定时投递(开箱即用)、Java 生态(阿里背景)
   - **Kafka 优势**:超大规模吞吐(百万级 QPS)、流处理生态(KStreams/KSQL)、生态成熟度、跨语言客户端
2. 以下场景优先 RocketMQ:
   - 需要事务消息保证最终一致性
   - 需要延迟消息(订单超时取消)
   - 业务规模中等(万~十万 QPS)
   - Java 单语言技术栈
3. 以下场景优先 Kafka:
   - 超大规模吞吐(十万+ QPS)
   - 需要流处理(实时统计、CEP、ML pipeline)
   - 多语言技术栈(Python / Go / Rust)
4. 选型 SHOULD 在项目立项时确定并写入 ADR(架构决策记录),切换工具需评审。
5. 同一组织内 SHOULD 统一消息中间件选型,避免一个组织用 RocketMQ + Kafka 混搭(运维成本高)。

---

## 十二、错误处理

### 规范 MSG-RMQ-018: 异常分类 [MUST]

**规则:**

1. 生产者异常处理:
   - `RemotingConnectException`(网络不通):重试 3 次,指数退避。
   - `MQClientException`(Broker 错误):记录日志,触发告警。
   - `MQBrokerException`(消息过大):拒绝发送,落本地消息表 + 人工处理。
   - `RemotingTimeoutException`(超时):本地事务回滚 + 告警。
2. 消费者异常处理:
   - `MessageConversionException`(消息反序列化失败):跳过 + 入死信(消息格式错误不可重试)。
   - `MqContextException`(上下文错误):重试 3 次后入死信。
   - `业务异常`:可重试则重试,否则入死信。
3. 死信队列消息处理 MUST 人工介入,禁止自动批量重投(可能引发数据不一致)。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-18 | 初版:Topic/Tag 设计 + 消息信封 + 生产者/消费者规范 + 事务消息 + 顺序消息 + 延迟消息 + 监控 18 条规则 |

---

*本包是 RocketMQ 专用规范,与 `messaging-kafka` 互斥。`arch-ddd/event-patterns` 定义的事件语义在本包落地为具体消息格式与传输协议。*
