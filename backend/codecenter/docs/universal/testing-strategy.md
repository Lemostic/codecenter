# 测试策略规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.2 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-NC(命名规范) |
| 互斥规范 | 无 |
| 下沉规则 | Java/JVM 框架约定下沉至 L1 `testing-jvm` 包 |

---

> 本规范聚焦"测试策略与协作约定",跨语言/跨架构通用。具体框架(JUnit 5、Mockito、TestContainers 等)的使用方法下沉至 L1 `testing-jvm` 包;前端测试(Vitest、Testing Library)下沉至 frontend-vue 子包。

---

## 一、测试金字塔与分层

### 规范 UNI-TS-001: 测试金字塔策略 [MUST]

**规则:**

1. 测试结构 MUST 遵循测试金字塔原则:**单元测试 ≈ 70%、集成测试 ≈ 20%、端到端测试 ≈ 10%**。
2. 单元测试 MUST 覆盖所有核心业务逻辑(领域模型、业务规则、工具类、纯函数)。
3. 集成测试 MUST 覆盖所有对外暴露的 API 端点(100%)及与外部依赖(DB、缓存、MQ)的真实交互。
4. 端到端测试 SHOULD 覆盖核心用户关键路径(注册/登录/下单/支付/审批)。
5. 禁止编写依赖外部网络环境的测试,所有测试 MUST 可在本地独立运行。
6. 不应颠倒金字塔:端到端测试占比 MUST NOT 超过 20%。

**示例:**

```
┌─────────────────┐
│  E2E Tests (10%)│   核心用户关键路径(注册/登录/下单)
├─────────────────┤
│  Integration    │   API 端点、DB 交互、MQ 真实消费
│  Tests (20%)    │
├─────────────────┤
│  Unit Tests     │   业务逻辑、工具类、领域模型、纯函数
│  (70%)          │
└─────────────────┘
```

---

### 规范 UNI-TS-002: 覆盖率要求 [MUST]

**规则:**

1. 核心业务逻辑(领域层、Service 层)行覆盖率 MUST ≥ **80%**,分支覆盖率 SHOULD ≥ **70%**。
2. 所有对外 API 端点 MUST 有对应的集成测试覆盖(100% 端点覆盖)。
3. 工具类、纯函数组件行覆盖率 SHOULD ≥ **90%**。
4. 覆盖率指标 MUST 在 CI 流水线中强制执行,低于阈值 MUST 阻断合并。
5. 新增代码的增量覆盖率 MUST ≥ **80%**(基于 diff 行统计)。
6. Controller 层、DAO 层等薄层代码 SHOULD 通过集成测试间接覆盖,不必单独编写单元测试。
7. 覆盖率 MUST NOT 通过"为覆盖率而测试"的形式刷高(如 `assertTrue(true)` 凑数)。

---

### 规范 UNI-TS-003: 测试隔离原则 [MUST]

**规则:**

1. 每个测试 MUST 独立运行,禁止测试间共享可变状态(全局变量、静态字段、外部文件)。
2. 每个测试 MUST 在执行前重置测试环境(具体机制因框架而异,JVM 见 `testing-jvm`)。
3. 测试 MUST 是确定性的(deterministic):相同输入 MUST 始终产生相同结果。
4. 测试 MUST NOT 依赖当前时间(`LocalDateTime.now()`)、随机值(`Math.random()`)或外部系统状态(网络、文件)。
5. 时间相关测试 SHOULD 抽象出 `Clock` / `now()` 函数,在测试中注入固定时间。
6. 随机值测试 SHOULD 使用固定种子(seed)或在 beforeEach 中重新设定。
7. 数据库测试 MUST 在每个测试前后清理数据,或使用事务回滚策略。
8. 测试 MUST NOT 依赖执行顺序,禁止假设其他测试先于当前测试运行。

---

### 规范 UNI-TS-004: Mock 策略 [MUST]

**规则:**

1. 单元测试 SHOULD 使用 Mock 替代外部依赖(数据库、缓存、消息队列、第三方 API)。
2. 集成测试 MUST 使用真实中间件(数据库、Redis)或嵌入式替代品(如 H2、TestContainers)。
3. 端到端测试 MUST NOT Mock 任何业务组件,使用完整技术栈运行。
4. Mock 对象 MUST 验证关键交互(调用次数、参数值),不能只验证返回值。
5. 禁止 Mock 被测试对象自身的方法(Spy 除外,且 SHOULD 谨慎使用)。
6. Mock 数量 SHOULD 控制在合理范围:被测类的直接外部依赖可 Mock,深层依赖 SHOULD 走真实调用。
7. 当 Mock 难以构造或行为复杂时,优先使用真实组件(如嵌入式数据库)而非过度 Mock。

