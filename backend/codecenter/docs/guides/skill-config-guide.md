# Claude Code Skill 配置指南

> 本指南深入讲解规范 Skill 引擎的工作原理、配置方式、自定义方法与故障排查。

---

## 什么是 Claude Code Skill？

Claude Code Skill（`SKILL.md`）是 Claude Code 的扩展能力载体。它是一份结构化的 Markdown 文件，定义了 AI 在会话中应该遵循的行为规则与操作流程。当 Claude Code 启动时，它读取项目 `CLAUDE.md` 中引用的 Skill 文件，将 Skill 内容作为系统级指令加载。

**Skill 与普通 prompt 的区别**：

| 维度 | 普通 prompt | Skill |
|------|-------------|-------|
| 加载方式 | 用户每次手动输入 | 项目配置后自动加载 |
| 持久性 | 单次会话有效 | 项目级别持久化 |
| 结构化 | 自由文本 | 有标准格式与生命周期 |
| 可维护性 | 散落在对话中 | 版本化管理，团队共享 |

---

## 规范路由 Skill 的工作原理

本规范体系的核心是一个名为"spec-routing"的 Skill，定义在 `docs/skill/SKILL.md` 中。它的工作流程如下：

```
Claude Code 启动
       ↓
读取 CLAUDE.md，发现 Skill 引用
       ↓
加载 SKILL.md 中的 spec-routing Skill
       ↓
Skill 读取项目的 .spec/spec-manifest.yaml
       ↓
解析项目指纹：architecture + tech_stack + domains
       ↓
匹配 docs/skill/routing-rules.yaml 中的路由规则
       ↓
组装规范上下文（L0 + L1 + L2）
       ↓
将规范加载到当前会话
```

### 路由规则文件结构

`docs/skill/routing-rules.yaml` 定义了三层匹配逻辑：

```yaml
routing:
  # L0 通用层：无条件加载
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

  # L1 架构层：按 fingerprint.architecture 匹配
  profiles:
    - match: "spring-boot-mvc"
      load:
        - spring-boot-mvc/structure
        - spring-boot-mvc/naming
        - spring-boot-mvc/persistence
        - spring-boot-mvc/api-layer
    - match: "ddd"
      load:
        - ddd/structure
        - ddd/naming
        - ddd/domain-model
        - ddd/event-patterns
        - ddd/cqrs
    - match: "react-vue-frontend"
      load:
        - react-vue-frontend/structure
        - react-vue-frontend/component
        - react-vue-frontend/state

  # L2 领域扩展层：按 fingerprint.domains 匹配
  spi:
    match_field: "domains"
    load_pattern: "spi/{domain}/*.md"
```

---

## 安装与激活 Skill

### 前提条件

- 团队已部署规范库（`docs/` 目录结构已存在）
- 项目已创建 `.spec/spec-manifest.yaml`（参见 [getting-started.md](getting-started.md)）

### 安装步骤

**第一步：在 CLAUDE.md 中引用 Skill**

在项目根目录的 `CLAUDE.md` 中添加规范 Skill 的引用路径：

```markdown
# {{project_name}}

## 项目概述
{{project_description}}

## 编码规范
本项目遵循企业编码规范体系。规范 Skill 引擎位于：
docs/skill/SKILL.md

启动编码前，请先读取并遵循该 Skill 中定义的规范路由规则。
```

**第二步：确认 Skill 文件存在**

验证以下文件路径可访问：

```
docs/skill/SKILL.md              ← Skill 定义文件
docs/skill/routing-rules.yaml    ← 路由规则文件
docs/universal/_index.yaml       ← L0 规范索引
```

**第三步：启动 Claude Code 验证**

进入项目目录，启动 Claude Code，询问：

```
"当前项目加载了哪些编码规范？"
```

如果 Skill 正常工作，你会看到一份包含 L0/L1/L2 规范列表的输出。

---

## 两阶段加载机制

Skill 引擎采用"索引优先、按需深入"的两阶段加载策略，以平衡上下文利用效率与规范覆盖完整性。

### 阶段一：索引加载（Skill 初始化时）

Skill 启动时，不加载所有规范的完整内容，而是只加载索引文件：

```yaml
# docs/universal/_index.yaml（示例片段）
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

此阶段的输出是在 Claude Code 上下文中建立一份**规范目录**——AI 知道有哪些规范可用，但尚未读取每条规则的详情。

### 阶段二：按需深入（编码过程中）

当开发者执行一个编码任务时（例如"编写一个 Controller"），Skill 引擎根据任务内容与索引中的 `keywords` 做匹配，动态加载相关规范的完整内容：

```
开发者任务："帮我写一个 UserController"
       ↓
Skill 匹配关键词："Controller" → api-layer, api-design
       ↓
加载以下规范的完整内容：
  - docs/profiles/spring-boot-mvc/api-layer.md（L1 Controller 层规范）
  - docs/universal/api-design.md（L0 API 设计规范）
       ↓
AI 在完整规范约束下生成代码
```

**这种设计的好处**：避免一次性加载所有规范导致上下文空间被占满，同时确保每次编码任务都能获得最相关的规范指导。

---

## 自定义 Skill 行为

### 通过 Manifest 覆盖

项目的 `spec-manifest.yaml` 提供了两种覆盖机制：

```yaml
overrides:
  # 禁用某些规范（即使路由规则匹配到了也不加载）
  disabled_specs:
    - "testing-standards"    # 例如：本项目是原型项目，暂不需要测试规范

  # 强制加载某些规范（即使路由规则没有匹配到也加载）
  forced_specs:
    - "react-vue-frontend/component"  # 例如：后端项目中有少量前端代码需要规范
