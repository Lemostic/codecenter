| 字段   | 值                        |
| ---- | ------------------------ |
| 版本   | 1.0                      |
| 创建日期 | 2026-06-17               |
| 状态   | Active                   |
| 作者   | {{governance_architect}} |

---

# 企业编码规范体系

> 一套可插拔、可自动匹配、可协作迭代的 AI 时代编码标准。
> 让 AI 编程助手在你的项目中自动遵循团队规范。

## 这套体系解决什么问题

传统的编码规范是一份 PDF 或 Wiki 页面——写完就沉在角落里，没人看，AI 更不会看。本体系的核心设计理念是：**规范不是给人读的文档，而是给 AI 执行的程序。**

它通过三级加载机制实现这一目标：

```
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│   第 1 级: 入口层                                            │
│   ┌───────────────────────────────────────────┐             │
│   │ CLAUDE.md / .cursorrules / AGENTS.md      │             │ 
│   │ 一行引导代码，告诉 AI 去哪里找规范引擎      │              │
│   └──────────────────┬────────────────────────┘             │
│                      │ 自动加载                              │
│                      ▼                                      │
│   第 2 级: 路由引擎                                          │
│   ┌───────────────────────────────────────────┐             │
│   │ skill/SKILL.md + routing-rules.yaml       │             │
│   │ 读取项目指纹 → 匹配架构类型 → 按需加载       │             │
│   └──────────────────┬────────────────────────┘             │
│                      │ 按需加载                              │
│                      ▼                                      │
│   第 3 级: 规范库                                            │
│   ┌───────────────────────────────────────────┐             │
│   │ L0 通用规范 (9份，强制加载)                 │             │
│   │ L1 架构配套方案 (3套，按架构类型选一)        │             │
│   │ L2 领域扩展插件 (按需激活)                  │             │
│   └───────────────────────────────────────────┘             │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

开发者不需要背诵任何规范。只要做一次配置（运行一个脚本），AI 在每次编码时就会自动读取项目指纹、匹配到正确的规范子集、并在代码生成中自动遵循。

## 设计灵感

本体系的架构设计借鉴了三个领域的核心思想：

**操作系统的三级启动**。就像计算机启动时从固件到引导程序再到操作系统内核的链式加载，规范体系通过 Agent 配置 → SKILL.md → 规范文件的三级引导实现"接入即可用"。每一层只做最小化工作，把复杂度留给下一层。

**插件化的扩展模式**。L0/L1/L2 三层分离的设计借鉴了插件化思想——通用规范（L0）是稳定的底座，架构配套方案（L1）是可替换的模块，领域扩展插件（L2）是可添加的能力。团队可以只激活自己需要的层，而不必吞下整个体系。

**API 设计的双模式选择**。API 设计标准提供了 Mode A 和 Mode B 两种风格——Mode A 使用完整的 HTTP 方法（PUT、PATCH、DELETE 等），适合熟悉 RESTful 风格的团队；Mode B 只用 GET 和 POST，降低前后端联调的学习成本。团队根据自身情况选择合适的模式，而不是被强制统一到某种风格。

## 目录结构

```
docs/
├── README.md                            ← 你正在读的文件
├── PLAN.md                              # 架构设计计划
├── ARCHITECTURE.md                      # 顶层架构建模
├── spec-manifest.schema.yaml            # 项目清单校验 Schema
│
├── governance/                          # 迭代管理层（体系核心枢纽）
│   ├── GOVERNANCE.md                    # 迭代管理框架总纲
│   ├── QUICKSTART.md                    # 复制与快速启动指南
│   ├── registry.yaml                    # 模块登记表（37 个模块）
│   ├── permissions.yaml                 # 角色权限矩阵（4 层角色）
│   ├── validation-rules.yaml            # 自动校验规则（14 项）
│   ├── CHANGELOG.md                     # 变更审计日志
│   ├── proposals/                       # 变更提案系统
│   └── scripts/                         # 管理与接入脚本
│       ├── bootstrap.sh                 # ★ 多 Agent 一键接入
│       ├── validate-spec.sh             # 规范校验脚本
│       ├── init.sh                      # 体系初始化脚本
│       └── .github-workflow.yml         # CI/CD 配置
│
├── universal/                           # L0 通用规范（9 份，全项目强制）
│   ├── naming-conventions.md            #   命名约定
│   ├── api-design.md                    #   API 设计
│   ├── git-workflow.md                  #   Git 流程
│   ├── security-baseline.md             #   安全基线
│   ├── testing-standards.md             #   测试标准
│   ├── logging-standards.md             #   日志标准
│   ├── exception-handling.md            #   异常处理
│   ├── request-tracing.md               #   链路追踪
│   └── change-scope-control.md          #   变更范围控制
│
├── profiles/                            # L1 架构配套方案（按类型选一）
│   ├── spring-boot-mvc/                 #   Spring Boot 三层架构
│   ├── ddd/                             #   领域驱动设计（按业务边界拆分代码）
│   └── react-vue-frontend/              #   React/Vue 前端架构  ——todo：这个不体现，就直接废弃
│
├── spi/                                 # L2 领域扩展插件（按需激活）
│   ├── data-governance/                 #   数据管理领域
│   └── _template/                       #   新插件开发模板
│
├── skill/                               # Skill 路由引擎
│   ├── SKILL.md                         #   9 阶段加载引擎
│   └── routing-rules.yaml               #   指纹→规范匹配配置
│
├── guides/                              # 使用指南
│   ├── getting-started.md               #   入门指南
│   ├── how-to-add-spec.md               #   新增规范指南
│   ├── how-to-write-spi.md              #   领域扩展插件开发指南
│   ├── skill-config-guide.md            #   Skill 引擎配置指南
│   └── design-review-checklist.md       #   设计评审检查清单
│
└── meta/                                # 辅助文档
    ├── versioning-strategy.md           #   版本管理策略
    └── dependency-matrix.md             #   模块依赖矩阵
