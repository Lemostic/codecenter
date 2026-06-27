# 主数据管理平台 — 文档目录

> 最后更新：2026-06-17

本目录是项目的**唯一**文档来源，涵盖规则、架构、需求、模块设计、API 全局约定。所有项目相关的 Markdown 文档应放在本目录及其子目录下。

## 目录索引

| 子目录 | 职责 | 入口文件 |
|---|---|---|
| `rules/` | 编码规范、Monorepo 约定、AI 辅助编码、Git 工作流 | [rules/coding-rules.md](./rules/coding-rules.md) |
| `architecture/` | 架构设计、工程结构 | [architecture/monorepo-design.md](./architecture/monorepo-design.md) |
| `requirements/` | 业务需求说明书 | [requirements/model-design.md](./requirements/model-design.md) |
| `modules/` | 模块设计文档（按模块分文件夹，与 `apps/*/src/modules/*` 一一对应） | [modules/model-design/README.md](./modules/model-design/README.md) |
| `api/` | API 全局约定 | [api/overview.md](./api/overview.md) |
| `tmp/` | 临时目录，存放工作过程中的中间产物（不纳入版本管理） | — |

## 文档维护规范

1. **文件命名**：使用 kebab-case 英文或中文短名。
2. **文档头部模板**：

   ```markdown
   # 文档标题

   > 最后更新：YYYY-MM-DD
   > 关联模块：module-name（如有）

   正文...
   ```

3. **同步更新责任**：
   - 修改业务模块代码时 → 同步更新对应 `modules/{name}/design.md`。
   - 新增/修改 API 时 → 同步更新对应 `modules/{name}/api.md`。
   - 修改编码规范时 → 同步更新 `rules/coding-rules.md`。

## 仓库根入口

- `CLAUDE.md`：Claude Code 入口，指向 `docs/rules/ai-coding-rules.md`。
- `AGENTS.md`：Agent 类 AI 工具入口，指向 `.wolf/OPENWOLF.md`。
- `.wolf/`：OpenWolf 上下文管理（AI 会话记忆、项目解剖图等），独立维护。

根目录不应再放置其他 `.md` 文档文件。
