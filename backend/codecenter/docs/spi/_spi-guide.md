# SPI 领域插件接入机制

> 版本: 1.0 | 状态: 已激活 | 最后更新: 2026-06-17

---

## 一、什么是 SPI 机制

SPI（Service Provider Interface）领域插件机制是一种将**特定业务领域的编码规范**与**通用技术规范**解耦的扩展方式。

核心目标：

- **不修改核心规范库**的前提下，为特定业务领域（如数据管理、支付、风控）加载专属编码规则
- 保证领域规范可被**独立开发、独立版本管理、独立废弃**
- 让 Skill 引擎能够根据项目元数据中的 `domains` 标签，**按需激活**对应规范
- 避免通用规范层（L0/L1）反向依赖业务领域知识

---

## 二、SPI 生命周期

```
创建 (Create)
   ↓
评审 (Review)
   ↓
激活 (Activate)
   ↓
废弃 (Deprecate)
```

| 阶段 | 操作 | 负责角色 | 产出 |
|------|------|----------|------|
| 创建 | 从 `_template` 复制脚手架，编写 `domain-spec.md` | 规范贡献者 | 完整插件目录 |
| 评审 | 提交 PR，由规范委员会审查依赖关系与内容质量 | 规范委员会 | 评审意见 |
| 激活 | 合并入主干，`routing-rules.yaml` 新增对应路由条目 | 规范维护者 | 激活状态 |
| 废弃 | 标记 `status: deprecated`，保留 2 个版本后归档 | 规范维护者 | 归档记录 |

---

## 三、如何创建一个新的 SPI 插件（步骤）

### 步骤 1：复制模板

```bash
# 以 data-governance 为新领域名示例
cp -r docs/spi/_template docs/spi/{{domain_name}}
```

### 步骤 2：填写 `_profile.yaml`

打开 `docs/spi/{{domain_name}}/_profile.yaml`，按照模板中的变量说明填写：

```yaml
version: "1.0"
profile_id: "spi-{{domain_name}}"
domain: "{{domain_name}}"
description: "{{domain_description}}"
activation:
  requires_domain: "{{domain_tag}}"
depends_on:
  - naming-conventions
  - api-design
```

字段说明：

| 字段 | 说明 |
|------|------|
| `version` | 该插件的语义化版本号 |
| `profile_id` | 全局唯一标识，格式固定为 `spi-{domain}` |
| `domain` | 领域标识符，与 manifest 中的 domains 数组对应 |
| `description` | 该领域规范的一句话描述 |
| `activation.requires_domain` | 激活该插件所需的项目领域标签 |
| `depends_on` | 该插件依赖的基础规范模块 ID 列表 |

### 步骤 3：编写 `domain-spec.md`

在 `domain-spec.md` 中编写该领域的具体编码规则，结构须包含：

- 元数据表（spec_id、层级、版本、状态）
- 核心规则章节（按优先级编号）
- 代码示例（至少覆盖正反两面）
- 验收标准

### 步骤 4：注册路由

在 `docs/skill/routing-rules.yaml` 的 `spi_domains` 节点下新增条目：

```yaml
spi_domains:
  - domain: "{{domain_name}}"
    profile_id: "spi-{{domain_name}}"
    spec_file: "docs/spi/{{domain_name}}/domain-spec.md"
```

### 步骤 5：提交评审

发起 PR，并在 PR 描述中说明：

- 该领域规范覆盖的核心场景
- 依赖的基础规范模块
- 与现有规范的冲突分析

---

## 四、文件结构要求

每个 SPI 插件目录必须包含以下两个文件：

```
docs/spi/{{domain_name}}/
├── _profile.yaml      # 插件元数据（必须以下划线开头，表示元数据文件）
└── domain-spec.md     # 领域规范正文
```

### `_profile.yaml` 规范

- 必须包含 `version`、`profile_id`、`domain`、`activation` 四个顶层字段
- `depends_on` 列表中的每一项必须是已注册的 L0 或 L1 规范 ID
- 禁止依赖其他 SPI 插件（SPI 之间不可互相依赖）

### `domain-spec.md` 规范

- 文件顶部必须包含 YAML front matter 元数据表
- spec_id 格式：`spi-{domain}-rules`
- 层级固定为 `L2`（领域层）
- 规则编号格式：`{DOMAIN_ABBR}-001`，例如 `DG-001`

---

## 五、SPI 插件加载机制

Skill 引擎按以下流程决定是否加载某个 SPI 插件：

