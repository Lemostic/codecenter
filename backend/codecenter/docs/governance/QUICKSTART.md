| 字段   | 值                        |
| ---- | ------------------------ |
| 版本   | 1.0                      |
| 创建日期 | 2026-06-17               |
| 状态   | Active                   |
| 作者   | {{governance_architect}} |

---

# 规范体系复制与快速启动指南

> 本文档帮助你在 10 分钟内完成规范体系的接入。开始之前，请先确认你的角色——不同角色需要的步骤不同。

## 前置条件

在开始之前，请确保团队环境满足以下最低要求：Git 2.30+、Python 3.8+（含 pip）、Bash 终端（macOS / Linux / WSL）。Python 用于运行校验脚本中的 YAML 解析和一致性检查，pip 会自动安装所需的 `pyyaml` 库。

## 选择你的路径

先确认你在团队中的角色——这决定了你需要走哪条路径：

```
你拿到 docs/ 后想做什么？
│
├── "我想参与维护和演进这套规范"
│    → 路径 A：规范维护者
│    → 你需要 init.sh（迭代管理环境）+ bootstrap.sh（项目接入）
│
└── "我只想让自己的项目遵循这套规范"
     → 路径 B：项目开发者
     → 你只需要 bootstrap.sh（项目接入）
```

**路径 A — 规范维护者**：你负责管理 registry.yaml、评审提案、合并 PR。你需要完整的迭代管理环境：pre-commit hook 拦截非法提交、registry 一致性校验、CHANGELOG 自动初始化。团队中通常 1~3 人走这条路径。

**路径 B — 项目开发者**：你只关心"AI 在帮我写代码时能自动遵循规范"。跳过迭代管理层，直接接入。团队中绝大多数人走这条路径。

| 步骤 | 路径 A（维护者） | 路径 B（开发者） |
|------|:-:|:-:|
| ① 克隆 docs/ | ✔ | ✔ |
| ② 校验迭代管理环境（init.sh） | ✔ | — |
| ③ 接入 AI 配置（bootstrap.sh） | ✔ | ✔ |
| ④ 配置项目指纹 | ✔ | ✔ |
| ⑤ 提交规范变更提案 | ✔ | — |
| ⑥ 日常校验 | ✔ | ✔ |

> 不确定选哪条？先走路径 B。等你需要修改规范文件时，再找维护者帮你补跑 `init.sh`。

---

## 第一步：克隆仓库

```bash
git clone <your-repo-url> my-coding-standards
cd my-coding-standards/docs
```

克隆完成后，目录结构应如下：

```
docs/
├── README.md             ← 体系概览（建议先读）
├── PLAN.md
├── ARCHITECTURE.md
├── spec-manifest.schema.yaml
├── governance/         ← 迭代管理核心
│   ├── GOVERNANCE.md
│   ├── registry.yaml
│   ├── permissions.yaml
│   ├── validation-rules.yaml
│   ├── CHANGELOG.md
│   ├── proposals/
│   └── scripts/
├── universal/          ← L0 通用规范（9 份）
├── profiles/           ← L1 架构配套方案（3 套）
├── spi/                ← L2 领域扩展插件
├── skill/              ← Skill 引擎
├── guides/             ← 使用指南
└── meta/               ← 辅助文档
```

## 第二步：校验迭代管理环境（仅路径 A）

> **路径 B 用户可跳过此步，直接前往第三步。**

```bash
bash governance/scripts/init.sh
```

初始化脚本将自动完成以下操作：检查必要目录和核心文件是否完整；验证 `registry.yaml` 中注册的 37 个模块路径是否都指向真实文件；在 CHANGELOG.md 中写入初始版本条目；安装 Git pre-commit hook（每次提交时自动做 YAML 语法和冲突标记快速检查）。

如需跳过 Git hooks 安装，可以加 `--skip-hooks` 参数。

> 如果你在路径 B 中遇到 `routing-rules.yaml` 缺失或结构异常等问题，说明 docs/ 目录可能被意外修改过，此时可以跑一次 `init.sh` 做完整性检查。

## 第三步：将规范接入项目（配置 AI 助手）

```bash
bash governance/scripts/bootstrap.sh
```

这是关键的"Hello World"步骤。bootstrap.sh 会将规范引擎接入到你的业务项目根目录，自动完成三件事：

1. **创建项目指纹**（`.spec/spec-manifest.yaml`）—— 脚本会交互式询问项目名称、架构类型和 HTTP 模式，生成指纹文件。Skill 引擎根据这份指纹自动匹配到正确的规范子集。

