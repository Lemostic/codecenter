# 如何编写 SPI 领域插件

> 本指南说明如何为规范体系编写一个 L2 SPI 领域插件，让涉及特定业务领域的项目自动加载领域编码规范。

---

## 先理解：领域 vs 产品

SPI 插件的核心约束是**以"领域"为单位，不以"产品"为单位**。

| 概念 | 说明 | 正确示例 | 错误示例 |
|------|------|----------|----------|
| 领域（Domain） | 一个独立的技术或业务领域，可被多个产品共享 | `data-governance`（数据管理） | `user-admin`（用户管理后台） |
| 产品（Product） | 一个具体的产品或项目，属于某个或多个领域 | `order-platform`（订单平台） | — |

**判断方法**：如果你能回答"哪些项目/产品会用到这个领域的规范？"并且答案超过 1 个，那它就是一个合理的领域。如果只有一个产品用到，那它可能是产品级别的规范，应该放在项目的 `CLAUDE.md` 里，而不是 SPI。

---

## 创建 SPI 插件的完整步骤

### 第一步：确定领域名称与标签

领域标签是 manifest 中 `fingerprint.domains` 的匹配值。命名规则：

- 全小写，用短横线分隔：`data-governance`、`metadata-management`
- 用领域本身的技术含义命名，不用产品名
- 长度控制在 30 字符以内

```yaml
# 正确
domains: ["data-governance", "event-sourcing"]

# 错误：不要用产品名
domains: ["order-platform", "crm-system"]
```

### 第二步：创建目录结构

在 `docs/spi/` 下创建以领域标签命名的目录：

```
docs/spi/{{domain_tag}}/
├── _profile.yaml        ← 插件元信息（必须）
└── domain-spec.md       ← 领域规范正文（必须）
```

**不要**在该目录下放置其他文件。如果一个领域需要多份规范文档，将其合并到 `domain-spec.md` 中，用二级标题分节。

### 第三步：编写 `_profile.yaml`

```yaml
# docs/spi/{{domain_tag}}/_profile.yaml
version: "1.0"
domain: "{{domain_tag}}"
name: "{{领域中文名称}}"
description: "{{一段话描述该领域的核心职责与规范范围}}"
status: Active           # Draft / Proposed / Active / Deprecated / Archived
activation:
  requires_domain: "{{domain_tag}}"    # manifest 的 domains 字段必须包含此值才会激活
depends_on:                            # 本插件依赖的 L0/L1 规范（可选）
  - naming-conventions
  - api-design
tags:                                  # 辅助关键词，帮助 Skill 做按需深入匹配
  - "{{keyword_1}}"
  - "{{keyword_2}}"
  - "{{keyword_3}}"
```

### 第四步：编写 `domain-spec.md`

领域规范正文遵循与其他规范相同的"规范-规则-示例"结构：

```markdown
# {{领域中文名称}}编码规范

## 元信息

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L2 |
| 状态 | Active |
| 领域标签 | {{domain_tag}} |
| 引入条件 | domains contains '{{domain_tag}}' |
| 依赖规范 | {{dependency_list}} |
| 作者 | {{author}} |

## 规范声明

（说明本领域规范覆盖的技术规则范围）

## 规则清单

### 规则 {{domain_tag}}-001: {{规则名称}}
- **级别**：MUST
- **描述**：{{具体规则描述}}
- **正例**：
```java
// 正确示例
```
- **反例**：
```java
// 错误示例
```
- **修复指引**：{{修复方法}}

## 验证方式

（说明如何在编码过程中自动验证规则）
```

### 第五步：测试插件

在一个测试项目的 manifest 中声明该领域标签：

```yaml
# 测试项目的 .spec/spec-manifest.yaml
fingerprint:
  architecture: "spring-boot-mvc"
  tech_stack:
    backend: ["java-17", "spring-boot-3.x"]
  domains:
    - "{{domain_tag}}"        # 添加你的领域标签
```

启动 Claude Code，询问："当前加载了哪些规范？"确认你的 SPI 插件出现在已加载列表中。然后尝试一个与该领域相关的编码任务，验证 AI 是否遵循了领域规范。

---

## 完整示例：构建 `event-sourcing` 领域插件

假设团队有多个项目使用事件溯源模式（订单系统、审计系统、IoT 平台），我们为它构建一个 SPI 插件。

**目录结构：**

```
docs/spi/event-sourcing/
├── _profile.yaml
└── domain-spec.md
```

**`_profile.yaml`：**

```yaml
version: "1.0"
domain: "event-sourcing"
name: "事件溯源"
description: "事件溯源（Event Sourcing）领域的编码规范，覆盖事件模型设计、事件存储、事件回放与快照策略"
status: Active
activation:
  requires_domain: "event-sourcing"
depends_on:
  - naming-conventions
  - exception-handling
tags:
  - "事件"
  - "event"
  - "event store"
  - "snapshot"
  - "replay"
```

**`domain-spec.md`（节选）：**