1. **读取项目 manifest**：解析 `.spec/spec-manifest.yaml` 中的 `fingerprint.domains` 数组
2. **匹配路由规则**：将 domains 数组与 `routing-rules.yaml` 中的 `spi_domains` 条目逐一比对
3. **检查依赖**：确认 `depends_on` 中列出的基础规范均已被加载
4. **激活插件**：将对应 `domain-spec.md` 的内容注入当前规范上下文

```
项目 manifest (domains: [data-governance])
          ↓
routing-rules.yaml (spi_domains 匹配)
          ↓
_profile.yaml (depends_on 依赖检查)
          ↓
domain-spec.md (规范内容注入)
```

---

## 六、依赖规则

```
依赖方向（单向）:

L2 (SPI 领域规范)  →  L1 (架构规范)  →  L0 (通用规范)
       ↑                    ↑                  ↑
   可依赖 L1/L0         可依赖 L0          不可依赖上层
```

**严格禁止：**

- L0/L1 规范引用 SPI 插件中的规则
- SPI 插件之间互相依赖（横向依赖）
- SPI 插件覆盖（override）L0 规范中的强制条款

**允许：**

- SPI 插件扩展 L1 规范（在 L1 规则基础上追加领域约束）
- SPI 插件在 `depends_on` 中声明 L0/L1 模块，确保前置条件满足
- 多个 SPI 插件同时激活（各自规则并行生效，冲突时按优先级裁决）

---

## 七、命名规范

| 类型 | 格式 | 示例 |
|------|------|------|
| 目录名 | kebab-case，与 domain 字段完全一致 | `data-governance` |
| profile_id | `spi-{domain}` | `spi-data-governance` |
| spec_id | `spi-{domain}-rules` | `spi-data-governance-rules` |
| 规则编号 | `{DOMAIN_ABBR}-{NNN}` | `DG-001`、`PAY-003` |
| 领域标签 | kebab-case，与 manifest domains 数组对应 | `data-governance` |

---

## 八、版本管理

SPI 插件遵循独立的语义化版本（参见 `meta/versioning-strategy.md`）：

- **MAJOR**：规则存在不兼容变更（如删除某条强制规则、改变规则语义）
- **MINOR**：新增规则，不破坏已有规则
- **PATCH**：修正规则描述歧义或代码示例错误

版本变更必须在 `_profile.yaml` 的 `version` 字段同步更新。

---

## 九、完整示例演练：data-governance 插件

### 目录结构

```
docs/spi/data-governance/
├── _profile.yaml
└── domain-spec.md
```

### _profile.yaml

```yaml
version: "1.0"
profile_id: "spi-data-governance"
domain: "data-governance"
description: "数据管理领域规范，涵盖数据质量、元数据管理、数据血缘等通用技术规则"
activation:
  requires_domain: "data-governance"
depends_on:
  - naming-conventions
  - api-design
  - logging-standards
```

### 激活场景

当某项目的 `spec-manifest.yaml` 包含：

```yaml
fingerprint:
  domains:
    - data-governance
```

Skill 引擎将：

1. 识别 `data-governance` 领域标签
2. 加载 `spi-data-governance` 插件
3. 先确保 `naming-conventions`、`api-design`、`logging-standards` 三个基础规范已加载
4. 将 `data-governance/domain-spec.md` 的规则注入上下文
5. 开发者编写数据相关代码时，自动应用数据质量校验、元数据标注等规则

### 冲突处理示例

若 `DG-002`（要求所有数据表必须有 `data_owner` 字段）与 `api-design` 中的某条规则产生冲突：

- Skill 引擎优先执行 `DG-002`，因为 L2 优先级高于 L1
- 若开发者认为该规则不适用，可在代码注释中添加 `@spec-override DG-002 reason` 申请豁免
- 豁免记录将被审计工具收集并在质量报告中展示

---

## 十、常见问题

**Q：一个项目可以同时激活多个 SPI 插件吗？**
A：可以。manifest 的 `domains` 数组支持多个领域标签，各插件规则并行生效。

**Q：SPI 插件可以覆盖 L0 强制规则吗？**
A：不可以。L0 强制规则（MUST）不允许被任何上层规范覆盖，只能扩展。

**Q：如何废弃一个 SPI 插件？**
A：将 `_profile.yaml` 中的 `status` 改为 `deprecated`，在 `domain-spec.md` 顶部添加废弃说明，并在 `routing-rules.yaml` 中标注 `active: false`。

**Q：SPI 插件的规则编号冲突怎么办？**
A：每个领域使用独立的前缀缩写（如 DG、PAY、RISK），编号空间天然隔离，不会冲突。
