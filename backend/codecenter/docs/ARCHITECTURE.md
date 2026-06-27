## 编码规范体系架构设计

> 本文档定义企业级编码规范体系的顶层架构，包括规范生命周期、Skill调度规则、三层模型、版本策略与演进路径。

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 创建日期 | 2026-06-17 |
| 状态 | Active |
| 作者 | {{author}} |

---

## 1. 体系定位与设计原则

### 1.1 定位

本规范体系是一套**面向AI Agent消费的工程化编码规范基础设施**。它不是一份供人阅读的文档合集，而是一个可被Claude Code自动解析、按需加载、动态组装的规范运行时。

核心目标：开发者在任何项目中编码时，AI Agent自动遵循正确的规范，无需人工干预。

### 1.2 设计原则

| 原则 | 说明 |
|------|------|
| 业务无关性 | 所有L0/L1规范不得包含业务逻辑或业务术语，仅聚焦技术规则 |
| 热插拔性 | 规范以模块为单位，通过manifest指纹匹配动态加载，不硬编码 |
| 渐进加载 | Skill先加载规范索引，再根据当前编码任务按需深入加载具体规则 |
| 单一调度源 | 所有规范通过Skill引擎统一调度，各产品线不得自行引入或修改 |
| 可演进性 | 规范支持版本化管理，可独立迭代而不影响其他模块 |

---

## 2. 三层规范模型

