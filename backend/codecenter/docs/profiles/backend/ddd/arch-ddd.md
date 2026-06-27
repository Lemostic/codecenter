| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | architecture |
| 引入条件 | fingerprint.profiles contains 'arch-ddd' |
| 适用场景 | 复杂业务领域、事件驱动、四层架构 |
| 依赖规范 | spring-boot-base |
| 互斥规范 | arch-mvc |

# DDD 四层架构结构

本包定义领域驱动设计（DDD）的四层架构结构、依赖方向与工程结构。领域模型设计（聚合根、实体、值对象等）请配合 `domain-model` 模块使用。

## 标准目录布局

```
project/
├── {{project}}-adapter/          # 适配层：对外暴露接口(HTTP/RPC/MQ)
│   └── src/main/java/{{package_base}}/adapter/
│       ├── web/                  # REST Controller
│       ├── rpc/                  # RPC 接口实现
│       └── mq/                   # 消息消费者
│
├── {{project}}-app/              # 应用层：用例编排，事务管理
│   └── src/main/java/{{package_base}}/app/
│       ├── service/              # 应用服务（命令/查询入口）
│       ├── command/              # 命令对象
│       ├── query/                # 查询对象
│       ├── assembler/            # DTO ↔ Domain Entity 转换
│       └── event/                # 事件发布/订阅编排
│
├── {{project}}-domain/           # 领域层：核心业务逻辑
│   └── src/main/java/{{package_base}}/domain/
│       ├── model/
│       │   ├── aggregate/        # 聚合根
│       │   ├── entity/           # 实体
│       │   ├── valueobject/      # 值对象
│       │   └── event/            # 领域事件
│       ├── service/              # 领域服务
│       ├── repository/           # 仓储接口
│       └── gateway/              # 外部网关接口
│
├── {{project}}-infrastructure/   # 基础设施层：技术实现
│   └── src/main/java/{{package_base}}/infrastructure/
│       ├── repository/impl/      # 仓储实现
│       ├── gateway/impl/         # 网关实现
│       ├── converter/            # DO ↔ Domain Entity 转换
│       ├── dataobject/           # 数据库映射对象(DO)
│       ├── mapper/               # 持久层 Mapper
│       └── config/               # 技术配置
│
└── {{project}}-start/            # 启动层：应用入口
    └── src/main/java/{{package_base}}/
        ├── Application.java
        └── config/               # 启动配置
```

## 分层职责详解

### 适配层（Adapter）

**职责**：接收外部请求（HTTP/RPC/MQ），转换后委托给应用层处理。

**PROF-DDD-001** 适配层 MUST 仅负责协议适配与参数转换，MUST NOT 包含任何业务逻辑。 [MUST]

**PROF-DDD-002** Controller MUST 直接调用应用服务（AppService），MUST NOT 跨层调用领域层或基础设施层。 [MUST]

```java
// adapter/web/UserController.java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandAppService userCommandAppService;
    private final UserQueryAppService userQueryAppService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid CreateUserCommand cmd) {
        return Result.ok(userCommandAppService.create(cmd));
    }

    @GetMapping("/{id}")
    public Result<UserDTO> detail(@PathVariable Long id) {
        return Result.ok(userQueryAppService.getById(id));
    }
}
```

### 应用层（App）

**职责**：编排用例，管理事务，协调领域对象与外部服务。

**PROF-DDD-003** 应用服务 MUST 编排用例流程，MUST NOT 实现核心业务规则（由领域层负责）。 [MUST]

**PROF-DDD-004** 应用服务 SHOULD 分为命令服务（写）与查询服务（读），支持读写分离。 [SHOULD]

```java
// app/service/UserCommandAppService.java
@Service
@RequiredArgsConstructor
public class UserCommandAppService {

    private final UserRepository userRepository;  // 领域层接口
    private final DomainEventPublisher eventPublisher;

    @Transactional(rollbackFor = Exception.class)
    public Long create(CreateUserCommand cmd) {
        // 1. 构建聚合根
        User user = User.create(cmd.getUsername(), cmd.getEmail());

        // 2. 持久化
        userRepository.save(user);

        // 3. 发布领域事件
        eventPublisher.publish(user.collectDomainEvents());

        return user.getId();
    }
}
```

