| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L1 |
| 包类型 | testing |
| 引入条件 | `fingerprint.profiles contains 'testing-jvm'` |
| 适用架构 | Java / JVM（Spring Boot 生态） |
| 依赖规范 | `universal/testing-strategy.md`（L0 通用策略） |
| 互斥规范 | 无 |

# JVM 测试框架约定

> 本包是 L0 `testing-strategy.md` 的 L1 下沉层。L0 定义"测试策略与协作约定"（测试金字塔、覆盖率、隔离、Mock 策略、CI 集成、已有测试保护），本包定义"Java/JVM 项目如何落地这些策略"——JUnit 5 生命周期、AssertJ 断言、Mockito 用法、Spring Boot 测试切片、TestContainers 集成测试、参数化测试。
>
> 前端测试规范（Vitest + Testing Library + Playwright）见 `frontend-vue` 子包。

---

## 一、JUnit 5 生命周期与注解

### 规范 TST-JVM-001: 测试类与测试方法结构 [MUST]

**规则:**

1. 测试类 MUST 使用 JUnit 5（`org.junit.jupiter.api.*`），禁止使用 JUnit 4 注解（`@Test` from `org.junit.*`）。
2. 测试类 MUST 标注 `@DisplayName` 提供可读的中文/英文描述,CI 报告与 IDE 树状视图 MUST 显示该描述。
3. 测试方法 MUST 用 `@Test` 标注,且 MUST 是 `void` 返回类型、无参数。
4. 测试方法 MUST 标注 `@DisplayName` 描述测试意图（如 "应拒绝重复邮箱注册"）。
5. 测试类 MUST NOT 包含业务代码,只包含测试逻辑。
6. 同一测试类内 MUST NOT 同时存在 `@Test` 和 JUnit 4 注解（混合使用）。
7. 禁止使用 `@Disabled` / `@Ignore` 长期跳过失败测试,失败测试 MUST 在测试名上标注 `@Disabled("原因 #Issue编号")` 关联 Issue。

**示例:**

```java
@DisplayName("用户服务 - 单元测试")
class UserServiceTest {

    @Test
    @DisplayName("应成功创建用户当输入合法")
    void should_createUserSuccessfully_when_validInput() {
        // given
        var input = new CreateUserInput("alice@example.com", "secret123");
        // when
        var result = userService.create(input);
        // then
        assertThat(result.getId()).isNotNull();
    }
}
```

---

### 规范 TST-JVM-002: Given-When-Then 三段式 [MUST]

**规则:**

1. 测试方法体 MUST 按"given（前置）-when（执行）-then（断言）"三段式组织,段间 MUST 用空行分隔。
2. given 段 MUST 只准备测试数据与依赖,禁止包含断言。
3. when 段 MUST 只调用被测方法,禁止包含业务逻辑。
4. then 段 MUST 只做断言,禁止修改测试环境或执行额外操作。
5. 复杂场景 SHOULD 用 `// given` `// when` `// then` 注释显式标注。
6. 一个测试方法 MUST 只验证一个行为,禁止多个不相关断言堆叠。
7. 当一个测试需要验证多个独立场景时,SHOULD 拆成多个测试方法（参数化测试除外）。

**示例:**

```java
@Test
@DisplayName("应拒绝重复邮箱注册")
void should_throwDuplicateEmailException_when_emailAlreadyExists() {
    // given
    var existing = userRepository.save(new User("alice@example.com"));
    var input = new CreateUserInput("alice@example.com", "newSecret");

    // when + then
    assertThatThrownBy(() -> userService.create(input))
        .isInstanceOf(DuplicateEmailException.class)
        .hasMessageContaining("alice@example.com");
}
```

---

## 二、AssertJ 流式断言

### 规范 TST-JVM-003: AssertJ 断言库 [MUST]

**规则:**

1. 项目 MUST 引入 `assertj-core` 依赖,禁止使用 JUnit 5 自带的 `Assertions.*` 静态方法。
2. 断言 MUST 使用 AssertJ 的 `assertThat(actual)` 流式 API,禁止使用 `assertEquals(expected, actual)` 参数顺序（JUnit 反直觉）。
3. 复杂对象断言 SHOULD 优先用 `extracting("field").contains(...)` 或 `usingRecursiveComparison()`,而非逐字段断言。
4. 异常断言 MUST 用 `assertThatThrownBy(() -> ...)` 而非 `@Test(expected=...)` 或 try-catch。
5. 集合断言 MUST 用 `assertThat(collection).hasSize(n).contains(element)`,禁止手写 `for` + `assertTrue`。
6. 时间断言 MUST 用 `assertThat(instant).isCloseTo(now, within(1, ChronoUnit.SECONDS))`,避免 `LocalDateTime.now()` 精确比较。
7. 软断言 SHOULD 用 `SoftAssertions.assertSoftly`,需要一次报告多个失败时使用（避免用例中断在首个失败）。