```
┌─────────────────────────────────────────────────────────┐
│                   规范加载优先级                          │
│                                                         │
│   ┌─────────────────────────────────────────────┐       │
│   │  L2  领域扩展插件层                          │       │
│   │  (Domain Plugin)                            │       │
│   │  按需加载 · 按领域标签匹配 · 可插拔          │       │
│   └─────────────────────────────────────────────┘       │
│   ┌─────────────────────────────────────────────┐       │
│   │  L1  架构配套方案层（原子化包）               │       │
│   │  (Composable Architecture Packages)         │       │
│   │  按技术栈自由组合 · 可多选持久化 · 可插拔     │       │
│   └─────────────────────────────────────────────┘       │
│   ┌─────────────────────────────────────────────┐       │
│   │  L0  通用规范层                              │       │
│   │  (Universal Standards)                      │       │
│   │  始终加载 · 业务无关 · 跨架构通用            │       │
│   └─────────────────────────────────────────────┘       │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

### 2.1 L0 通用规范层（Universal Standards）

通用规范定义跨项目、跨架构、跨技术栈的基础技术规则。无论项目采用何种架构，L0规范始终被加载。

**L0 规范清单**：

| 规范ID | 名称 | 职责 |
|--------|------|------|
| naming-conventions | 命名规范 | 跨语言的标识符命名规则（变量、函数、类、文件等） |
| git-workflow | Git工作流 | 分支策略、提交规范、PR流程、版本标签 |
| api-design | API设计规范 | RESTful设计原则、响应格式、版本控制、错误码 |
| security-baseline | 安全基线 | 输入验证、注入防护、认证授权、敏感数据处理 |
| testing-standards | 测试规范 | 测试策略、覆盖率要求、测试命名、分层测试 |
| logging-standards | 日志规范 | 日志格式、级别使用、脱敏规则、结构化日志 |
| exception-handling | 异常处理规范 | 全局异常处理、自定义异常体系、异常传播策略 |
| request-tracing | 链路追踪与AOP | traceId传递、请求上下文、AOP切面设计 |

**L0 规范的准入标准**：一条规则要进入L0，必须满足——它适用于团队内任意架构、任意技术栈的任意项目。如果某条规则仅适用于Java项目而不适用于前端项目，它应该放在L1而非L0。

### 2.2 L1 架构配套方案层（原子化包）

L1 采用**原子化组合**设计，将架构规范拆分为两大平台的独立包，开发者根据项目技术栈自由组合。

**规范包按平台维度区分**：

| 平台 | 目录 | 说明 |
|------|------|------|
| **后端** | `profiles/backend/` | 框架基座 + 架构风格 + 持久化技术 |
| **前端** | `profiles/frontend/` | 前端框架（Vue 3） |

详细子文件清单与规则数见 `profiles/PROFILES.md`。

**后端维度**：

| 维度 | 说明 | 可选包 |
|------|------|--------|
| 框架基座 | Spring Boot 通用基础 | spring-boot-base |
| 架构风格 | 代码组织方式（互斥） | arch-mvc（三层）/ arch-ddd（四层） |
| 持久化技术 | 数据访问框架（可多选） | persistence-mybatis-plus, persistence-mybatis, persistence-jpa, persistence-redis, persistence-elasticsearch |
| 消息队列 | 消息中间件（互斥） | messaging-kafka, messaging-rocketmq |
| 数据库迁移 | schema 演进管理 | db-migration（Flyway / Liquibase） |
| 测试框架 | JVM 测试约定 | testing-jvm（JUnit 5 + AssertJ + Mockito + TestContainers） |

**后端框架基座包**：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| spring-boot-base | 构造器注入、统一响应、全局异常、事务管理、测试结构、通用编码约定 | 24 |

**后端架构风格包**（互斥，后端二选一）：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| arch-mvc | MVC 三层架构：分层结构、职责边界、Controller/Service 规范、REST 接口 | 34 |
| arch-ddd | DDD 四层架构（含 4 个子模块：结构、领域模型、事件模式、读写分离） | ~90 |

**后端持久化技术包**（可按需多选）：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| persistence-mybatis-plus | MyBatis-Plus：BaseMapper、Entity、分页、SQL | 18 |
| persistence-mybatis | 原生 MyBatis：XML 映射、动态 SQL、结果映射 | 13 |
| persistence-jpa | Spring Data JPA / QueryDSL | 15 |
| persistence-redis | Redis：Key 设计、缓存一致性、分布式锁 | 14 |
| persistence-elasticsearch | Elasticsearch：索引设计、查询规范 | 14 |

**后端消息队列包**（互斥，二选一）：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| messaging-kafka | Kafka：Topic 命名、消息信封、生产者/消费者规范、事务消息、监控 | 20 |
| messaging-rocketmq | RocketMQ：Topic/Tag 设计、事务消息、顺序消息、延迟消息、监控 | 18 |

**后端数据库迁移包**（按需启用）：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| db-migration | Flyway/Liquibase 选型、版本号规则、脚本编写、回滚、多环境执行、跨服务协调 | 11 |

**后端测试框架包**（JVM 项目）：

| 包 ID | 说明 | 规则数 |
|-------|------|--------|
| testing-jvm | JUnit 5 + AssertJ + Mockito 5 + Spring Boot 测试切片 + TestContainers + 参数化测试 + 反模式清单 | 12 |

**前端包**（`frontend-vue`，仅支持 Vue 3）：

| 子目录 | 说明 | 规则数 |
|--------|------|--------|
| `vue/00-overview.md` | 包元信息 + 子文件清单 | — |
| `vue/common/` | Vue 3 通用规范（架构、目录、API、类型、i18n） | ~78 |
| `vue/vue3/` | Vue 3 特有规范（script setup、组件、状态、Element Plus、强制封装、页面模式、设计 Token） | ~93 |
| `vue/vue3/skeletons/` | 8 份骨架代码参考 | 参考 |
| `vue/selfcheck.md` | AI 编码自检清单 50+ 项 | — |

**关键设计**：
- 单一包 ID `frontend-vue` 容纳所有前端规范（与 arch-ddd 单包 4 子文件对齐）
- 强制封装组件场景用 `{EncapsulatedXxx}` 占位符（Dm- / Pro- / App- / Custom- 等前缀由项目配置定义）

**包互斥规则**：

| 包 A | 包 B | 原因 |
|------|------|------|
| arch-mvc | arch-ddd | 三层与四层是互斥的架构风格 |
| persistence-mybatis-plus | persistence-mybatis | MyBatis-Plus 是 MyBatis 增强版 |
| persistence-mybatis-plus | persistence-jpa | 不应同时使用两种 ORM |
| persistence-mybatis | persistence-jpa | 不应同时使用两种 ORM |
| messaging-kafka | messaging-rocketmq | Kafka 与 RocketMQ 是互斥的消息中间件选型 |

**组合推荐预设**：详见 `profiles/_composition-presets.yaml`。常见组合示例：

```
Spring MVC + MyBatis-Plus → spring-boot-base + arch-mvc + persistence-mybatis-plus
DDD + MyBatis-Plus + Redis → spring-boot-base + arch-ddd + persistence-mybatis-plus + persistence-redis
DDD + JPA                → spring-boot-base + arch-ddd + persistence-jpa
Vue 3 前端              → frontend-vue
全栈 DDD + Vue 3        → spring-boot-base + arch-ddd + persistence-mybatis-plus + persistence-redis + frontend-vue
```

### 2.3 L2 领域扩展插件层

扩展插件以"领域"为单位组织业务相关规范。插件与具体产品解耦，同一领域可被多个产品引用。

**扩展插件设计约束**：

| 约束 | 说明 |
|------|------|
| 领域粒度 | 一个扩展插件对应一个独立的技术领域（如数据管理），不对应产品线 |
| 单向依赖 | 扩展插件可依赖L0/L1规范，L0/L1不得依赖SPI |
| 即插即用 | 新增扩展插件只需在spi/目录添加文件，在manifest中声明domain标签 |
| 统一调度 | 扩展插件的加载、版本、冲突检测均由Skill引擎管理 |

---

## 3. Skill 规范引擎

### 3.1 引擎架构

Skill规范引擎是Claude Code与规范库之间的桥梁。它负责：

```
1. 读取项目 manifest（.spec/spec-manifest.yaml）
2. 解析项目指纹（profiles + tech_stack + domains）
3. 根据 routing-rules.yaml 匹配应加载的规范
4. 按"索引优先、按需深入"策略加载规范内容
5. 将规范加载到 Claude Code 会话上下文
```

### 3.2 按需加载策略

```
阶段一：索引加载（Skill初始化时）
  → 加载 _index.yaml 索引文件，获取所有可用规范ID和摘要
  → 解析manifest，确定当前项目应激活的规范子集
  → 在Claude Code上下文中建立"规范目录"

