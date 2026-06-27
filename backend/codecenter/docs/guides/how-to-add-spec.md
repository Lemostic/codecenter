# 如何新增规范模块

> 本指南说明在规范库中新增一个规范模块的完整流程，包括层级选择、文件创建、ID 分配与质量检查。

---

## 先判断：应该放在哪一层？

在动手写规范之前，最重要的决策是**确定新规范属于哪一层**。错误的层级会导致规范被不该适用的项目强制加载，或者被应该适用的项目遗漏。

### 决策流程图

```
这个规范是否适用于团队内所有项目？
（无论 Java/Python/Go、无论 MVC/DDD/前端）
        │
   ┌────┴────┐
   是         否
   ↓          ↓
  L0         这个规范是否与特定架构范式绑定？
 通用层      （只对 Spring Boot MVC / DDD / 前端有意义）
                    │
               ┌────┴────┐
               是         否
               ↓          ↓
              L1         这个规范是否与特定业务领域相关？
            架构配套方案   （数据管理、元数据管理、支付领域等）
                              │
                         ┌────┴────┐
                         是         否
                         ↓          ↓
                        L2        重新审视需求：
                      领域扩展层    可能不需要新增规范
```

### 三层的核心区别

| 维度 | L0 通用层 | L1 架构配套方案 | L2 领域扩展层 |
|------|-----------|-----------------|---------------|
| 加载条件 | 始终加载 | 按 `fingerprint.architecture` 匹配 | 按 `fingerprint.domains` 匹配 |
| 适用范围 | 所有项目 | 特定架构类型的项目 | 涉及特定领域的项目 |
| 存放位置 | `docs/universal/` | `docs/profiles/{profile_id}/` | `docs/spi/{domain}/` |
| 典型内容 | 命名、安全、日志 | 分层结构、持久化、组件设计 | 领域术语、业务模型约束 |
| 准入门槛 | 必须跨技术栈通用 | 必须绑定一种架构范式 | 必须绑定一个技术领域，不绑定产品 |

**关键原则**：宁可放到更具体的层级，也不要把特定规范混入 L0。L0 规范会被所有项目加载，错误的 L0 规范会对不适用它的项目造成干扰。

---

## 新增 L0 通用规范

**准入标准**（全部满足才能进 L0）：
1. 规则适用于任意技术栈（Java/Python/Go/TypeScript）
2. 规则适用于任意架构类型（MVC/DDD/微服务/单体）
3. 规则不包含任何业务术语或领域概念
4. 团队内至少 80% 的项目会受益于这条规则

### 步骤一：创建 Markdown 文件

在 `docs/universal/` 下创建新文件，文件名即规范 ID：

```
docs/universal/{{spec_id}}.md
```

文件必须包含标准元信息头：

```markdown
# {{规范名称}}

## 元信息

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 状态 | Draft |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | {{dependency_list 或"无"}} |
| 互斥规范 | {{conflict_list 或"无"}} |
| 作者 | {{author}} |

## 规范声明

（说明本规范定义了哪些技术规则，为什么它们跨项目通用）

## 规则清单

### 规则 {{spec_id}}-001: {{规则名称}}
- **级别**：MUST
- **描述**：{{具体规则描述}}
- **正例**：
```java
// 正确示例代码
```
- **反例**：
```java
// 错误示例代码
```
- **修复指引**：{{如何将反例修正为正例}}

## 验证方式

（说明如何在 AI 编码过程中自动验证本规则是否被遵循）
```

### 步骤二：分配规则 ID

每条规则必须有唯一 ID，格式为 `{spec_id}-{序号}`：

```markdown
### 规则 logging-001: 日志必须使用结构化格式
### 规则 logging-002: 禁止在日志中输出敏感信息
### 规则 logging-003: ERROR 级别日志必须包含异常堆栈
```

ID 一旦分配不得复用。废弃的规则 ID 永久保留（标记为 Archived），新规则使用新 ID。

### 步骤三：注册到索引文件

编辑 `docs/universal/_index.yaml`，追加新规范的索引条目：

```yaml
# docs/universal/_index.yaml
specs:
  # ... 已有条目 ...
  - id: {{spec_id}}
    version: "1.0"
    summary: "{{一句话描述本规范的核心职责}}"
    keywords: ["{{关键词1}}", "{{关键词2}}", "{{关键词3}}"]
```

`keywords` 用于 Skill 引擎的按需深入加载——当开发者涉及这些关键词相关的编码任务时，Skill 会加载本规范的完整内容。

### 步骤四：更新路由规则

编辑 `docs/skill/routing-rules.yaml`，在 `universal.always_load` 列表中添加新规范 ID：

```yaml
routing:
  universal:
    always_load:
      - naming-conventions
      - git-workflow
      # ... 已有条目 ...
      - {{spec_id}}          # 新增
```

---

## 新增 L1 架构 Profile 模块

### 在已有 Profile 下新增模块

如果新规范属于已有的 Profile（如 `spring-boot-mvc`），只需在该 Profile 目录下创建新的 `.md` 文件：