---

## 二、命名与组织

### 规范 UNI-TS-005: 测试命名规范 [MUST]

**规则:**

1. 测试类/文件/Suite 名 MUST 以被测试对象名 + `Test` 后缀命名(如 `UserServiceTest`、`UserService.spec.ts`)。
2. 测试方法名 MUST 使用 `should_<expectedBehavior>_when_<condition>` 格式,描述测试意图。
3. 测试方法名 SHOULD 用英文编写,保持语义清晰、动词明确。
4. 测试类 MUST 放在与被测试类相同的包/目录结构下,目录为 `test/` 或 `__tests__/`。
5. 测试方法 SHOULD 描述"行为 + 场景",而非"方法名 + 入参"(避免与实现耦合)。

**示例:**

```java
class UserServiceTest {
    @Test
    void should_createUserSuccessfully_when_validInput() { /* ... */ }
    @Test
    void should_throwValidationException_when_duplicateEmail() { /* ... */ }
    @Test
    void should_returnEmptyList_when_noOrdersFound() { /* ... */ }
}
```

```typescript
describe('UserService', () => {
  it('should create user successfully when valid input', async () => { /* ... */ });
  it('should throw error when duplicate email', async () => { /* ... */ });
});
```

---

## 三、CI 集成要求

### 规范 UNI-TS-006: CI 集成要求 [MUST]

**规则:**

1. 所有测试 MUST 在 CI 流水线中自动执行,CI MUST 在每次 Push 与 PR 时触发。
2. CI 流水线 MUST 包含阶段顺序:**编译 → 单元测试 → 集成测试 → 代码质量扫描 → 覆盖率检查**。
3. 任何阶段失败 MUST 阻断合并,禁止通过 `--no-verify`、环境变量跳过测试。
4. CI 执行时间 SHOULD 不超过 **15 分钟**;超过时 SHOULD 优化(并行执行、依赖缓存、测试分层)。
5. CI 日志 MUST 清晰展示测试结果(通过/失败/跳过的数量、失败用例摘要)。
6. 禁止使用 `@Disabled` / `test.skip` / `it.skip` 长期跳过失败的测试。
7. 跳过的测试 MUST 在测试名上明确标注跳过原因,并关联 Issue 跟踪。
8. 失败的测试在修复前 MUST NOT 被禁用;修复策略是修复实现或测试,不是隐藏失败。

---

## 四、AI 协作: 已有测试保护

### 规范 UNI-TS-007: 已有测试保护 [MUST]

**规则:**

1. AI MUST NOT 修改、删除、重构任何已有的测试文件(`*Test.*`、`*.test.*`、`*.spec.*`),除非开发者在任务中明确指令"修改测试"或"更新测试"。
2. 当接口签名未变但内部实现发生变化时,已有测试 MUST 继续通过,AI MUST 调整实现而非测试去通过变更。
3. 如果实现变更导致已有测试失败,AI MUST 优先调整实现以通过已有测试,而非修改测试去适配实现。
4. 仅当开发者明确判定已有测试过时或需要调整时,AI MAY 修改测试,且修改 MUST 在变更日志中注明原因。
5. AI 在为新功能编写测试时,MUST 新建独立的测试方法或测试类,MUST NOT 修改已有测试方法"顺便"覆盖新功能。
6. AI 不得通过删除/禁用失败测试来"修复"构建失败——这是规避行为,违反测试保护铁律。

**测试保护决策树:**

```
收到编码任务
  ↓
检查任务是否涉及修改测试
  ↓
├─ 任务明确说"修改测试/更新UT" → 可以修改指定的测试文件
│   └─ 仅限开发者指定的测试文件,不可扩大范围
├─ 任务未提及测试 → 测试文件归入禁止触碰区
│   ├─ 实现变更导致测试失败 → 调整实现,不修改测试
│   ├─ 需要为新功能写测试 → 新建测试方法/类,不动已有测试
│   └─ 发现已有测试有bug → 记录问题,不修改,建议开发者单独处理
└─ 不确定 → 向开发者确认是否可以修改测试
```

**测试保护的 manifest 配置:**

```yaml
# .spec/spec-manifest.yaml
protection:
  # 默认已包含测试文件保护,此处可添加额外受保护路径
  protected_globs:
    - "**/*Test.java"
    - "**/*Tests.java"
    - "**/*.test.ts"
    - "**/*.spec.ts"
    - "**/*IntegrationTest.java"
    - "**/*.test.py"
```