阶段二：按需深入（编码过程中）
  → 当开发者涉及特定编码任务时（如"写Controller"），
    Skill根据任务关键词匹配相关规范
  → 动态加载对应规范的完整规则内容
  → 任务结束后，规范内容可从上下文中释放
```

**阶段一的索引格式示例**：

```yaml
# universal/_index.yaml
specs:
  - id: naming-conventions
    version: "1.0"
    summary: "跨语言命名规则：变量、函数、类、文件、包的命名约定"
    keywords: ["命名", "naming", "变量", "函数", "类名"]
    
  - id: logging-standards
    version: "1.0"
    summary: "日志格式、级别使用、脱敏规则与结构化日志规范"
    keywords: ["日志", "log", "slf4j", "logback", "脱敏"]
```

### 3.3 规范冲突解决

当多个规范存在潜在冲突时，按以下优先级解决：

```
领域扩展 (L2) > 架构方案 (L1) > Universal (L0)
```

更具体的规范覆盖更通用的规范。例如：
- L0定义通用命名规则（camelCase函数名）
- L1 arch-ddd 包定义核心业务对象方法命名规则（领域语义命名）
- L1规则在DDD项目中覆盖L0对应规则

如果存在不可调和的冲突，Skill引擎应输出警告并提示开发者确认。

---

## 4. 规范生命周期

### 4.1 规范状态流转

```
Draft → Proposed → Active → Deprecated → Archived
  ↑                                          ↓
  └──────────────── Revision ────────────────┘
```

| 状态 | 说明 | Skill行为 |
|------|------|-----------|
| Draft | 编写中，未正式发布 | 不加载 |
| Proposed | 已提交审核，等待团队评审 | 不加载 |
| Active | 已发布，可被项目使用 | 正常加载 |
| Deprecated | 已废弃，建议迁移 | 加载但输出警告 |
| Archived | 已归档，不再可用 | 不加载 |

### 4.2 规范变更流程

```
1. 提案 → 创建变更提案文档，说明修改内容与影响范围
2. 评审 → 团队技术评审，评估兼容性
3. 实施 → 修改规范文档，更新版本号
4. 发布 → 标记为Active，更新changelog
5. 通知 → 各项目manifest自动感知最新版（不锁版本）
```

---

## 5. 版本策略

### 5.1 语义化版本

每个规范模块独立版本化，遵循语义化版本号（SemVer）：

| 版本变更 | 含义 | 示例 |
|----------|------|------|
| MAJOR（主版本） | 破坏性变更，可能影响已有项目 | 1.0 → 2.0 |
| MINOR（次版本） | 新增规则，向后兼容 | 1.0 → 1.1 |
| PATCH（修订号） | 修正描述、示例，无规则变更 | 1.0.0 → 1.0.1 |

### 5.2 版本跟随策略

项目manifest不锁定规范版本，始终跟随规范库最新版。这意味着：
- 规范更新后，所有项目自动生效
- 项目可通过manifest的overrides.disabled_specs临时屏蔽某条有问题的规范
- 重大变更（MAJOR升级）应提前通知并留出迁移期

---

## 6. 项目接入流程

一个项目接入规范体系的完整步骤：

```
1. 在项目根目录创建 .spec/ 目录
2. 编写 spec-manifest.yaml，声明项目指纹
3. 编写 project-inventory.yaml，索引项目已有资产
4. 编写 glossary.yaml，统一项目术语
5. 确保项目根目录的 CLAUDE.md 引用了规范Skill
6. 启动 Claude Code，Skill 自动读取 manifest 并加载规范
7. 开始编码，AI 自动遵循匹配的规范，并受变更边界约束
```

**最小接入示例**：

```yaml
# .spec/spec-manifest.yaml
version: "2.0"
project:
  name: "{{project_name}}"
fingerprint:
  profiles:
    - spring-boot-base
    - arch-mvc
    - persistence-mybatis-plus
  tech_stack: ["java", "spring-boot", "mybatis-plus"]
  domains: []
  http_mode: "A"
protection:
  protected_globs:
    - "**/*Test.java"
    - "**/*.test.ts"
    - "**/common/utils/**"