**示例:**

```java
// ✅ 正确
assertThat(result)
    .isNotNull()
    .extracting("email", "status")
    .containsExactly("alice@example.com", UserStatus.ACTIVE);

// ❌ 错误
assertEquals("alice@example.com", result.getEmail());  // 参数顺序易错
assertEquals(UserStatus.ACTIVE, result.getStatus());
```

---

## 三、Mockito Mock 框架

### 规范 TST-JVM-004: Mockito 用法 [MUST]

**规则:**

1. Mock 框架 MUST 使用 Mockito 5.x,禁止使用过时版本（< 4.x）。
2. Mock 对象的创建 MUST 优先用 `@Mock` 注解 + `@ExtendWith(MockitoExtension.class)`,禁止 `Mockito.mock(SomeClass.class)` 散落调用。
3. Stub 行为 SHOULD 优先用 `given(method()).willReturn(value)` BDD 风格,禁止 `when(method()).thenReturn(value)`。
4. Mock 对象 MUST 验证关键交互:调用次数（`verify(mock, times(1)).method()`）、参数值（`verify(mock).method(expectedArg)`）。
5. 禁止 Mock 被测对象自身的方法（违反"测试对象行为"原则）。
6. 静态方法 Mock SHOULD 优先用 `mockStatic(SomeClass.class, CALLS_REAL_METHODS)` 临时作用域,禁止全局静态 Mock。
7. 当 Mock 难以构造或行为复杂时,优先用真实组件（如嵌入式数据库 H2、TestContainers）替代。
8. Spy 对象 SHOULD 谨慎使用,仅在需要"部分真实 + 部分 Mock"场景下使用,默认用 `@Mock`。
9. 禁止在测试中通过 `verifyNoMoreInteractions(mock)` 验证"没调用其他方法"——这会与未来新增方法冲突,改用 `verify(mock, only()).method()` 验证具体调用。

