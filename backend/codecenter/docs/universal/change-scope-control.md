# 变更范围控制规范

| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L0 |
| 引入条件 | always |
| 适用架构 | 全部 |
| 依赖规范 | UNI-TS(测试规范), UNI-NC(命名规范) |
| 互斥规范 | 无 |

---

## 规范 UNI-CS-001: 变更边界声明

**规则:**

1. 每次编码任务启动前，AI MUST 先识别并声明本次变更的边界（涉及哪些文件/模块），未经声明的文件 MUST NOT 被修改。
2. 变更边界 MUST 分为三级：
   - **核心变更区**：本次任务必须修改的文件（如新建的类、需改动的 Service）
   - **关联影响区**：因核心变更而必须联动修改的文件（如对应的 DTO、Mapper 接口）
   - **禁止触碰区**：本次任务无关的所有文件，即使 AI 发现其中有问题也 MUST NOT 修改
3. AI 在编码开始前 SHOULD 向开发者确认变更范围清单，获得确认后再开始编码。
4. 如果在编码过程中发现需要超出已声明的边界，AI MUST 暂停并向开发者报告，获得许可后才能扩展范围。

**变更边界判定流程:**

```
收到编码任务
  ↓
1. 分析任务涉及的业务模块
  ↓
2. 读取 .spec/project-inventory.yaml 了解现有代码结构
  ↓
3. 确定核心变更区（需要新建/修改的文件）
  ↓
4. 确定关联影响区（必须联动修改的文件）
  ↓
5. 其余文件自动归入禁止触碰区
  ↓
6. 向开发者确认变更范围
  ↓
7. 在确认的范围内编码，超出则暂停
```

**示例 — AI 编码前的范围声明:**

```
本次任务：为用户管理模块新增"批量导入"功能

核心变更区（将新建/修改）:
  - src/main/java/{{package_base}}/service/UserImportService.java  [新建]
  - src/main/java/{{package_base}}/controller/UserController.java  [修改]
  - src/main/java/{{package_base}}/model/dto/UserImportDTO.java    [新建]

关联影响区（必须联动修改）:
  - src/main/java/{{package_base}}/service/UserService.java        [修改: 新增调用入口]

禁止触碰区:
  - 所有测试文件（*.test.*, *Test.java）—— 除非开发者明确要求
  - 所有工具类（common/utils/*）—— 复用现有工具，不新建
  - 其他模块代码 —— 与本次任务无关

是否确认此变更范围？
```

---

## 规范 UNI-CS-002: 防止重复造轮子

**规则:**

1. 在创建任何新的工具类、通用方法、API 端点之前，AI MUST 先检查 `.spec/project-inventory.yaml` 中是否已存在功能相同或相似的实现。
2. 如果已存在相似实现，AI MUST 优先复用，MUST NOT 创建重复的功能。
3. 如果现有实现不完全满足需求，AI SHOULD 通过扩展现有实现（如添加方法、添加参数）来解决，MUST NOT 新建平行类。
4. AI 在创建新工具类时 MUST 说明为什么不能复用已有的工具类，并记录在变更日志中。

**复用决策树:**

```
需要实现一个功能（工具方法/API/组件）
  ↓
检查 project-inventory.yaml
  ↓
├─ 已有完全相同的实现 → 直接复用，不新建
├─ 已有相似但不完全满足的 → 扩展现有实现（添加方法/参数）
├─ 已有同领域但不同功能的 → 在现有类中新增方法
└─ 确实不存在 → 新建，并更新 project-inventory.yaml
```

**示例:**

```
# 错误行为：AI 新建了已存在的工具
❌ 新建 StringUtils.formatDate()
   → project-inventory.yaml 已记录 DateUtils.format()

# 正确行为：复用已有工具
✅ 直接调用 DateUtils.format(date, "yyyy-MM-dd")

# 错误行为：AI 新建了重复的 API
❌ 新建 /api/metadata/models/{id}/info
   → project-inventory.yaml 已记录 /api/metadata/models/{id} 包含模型详情

# 正确行为：复用已有 API
✅ 调用 GET /api/metadata/models/{id} 获取模型元数据
```

**project-inventory.yaml 中的工具类/API 索引格式:**

```yaml
# .spec/project-inventory.yaml（节选）
utilities:
  - class: "DateUtils"
    package: "{{package_base}}.common.utils"
    methods:
      - name: "format"
        signature: "String format(Date date, String pattern)"
        description: "将日期格式化为指定格式的字符串"
      - name: "parse"
        signature: "Date parse(String dateStr, String pattern)"
        description: "将字符串解析为Date对象"

  - class: "JsonUtils"
    package: "{{package_base}}.common.utils"
    methods:
      - name: "toJson"
        signature: "String toJson(Object obj)"
        description: "对象序列化为JSON字符串"
      - name: "fromJson"
        signature: "<T> T fromJson(String json, Class<T> clazz)"
        description: "JSON字符串反序列化为对象"

api_endpoints:
  - method: GET
    path: "/api/metadata/models"
    description: "查询模型列表（支持分页、条件过滤）"
    controller: "ModelController"

  - method: GET
    path: "/api/metadata/models/{id}"
    description: "查询模型详情（含字段定义、元数据）"
    controller: "ModelController"

  - method: POST
    path: "/api/users"
    description: "创建用户"
    controller: "UserInternalController"
```

