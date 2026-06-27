## 编码规范架构设计计划

**版本**：v1.0
**日期**：2026-06-17
**状态**：已确认

---

## 一、战略定位

本规范体系不是对现有《AI辅助前后端开发规范》的替代或修订，而是一套**全新构建的、面向多项目多架构的企业级编码规范基础设施**。它通过"热插拔"机制，让AI编码工具（以Claude Code为主）在开发任何项目时，都能自动加载并遵循正确的规范，无需开发者手动切换。

**与现有文档的关系**：完全独立共存。现有《AI辅助前后端开发规范》继续作为团队通用参考，本体系作为可被AI Agent消费的工程化规范运行时。

---

## 二、顶层架构概览

```
┌──────────────────────────────────────────────────────────────────────┐
│                        Claude Code 运行时                            │
│                                                                      │
│   项目根目录/.spec/spec-manifest.yaml                                │
│          ↓                                                           │
│   ┌──────────────────────────────────────────────┐                   │
│   │          Skill 规范路由引擎                    │                   │
│   │    (读取 manifest → 解析项目指纹 → 组装规范)   │                   │
│   └──────────────┬───────────────────────────────┘                   │
│                  │                                                    │
│          ┌───────┼────────┐                                          │
│          ↓       ↓        ↓                                          │
│   ┌─────────┐ ┌────────┐ ┌───────────┐                              │
│   │ 通用层  │ │架构层  │ │ SPI 领域层│                              │
│   │Universal│ │Profile │ │  Domain   │                              │
│   │         │ │        │ │  Plugin   │                              │
│   └─────────┘ └────────┘ └───────────┘                              │
│                                                                      │
└──────────────────────────────────────────────────────────────────────┘
```

**三层规范模型**：

| 层级 | 名称 | 职责 | 加载策略 |
|------|------|------|----------|
| L0 | 通用层（Universal） | 跨项目跨架构的技术规则：命名、Git、API、安全、测试、日志、异常处理、链路追踪与AOP | **始终加载** |
| L1 | 架构层（Profile） | 特定架构的编码范式：Spring Boot MVC / DDD / React+Vue | **按项目类型加载** |
| L2 | 领域扩展层（Domain Plugin） | 领域相关规范：数据管理、主数据管理等 | **按需插件式加载** |

---

## 三、Skill 规范路由引擎设计

### 3.1 核心机制

Skill 规范引擎是整套体系的中枢，以 Claude Code 的 Skill（SKILL.md）为载体，实现以下流程：

```
1. 项目注册 → 各项目在 .spec/spec-manifest.yaml 声明自身指纹
2. 指纹识别 → Skill 读取 manifest，识别项目的架构类型、技术栈、所属领域
3. 规范组装 → 根据指纹匹配规则，从规范库中选择 L0 + L1 + L2 规范
4. 上下文注入 → 将组装后的规范作为 Claude Code 的上下文注入
```

### 3.2 spec-manifest.yaml 格式

每个接入项目在项目根目录放置一个 manifest 文件：

```yaml
# .spec/spec-manifest.yaml
version: "1.0"
project:
  name: "{{project_name}}"
  description: "{{project_description}}"

# 架构指纹 —— Skill 根据这些字段匹配规范
fingerprint:
  architecture: "spring-boot-mvc"     # 可选值见架构Profile清单
  tech_stack:
    backend: ["java-17", "spring-boot-3.x", "mybatis-plus"]
    frontend: ["react-18", "typescript"]
  domains:                            # 领域标签，匹配SPI插件
    - "data-governance"
    - "metadata-management"

# 可选覆盖：禁用或强制加载某些规范
overrides:
  disabled_specs: []                  # 禁用的规范ID列表
  forced_specs: []                    # 强制加载的规范ID列表
```

### 3.3 Skill 路由规则（YAML 格式）