**示例:**

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @InjectMocks private UserService userService;

    @Test
    @DisplayName("应发送欢迎邮件当用户注册成功")
    void should_sendWelcomeEmail_when_userCreated() {
        // given
        var input = new CreateUserInput("alice@example.com", "secret");
        given(userRepository.existsByEmail(anyString())).willReturn(false);

        // when
        userService.create(input);

        // then
        verify(emailService, times(1)).sendWelcomeEmail(eq("alice@example.com"));
    }
}
```

---

## 四、Spring Boot 测试切片

### 规范 TST-JVM-005: Spring Boot 测试注解选择 [MUST]

**规则:**

1. 测试切片注解 MUST 按场景选择,禁止一律用 `@SpringBootTest`:
   - `@WebMvcTest` — 只测 Controller 层（HTTP 接口,Mock Service/Repository）
   - `@DataJpaTest` — 只测 Repository 层（H2 嵌入式数据库,自动回滚）
   - `@SpringBootTest` — 集成多组件端到端场景,加载完整 ApplicationContext
   - `@JdbcTest` — 纯 JDBC 测
   - `@JsonTest` — JSON 序列化/反序列化测
2. `@SpringBootTest` SHOULD 配合 `@ActiveProfiles("test")` 切换到 test profile,禁止用 prod profile 跑测试。
3. 测试配置 SHOULD 用 `@TestConfiguration` 嵌套类,禁止污染主 ApplicationContext。
4. `@MockBean` 已弃用（MOCKITO 5 + Spring Boot 3.4+）,MUST 改用 `@MockitoBean`(Spring Framework 6.2+)。
5. 测试间 MUST 用 `@DirtiesContext(classMode = AFTER_EACH_TEST_METHOD)` 仅在必要时重置 Spring 上下文（性能成本高,默认共享）。
6. 禁止在 `@SpringBootTest` 中加载 Kafka/RabbitMQ 连接（应用启动时阻塞测试套件,使用 TestContainers 或 @MockBean 替代）。
7. Controller 测试 MUST 用 `MockMvc` 或 `WebTestClient` 验证 HTTP 行为,禁止启动真实 Tomcat。

**示例:**

```java
// 纯 Controller 测试 - 快
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UserService userService;

    @Test
    void should_return201_when_validInput() throws Exception {
        given(userService.create(any())).willReturn(new UserDTO(...));

        mockMvc.perform(post("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                  {"email": "alice@example.com", "password": "secret123"}
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty());
    }
}
```

---

### 规范 TST-JVM-006: 测试上下文复用与速度优化 [MUST]

**规则:**

1. 默认 SHOULD 复用 Spring 上下文（仅第一个测试加载,后续测试共享）,禁止在每个测试上加 `@DirtiesContext`。
2. 当使用 `@MockBean` 改变上下文时,SHOULD 用 `@MockBean(BeanPostProcessor.class)` 区分上下文,或拆成独立的测试类。
3. 集成测试 SHOULD 控制在 **30 秒以内**完成,超过 MUST 优化:
   - 用 `@WebMvcTest` 替代 `@SpringBootTest`(Controller 测试)
   - 用 H2 替代真实 DB（Repository 测试）
   - 用 TestContainers 复用容器（多模块共享）
4. 测试 profile (`application-test.yml`) MUST 关闭重试、关闭异步、缩短连接超时,避免测试运行变慢。
5. CI 流水线 MUST 启用 Surefire/Failsafe 的 fork + reuse 模式,支持并行执行。

---

## 五、TestContainers 集成测试

### 规范 TST-JVM-007: TestContainers 真实中间件 [MUST]

**规则:**

1. 集成测试涉及 DB / Redis / Kafka / Elasticsearch 等中间件时,优先用 TestContainers 启动 Docker 容器,MUST NOT 连接共享的开发/测试环境。
2. TestContainers 容器 SHOULD 在测试类级别复用,使用 `@Container` + `static` 修饰或 `withReuse(true)`。
3. 容器端口分配 MUST 用 `container.getMappedPort(...)` 动态获取,禁止硬编码端口。
4. Spring Boot 集成测试 MUST 用 `@ServiceConnection` 注解（Spring Boot 3.1+）自动配置 `application-test.yml` 的连接信息,禁止手动读 environment 变量。
5. 容器启动失败 MUST 跳过该测试类（`@EnabledIfDockerAvailable`）而非失败,避免无 Docker 环境的开发者无法跑测试。
6. 跨模块共享的容器 SHOULD 用 `.withNetwork(NETWORK_NAME).withNetworkAliases("mysql")` 加入同一网络,模拟真实微服务环境。
7. TestContainers 配置 SHOULD 在 `src/test/resources/application-test.yml` 中声明默认镜像,禁止在测试类里 hardcode。

**示例:**

```java
@SpringBootTest
@ActiveProfiles("test")
class UserRepositoryIntegrationTest {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("test")
        .withUsername("test")
        .withPassword("test");

    @Autowired private UserRepository repository;

    @Test
    void should_saveAndFindByEmail() {
        var user = new User("alice@example.com");
        repository.save(user);

        var found = repository.findByEmail("alice@example.com");

        assertThat(found).isPresent()
            .get()
            .extracting(User::getEmail)
            .isEqualTo("alice@example.com");
    }
}
```

---

## 六、参数化测试

### 规范 TST-JVM-008: 参数化测试 [SHOULD]

**规则:**

1. 同一测试逻辑需多组数据验证时,优先用 `@ParameterizedTest` + 数据源（`@ValueSource` / `@CsvSource` / `@MethodSource`）。
2. `@ParameterizedTest` MUST 标注 `@DisplayName` 含占位符（`{0}` / `{arguments}`）以便 IDE/CI 显示具体数据。
3. 测试方法 SHOULD 用 `@MethodSource("dataProvider")` 引用静态方法作为数据源,禁止用 `@CsvSource` 拼接复杂对象（应序列化为 JSON 字符串）。
4. 枚举类作为数据源时,MUST 用 `EnumSource` 注解而非字符串匹配。
5. 参数化测试 MUST 保持每个用例的语义独立性（不依赖顺序、可独立执行）。

**示例:**

```java
@ParameterizedTest(name = "应拒绝 {0} 作为邮箱")
@ValueSource(strings = {"", "  ", "no-at-sign", "@no-local-part"})
@DisplayName("应拒绝非法邮箱格式")
void should_rejectInvalidEmail(String invalidEmail) {
    var input = new CreateUserInput(invalidEmail, "secret");

    assertThatThrownBy(() -> userService.create(input))
        .isInstanceOf(ValidationException.class);
}
```

---

## 七、Spring Boot Repository 集成测试

### 规范 TST-JVM-009: `@DataJpaTest` 约定 [MUST]

**规则:**

1. Repository 层测试 MUST 用 `@DataJpaTest`,自动配置 H2 嵌入式数据库 + JPA Repository。
2. `@DataJpaTest` 默认禁用 Spring Boot 启动时的 Flyway/Liquibase（`@AutoConfigureTestDatabase(replace = NONE)` 关闭后例外）。
3. 每个测试方法执行后 MUST 自动回滚事务,禁止在测试中显式 commit。
4. 测试数据 SHOULD 用 TestEntityManager 持久化,而非直接调用 Repository.save。
5. 自定义 Repository 方法的测试 MUST 覆盖空结果、单结果、多结果、异常 4 种场景。
6. 批量插入性能测试 SHOULD 显式开启 `@Transactional(propagation = NOT_SUPPORTED)` + `@Sql` 准备数据,避免回滚干扰。
7. 禁止用 `@SpringBootTest` + 真实数据库做 Repository 测试（启动慢、污染数据库）。

---

## 八、性能与稳定性

### 规范 TST-JVM-010: 测试性能约束 [MUST]

**规则:**

1. 单元测试单条 MUST ≤ **100ms**,集成测试单条 MUST ≤ **5s**,E2E 单条 MUST ≤ **30s。
2. 任何超过上述阈值的测试 MUST 标注 `@Tag("slow")` + `@EnabledIfSystemProperty(named = "runSlowTests", matches = "true")`,默认不跑。
3. 单元测试 MUST NOT 触发网络调用、文件 IO、数据库连接、Thread.sleep。
4. 集成测试 SHOULD 用 `Awaitility` 等待异步结果,禁止 `Thread.sleep(N)` 硬等待。
5. 测试用随机数据 MUST 用固定种子（`@RandomParameters` 或 `new Random(42)`）,保证可复现。
6. 测试间共享的 Spring 上下文 SHOULD 在第一个测试加载,后续测试复用,避免每个测试重新启动。

**示例:**

```java
await()
    .atMost(5, TimeUnit.SECONDS)
    .pollInterval(100, TimeUnit.MILLISECONDS)
    .until(() -> userRepository.findById(id).isPresent());