### 领域层（Domain）

**职责**：承载核心业务逻辑，定义聚合根、实体、值对象、领域事件。

**PROF-DDD-005** 领域层 MUST 保持纯净，MUST NOT 依赖任何框架注解（Spring、MyBatis 等）。 [MUST]

**PROF-DDD-006** 领域层 MUST NOT 依赖基础设施层，通过接口反转（Repository 接口在 domain，实现在 infrastructure）实现解耦。 [MUST]

```java
// domain/repository/UserRepository.java — 领域层定义接口
public interface UserRepository {
    void save(User user);
    User findById(Long id);
    Optional<User> findByUsername(String username);
}

// infrastructure/repository/impl/UserRepositoryImpl.java — 基础设施层实现
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserConverter userConverter;

    @Override
    public void save(User user) {
        UserDO dataObject = userConverter.toDataObject(user);
        if (user.getId() == null) {
            userMapper.insert(dataObject);
        } else {
            userMapper.updateById(dataObject);
        }
    }
}
```

### 基础设施层（Infrastructure）

**职责**：实现领域层定义的接口，封装技术细节（数据库、缓存、消息队列、外部 API）。

**PROF-DDD-007** 基础设施层 MUST 实现领域层定义的 Repository 和 Gateway 接口。 [MUST]

**PROF-DDD-008** 数据库 DO（Data Object）MUST 定义在基础设施层，MUST NOT 与领域 Entity 混用。 [MUST]

## 依赖方向

**PROF-DDD-009** Maven 模块依赖 MUST 严格遵循以下方向：[MUST]

```
adapter → app → domain ← infrastructure
                  ↑
               start（聚合启动）
```

```xml
<!-- adapter/pom.xml -->
<dependencies>
    <dependency>
        <groupId>{{groupId}}</groupId>
        <artifactId>{{project}}-app</artifactId>
    </dependency>
</dependencies>

<!-- app/pom.xml -->
<dependencies>
    <dependency>
        <groupId>{{groupId}}</groupId>
        <artifactId>{{project}}-domain</artifactId>
    </dependency>
</dependencies>

<!-- infrastructure/pom.xml -->
<dependencies>
    <dependency>
        <groupId>{{groupId}}</groupId>
        <artifactId>{{project}}-domain</artifactId>
    </dependency>
</dependencies>
```

**PROF-DDD-010** domain 模块 MUST NOT 依赖 app、adapter 或 infrastructure 模块。 [MUST]

**PROF-DDD-011** infrastructure MUST NOT 依赖 adapter 或 app 模块。 [MUST]

**PROF-DDD-012** start 模块 SHOULD 依赖 adapter 和 infrastructure，作为启动聚合点。 [SHOULD]

## 测试目录

**PROF-DDD-013** 各层测试 MUST 放在对应模块的 `src/test/java/` 下，遵循相同包结构。 [MUST]

**PROF-DDD-014** 领域层单元测试 MUST 不依赖 Spring 容器，纯 POJO 测试。 [MUST]

```java
// domain 层纯单元测试
class UserTest {
    @Test
    void should_create_user_with_valid_params() {
        User user = User.create("zhangsan", "user@example.com");
        assertNotNull(user.getId());
        assertEquals("zhangsan", user.getUsername());
    }

    @Test
    void should_reject_empty_username() {
        assertThrows(DomainException.class,
            () -> User.create("", "user@example.com"));
    }
}
```

## 分层职责绝对边界

**PROF-DDD-015** 各层 MUST 严格遵守职责边界矩阵：[MUST]

| 层级 | 可以做什么 | 禁止做什么 |
|------|-----------|-----------|
| 适配层/接口层 | 参数校验、权限检查、DTO 组装、调用 AppService | 业务逻辑、事务管理、直接调用 Repository |
| 应用层 | 用例编排、事务声明、DTO 转换、防腐调用、事件发布 | 业务规则、领域计算、状态变更逻辑 |
| 领域层 | 业务规则、领域计算、状态变更、领域事件注册 | 调用 Repository、调用 AppService、框架注解 |
| 基础设施层 | 持久化实现、外部服务调用、技术配置 | 业务逻辑、领域规则、直接返回领域对象无转换 |