**复用优先级（4 级）:**

1. **L1 - 直接复用** — 已有实现完全满足需求，直接调用
2. **L2 - 扩展现有** — 已有实现接近需求，添加方法/参数即可
3. **L3 - 同领域新增** — 已有同领域类，在该类新增方法
4. **L4 - 新建并登记** — 确实不存在，新建后更新 `project-inventory.yaml`

**复用检查报告模板（AI 编码前必填）:**

```markdown
# 复用检查报告

## 需求功能
- 功能名：（如：用户密码加密）
- 调用点：（如：UserService.register）
- 输入/输出签名：（如：String rawPassword → String encodedHash）
- 触发频次：（如：每次注册时）

## 检查结果

| 检查项 | 结果 | 引用 |
|--------|------|------|
| 项目内是否已有同类工具/类？ | ✅ 是 / ❌ 否 | .spec/project-inventory.yaml 第 X 行 |
| 已有实现的 API 签名是否匹配？ | ✅ 完全匹配 / ⚠️ 部分匹配 / ❌ 不匹配 | `path/to/File.java:line` |
| 已有实现的性能/可靠性是否满足？ | ✅ 满足 / ⚠️ 需评估 / ❌ 不满足 | 简述 |
| 第三方库是否已有等价功能？ | ✅ 有 / ❌ 无 | `pom.xml:groupId:artifactId` |
| 社区生态是否已有成熟方案？ | ✅ 有 / ❌ 无 | URL |

## 决策

- [ ] **L1 - 直接复用**：调用 `ExistingClass.method()`，无需新建
- [ ] **L2 - 扩展现有**：在 `ExistingClass` 新增方法 `newMethod()`
- [ ] **L3 - 同领域新增**：在 `SameDomainClass` 新增方法
- [ ] **L4 - 新建并登记**：新建 `NewClass`，同步更新 `project-inventory.yaml`

## 结论

- 决策等级：L?
- 新建文件清单：（如：NewClass.java 位于 src/main/java/.../NewClass.java）
- 更新清单：（如：project-inventory.yaml 新增 NewClass 条目）
- 拒绝复用的理由：（如：现有实现缺少 X 功能，扩展成本高于新建）
```

**UNI-CS-006** AI 在创建任何新工具类、API 端点或通用方法前，MUST 填写"复用检查报告"并随代码提交。 [MUST]

**UNI-CS-007** "复用检查报告"中的决策等级（L1-L4） MUST 与代码实际行为一致——若标注 L1 但实际新建了类，视为虚假复用。 [MUST]

---

## 规范 UNI-CS-003: 已有代码保护

**规则:**

1. AI MUST NOT 对与当前任务无关的已有代码进行任何形式的"改进"、"优化"、"重构"或"格式化"。
2. 如果 AI 在编码过程中发现已有代码存在问题（bug、风格不一致、潜在风险），MUST 在变更日志中记录发现，但 MUST NOT 在本次任务中修改。
3. AI 对已有文件的修改 MUST 限定在最小必要范围——仅修改与当前任务直接相关的代码段。
4. 以下类型的文件默认受保护，AI MUST NOT 修改，除非开发者明确指令:
   - 测试文件（`*Test.java`、`*.test.ts`、`*.spec.ts`）—— 详见 UNI-TS-007
   - 配置文件（`application.yml`、`pom.xml`、`package.json`）—— 除非任务明确要求
   - 基础设施代码（`config/`、`common/`、`framework/`）—— 除非任务明确要求
   - 其他团队/模块的代码 —— 绝对禁止

**受保护文件清单可通过 manifest 配置:**

```yaml
# .spec/spec-manifest.yaml
protection:
  protected_globs:
    - "**/common/**"           # 公共工具类默认保护
    - "**/config/**"           # 配置类默认保护
    - "**/*Test.java"          # 测试类默认保护
    - "**/*.test.ts"           # 前端测试默认保护
    - "**/*.spec.ts"           # 前端测试默认保护
  protected_files:             # 额外的受保护文件
    - "src/main/java/{{package_base}}/framework/"
```

---

## 规范 UNI-CS-004: 项目清单自动维护

**规则:**

1. 每个项目 MUST 维护 `.spec/project-inventory.yaml` 文件，记录项目中的工具类、API 端点、共享组件、模块结构等元信息。
2. 当 AI 创建新的工具类、API 端点、共享组件时，MUST 同步更新 `project-inventory.yaml`。
3. `project-inventory.yaml` SHOULD 在以下时机刷新:
   - 新建模块/功能后 —— 添加新条目
   - 重构/重命名后 —— 更新条目
   - 定期维护（建议每两周） —— 全量扫描校正
4. AI 在每次编码任务开始时 MUST 读取 `project-inventory.yaml` 以了解项目现有资产。