```markdown
# 事件溯源编码规范

## 元信息

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L2 |
| 状态 | Active |
| 领域标签 | event-sourcing |
| 引入条件 | domains contains 'event-sourcing' |
| 依赖规范 | naming-conventions, exception-handling |
| 作者 | {{author}} |

## 规范声明

本规范定义事件溯源架构下的编码规则，包括事件对象设计、事件存储接口、事件回放机制与快照策略。

## 规则清单

### 规则 event-sourcing-001: 事件对象必须是不可变的
- **级别**：MUST
- **描述**：所有事件对象（Domain Event）一旦创建不得修改。字段必须是 final/readonly，不提供 setter 方法。
- **正例**：
```java
public record OrderCreatedEvent(
    String orderId,
    String userId,
    BigDecimal totalAmount,
    Instant occurredAt
) {}
```
- **反例**：
```java
public class OrderCreatedEvent {
    private String orderId;
    // 有 setter，事件可被篡改
    public void setOrderId(String orderId) { this.orderId = orderId; }
}
```
- **修复指引**：将事件类改为 record 或不可变类（所有字段 final，无 setter）

### 规则 event-sourcing-002: 事件命名使用过去时态
- **级别**：MUST
- **描述**：事件表示已经发生的事实，命名必须使用过去时态（如 OrderCreated，而非 CreateOrder）
- **正例**：`OrderCreatedEvent`, `PaymentCompletedEvent`, `InventoryDeductedEvent`
- **反例**：`CreateOrderEvent`, `CompletePaymentEvent`, `DeductInventoryEvent`
- **修复指引**：将动词改为过去分词形式

### 规则 event-sourcing-003: 事件必须携带发生时间戳
- **级别**：MUST
- **描述**：每个事件对象必须包含 occurredAt 字段（UTC 时间戳），用于事件排序与回放
- **正例**：见 event-sourcing-001 正例
- **反例**：
```java
public record OrderCreatedEvent(String orderId, String userId) {}
// 缺少时间戳字段
```
- **修复指引**：添加 Instant occurredAt 字段

## 验证方式

- 事件类可通过反射检查是否存在 setter 方法（验证不可变性）
- 命名检查可通过类名正则匹配（验证过去时态）
- 时间戳字段可通过编译期检查
```

**项目接入测试：**

```yaml
# 订单平台的 manifest
fingerprint:
  architecture: "ddd"
  tech_stack:
    backend: ["java-17", "spring-boot-3.x", "cola-4.x"]
  domains:
    - "event-sourcing"        # 激活事件溯源规范
```

---

## 常见错误

### 错误一：把产品名当领域名

```yaml
# 错误
domain: "order-platform"     # 这是产品，不是领域
domain: "crm-backend"        # 这也是产品
```

```yaml
# 正确
domain: "order-management"   # 这是领域（多个产品可能涉及订单管理）
domain: "customer-relationship"  # 这是领域
```

### 错误二：规范过于抽象，无可执行规则

```markdown
# 错误：只有原则没有规则
## 规范声明
事件溯源是一种重要的架构模式，团队应该重视事件的一致性。
```

```markdown
# 正确：有明确的、可验证的规则
### 规则 event-sourcing-001: 事件对象必须是不可变的
- **级别**：MUST
- **描述**：所有事件对象一旦创建不得修改...
```

### 错误三：SPI 插件依赖了其他 SPI 插件

SPI 插件可以依赖 L0/L1 规范（在 `depends_on` 中声明），但**不应依赖其他 SPI 插件**。SPI 之间应保持独立。如果两个领域强相关到需要互相依赖，考虑合并为一个领域。

### 错误四：在 SPI 中重定义 L0/L1 已有的规则

```markdown
# 错误：这条规则已经在 L0 naming-conventions 中定义
### 规则 event-sourcing-010: 变量使用 camelCase 命名
```

SPI 只定义该领域特有的规则。通用规则由 L0/L1 负责。如果需要覆盖 L0/L1 规则（如 DDD 中的领域语义命名覆盖通用命名），在规则中明确说明"本规则覆盖 L0 naming-conventions 中的对应规则"。

---

## 废弃 SPI 插件

当一个领域不再被任何项目使用时，按以下步骤废弃：

1. **标记为 Deprecated**：修改 `_profile.yaml` 的 `status` 字段为 `Deprecated`，添加废弃说明：

```yaml
version: "1.1"
domain: "{{domain_tag}}"
status: Deprecated
deprecation:
  reason: "{{废弃原因，如：该领域已合并到 xxx 领域}}"
  replacement: "{{替代方案，如新的领域标签，或"无"}}"
  effective_date: "2026-09-01"
```

2. **Skill 引擎行为**：Deprecated 状态的插件仍会被加载，但 Skill 会在会话开始时输出警告，提醒开发者迁移。

3. **归档**：在废弃生效日期之后，将 `status` 改为 `Archived`，Skill 不再加载该插件。文件保留在仓库中作为历史记录，不删除。

---

## 参考

- [getting-started.md](getting-started.md)：快速入门，了解规范体系全貌
- [how-to-add-spec.md](how-to-add-spec.md)：新增 L0/L1 规范模块的流程
- [skill-config-guide.md](skill-config-guide.md)：Skill 引擎配置与故障排查
- [docs/spi/_template/](../spi/_template/)：SPI 插件空壳模板（可直接复制使用）
- [docs/ARCHITECTURE.md](../ARCHITECTURE.md)：规范体系完整架构文档
