# 企业编码规范 Skill 引擎

> 版本: 1.1 | 规范体系: L0-L1-L2 三层架构

---

## Skill 元数据

- **名称**: spec-engine
- **描述**: 企业编码规范智能加载引擎。根据项目指纹自动识别并加载适用的编码规范，在代码生成过程中强制执行规范要求，同时控制变更范围、保护已有代码和测试。
- **版本**: 1.1
- **触发条件**: 当项目根目录存在 `.spec/spec-manifest.yaml` 文件时自动激活

---

## 引擎工作流程

当本 Skill 被激活时，严格按以下九个阶段执行：

### 阶段 1：读取项目清单

在项目根目录查找并读取 `.spec/spec-manifest.yaml` 文件。该文件包含项目的完整身份信息：

- `project.name`：项目名称
- `project.description`：项目描述
- `fingerprint.architecture`：架构风格（如 `ddd`、`spring-boot-mvc`、`react-vue-frontend`）
- `fingerprint.tech_stack`：技术栈数组
- `fingerprint.domains`：业务领域标签数组
- `fingerprint.http_mode`：HTTP 方法模式（`full` 或 `simple`）
- `protection.protected_globs`：受保护文件 glob 模式列表
- `overrides.disabled_specs`：显式禁用的规范 ID 列表
- `overrides.forced_specs`：强制加载的规范 ID 列表

如果 manifest 文件不存在，告知开发者需要先初始化规范清单，并给出参考路径：`docs/spec-manifest.schema.yaml`

### 阶段 2：读取项目资产清单与术语字典

**2a. 读取项目清单** `.spec/project-inventory.yaml`

该文件记录项目已有的全部可复用资产：

- `modules`：现有模块列表及其核心类
- `utilities`：工具类索引（类名、方法签名、用途）
- `api_endpoints`：已有 API 端点索引（方法、路径、描述）
- `shared_components`：前端共享组件索引

如果清单不存在，提示开发者运行初始扫描建立清单，并在后续编码中逐步补充。

**2b. 读取术语字典** `.spec/glossary.yaml`

该文件记录项目的统一术语定义：

- `domain_terms`：领域术语（中英文对照、Java/TypeScript 类名、API 路径段）
- `abbreviations`：技术缩写（DTO、VO、CQRS 等）
- `naming_conventions`：项目特有的命名约定

AI 在编码中 MUST 使用术语字典中的标准命名，不得自行发明新术语。

### 阶段 3：解析项目指纹与保护配置

从 manifest 中提取核心指纹维度，并建立保护边界：

```
architecture  →  决定加载哪套架构规范（L1 层）
tech_stack    →  决定加载哪些技术标准（L0/L1 层）
domains       →  决定激活哪些 SPI 领域插件（L2 层）
http_mode     →  决定 API 设计模式（full/simple）
protection    →  决定哪些文件默认不可修改
```

### 阶段 4：加载路由规则与构建规范目录

读取 `docs/skill/routing-rules.yaml`，根据项目指纹确定应加载的规范子集。

仅加载各规范的 `_index.yaml` 索引文件，构建完整的规范目录（Spec Catalog）。此阶段不加载完整规范内容，仅建立索引，以节省上下文窗口空间。

### 阶段 5：声明变更边界（编码前必须执行）

在开始任何编码任务之前，AI MUST 执行以下变更边界声明流程：

```
1. 分析任务涉及的业务模块
2. 对照 project-inventory.yaml 识别已有资产
3. 划分变更边界：
   ├── 核心变更区：必须新建/修改的文件
   ├── 关联影响区：必须联动修改的文件
   └── 禁止触碰区：所有其他文件（含测试、配置、基础设施代码）
4. 检查 protection.protected_globs 中的文件是否被误纳入变更范围
5. 向开发者展示变更边界，等待确认
6. 仅在确认的范围内编码
```

**关键约束**：
- 测试文件（`*Test.java`、`*.test.ts`、`*.spec.ts`）默认归入禁止触碰区
- 工具类目录（`common/utils/`）默认归入禁止触碰区，除非开发者明确指令
- 如果编码过程中发现需要超出已确认的边界，MUST 暂停并报告

### 阶段 6：按需加载完整规范（任务驱动加载）

当开发者开始编码任务时，根据以下信号决定加载哪些完整规范：

**信号 1：关键词匹配**
从开发者的提问或代码上下文中提取关键词，与规范目录的 `keywords` 字段比对。

**信号 2：文件路径匹配**
根据开发者正在编辑的文件路径推断领域。

**信号 3：强制加载**
manifest 中 `overrides.forced_specs` 列表中的规范始终加载。

### 阶段 7：防重复检查（创建新代码前必须执行）

在创建任何新的工具类、API 端点、共享组件之前，AI MUST 执行：

```
1. 查阅 project-inventory.yaml 中的 utilities / api_endpoints / shared_components
2. 检查是否已存在功能相同或相似的实现
3. 决策路径：
   ├── 已有完全相同的 → 直接复用
   ├── 已有相似但不满足的 → 扩展现有实现
   ├── 已有同领域不同功能的 → 在现有类中新增方法
   └── 确实不存在 → 新建，并同步更新 project-inventory.yaml
4. 参考 glossary.yaml 确保命名一致性
```

### 阶段 8：应用规范指导代码生成

将加载的规范规则应用于当前编码任务：

1. **MUST 规则强制执行**：生成的代码必须满足所有 MUST 级别规则
2. **SHOULD 规则默认遵守**：除非开发者明确要求豁免
3. **MAY 规则不主动应用**：仅在开发者明确提及时参考

### 阶段 9：完成后更新项目资产

编码任务完成后，AI MUST：

1. 如果新建了工具类/API/组件 → 更新 `.spec/project-inventory.yaml`
2. 如果引入了新术语 → 更新 `.spec/glossary.yaml`
3. 如果发现已有代码存在问题 → 记录在变更日志中，但不修改
4. 如果修改了受保护文件 → 在变更日志中说明原因

---

## 行为准则

1. **透明性**：每次应用规范时，告知开发者正在使用哪条规范（spec_id + 规则编号）
2. **最小变更**：仅修改与当前任务直接相关的代码，不对无关代码进行"改进"或"优化"
3. **测试保护**：不修改已有测试文件，除非开发者明确指令"修改测试"
4. **复用优先**：创建新功能前必须先检查 project-inventory 中是否有可复用的实现
5. **术语一致**：使用 glossary.yaml 中的标准术语，不自行发明命名
6. **可豁免性**：开发者可通过 `@spec-override {RULE_ID} {reason}` 注释申请豁免某条规则
7. **可追溯性**：生成的代码中关键决策点附带规范引用注释
8. **渐进加载**：首次接触项目时优先加载 L0 基础规范，随任务深入逐步加载 L1/L2 规范

---

## 快速参考命令

开发者可使用以下指令与规范引擎交互：

- `spec:load {spec_id}` — 手动加载指定规范
- `spec:catalog` — 展示当前已加载的规范目录
- `spec:check` — 检查当前代码是否符合所有已加载规范
- `spec:override {rule_id} {reason}` — 申请豁免某条规则
- `spec:conflict` — 查看当前存在的规范冲突及裁决结果
- `spec:inventory` — 展示项目已有资产清单
- `spec:glossary` — 展示项目术语字典
- `spec:scope` — 展示当前编码任务的变更边界声明