2. **创建项目资产模板**（`.spec/project-inventory.yaml` 和 `.spec/glossary.yaml`）—— 分别用于记录项目已有的可复用资产和统一术语定义，AI 在编码时会查阅这两个文件以避免重复造轮子和命名不一致。

3. **生成 AI Agent 配置文件** —— 脚本自动检测并为以下工具生成配置：

| AI 工具            | 生成的配置文件          | 作用                   |
| ---------------- | ---------------- | -------------------- |
| Claude Code      | `CLAUDE.md`      | 告诉 AI 读取 SKILL.md 引擎 |
| Cursor           | `.cursorrules`   | 同上                   |
| Codex / OpenAI   | `AGENTS.md`      | 同上                   |
| Windsurf         | `.windsurfrules` | 同上                   |
| Cline / Roo Code | `.clinerules`    | 同上                   |

所有配置文件的核心理相同——引用一行引导指令，让 AI 去读 `docs/skill/SKILL.md`。剩下的匹配、加载、执行全部由 SKILL.md 引擎统一处理。

常用参数示例：

```bash
# 非交互式（适合 CI 或批量部署）
bash governance/scripts/bootstrap.sh \
  --project-name order-service \
  --arch ddd \
  --http-mode A

# 仅配置 Claude Code
bash governance/scripts/bootstrap.sh --agent claude

# 预览模式（不实际写入）
bash governance/scripts/bootstrap.sh --dry-run

# 强制覆盖已有配置
bash governance/scripts/bootstrap.sh --force
```

## 第四步：配置项目规范清单

bootstrap.sh 已在 `.spec/spec-manifest.yaml` 中创建了初始指纹。现在请编辑它，补充 `tech_stack` 和 `description`：

```yaml
# .spec/spec-manifest.yaml — 项目规范清单
version: "1.0"
project:
  name: "your-project-name"
  description: "项目一句话描述"     # ← 请补充
fingerprint:
  architecture: "ddd"
  tech_stack:                       # ← 请补充
    - "java"
    - "spring-boot"
    - "jpa"
  domains: []
  http_mode: "A"
```

`fingerprint.architecture` 决定加载哪个 L1 架构配套方案——选 `ddd` 则加载领域驱动设计规范（按业务边界拆分代码），选 `spring-boot-mvc` 则加载传统三层架构规范。`fingerprint.tech_stack` 影响 Skill 引擎的细粒度匹配（如检测到 `jpa` 会加载持久层相关规则）。`fingerprint.domains` 用于激活 L2 领域扩展插件。`fingerprint.http_mode` 在团队 API 设计风格上做了二元选择——Mode A 使用完整的 PUT/PATCH/DELETE，Mode B 只用 GET/POST 以降低前后端联调复杂度。

## 第五步：提交第一个规范变更（仅路径 A）

> **路径 B 用户通常不需要此步。如果你只是想使用规范，跳过此步直接前往第六步。**

规范体系的核心约束是**所有变更必须通过提案系统**，不允许任何人直接编辑 .md 规范文件。以下是完整的变更流程。

首先，复制提案模板并填写内容：

```bash
cp governance/proposals/_template.yaml \
   governance/proposals/PROP-2026-0002-your-change.proposal.yaml
```

打开新创建的提案文件，至少需要填写以下关键字段：

```yaml
proposal:
  id: "PROP-2026-0002"
  type: patch              # patch（修正示例/格式）| minor（新增规则）| major（结构调整）
  author: "your-name"
  date: 2026-06-17
  status: draft

summary: "简述你要做什么变更以及为什么"

changes:
  - target: "universal/api-design.md"
    action: modify
    rule_ids: ["UNI-AD-003"]
    description: "具体描述修改内容"
    rationale: "修改理由"

impact:
  affected_specs: ["api-design"]
  affected_profiles: []
  breaking_change: false
```

然后创建 PR 并请求评审。审批要求取决于变更类型：patch 需要 1 名维护者审批，minor 需要 2 名维护者审批，major 需要评审委员会全员通过。PR 提交后 CI 自动运行校验流水线，校验不通过则阻止合并。

## 第六步：运行与验证

```bash
# 增量校验（日常使用）
bash governance/scripts/validate-spec.sh

# 全量巡检（建议每两周一次）
bash governance/scripts/validate-spec.sh --full
```

校验脚本覆盖 14 项检查，包括 YAML 语法、registry 路径完整性、架构配套方案模块完整性、匹配规则完整性、规则 ID 唯一性、双向引用一致性、版本号合规、依赖完整性、权限校验、受保护文件检查、占位符完整性和 CHANGELOG 条目检查。全量模式还会额外检查孤立文件（存在于磁盘但未在 registry 注册的文件）。

## 角色分配建议