**project-inventory.yaml 完整结构:**

```yaml
# .spec/project-inventory.yaml
version: "1.0"
last_updated: "2026-06-17"

# 模块结构
modules:
  - name: "user-management"
    description: "用户管理模块，负责用户的CRUD、权限分配"
    packages:
      - "{{package_base}}.controller"
      - "{{package_base}}.service"
      - "{{package_base}}.mapper"
    key_classes:
      - "UserService"
      - "UserInternalController"
      - "UserMapper"

  - name: "metadata"
    description: "元数据管理模块，负责模型定义、字段管理、元数据查询"
    packages:
      - "{{package_base}}.metadata.controller"
      - "{{package_base}}.metadata.service"
    key_classes:
      - "ModelController"
      - "ModelService"
      - "FieldService"

# 工具类索引
utilities:
  - class: "DateUtils"
    package: "{{package_base}}.common.utils"
    methods:
      - { name: "format", signature: "String format(Date, String)", description: "日期格式化" }
      - { name: "parse", signature: "Date parse(String, String)", description: "字符串解析为日期" }

  - class: "JsonUtils"
    package: "{{package_base}}.common.utils"
    methods:
      - { name: "toJson", signature: "String toJson(Object)", description: "对象→JSON" }
      - { name: "fromJson", signature: "<T> T fromJson(String, Class<T>)", description: "JSON→对象" }

# API 端点索引
api_endpoints:
  - { method: GET,    path: "/api/users",                  description: "用户分页列表",  controller: "UserInternalController" }
  - { method: GET,    path: "/api/users/{id}",             description: "用户详情",      controller: "UserInternalController" }
  - { method: POST,   path: "/api/users",                  description: "创建用户",      controller: "UserInternalController" }
  - { method: GET,    path: "/api/metadata/models",        description: "模型列表",      controller: "ModelController" }
  - { method: GET,    path: "/api/metadata/models/{id}",   description: "模型详情",      controller: "ModelController" }

# 共享组件（前端）
shared_components:
  - name: "DataTable"
    path: "src/components/DataTable"
    description: "通用数据表格组件，支持分页、排序、筛选"
    props: ["columns", "dataSource", "pagination", "loading"]

  - name: "SearchForm"
    path: "src/components/SearchForm"
    description: "通用搜索表单组件，支持多条件组合查询"
    props: ["fields", "onSearch", "onReset"]

# 领域概念（快速参考）
domain_concepts:
  - term: "Model"
    description: "数据模型定义，包含字段列表和元数据"
    key_class: "Model"
  - term: "Field"
    description: "模型字段定义，包含名称、类型、约束"
    key_class: "ModelField"
```

---

## 规范 UNI-CS-005: 术语字典维护

**规则:**

1. 每个项目 SHOULD 维护 `.spec/glossary.yaml` 文件，记录项目中的领域术语、技术缩写、模块命名约定等。
2. AI 在编码过程中 MUST 参考 `glossary.yaml` 中的术语定义，确保命名一致性。
3. 当引入新的领域概念或技术缩写时，MUST 同步更新 `glossary.yaml`。
4. `glossary.yaml` 中的术语 MUST 前后端一致，避免同一概念在前端和后端的命名不同。

**glossary.yaml 结构:**

```yaml
# .spec/glossary.yaml
version: "1.0"
last_updated: "2026-06-17"

# 领域术语（统一前后端命名）
domain_terms:
  - term: "模型"
    english: "Model"
    java_class: "Model"
    typescript_type: "Model"
    api_path_segment: "models"
    description: "数据模型，是元数据管理的核心实体"

  - term: "字段"
    english: "Field"
    java_class: "ModelField"
    typescript_type: "ModelField"
    api_path_segment: "fields"
    description: "模型下的字段定义"

  - term: "数据源"
    english: "DataSource"
    java_class: "DataSource"
    typescript_type: "DataSource"
    api_path_segment: "data-sources"
    description: "外部数据连接配置（如MySQL、PostgreSQL）"

# 技术缩写
abbreviations:
  - abbr: "DTO"
    full: "Data Transfer Object"
    usage: "Controller 层的入参对象，如 UserCreateDTO"
  - abbr: "VO"
    full: "View Object"
    usage: "Controller 层的返回对象，如 UserVO"
  - abbr: "CQRS"
    full: "Command Query Responsibility Segregation"
    usage: "读写分离架构模式，详见 profiles/ddd/cqrs.md"

# 路径/模块命名约定
naming_conventions:
  - concept: "模块目录"
    pattern: "kebab-case"
    example: "user-management, data-governance"
  - concept: "API路径"
    pattern: "kebab-case 复数名词"
    example: "/api/data-sources, /api/user-roles"
  - concept: "枚举值"
    pattern: "UPPER_SNAKE_CASE"
    example: "DATA_SOURCE_TYPE, USER_STATUS"
```

---

## 变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-17 | 初始版本：变更边界声明、防重复造轮子、已有代码保护、项目清单、术语字典 |
