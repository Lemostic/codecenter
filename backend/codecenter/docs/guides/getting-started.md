# 快速入门：5 分钟接入编码规范体系

> 本指南帮助你在 5-10 分钟内完成项目接入，让 AI 编码助手自动遵循团队规范。

---

## 这套规范体系是什么？

这是一套**面向 AI Agent 消费的工程化编码规范基础设施**。它不是一堆供人阅读的文档，而是一个可被 Claude Code 自动解析、按需加载的规范运行时。

核心价值：**你在任何项目中编码时，AI 自动遵循正确的规范，无需手动提醒。**

规范的加载逻辑是"三层模型"——通用规范始终生效，架构规范按项目类型匹配，领域规范按业务标签插拔。后文会详细说明。

---

## 三步接入

### 第一步：创建项目 Manifest

在项目根目录创建 `.spec/spec-manifest.yaml`，声明项目的"架构指纹"：

```yaml
# .spec/spec-manifest.yaml
version: "1.0"
project:
  name: "{{project_name}}"
  description: "{{project_description}}"

fingerprint:
  architecture: "spring-boot-mvc"       # 你的架构类型
  tech_stack:
    backend: ["java-17", "spring-boot-3.x", "mybatis-plus"]
    frontend: []
  domains: []                           # 领域标签，暂时留空

overrides:
  disabled_specs: []                    # 暂时不需要禁用任何规范
  forced_specs: []                      # 暂时不需要强制加载任何规范
```

`fingerprint` 是 Skill 引擎的匹配依据——它根据这三个字段决定加载哪些规范模块。

### 第二步：在 CLAUDE.md 中引用 Skill

确保项目根目录的 `CLAUDE.md`（或 `.claude/CLAUDE.md`）包含对规范 Skill 的引用：

```markdown
# 项目说明
{{project_description}}

# 编码规范
本项目遵循企业编码规范体系，规范 Skill 位于：
docs/skill/SKILL.md
```

Claude Code 启动时会读取 `CLAUDE.md`，发现 Skill 引用后自动激活规范引擎。

### 第三步：开始编码

启动 Claude Code，进入项目目录，正常进行开发。Skill 引擎自动完成以下工作：

1. 读取 `.spec/spec-manifest.yaml`
2. 识别项目指纹（架构类型 + 技术栈 + 领域标签）
3. 根据路由规则匹配并加载对应规范
4. 将规范加载到当前会话上下文

**你不需要做任何额外操作。** AI 在编码时自动遵循匹配到的规范。

---

## 三层模型简介

```
┌────────────────────────────────────────────────┐
│  L2  领域扩展插件层                            │
│  按需加载 · 按领域标签匹配 · 可插拔             │
├────────────────────────────────────────────────┤
│  L1  架构配套方案层                             │
│  按架构类型加载 · 技术栈相关                    │
├────────────────────────────────────────────────┤
│  L0  通用规范层                                 │
│  始终加载 · 业务无关 · 跨架构通用               │
└────────────────────────────────────────────────┘
```

**L0 通用规范层**：无论项目用什么架构，这 8 个模块始终生效——命名、Git、API、安全、测试、日志、异常处理、链路追踪。

**L1 架构配套方案层**：根据 `fingerprint.architecture` 字段匹配。目前支持三种方案：`spring-boot-mvc`（三层 MVC）、`ddd`（领域驱动设计——按业务边界拆分代码的四层架构）、`react-vue-frontend`（前端）。

**L2 领域扩展插件层**：根据 `fingerprint.domains` 列表匹配。每个领域标签对应一个扩展插件，如 `data-governance`（数据管理）。不声明领域标签则不加载任何扩展插件。

规范优先级：**L2 > L1 > L0**。更具体的规范覆盖更通用的规范。

---

## 示例一：Spring Boot MVC 项目

假设你有一个管理后台项目 `user-admin`，技术栈为 Java 17 + Spring Boot 3.x + MyBatis-Plus：

```yaml
# .spec/spec-manifest.yaml
version: "1.0"
project:
  name: "user-admin"
  description: "用户管理后台，提供用户增删改查与权限管理功能"

fingerprint:
  architecture: "spring-boot-mvc"
  tech_stack:
    backend: ["java-17", "spring-boot-3.x", "mybatis-plus"]
    frontend: []
  domains: []

overrides:
  disabled_specs: []
  forced_specs: []
```

