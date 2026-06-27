| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 创建日期 | 2026-06-17 |
| 状态 | Active |
| 作者 | {{governance_architect}} |

---

# 规范体系迭代管理框架

> 本文档定义规范体系的自描述、自迭代、可审计的迭代管理机制。所有对规范文件的变更**必须**通过本框架规定的流程执行，**严禁直接手动编辑 .md 规范文件**。

## 1. 设计原则

| 原则 | 说明 |
|------|------|
| **入口唯一** | 所有变更必须通过 `governance/proposals/` 提交变更提案，禁止绕过入口直接修改 |
| **元数据驱动** | 规范体系的状态由 `registry.yaml` 单一事实源描述，.md 文件仅为渲染输出 |
| **权限分层** | 不同角色拥有不同的操作边界，核心元数据变更必须经过评审委员会 |
| **机器校验优先** | 每次 PR 合并前自动执行校验脚本，校验不通过则阻止合并 |
| **防退化保证** | 新版本不得破坏旧版本已建立的结构、依赖和规则引用关系 |
| **可复制性** | 整套体系可通过模板快速克隆，外部团队可在本地启动相同的迭代管理流程 |

## 2. 体系目录结构

```
docs/
├── README.md                          # ★ 体系概述与快速引导（首先阅读）
├── PLAN.md                            # 架构设计计划
├── ARCHITECTURE.md                    # 顶层架构建模
├── spec-manifest.schema.yaml          # manifest 校验 Schema
│
├── governance/                        # ★ 迭代管理层（体系核心枢纽）
│   ├── GOVERNANCE.md                  # 本文档 — 迭代管理框架总纲
│   ├── QUICKSTART.md                  # 复制与快速启动指南
│   ├── registry.yaml                  # 元数据注册表（单一事实源）
│   ├── permissions.yaml               # 角色权限矩阵
│   ├── validation-rules.yaml          # 自动化校验规则配置
│   ├── CHANGELOG.md                   # 变更审计日志
│   ├── proposals/                     # 变更提案目录
│   │   ├── _template.yaml             # 提案模板
│   │   ├── _examples/                 # 提案示例
│   │   └── *.proposal.yaml            # 实际提交的提案文件
│   └── scripts/                       # 管理与接入脚本
│       ├── bootstrap.sh               # ★ 多 AI Agent 一键接入脚本
│       ├── .github-workflow.yml       # GitHub Actions CI/CD 配置
│       ├── validate-spec.sh           # 自动校验脚本（14项规则）
│       └── init.sh                    # 体系初始化脚本（校验完整性）
│
├── universal/                         # L0 通用规范（9份）
├── profiles/                          # L1 架构配套方案（3套）
├── spi/                               # L2 领域扩展插件
├── skill/                             # Skill 引擎定义
├── guides/                            # 使用指南
└── meta/                              # 迭代管理辅助文档
```

## 3. 核心机制

### 3.1 元数据注册表（registry.yaml）

`governance/registry.yaml` 是整套规范体系的**唯一事实源**，它描述了：

- 每个规范模块的 ID、版本、文件路径、层级（L0/L1/L2）
- 模块间的依赖关系（depends_on）
- 模块状态（active / deprecated / archived）
- 关联的 skill 路由条目
- 最后修改人和修改时间

任何对 .md 文件的变更，都必须先在 registry.yaml 中体现记录变更。registry.yaml 本身的变更属于"核心元数据变更"，需要评审委员会审批。

### 3.2 变更提案系统（proposals/）

所有规范变更必须通过**变更提案**发起，提案是一个结构化 YAML 文件，包含：

```yaml
proposal:
  id: "PROP-2026-0042"           # 唯一提案编号
  type: patch | minor | major    # 变更类型（决定审批流程）
  author: "{{contributor_id}}"   # 提案人
  date: 2026-06-17
  status: draft | review | approved | merged | rejected

summary: "简述变更目的"

changes:
  - target: "universal/api-design.md"
    action: modify | create | deprecate
    rule_ids: ["UNI-AD-003"]     # 受影响的规则 ID
    description: "调整 HTTP Mode B 的示例格式"
    rationale: "修正示例中的路径错误"

impact:
  affected_specs: ["api-design", "routing-rules"]
  affected_profiles: ["ddd", "spring-boot-mvc"]
  breaking_change: false

review:
  required_approvals: 1           # patch=1, minor=2, major=全员
  reviewers: []
  committee_approval: false       # major 变更需 true
```