```

## 快速开始：你的 "Hello World"

假设你有一个 Java + Spring Boot 项目，想用领域驱动设计（DDD）的方式来组织代码。三步完成接入：

**第一步：引入 docs 目录**

```bash
# 方式 A：作为子模块（推荐，便于同步更新）
git submodule add <standards-repo-url> docs

# 方式 B：直接复制
cp -r /path/to/enterprise-coding-standards/docs ./docs
```

**第二步：运行引导脚本**

```bash
bash docs/governance/scripts/bootstrap.sh
```

这个脚本会做几件事：
1. 检查 docs/ 目录中引擎文件和路由配置是否完好
2. 在项目根目录创建 `.spec/spec-manifest.yaml`（项目指纹，脚本会交互式询问项目名称、架构类型和 HTTP 模式）
3. 检测你使用的 AI 编程工具，自动生成对应的配置文件（CLAUDE.md / .cursorrules / AGENTS.md 等）
4. 创建 `.spec/project-inventory.yaml` 和 `.spec/glossary.yaml` 的空模板
5. 输出一份接入报告，告诉你哪些 Agent 已配置成功

**第三步：开始编程**

打开你的 AI 编程助手，像平常一样对话。AI 会自动读取 SKILL.md 引擎，根据你的项目指纹匹配到 DDD 架构规范，并在代码生成中自动遵循。你不需要记住任何规范内容——规范会在正确的时机自动出现。

```
你: "帮我创建一个订单（Order）的核心业务对象"
AI: (自动加载相关规范，生成符合团队标准的代码)
```

## 支持的 AI 编程工具

| 工具               | 配置文件                | 接入方式                |
| ---------------- | ------------------- | ------------------- |
| Claude Code      | `CLAUDE.md`         | 引用 SKILL.md 引擎，自动激活 |

所有工具的接入逻辑相同——第 1 级只做一件事：告诉 AI 去读 `docs/skill/SKILL.md`。剩下的匹配、加载、执行全部由 SKILL.md 引擎统一处理。这确保了不同 AI 工具之间的行为一致性。

## 推荐增强：OpenWolf

[OpenWolf](https://github.com/cytostack/openwolf) 是一款开源的 AI 编程助手中件间，为 Claude Code 提供"第二大脑"能力。它与本规范引擎**天然互补**，但完全**可选**——不安装也不影响规范引擎的任何功能。

| 维度 | 规范引擎（本体系） | OpenWolf |
|------|-------------------|----------|
| 核心能力 | 告诉 AI "该遵循什么规则" | 给 AI "项目记忆和学习能力" |
| 工作方式 | 读取 .md 规范 → 匹配 → 加载 | 钩子脚本 → 追踪 → 持久化记忆 |
| 产物位置 | docs/ + .spec/ | .wolf/（运行时产物，无需提交 Git） |
| 跨会话 | 每次从项目指纹重新匹配 | cerebrum.md 跨会话保留学习成果 |

两者的协作方式是：规范引擎在每次编码任务开始时决定"加载哪些规范"，OpenWolf 则在编码过程中持续记录"AI 学到了什么、犯了什么错、下次要注意什么"。规范引擎是**规则源**，OpenWolf 是**记忆层**。

bootstrap.sh 在运行结束时会自动检测 OpenWolf 是否已安装，并给出相应的初始化建议。如果尚未安装，提示如下：

```bash
# 安装 OpenWolf（完全可选）
npm install -g openwolf
cd your-project
openwolf init
```

`.wolf/` 目录是运行时产物，建议加入 `.gitignore`。

## 规范加载机制

SKILL.md 引擎采用"索引先行、按需深加载"策略，分九个阶段工作：

```
阶段 1  读取 .spec/spec-manifest.yaml（项目指纹）
   ↓