```

---

## 九、测试质量反模式

### 规范 TST-JVM-011: 反模式清单 [MUST NOT]

**规则:**

禁止以下反模式:

1. **为覆盖率而测试** — `assertTrue(true)`、`assertNotNull(new Object())` 等无意义断言凑覆盖率。
2. **测试内部实现** — 验证私有方法、Mock 私有字段。测试应验证行为（public API + 外部可观察结果）。
3. **共享可变状态** — `@BeforeAll` 中初始化 List,各测试方法 push/clear（违反隔离原则）。
4. **依赖执行顺序** — 测试 a 修改了 DB,测试 b 读取相同数据并"假设"该数据存在。
5. **过度 Mock** — Mock 整个对象图（包括 5 层以上的依赖）,使测试无法验证任何真实交互。
6. **断言不充分** — `assertThat(result).isNotNull()` 后不验证具体字段值。
7. **过度指定** — `verify(mock).someMethod(eq("alice"), eq("secret"), eq("2024-01-01"), ...)` 验证每一个参数,使测试与实现强耦合。
8. **长测试方法** — 单条测试 > 50 行,违反 TST-JVM-002 三段式。
9. **try-catch + assertTrue** — 用 `try { ... fail("expected exception") } catch (Exception e) { }` 替代 AssertJ 的 `assertThatThrownBy`。
10. **Test 套件依赖生产数据** — 从 `application.yml` 读 prod 配置,在测试中连接线上 DB。

---

## 十、依赖注入

### 规范 TST-JVM-012: 测试依赖注入 [MUST]

**规则:**

1. 测试类 MUST 用构造器注入（如 `@InjectMocks` 自动注入 `@Mock` 字段）优先于字段注入,便于 IDE 重构。
2. 共享的测试夹具（Fixture）SHOULD 用 `@TestInstance(Lifecycle.PER_CLASS)` + 静态工厂方法,避免 `@BeforeEach` 重复创建。
3. 静态方法返回测试数据 SHOULD 用 `Lombok` `@Builder` 配合构造器,提升可读性。
4. 测试中需要的"随机但可复现"数据 SHOULD 用 `Random` + 固定种子,或 Java Faker 库的固定 seed。
5. 多线程场景测试 MUST 用 `CountDownLatch` / `CyclicBarrier` 同步,禁止用 `Thread.sleep(100)` 猜测时序。

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-18 | 初版:JUnit 5 + AssertJ + Mockito + Spring Boot 测试切片 + TestContainers + 参数化测试 + 反模式清单 12 条规则 |

---

*本包是 `testing-strategy.md` 的 L1 下沉层。本包 MUST NOT 引入任何违反 L0 策略的内容（覆盖率阈值、Mock 策略、测试保护等铁律以 L0 为准）。*