### 3.3 变更类型与审批流程

```
┌──────────────┬──────────────┬──────────────────┬──────────────┐
│   变更类型    │   触发条件    │    审批要求       │   执行者     │
├──────────────┼──────────────┼──────────────────┼──────────────┤
│ patch（补丁） │ 修正示例/    │ 1名维护者 review  │ 任意维护者   │
│              │ 拼写/格式    │ + CI 校验通过     │              │
├──────────────┼──────────────┼──────────────────┼──────────────┤
│ minor（次版） │ 新增规则/    │ 2名维护者 review  │ 指定维护者   │
│              │ 新增模块     │ + CI 校验通过     │              │
├──────────────┼──────────────┼──────────────────┼──────────────┤
│ major（主版） │ 结构调整/    │ 评审委员会全员    │ 首席维护者   │
│              │ 元数据变更/  │ 通过 + CI 校验   │              │
│              │ 权限变更     │ 通过             │              │
└──────────────┴──────────────┴──────────────────┴──────────────┘
```

### 3.4 审批流程图

```
提案人提交 proposal.yaml
         │
         ▼
   CI 自动校验
   (格式/一致性/完整性)
         │
    ┌────┴────┐
    │ 校验通过 │    │ 校验失败 │
    ▼         ▼
  进入评审   退回修正
    │
    ▼
  判断变更类型
    │
    ├── patch → 1名维护者审批 → 合并
    ├── minor → 2名维护者审批 → 合并
    └── major → 评审委员会审议 → 首席维护者执行合并
         │
         ▼
   更新 registry.yaml
   更新 CHANGELOG.md
   更新版本号
   合并 PR
```

## 4. 角色权限矩阵

### 4.1 角色定义

| 角色 | 代号 | 说明 |
|------|------|------|
| 贡献者 | Contributor | 普通开发人员，可提交 patch 级提案 |
| 维护者 | Maintainer | 规范日常维护，可审批 patch/minor，可执行合并 |
| 首席维护者 | Lead Maintainer | 评审委员会主席，唯一可执行 major 合并的角色 |
| 评审委员 | Reviewer | 评审委员会成员，审批 major 变更 |

### 4.2 操作权限矩阵

| 操作 | Contributor | Maintainer | Lead | Reviewer |
|------|:-----------:|:----------:|:----:|:--------:|
| 提交 patch 提案 | ✅ | ✅ | ✅ | ✅ |
| 提交 minor 提案 | ⚠️需代交 | ✅ | ✅ | ✅ |
| 提交 major 提案 | ❌ | ✅ | ✅ | ✅ |
| 审批 patch PR | ❌ | ✅ | ✅ | ❌ |
| 审批 minor PR | ❌ | ✅ | ✅ | ❌ |
| 审批 major PR | ❌ | ❌ | ✅ | ✅ |
| 修改 registry.yaml | ❌ | ❌ | ✅ | ❌ |
| 修改 permissions.yaml | ❌ | ❌ | ✅ | ✅(审议) |
| 修改 GOVERNANCE.md | ❌ | ❌ | ✅ | ✅(审议) |
| 修改 validation-rules | ❌ | ✅ | ✅ | ❌ |
| 直接编辑 .md 规范 | ❌ | ❌ | ❌ | ❌ |
| 创建/合并 PR | ❌ | ✅ | ✅ | ❌ |

> ⚠️ 代交：Contributor 可编写提案文件，但需 Maintainer 代为创建 PR。

### 4.3 关键约束

1. **绝对禁止直接编辑**：任何人不得绕过提案系统直接修改 .md 规范文件
2. **迭代管理层保护**：`governance/GOVERNANCE.md`、`registry.yaml`、`permissions.yaml` 属于受保护文件，仅 Lead Maintainer 可修改且必须有评审委员会决议
3. **CI 门控**：所有 PR 合并前必须通过 `validate-spec.sh` 校验，校验失败一律阻止合并