```
docs/profiles/spring-boot-mvc/{{module_name}}.md
```

然后更新 `_profile.yaml` 的 `modules` 列表：

```yaml
# docs/profiles/spring-boot-mvc/_profile.yaml
modules:
  - structure
  - naming
  - persistence
  - api-layer
  - {{module_name}}          # 新增模块
```

同时更新 `docs/skill/routing-rules.yaml` 中对应 Profile 的 `load` 列表：

```yaml
- match: "spring-boot-mvc"
  load:
    - spring-boot-mvc/structure
    - spring-boot-mvc/naming
    - spring-boot-mvc/persistence
    - spring-boot-mvc/api-layer
    - spring-boot-mvc/{{module_name}}     # 新增
```

### 新建一个完整 Profile

如果现有三个 Profile（spring-boot-mvc / ddd / react-vue-frontend）都不适合你的架构，需要新建：

1. 在 `docs/profiles/` 下创建新目录：`docs/profiles/{{profile_id}}/`
2. 创建 `_profile.yaml`，参考现有 Profile 格式：

```yaml
version: "1.0"
profile_id: "{{profile_id}}"
name: "{{Profile 中文名称}}"
description: "{{适用场景说明}}"
architecture_type: "{{架构类型标识}}"
tech_stack:
  - "{{tech_stack_1}}"
  - "{{tech_stack_2}}"
modules:
  - structure
  - naming
conflicts_with:
  - "{{互斥的 profile_id}}"
```

3. 为该 Profile 编写至少 2 个模块（建议包含 `structure.md` 和 `naming.md`）
4. 在 `routing-rules.yaml` 的 `profiles` 列表中添加匹配规则

---

## 新增 L2 领域扩展规范

领域扩展规范的创建流程与 L0/L1 略有不同，详见 [how-to-write-spi.md](how-to-write-spi.md)。

---

## 质量检查清单

新增规范提交前，按以下清单逐项核查：

- [ ] **业务无关性**：规范内容不包含任何业务术语、产品名或领域概念（L0/L1 必须满足）
- [ ] **层级正确**：规范放在正确的层级（用决策流程图验证）
- [ ] **元信息完整**：Markdown 文件包含标准元信息头，所有字段填写完整
- [ ] **规则 ID 唯一**：每条规则有唯一 ID，格式为 `{spec_id}-{序号}`
- [ ] **正反例齐全**：每条 MUST/SHOULD 级别规则同时提供正例和反例
- [ ] **关键词覆盖**：索引文件中提供了 3-5 个关键词，覆盖常见编码场景
- [ ] **路由已注册**：`routing-rules.yaml` 已更新，Skill 能正确匹配到新规范
- [ ] **索引已更新**：`_index.yaml` 或 `_profile.yaml` 已追加新条目
- [ ] **无循环依赖**：新规范不引入循环依赖（A 依赖 B，B 又依赖 A）
- [ ] **验证方式明确**：文档说明了规则如何被自动验证

---

## 评审与发布流程

```
1. 草案阶段（Draft）
   - 完成文件创建与质量检查清单
   - 自测：在一个示例项目 manifest 中加载新规范，验证 Skill 能正确解析

2. 提案阶段（Proposed）
   - 提交 PR，附上变更说明：
     * 新增了什么规则
     * 为什么放在这个层级
     * 影响了哪些现有规范（依赖/冲突）
   - 指定至少 1 名评审人

3. 评审阶段
   - 评审人按质量检查清单逐项核查
   - 重点检查：层级是否正确、业务无关性是否满足、规则 ID 是否冲突
   - 评审通过后合并 PR

4. 发布阶段（Active）
   - 更新规范文件的"状态"字段为 Active
   - 更新 `docs/meta/changelog.md` 记录变更
   - 通知相关项目（如有破坏性变更，留出迁移期）
```

---

## 常见错误

**错误一：把架构特定规范放进 L0**

例如把"Spring Bean 必须用构造器注入"放进 L0。这条规则对 Python 项目和前端项目毫无意义，应该放在 L1 的 `spring-boot-mvc` Profile 下。

**错误二：规则缺少反例**

只写"应该怎么做"但不写"不该怎么做"，AI 在识别违规代码时缺少比对基准。每条规则都必须有反例。

**错误三：忘记更新路由规则**

创建了规范文件但忘记在 `routing-rules.yaml` 中注册，导致 Skill 引擎永远加载不到新规范。

**错误四：关键词过于宽泛**

索引中的 `keywords` 如果写了 `["代码", "编程"]` 这种泛词，会导致 Skill 在几乎每次编码任务中都深度加载本规范，浪费上下文空间。关键词应该具体且有区分度。

---

## 参考

- [getting-started.md](getting-started.md)：快速入门，了解规范体系全貌
- [how-to-write-spi.md](how-to-write-spi.md)：L2 领域扩展插件的编写指南
- [skill-config-guide.md](skill-config-guide.md)：Skill 引擎配置与工作原理
- [docs/ARCHITECTURE.md](../ARCHITECTURE.md)：规范体系完整架构文档