```yaml
# Skill 内部的路由匹配规则
routing:
  # 通用层 —— 无条件加载
  universal:
    always_load:
      - naming-conventions
      - git-workflow
      - api-design
      - security-baseline
      - testing-standards
      - logging-standards
      - exception-handling
      - request-tracing

  # 架构层 —— 按 fingerprint.architecture 匹配
  profiles:
    - match: "spring-boot-mvc"
      load:
        - spring-boot-mvc/structure
        - spring-boot-mvc/naming
        - spring-boot-mvc/persistence
        - spring-boot-mvc/api-layer
    - match: "ddd"
      load:
        - ddd/structure          # COLA 四层结构
        - ddd/naming
        - ddd/domain-model
        - ddd/event-patterns
        - ddd/cqrs               # 读写分离职责
    - match: "react-vue-frontend"
      load:
        - react-vue-frontend/structure
        - react-vue-frontend/component
        - react-vue-frontend/state

  # SPI领域层 —— 按 fingerprint.domains 匹配
  spi:
    match_field: "domains"
    load_pattern: "spi/{domain}/*.md"
```

---

## 四、文档目录结构

```
docs/                                         ← 规范体系根目录（新建）
│
├── ARCHITECTURE.md                           ← 顶层架构建模文档
├── spec-manifest.schema.yaml                 ← manifest 的 JSON Schema
│
├── skill/                                    ← Skill 规范引擎
│   ├── SKILL.md                              ← Claude Code Skill 定义
│   └── routing-rules.yaml                    ← 路由匹配规则
│
├── universal/                                ← L0 通用规范层
│   ├── _index.yaml                           ← 通用规范索引与元信息
│   ├── naming-conventions.md                 ← 命名规范（跨语言）
│   ├── git-workflow.md                       ← Git 工作流与提交规范
│   ├── api-design.md                         ← RESTful API 设计规范
│   ├── security-baseline.md                  ← 安全基线规范
│   ├── testing-standards.md                  ← 测试策略与规范
│   ├── logging-standards.md                  ← 日志规范（格式、级别、脱敏）
│   ├── exception-handling.md                 ← 异常处理规范（全局处理、自定义异常）
│   └── request-tracing.md                    ← 链路追踪与AOP切面规范（traceId等）
│
├── profiles/                                 ← L1 架构Profile层
│   ├── spring-boot-mvc/
│   │   ├── _profile.yaml                     ← Profile元信息
│   │   ├── structure.md                      ← 项目分层与目录结构
│   │   ├── naming.md                         ← Java/Spring命名规范
│   │   ├── persistence.md                    ← 数据持久化规范
│   │   └── api-layer.md                      ← Controller层规范
│   ├── ddd/                                  ← COLA 四层 + CQRS
│   │   ├── _profile.yaml
│   │   ├── structure.md                      ← COLA 四层分层结构
│   │   ├── naming.md                         ← DDD术语命名规范
│   │   ├── domain-model.md                   ← 聚合根、实体、值对象规范
│   │   ├── event-patterns.md                 ← 领域事件模式
│   │   └── cqrs.md                           ← 读写分离职责规范
│   └── react-vue-frontend/
│       ├── _profile.yaml
│       ├── structure.md                      ← 前端项目结构
│       ├── component.md                      ← 组件设计规范
│       └── state.md                          ← 状态管理规范
│
├── spi/                                      ← L2 SPI 领域插件层
│   ├── _spi-guide.md                         ← SPI 接入机制文档
│   ├── _template/                            ← 空壳模板目录
│   │   ├── _profile.yaml                     ← SPI插件元信息模板
│   │   └── domain-spec.md                    ← 领域规范文档模板
│   └── data-governance/                      ← 示例：数据管理领域
│       ├── _profile.yaml
│       └── domain-spec.md
│
├── guides/                                   ← 入门指南与使用说明
│   ├── getting-started.md                    ← 快速入门
│   ├── how-to-add-spec.md                    ← 如何新增规范
│   ├── how-to-write-spi.md                   ← 如何编写SPI插件
│   └── skill-config-guide.md                 ← Claude Code Skill 配置指南
│
└── meta/                                     ← 迭代管理文档
    ├── versioning-strategy.md                ← 版本管理策略
    ├── dependency-matrix.md                  ← 规范依赖关系矩阵
    └── changelog.md                          ← 变更日志
```

---

## 五、规范模块内部结构

每个规范模块遵循统一的"规范-规则-示例"三级结构：