团队首次接入时，建议按以下方式分配角色。指定 1 名首席维护者（Lead Maintainer），负责 registry.yaml 和迭代管理层文件的维护，同时担任评审委员会主席。指定 2~3 名维护者（Maintainer），负责日常提案评审和 PR 合并。其余开发人员作为贡献者（Contributor），通过提交提案参与规范迭代。评审委员（Reviewer）由技术负责人或架构师担任，专责审批 major 级变更。

角色配置记录在 `governance/permissions.yaml` 中。如需调整角色人员，由 Lead Maintainer 通过 major 级提案修改该文件。

## CI/CD 接入

仓库中已提供 GitHub Actions 配置文件 `governance/scripts/.github-workflow.yml`。将其复制到 `.github/workflows/spec-validation.yml` 即可启用：

```bash
mkdir -p .github/workflows
cp docs/governance/scripts/.github-workflow.yml .github/workflows/spec-validation.yml
```

如果使用 GitLab CI 或 Jenkins 等其他 CI 平台，核心逻辑相同——在 PR/MR 触发时执行 `validate-spec.sh` 脚本，校验失败则阻止合并。可以参考 `.github-workflow.yml` 中的步骤定义移植到对应平台。

## 定制与扩展

团队可以根据自身需要对规范体系进行定制。新增 L1 架构配套方案（例如 `go-microservice`）时，在 `profiles/` 下创建新目录和 `_profile.yaml`，编写对应的 .md 规范文件，然后在 `registry.yaml` 中注册新模块，最后在 `skill/routing-rules.yaml` 中添加匹配规则。新增 L2 领域扩展插件时，复制 `spi/_template/` 目录并按 `guides/how-to-write-spi.md` 中的指南填写。

所有定制都遵循同一个原则：通过提案系统发起变更，经 CI 校验和人工评审后合并。这是规范体系保持长期健康运转的核心保证。

## 常见问题

**Q: 校验脚本报错 "PyYAML 未安装" 怎么办？**
运行 `pip install pyyaml` 即可。初始化脚本会自动尝试安装，但某些环境可能需要手动操作。

**Q: 我可以直接修改 .md 规范文件吗？**
不可以。这是规范体系最核心的约束。所有变更必须通过 `governance/proposals/` 提交提案，经评审后合并。直接修改会被 CI 校验拦截。

**Q: 提案被拒绝了怎么办？**
根据评审反馈修改提案内容，更新 `status` 为 `draft`，重新提交即可。提案文件本身不需要重新创建。

**Q: 如何查看规范体系当前的整体状态？**
运行 `bash governance/scripts/validate-spec.sh --full` 查看全量巡检报告，或直接查阅 `governance/registry.yaml` 了解每个模块的版本和状态。

**Q: 体系版本和模块版本是什么关系？**
体系版本（`registry.yaml` 中的 `system_version`）描述整体架构状态，模块版本描述单个规范的迭代状态。patch 级修改只升模块版本的 patch 位；minor 级修改同时升模块和体系版本的 minor 位；major 级修改升体系版本的 major 位。

**Q: bootstrap.sh 生成的 CLAUDE.md 会被我原有的内容覆盖吗？**
不会。bootstrap.sh 使用标记（`<!-- enterprise-coding-standards:start -->` ... `<!-- enterprise-coding-standards:end -->`）追加内容，不会触碰文件中的已有内容。重复运行也不会重复追加——脚本会检测标记是否已存在。如果你手动删除了标记内的内容，重新运行 `bootstrap.sh --force` 即可恢复。

**Q: 我只用 Claude Code，不需要 Cursor 配置，怎么办？**
使用 `--agent claude` 参数仅生成 Claude Code 的配置：`bash governance/scripts/bootstrap.sh --agent claude`。支持的值有 `claude`、`cursor`、`codex`、`windsurf`、`cline`，以及 `all`（默认，生成全部）。

**Q: AI 真的会自动遵循规范吗？我怎么验证？**
接入后，在 AI 编程助手中说"帮我创建一个 Controller"或"帮我设计一个 API"。如果 AI 在回复中引用了规则编号（如 `UNI-AD-003`）或主动声明了变更边界，说明引擎已生效。你也可以使用 `spec:catalog` 指令让 AI 展示当前已加载的规范目录来确认。

**Q: 我走了路径 B，后来想修改某条规范怎么办？**
找团队中的路径 A 维护者发起提案。如果你自己也想成为维护者，让现有 Lead Maintainer 在 `permissions.yaml` 中为你分配角色，然后补跑一次 `init.sh` 建立迭代管理环境即可。

---

*如需进一步帮助，请参阅 `governance/GOVERNANCE.md` 迭代管理框架总纲，或 `guides/` 目录下的各专题指南。*
