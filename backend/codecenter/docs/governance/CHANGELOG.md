# 规范体系变更日志

> 本文件自动记录所有通过迭代管理流程的变更，作为审计追溯的依据。

---

## [1.2.0] — 2026-06-17

### 新增 — 迭代管理框架
- **governance/GOVERNANCE.md** — 迭代管理框架总纲，含提案系统、审批流程、角色权限、14 项校验规则、防退化机制
- **governance/registry.yaml** — 元数据注册表（单一事实源），37 个模块注册
- **governance/permissions.yaml** — 4 层角色权限矩阵（Contributor → Maintainer → Reviewer → Lead Maintainer）+ 8 个受保护文件
- **governance/validation-rules.yaml** — 14 项自动校验规则声明式配置（V-001 ~ V-303）
- **governance/CHANGELOG.md** — 变更审计日志（本文件）
- **governance/QUICKSTART.md** — 复制与快速启动指南，含 6 步接入流程和 Agent 集成 FAQ

### 新增 — 变更提案系统
- **governance/proposals/_template.yaml** — 结构化提案模板（patch/minor/major 三级变更类型）
- **governance/proposals/_examples/PROP-2026-0001-fix-api-path.proposal.yaml** — 完整提案示例

### 新增 — 引导链与接入工具
- **README.md** — 体系概述与快速引导，含三级引导链图解、Hello World 接入流程、AI 工具矩阵、OpenWolf 推荐增强
- **governance/scripts/bootstrap.sh** — 多 AI Agent 一键接入脚本，支持 Claude Code / Cursor / Codex / Windsurf / Cline，含 OpenWolf 检测与推荐
- **governance/scripts/validate-spec.sh** — 规范校验脚本，实现全部 14 项规则（V-001 ~ V-303），支持 --full 全量巡检和 --ci 模式
- **governance/scripts/init.sh** — 体系初始化脚本（5 步：结构检查 → registry 一致性 → CHANGELOG 初始化 → Git hooks → 接入指南）
- **governance/scripts/.github-workflow.yml** — GitHub Actions CI/CD 配置，调用 validate-spec.sh 确保全规则覆盖

### Registry 变更
- 新增 `bootstrap` 级别，注册 `bootstrap-readme`（README.md）和 `bootstrap-script`（bootstrap.sh）
- 模块总数 35 → 37
- 体系版本 1.1.0 → 1.2.0

### 推荐集成
- **OpenWolf** — 作为可选增强工具集成到 bootstrap.sh 和 README.md，不强制不阻塞

---

## [1.1.0] — 2026-06-17

### 新增
- **aggregate-design.md** (PROF-DDD-501~512) — 聚合设计深度规范，含黄金法则、粒度决策矩阵、实现规范
- **shared-kernel.md** (PROF-DDD-601~612) — 共享内核与工程实践规范，含基类设计、DI规范、异常体系
- **design-review-checklist.md** — 概要设计自检指南（灵魂5问 + 范式检查 + POC验证）

### 更新
- **structure.md** (PROF-DDD-015~021) — 新增分层职责边界、多模块工程结构、Client模式
- **domain-model.md** (PROF-DDD-219~222) — 新增核心四原则、领域服务辨析
- **_profile.yaml** v1.0→v1.1 — 新增 aggregate-design、shared-kernel 模块
- **routing-rules.yaml** — 新增 ddd-aggregate-design、ddd-shared-kernel 路由
- **ARCHITECTURE.md** — 更新 DDD Profile 模块列表和文件索引


---

## [1.0.0] — 2026-06-17

### 初始发布
- L0 通用规范 9 份（naming, git, api, security, testing, logging, exception, tracing, change-scope）
- L1 架构配套方案 3 套（spring-boot-mvc, ddd, react-vue-frontend）
- L2 领域扩展插件 1 个（data-governance）
- Skill 引擎 v1.1（9阶段加载）
- 指南文档 5 份
- 元数据文档 2 份（versioning-strategy, dependency-matrix）
- Schema 定义 1 份

---

*本文件由迭代管理流程自动维护，请勿手动编辑。*