```markdown
# {{规范名称}}

## 元信息
| 字段 | 值 |
|------|-----|
| 版本 | {{version}} |
| 层级 | L0/L1/L2 |
| 引入条件 | {{always / fingerprint.architecture == 'xxx' / domains contains 'xxx'}} |
| 适用架构 | {{architecture_types}} |
| 依赖规范 | {{dependency_list}} |
| 互斥规范 | {{conflict_list}} |
| 作者 | {{author}} |

## 规范声明
（What —— 本规范定义了什么）

## 规则清单
（How —— 具体的可执行规则，每条规则有唯一ID）

### 规则 {{rule_id}}: {{rule_name}}
- **级别**：MUST / SHOULD / MAY
- **描述**：{{description}}
- **正例**：（代码示例）
- **反例**：（违规代码示例）
- **修复指引**：{{fix_guidance}}

## 验证方式
（如何在CI/CD或AI Agent中自动验证本规范是否被遵循）
```

---

## 六、SPI 领域插件接入机制

SPI（Service Provider Interface）是业务规范的热插拔接口。核心原则：

1. **领域驱动，非产品驱动** —— 插件以"领域"为单位（如数据管理、元数据管理），不绑定任何具体产品
2. **规范引擎统一管理** —— 任何SPI插件的注册、加载、版本均由Skill引擎调度，产品线不得自行引入
3. **即插即用** —— 只需在 `spi/` 下新增目录、编写 `_profile.yaml` + `domain-spec.md`，然后在项目的 manifest 中声明对应领域标签即可生效

SPI 插件结构：

```
spi/{{domain_name}}/
├── _profile.yaml        ← 声明领域名称、版本、适用条件
└── domain-spec.md       ← 领域规范正文
```

_profile.yaml 示例：

```yaml
version: "1.0"
domain: "{{domain_name}}"
description: "{{domain_description}}"
activation:
  requires_domain: "{{domain_tag}}"   # manifest 中的 domains 字段需包含此值
depends_on:                           # 依赖的通用或架构规范
  - naming-conventions
  - api-design
```

---

## 七、Skill 引擎运行流程

```
开发者启动 Claude Code → 进入项目目录
        ↓
Claude Code 加载 Skill（SKILL.md）
        ↓
Skill 读取 .spec/spec-manifest.yaml
        ↓
解析项目指纹（architecture + tech_stack + domains）
        ↓
根据 routing-rules.yaml 匹配规范：
  ├── 加载 L0 通用规范（始终）
  ├── 加载 L1 架构Profile（按 architecture 匹配）
  └── 加载 L2 SPI插件（按 domains 匹配）
        ↓
应用 overrides（禁用/强制加载）
        ↓
组装为完整规范上下文，注入 Claude Code 会话
        ↓
开发者正常编码，AI 自动遵循正确规范
```

---

## 八、实施里程碑

| 阶段 | 产出物 | 预估工作量 | 依赖 |
|------|--------|-----------|------|
| **M1 - 基础骨架** | docs目录结构 + ARCHITECTURE.md + spec-manifest.schema | 1天 | 计划确认 |
| **M2 - 通用层** | L0 全部8份通用规范（含日志、异常处理、链路追踪） | 3-4天 | M1 |
| **M3 - 架构Profile** | L1 三个Profile（Spring Boot MVC + DDD/COLA + React/Vue） | 3-4天 | M2 |
| **M4 - Skill引擎** | SKILL.md + routing-rules.yaml + 按需加载逻辑 | 2天 | M3 |
| **M5 - SPI机制** | SPI接入指南 + 空壳模板 + data-governance示例插件 | 1天 | M3 |
| **M6 - 指南文档** | 入门指南4份 | 1-2天 | M4, M5 |
| **M7 - 迭代管理与验证** | 版本策略 + 依赖矩阵 + 端到端验证 | 1天 | M6 |

**总计预估**：约 12-14 天

---

## 九、决策记录（已确认）

| 决策点 | 最终结论 |
|--------|----------|
| L0 通用规范模块 | 8个模块：命名、Git、API、安全、测试、日志、异常处理、链路追踪与AOP |
| DDD Profile 分层模型 | COLA 四层架构（adapter/app/domain/infrastructure）+ CQRS 读写分离 |
| SPI 示例领域 | data-governance（数据管理）作为首个示例插件 |
| Skill 加载深度 | 按需加载：先加载规范索引，再根据具体编码任务加载相关规范详情 |
| 规范版本耦合 | 不锁定版本，manifest 始终跟随规范库最新版 |

---

*计划已确认，进入实施阶段。*