阶段 2  读取 .spec/project-inventory.yaml + glossary.yaml（已有资产）
   ↓
阶段 3  解析指纹维度（architecture / tech_stack / domains / http_mode）
   ↓
阶段 4  加载 routing-rules.yaml → 构建规范目录索引（不加载全文）
   ↓
阶段 5  声明变更边界（核心变更区 / 关联影响区 / 禁止触碰区）
   ↓
阶段 6  按需加载完整规范（关键词匹配 + 文件路径匹配 + 强制加载）
   ↓
阶段 7  防重复检查（先查 inventory，再决定新建还是复用）
   ↓
阶段 8  应用规范指导代码生成（强制 / 建议 / 可选）
   ↓
阶段 9  完成后更新项目资产（inventory + glossary）
```

这意味着 AI 不会一次性把 37 份规范全部塞进上下文——它只在需要时加载需要的部分。

## 规范层级详解

**L0 通用规范**（9 份，所有项目强制加载）涵盖了跨语言命名、Git 工作流、API 设计、安全基线、测试标准、日志规范、异常处理、链路追踪和变更范围控制。这些是"底线规范"，无论你用什么技术栈都必须遵守。

**L1 架构配套方案**（3 套，按项目架构选其一）提供了针对特定架构风格的深度规范。`spring-boot-mvc` 适合传统三层架构项目，`ddd` 适合采用领域驱动设计（按业务边界拆分代码）的项目，`react-vue-frontend` 适合现代前端项目。每套方案包含 3~7 份规范文件，覆盖结构、命名、业务模型等维度。

**L2 领域扩展插件**（按需激活）允许团队为特定业务领域编写扩展规范。例如数据相关的项目可以激活数据质量、元数据管理、数据血缘等专项规则。通过 `spi/_template/` 可以快速创建新的领域插件。

## 规范的迭代管理

规范不是想改就改的。所有变更必须通过 `governance/proposals/` 提交结构化提案，经自动校验和人工评审后合并。这套机制确保了规范体系在长期迭代中不会退化——受保护文件不可直接修改，规则 ID 全局唯一，依赖关系不悬空，占位符不丢失。

详细的迭代管理流程请参阅 `governance/GOVERNANCE.md`。


## 常见问题

**我不需要全部这么多份规范，怎么办？**
bootstrap.sh 会根据你在 spec-manifest.yaml 中声明的架构类型自动匹配。一个 Spring Boot 三层架构项目只会加载 9 份通用规范 + 4 份架构规范 = 13 份；一个领域驱动设计项目加载 9 + 7 = 16 份。领域扩展插件更是完全按需的。

**AI 加载规范会消耗大量上下文窗口吗？**
不会。引擎采用"索引先行"策略——阶段 4 只加载每份规范的索引信息（几百字节），阶段 6 才根据当前任务关键词加载相关规范的完整内容。实测单次编码任务通常只加载 2~4 份完整规范。

**团队里有人不用 AI 编程工具，规范对他们有效吗？**
规范体系本身是 Markdown 文档，人类可以直接阅读。`guides/` 目录提供了各专题的使用指南。只是不使用 AI 工具的人无法享受自动匹配和自动遵循的能力。

**体系更新后，已接入的项目怎么同步？**
如果用 git submodule，执行 `git submodule update` 即可。如果用复制方式，重新运行 `bootstrap.sh` 会检测已有配置并提示更新。

---

*本体系通过迭代管理框架（GOVERNANCE.md）持续演进。如需贡献或反馈，请在 `governance/proposals/` 目录下提交变更提案。*