---

## 五、TDD 与快速开发模式选择

### 规范 UNI-TS-008: TDD 与快速开发决策树 [MUST]

**决策树:**

```
开发一个新功能
  ↓
Q1: 这个功能逻辑是否复杂（≥3 个分支/状态/边界）？
  ├─ 是 → Q2: 是否存在"红→绿"反馈循环的快速验证手段？
  │       ├─ 是 → 进入 TDD 模式（先写测试）
  │       └─ 否（编译/启动慢）→ 快速开发模式 + 写后补测试
  └─ 否（简单 CRUD/配置/脚本）→ 快速开发模式 + 必要边界单测
```

### 规范 UNI-TS-009: TDD 模式触发与流程 [MUST]

**触发方式（Prompt 模板）:**

```
用 TDD 模式，实现 [功能]：[功能描述]
```

或:
```
请用 TDD 的方式实现 [功能名称]
```

**RED → GREEN → REFACTOR 三段式:**

```
   RED          GREEN         REFACTOR
 写失败测试  → 写通过代码 →  重构保持绿
   ↓            ↓              ↓
 描述期望行为  只写使测试    消除重复
              通过的最少代码   提升可读性
```

**TDD 红绿重循环中各阶段要点:**

| 阶段 | 目标 | 禁止 | 产出 |
|------|------|------|------|
| RED | 写一个会失败的测试，描述期望行为 | 不要写任何生产代码 | 一个失败的测试 |
| GREEN | 编写使测试通过的最少代码 | 不要优化，不要完美 | 通过的测试 |
| REFACTOR | 在保持绿的前提下消除重复、改善命名 | 不要改行为 | 重构后仍绿的代码 |

### 规范 UNI-TS-010: 快速开发模式适用场景 [MUST]

**适用场景（满足任一即可）:**

- 简单 CRUD/配置/脚本（逻辑分支 < 3）
- 探索性原型（PoC、调研性代码）
- 编译/启动耗时 > 5 秒（红绿循环成本过高）
- 临时补丁（hotfix），需要事后补测试
- 配置文件/DDL/SQL 脚本

**禁止场景（必须用 TDD 或事后补测试）:**

- 核心业务逻辑（订单/支付/权限）
- 并发/竞态逻辑
- 安全相关（认证、加密、鉴权）

### 规范 UNI-TS-011: 快速开发必须事后补测试 [MUST]

- 临时省略的测试 MUST 在 PR 中标 `// TODO: AI fast-mode, add tests before merge`
- 提交前 Reviewer 必须审查 TODO 项
- 合并后 24 小时内补齐单测

### 规范 UNI-TS-012: TDD 与快速开发使用约束 [MUST]

- AI 必须在生成代码的回复中显式标注使用的模式：`TDD mode` 或 `Fast mode`
- 若使用 Fast mode，必须列出"已省略的测试列表"作为 PR 描述的一部分
- 强制覆盖：核心业务逻辑、并发逻辑、安全相关 MUST 使用 TDD
- 单测通过率 100% 才允许合并（即使 Fast mode 后续补的也必须 100% 绿）

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-17 | 初始版本 |
| 1.1 | 2026-06-17 | 新增 UNI-TS-007 已有测试保护规则 |
| 1.2 | 2026-06-18 | 拆分: 剥离 Java/JVM 框架约定至 L1 `testing-jvm` 包;L0 仅保留跨语言通用策略 |

---

## 下沉规则索引

以下规则的具体写法下沉至 L1 包,被加载时由 Skill 引擎按 profiles 匹配:

| 下沉主题 | 归属包 | 用途 |
|---------|--------|------|
| JUnit 5 生命周期与注解 | `testing-jvm` | JVM 项目的单元/集成测试基础 |
| AssertJ 断言库 | `testing-jvm` | JVM 项目的流式断言 |
| Mockito 用法 | `testing-jvm` | JVM 项目的 Mock 框架约定 |
| Spring Boot 测试切片 | `testing-jvm` | @SpringBootTest / @WebMvcTest / @DataJpaTest |
| TestContainers 集成测试 | `testing-jvm` | Docker 化的真实中间件测试 |
| 参数化测试 | `testing-jvm` | @ParameterizedTest + 多数据源 |
| 前端组件测试 | `frontend-vue` | Vitest + Testing Library(待新增) |
| 前端 E2E | `frontend-vue` | Playwright(待新增) |

---

*本规范是 L0 通用策略层。具体框架实现细节由 L1 包承载,本文件 MUST NOT 引入任何 Java/JS/Python 特定的 API 示例。*