```

---

## 7. 项目级运行时配置

除规范库本身外，每个接入项目还需维护以下运行时文件：

### 7.1 项目资产清单（project-inventory.yaml）

位置：`.spec/project-inventory.yaml`

该文件是项目已有资产的"活目录"，供 AI 在编码前查阅，防止重复创建已有功能。

包含内容：
- **modules**：项目模块列表及其核心类
- **utilities**：工具类索引（类名、方法签名、用途）
- **api_endpoints**：已有 API 端点索引（方法、路径、描述、所属 Controller）
- **shared_components**：前端共享组件索引（名称、路径、props）

维护规则：
- AI 每次新建工具类/API/组件后 MUST 同步更新此文件
- 项目 SHOULD 每两周进行一次全量扫描校正
- 详细格式见 `universal/change-scope-control.md` UNI-CS-004

### 7.2 术语字典（glossary.yaml）

位置：`.spec/glossary.yaml`

该文件维护项目的统一术语定义，确保前后端命名一致性。

包含内容：
- **domain_terms**：领域术语（中英文、Java类名、TypeScript类型、API路径段）
- **abbreviations**：技术缩写（DTO、VO 等）
- **naming_conventions**：项目特有命名约定

维护规则：
- 引入新领域概念时 MUST 同步更新
- AI 编码时 MUST 参考此文件中的术语定义
- 详细格式见 `universal/change-scope-control.md` UNI-CS-005

### 7.3 文件保护配置（manifest.protection）

位于 `spec-manifest.yaml` 中的 `protection` 字段，定义哪些文件默认不可被 AI 修改：

```yaml
protection:
  protected_globs:        # glob 模式匹配的受保护文件
    - "**/*Test.java"
    - "**/*.test.ts"
    - "**/common/utils/**"
    - "**/config/**"
  protected_files:        # 精确路径的受保护文件/目录
    - "src/main/java/{{package_base}}/framework/"
```

受保护的文件在 AI 编码时自动归入"禁止触碰区"，除非开发者明确指令修改。

---

## 8. 文件索引

| 路径 | 说明 |
|------|------|
| `PLAN.md` | 架构设计计划（已确认） |
| `ARCHITECTURE.md` | 本文档——顶层架构建模 |
| `spec-manifest.schema.yaml` | manifest 校验 Schema |
| `skill/SKILL.md` | Claude Code Skill定义（9阶段引擎） |
| `skill/routing-rules.yaml` | 路由匹配规则 |
| `universal/` | L0通用规范目录（10份规范） |
| `profiles/` | L1架构配套方案目录（原子化包，详见 PROFILES.md） |
| `profiles/backend/` | L1 后端包目录（spring-boot-base, arch-mvc, arch-ddd, persistence-*, messaging-*） |
| `profiles/backend/ddd/` | arch-ddd 子模块（领域模型、事件模式、CQRS） |
| `profiles/frontend/` | L1 前端包目录（frontend-vue，仅 Vue 3） |
| `profiles/frontend/README.md` | frontend 目录索引说明 |
| `profiles/frontend/vue/00-overview.md` | frontend-vue 包元信息 + 子文件清单 |
| `profiles/frontend/vue/common/` | Vue 通用规范（5 份） |
| `profiles/frontend/vue/vue3/` | Vue 3 特有规范（7 份 + 8 份骨架参考） |
| `profiles/frontend/vue/selfcheck.md` | AI 编码自检清单（50+ 项） |
| `spi/` | L2 领域扩展插件目录 |
| `guides/` | 入门指南与使用说明（含设计自检指南） |
| `meta/` | 迭代管理文档（版本策略、依赖矩阵） |

### L0 通用规范清单（10份）

| 规范ID | 文件 | 说明 |
|--------|------|------|
| UNI-NC | `naming-conventions.md` | 跨语言命名规范 |
| UNI-GW | `git-workflow.md` | Git工作流与提交规范 |
| UNI-AD | `api-design.md` | API设计规范（含内部/外部接口分类） |
| UNI-SB | `security-baseline.md` | 安全基线规范 |
| UNI-TS | `testing-standards.md` | 测试策略规范（含UT保护规则） |
| UNI-LS | `logging-standards.md` | 日志规范 |
| UNI-EH | `exception-handling.md` | 异常处理规范 |
| UNI-RT | `request-tracing.md` | 链路追踪与AOP切面规范 |
| UNI-CS | `change-scope-control.md` | 变更范围控制规范（防重复造轮子、项目清单、术语字典） |
| UNI-PB | `performance-baseline.md` | 性能基线规范（N+1、分页、批量、懒加载、缓存策略） |

---

*本文档是规范体系的最高架构纲领，所有规范模块的设计与编写必须遵循本文档定义的模型与规则。*