**PROF-DDD-016** 领域层代码 MUST 满足以下三项可验证性约束：[MUST]

- 可在纯 JUnit 环境运行测试（不启动 Spring 容器）
- 替换持久化框架（如 JPA → MyBatis），领域层代码零改动
- 切换数据库（如 PostgreSQL → MySQL），领域层代码零改动

## 工程完整约束

**PROF-DDD-017** 执行任何 DDD 模块落地前，MUST 确认以下约束已理解并遵守：[MUST]

| 编号 | 约束项 | 说明 |
|------|--------|------|
| C001 | 强制四层 | 必须按四层分包，禁止三层混杂 |
| C002 | 分层解耦 | 领域层无任何框架依赖 |
| C003 | 统一命名 | 按规范命名，禁止 Service/Manager/Dao 混用 |
| C004 | 需求对齐 | 100% 对齐设计文档，禁止自由发挥 |
| C005 | 可运行标准 | 必须可编译、可启动、可测试核心功能 |

## 多模块工程结构

**PROF-DDD-018** 大型 DDD 项目 SHOULD 采用 4 层平级工程结构，各层在目录上完全平级：[SHOULD]

```
{{project}}-backend/
├── {{project}}-parent/        ← 版本/仓库管理（纯 pom，无代码）
├── {{project}}-product/       ← 业务聚合器（pom，聚合所有业务-core）
├── {{project}}-commons/       ← 公共包（pom，聚合 infra + contract）
└── {{project}}-boot/          ← 启动器（jar，Spring Boot 入口）
```

各 module 职责约束：

| Module | packaging | 职责 | 禁止 |
|--------|-----------|------|------|
| parent | pom | 统一版本号、依赖管理 | 代码、`<modules>` 列表 |
| product | pom | 聚合所有业务-core | dependencies、代码 |
| commons | pom | 聚合 infra + contract | 业务代码 |
| boot | jar | Spring Boot 启动入口 | 业务逻辑（只做装配） |

**PROF-DDD-019** 公共包 SHOULD 分为三大类，命名自带分类：[SHOULD]

| 大类 | 命名模式 | 作用 | 引用策略 |
|------|---------|------|---------|
| 基础层 | `{{project}}-common-core` | 零技术依赖的通用工具 | 所有-core 必引 |
| 基础设施层 | `{{project}}-common-infra-*` | 按技术能力拆分（redis/mq/es 等） | 按需引 |
| 数据契约层 | `{{project}}-common-contract` | 所有 Client 接口 + DTO 集中 | 所有-core 都引 |

**PROF-DDD-020** 模块间引用 MUST 采用 Client 模式，消费方仅 `@Autowired` 一个 Client，MUST NOT 直接 import 对方 AppService：[MUST]

```java
// Provider 侧：定义 Client 接口（在 common-contract 中）
public interface MetaModelClient {
    Optional<MetaModelDTO> findByCode(String code);
}

// Provider 侧：实现 Client（在 infrastructure/client/ 中）
@Component
@RequiredArgsConstructor
public class MetaModelClientImpl implements MetaModelClient {
    private final MetaModelAppService appService;
}

// Consumer 侧：使用 Client（1 个 @Autowired）
@Service
@RequiredArgsConstructor
public class DataRecordAppService {
    private final MetaModelClient metaModelClient;
}
```

Client 拆分策略：方法 ≤ 5 个不拆，5-10 个加注释分组，≥ 10 个按角色拆（查询/校验/生命周期），1 个 impl 实现多个接口。

## 演进路径

**PROF-DDD-021** 项目从单体演进到微服务时，SHOULD 遵循零代码改动迁移路径：[SHOULD]

1. Phase 1（单体）：所有-core 在同一 JVM，Client 直接 in-JVM 调用
2. Phase 2（拆微服务）：被拆模块的 Client 从 common-contract 迁到独立 SDK，AppService 内部零改动