**此 Manifest 会加载的规范：**

| 层级 | 加载内容 |
|------|----------|
| L0（全部） | naming-conventions, git-workflow, api-design, security-baseline, testing-standards, logging-standards, exception-handling, request-tracing |
| L1（spring-boot-mvc） | structure（项目分层）, naming（Java/Spring命名）, persistence（持久化规范）, api-layer（Controller层规范） |
| L2 | 无（domains 为空） |

---

## 示例二：领域驱动设计（DDD）项目

假设你有一个复杂业务系统 `order-platform`，采用领域驱动设计的四层架构，涉及数据管理领域：

```yaml
# .spec/spec-manifest.yaml
version: "1.0"
project:
  name: "order-platform"
  description: "订单平台，基于 DDD 构建，处理订单生命周期与数据管理"

fingerprint:
  architecture: "ddd"
  tech_stack:
    backend: ["java-17", "spring-boot-3.x", "cola-4.x"]
    frontend: ["react-18", "typescript"]
  domains:
    - "data-governance"

overrides:
  disabled_specs: []
  forced_specs: []
```

**此 Manifest 会加载的规范：**

| 层级 | 加载内容 |
|------|----------|
| L0（全部） | 同上，8 个通用模块 |
| L1（ddd） | structure（四层分层）, naming（领域术语命名）, domain-model（核心业务对象/实体/值对象）, event-patterns（领域事件）, cqrs（读写分离） |
| L2（data-governance） | spi/data-governance/domain-spec.md（数据管理领域规范） |

---

## 常见问题

### Q1：我忘了创建 manifest 会怎样？

Skill 引擎找不到 manifest 时，只会加载 L0 通用规范。项目不会报错，但无法获得架构层和领域层的规范支持。

### Q2：我的项目架构不在已有 Profile 列表中怎么办？

有两个选择：(1) 使用最接近的 Profile（例如普通 Spring Boot 项目用 `spring-boot-mvc`）；(2) 联系架构组新增一个 Profile。参见 [how-to-add-spec.md](how-to-add-spec.md)。

### Q3：我想临时禁用某条规范，可以吗？

在 manifest 的 `overrides.disabled_specs` 中填写规范 ID 即可：

```yaml
overrides:
  disabled_specs:
    - "logging-standards"   # 临时禁用日志规范
```

这应该是例外情况，长期使用需要说明原因并提 PR 修复规范本身。

### Q4：规范更新后，我需要改项目代码吗？

不需要。项目 manifest 不锁定版本，始终跟随规范库最新版。规范更新后，下次启动 Claude Code 时自动生效。如果某次更新引入了问题，用 `disabled_specs` 临时屏蔽并反馈。

### Q5：全栈项目（前后端都有）怎么处理？

`spring-boot-mvc` 和 `react-vue-frontend` 可以组合使用。但 `architecture` 字段只能填一个，前端规范需通过 `forced_specs` 手动加载：

```yaml
fingerprint:
  architecture: "spring-boot-mvc"
  # ...
overrides:
  forced_specs:
    - "react-vue-frontend/structure"
    - "react-vue-frontend/component"
    - "react-vue-frontend/state"
```

### Q6：我怎么确认规范真的被加载了？

在 Claude Code 会话中直接询问："当前加载了哪些编码规范？" Skill 引擎会列出所有已激活的规范模块及其版本。详细方法参见 [skill-config-guide.md](skill-config-guide.md)。

---

## 进一步阅读

| 指南 | 内容 |
|------|------|
| [how-to-add-spec.md](how-to-add-spec.md) | 如何为规范库新增一个规范模块（L0/L1/L2） |
| [how-to-write-spi.md](how-to-write-spi.md) | 如何编写领域扩展插件，扩展业务领域规范 |
| [skill-config-guide.md](skill-config-guide.md) | Skill 引擎深度配置与故障排查指南 |
| [docs/ARCHITECTURE.md](../ARCHITECTURE.md) | 规范体系完整架构设计文档 |
| [docs/PLAN.md](../PLAN.md) | 架构设计计划与实施里程碑 |