```

**使用原则**：
- `disabled_specs` 仅用于临时应急，长期禁用需在规范库层面解决（提 PR 修改路由规则）
- `forced_specs` 用于跨架构组合场景（如全栈项目），不应用于绕过层级设计

### 通过修改路由规则

如果需要调整全局路由逻辑（例如为新的架构类型添加匹配规则），直接编辑 `docs/skill/routing-rules.yaml`：

```yaml
profiles:
  # 已有规则...
  - match: "spring-boot-mvc"
    load: [...]

  # 新增规则
  - match: "serverless"
    load:
      - serverless/structure
      - serverless/naming
      - serverless/function-design
```

修改路由规则后，所有使用该规范库的项目在下次启动 Claude Code 时自动感知变更。

---

## 故障排查

### 问题一：Skill 未被加载

**症状**：询问"加载了哪些规范"时，Claude Code 没有返回规范列表。

**排查步骤**：

1. 确认 `CLAUDE.md` 中存在对 `docs/skill/SKILL.md` 的引用
2. 确认 `docs/skill/SKILL.md` 文件存在且格式正确
3. 确认 Claude Code 启动时的工作目录是项目根目录（不是子目录）

### 问题二：L0 规范未加载

**症状**：已加载 L1 但缺少 L0 通用规范。

**排查步骤**：

1. 检查 `docs/universal/_index.yaml` 是否存在且包含所有 8 个规范 ID
2. 检查 `routing-rules.yaml` 的 `universal.always_load` 列表是否完整
3. 检查 manifest 的 `overrides.disabled_specs` 是否意外禁用了 L0 规范

### 问题三：L1 Profile 匹配失败

**症状**：manifest 中声明了 `architecture: "spring-boot-mvc"`，但对应的 Profile 规范未加载。

**排查步骤**：

1. 检查 `routing-rules.yaml` 中 `profiles` 列表是否有 `match: "spring-boot-mvc"` 条目
2. 检查 manifest 中 `fingerprint.architecture` 的拼写是否与路由规则一致（注意短横线 vs 下划线）
3. 检查 `docs/profiles/spring-boot-mvc/_profile.yaml` 是否存在

### 问题四：领域扩展插件未激活

**症状**：manifest 的 `domains` 列表包含了领域标签，但领域扩展规范未加载。

**排查步骤**：

1. 确认领域标签拼写与扩展插件目录名一致：`domains: ["data-governance"]` 对应 `docs/spi/data-governance/`
2. 确认 `docs/spi/data-governance/_profile.yaml` 存在且 `status: Active`
3. 确认 `routing-rules.yaml` 的 `spi` 部分配置了正确的 `match_field` 和 `load_pattern`

### 问题五：规范冲突未解决

**症状**：AI 生成的代码同时遵循了两条互相矛盾的规则。

**排查步骤**：

1. 确认规范的优先级规则是否生效（L2 > L1 > L0）
2. 如果两条规范属于同一层级，检查是否有显式的冲突声明（`conflicts_with` 字段）
3. 如无法自动解决，Skill 应输出警告提示开发者手动选择

---

## 验证 Skill 是否正常工作的完整检查清单

| 检查项 | 操作 | 预期结果 |
|--------|------|----------|
| Skill 文件存在 | `ls docs/skill/SKILL.md` | 文件存在 |
| 路由规则文件存在 | `ls docs/skill/routing-rules.yaml` | 文件存在 |
| Manifest 格式正确 | 检查 YAML 语法 | 无解析错误 |
| L0 索引文件存在 | `ls docs/universal/_index.yaml` | 文件存在 |
| Skill 被激活 | 询问"加载了哪些规范" | 返回规范列表 |
| L0 全部加载 | 检查返回列表 | 包含 8 个通用规范 |
| L1 正确匹配 | 检查返回列表 | 包含对应 Profile 的模块 |
| L2 按需加载 | 添加 domain 标签后检查 | 领域扩展插件出现在列表中 |
| 规范实际生效 | 让 AI 写一段代码 | 代码遵循规范（如命名规则、日志格式） |

---

## 与 CLAUDE.md 的集成最佳实践

### 推荐的 CLAUDE.md 结构

```markdown
# {{project_name}}

## 项目概述
{{一段话描述项目的功能与技术栈}}

## 编码规范
本项目遵循企业编码规范体系。
规范 Skill：docs/skill/SKILL.md
规范 Manifest：.spec/spec-manifest.yaml

## 项目特定约定
（此处放置项目级别、不属于通用规范体系的约定，如特殊目录结构、特定依赖版本等）

## 常用命令
- 构建：{{build_command}}
- 测试：{{test_command}}
- 启动：{{start_command}}
```

### 避免的做法

- **不要**在 `CLAUDE.md` 中重复定义规范体系已有的规则（如手动写"命名用 camelCase"）
- **不要**在 `CLAUDE.md` 中覆盖 Skill 的路由逻辑（如写"不要加载日志规范"）
- **不要**把 `CLAUDE.md` 变得过长——项目特定约定控制在 20 行以内

---

## 参考

- [getting-started.md](getting-started.md)：快速入门，5 分钟完成项目接入
- [how-to-add-spec.md](how-to-add-spec.md)：新增规范模块的完整流程
- [how-to-write-spi.md](how-to-write-spi.md)：编写领域扩展插件的指南
- [docs/skill/SKILL.md](../skill/SKILL.md)：Skill 定义文件
- [docs/skill/routing-rules.yaml](../skill/routing-rules.yaml)：路由规则文件
- [docs/ARCHITECTURE.md](../ARCHITECTURE.md)：规范体系完整架构文档
