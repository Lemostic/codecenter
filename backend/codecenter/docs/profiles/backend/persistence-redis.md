| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | persistence |
| 引入条件 | fingerprint.profiles contains 'persistence-redis' |
| 适用场景 | 使用 Redis 作为缓存或数据存储的 Spring Boot 项目 |
| 依赖规范 | spring-boot-base |

# Redis 持久化规范

本包定义 Redis 在项目中的使用约定，包括缓存策略、数据结构选型与操作规范。

## 连接与配置

**PROF-REDIS-001** Redis 连接配置 MUST 集中在 `application.yml` 中管理，MUST NOT 硬编码连接信息。 [MUST]

```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          max-active: 16
          max-idle: 8
          min-idle: 2
```

**PROF-REDIS-002** 生产环境 MUST 配置连接池，MUST NOT 使用默认单连接。 [MUST]

## Key 设计

**PROF-REDIS-003** Key MUST 使用冒号分隔的层级命名：`{项目}:{模块}:{业务}:{标识}`。 [MUST]

```
myapp:user:info:1001          # 用户信息
myapp:order:detail:20240001   # 订单详情
myapp:lock:create-order:1001  # 分布式锁
myapp:rate-limit:api:192.168.1.1  # 限流
```

**PROF-REDIS-004** Key MUST NOT 过长（建议 ≤ 128 字符），MUST NOT 使用空格或特殊字符。 [MUST]

**PROF-REDIS-005** Key MUST 设置 TTL，MUST NOT 存在永不过期的 Key（除特殊配置类数据）。 [MUST]

```java
@Component
@RequiredArgsConstructor
public class UserCacheService {

    private final StringRedisTemplate redisTemplate;

    private static final String USER_KEY_PREFIX = "myapp:user:info:";
    private static final Duration USER_TTL = Duration.ofMinutes(30);

    public void cacheUser(Long userId, String userJson) {
        redisTemplate.opsForValue().set(
            USER_KEY_PREFIX + userId,
            userJson,
            USER_TTL
        );
    }
}
```

## 数据结构选型

**PROF-REDIS-006** MUST 根据场景选择合适的数据结构：[MUST]

| 场景 | 推荐结构 | 说明 |
|------|---------|------|
| 对象缓存 | String (JSON) | 序列化整个对象 |
| 计数器 | String (INCR) | 阅读量、点赞数 |
| 排行榜 | Sorted Set | 按分数排序 |
| 标签/去重 | Set | 无序、唯一 |
| 队列/消息 | List / Stream | FIFO 消费 |
| 分布式锁 | String + Lua | 或 Redisson |
| 限流 | String + Lua | 固定窗口/滑动窗口 |

## 操作规范

**PROF-REDIS-007** MUST NOT 在生产环境使用 `KEYS *` 命令，SHOULD 使用 `SCAN` 替代。 [MUST/SHOULD]

**PROF-REDIS-008** 批量操作 SHOULD 使用 Pipeline，减少网络往返。 [SHOULD]

**PROF-REDIS-009** 分布式锁 MUST 使用 Redisson 或 Lua 脚本保证原子性，MUST NOT 使用简单的 SETNX。 [MUST]

```java
// 正确：使用 Redisson
RLock lock = redissonClient.getLock("lock:create-order:" + userId);
try {
    if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        // 执行业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) {
        lock.unlock();
    }
}
```

## 缓存一致性

**PROF-REDIS-010** 缓存更新策略 SHOULD 使用"先更新数据库，再删除缓存"（Cache Aside Pattern）。 [SHOULD]

**PROF-REDIS-011** 高并发场景 SHOULD 使用延迟双删或消息队列保证最终一致性。 [SHOULD]

**PROF-REDIS-012** 缓存穿透 MUST 使用空值缓存或布隆过滤器防护。 [MUST]

## 序列化

**PROF-REDIS-013** 序列化方式 MUST 统一配置，SHOULD 使用 `GenericJackson2JsonRedisSerializer`（JSON 可读）。 [MUST/SHOULD]

**PROF-REDIS-014** 缓存对象 MUST 实现 `Serializable` 或提供 JSON 序列化支持。 [MUST]