## 5. 自动化校验规则

每次 Pull Request 提交时，CI 自动执行以下校验（详见 `validation-rules.yaml`）：

| 校验项 | 类型 | 失败级别 | 说明 |
|--------|------|---------|------|
| 提案文件存在性 | 结构 | ERROR | PR 必须包含至少一个 proposal.yaml |
| YAML 语法检查 | 格式 | ERROR | 所有 YAML 文件语法合法 |
| 冲突标记检查 | 格式 | ERROR | 无 Git 冲突标记残留 |
| registry 一致性 | 一致性 | ERROR | registry.yaml 中所有路径指向真实文件 |
| 架构配套方案模块完整性 | 一致性 | ERROR | _profile.yaml 声明的每个 module 有对应 .md |
| 路由规则完整性 | 一致性 | ERROR | routing-rules.yaml 引用的文件路径存在 |
| 规则 ID 唯一性 | 一致性 | ERROR | 跨文件无重复 rule ID |
| 双向引用一致性 | 一致性 | ERROR | profile_rules 与 spec_registry 的 spec_id 一致 |
| 版本号合规 | 版本 | ERROR | 变更模块的版本号遵循 SemVer |
| 依赖完整性 | 完整性 | ERROR | 所有 depends_on 引用的模块存在且 active |
| CHANGELOG 更新 | 审计 | WARN | 每个变更有对应的 CHANGELOG 条目 |
| 受保护文件检查 | 安全 | ERROR | 无直接修改受保护 .md 文件的 diff |
| 占位符完整性 | 一致性 | WARN | `{{variable}}` 占位符未被意外消除 |
| 权限校验 | 安全 | ERROR | 提案作者的角色有权提交该类型变更 |

## 6. 版本管理

规范体系采用双层版本机制：

- **体系版本**（`registry.yaml` 顶部 `system_version`）：描述整体架构状态
- **模块版本**（每个模块独立版本号）：描述单个规范的迭代状态

版本升降规则：
- patch：修正示例、文案、格式 → 模块版本 PATCH+1
- minor：新增规则、新增模块 → 模块版本 MINOR+1，体系版本 MINOR+1
- major：结构调整、权限变更、元数据逻辑修改 → 体系版本 MAJOR+1，所有模块版本不变但需兼容性评估

## 7. 防退化机制

为防止规范体系在长期迭代中退化，以下约束必须始终满足：

1. **向后兼容**：新版本不得删除旧版本已定义的规则 ID，只能标记为 deprecated
2. **依赖不悬空**：任何模块的 depends_on 目标必须存在且 active
3. **路由不断裂**：routing-rules.yaml 中引用的所有文件路径必须实际存在
4. **Profile 完整性**：_profile.yaml 声明的每个 module 必须有对应 .md 文件
5. **占位符保护**：已定义的 `{{variable_name}}` 占位符不得在修改中丢失
6. **定期巡检**：建议每两周运行一次 `validate-spec.sh --full` 全量扫描

## 8. 复制与快速启动

外部团队克隆整套规范体系后，请参照 `governance/QUICKSTART.md` 完成初始化。快速流程如下：

```bash
# 1. 克隆仓库
git clone <repo-url> && cd docs/

# 2. 运行初始化脚本
bash governance/scripts/init.sh

# 3. 初始化脚本会：
#    - 检查目录结构完整性
#    - 验证 registry.yaml 与实际文件的一致性
#    - 生成初始 CHANGELOG.md
#    - 安装 Git pre-commit hook
#    - 输出接入指南

# 4. 运行全量校验确认体系健康
bash governance/scripts/validate-spec.sh --full

# 5. 开始使用
#    参照 guides/getting-started.md 配置 spec-manifest.yaml
#    参照 governance/QUICKSTART.md 了解完整接入流程
```

---

*本文档是规范体系迭代管理的最高纲领。所有变更流程、权限分配、校验规则的最终解释权归评审委员会所有。*
