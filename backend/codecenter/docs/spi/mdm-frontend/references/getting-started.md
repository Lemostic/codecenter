description: MDM 项目技术栈基线、pnpm monorepo 结构、7 大设计原则、依赖引入规则。
---

# 快速上手（MDM 前端）

> 本文件仅说明 MDM 项目特有的工程约定。命名规范、API 5 件套、类型 5 件套、script setup 10 步顺序等通用规则见 `profiles/frontend/vue/common/*` 与 `vue3/*`。

## 1 技术栈基线

| 技术 | 推荐最低版本 |
|------|------------|
| Vue | 3.5+ |
| TypeScript | 5.6+ |
| Vite | 5.4+ |
| Element Plus | 2.13+ |
| Tailwind CSS | 3.4+ |
| Pinia | 2.2+ |
| pnpm | 9+ |

## 2 monorepo 结构（pnpm workspaces）

```
mdm-product/
├── apps/                      # 业务应用
│   ├── model-design/          # 模型设计应用
│   │   └── src/modules/       # 应用内业务模块（强制）
│   ├── data-item/             # 数据项应用
│   └── ...
├── packages/                  # 共享包
│   ├── core/                  # @mdm/core（HTTP/Auth/Router/i18n/Error）
│   ├── common/                # @mdm/common（组件/Composables/工具/类型）
│   └── types/                 # @mdm/types（跨应用类型）
├── pnpm-workspace.yaml
└── package.json
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-GS-001** | 业务代码 MUST 落在 `apps/{app}/src/modules/{m}` 下，禁止平铺到 `src/`。 |
| **MDF-FE-GS-002** | 跨应用复用的组件/Composables/工具 MUST 放 `packages/common/`，导入用 `@mdm/common`。 |
| **MDF-FE-GS-003** | 跨应用类型 MUST 放 `packages/types/`，导入用 `@mdm/types`。 |
| **MDF-FE-GS-004** | 禁止引入项目未声明的第三方依赖；新增依赖 MUST 经评审。 |
| **MDF-FE-GS-005** | 禁止使用 CommonJS 语法，必须 ES Module。 |

## 3 设计原则（MDM 7 条）

| # | 原则 | 含义 |
|---|------|------|
| 1 | **显式优于隐式** | 所有规则必须显式写出，禁止"猜测意图" |
| 2 | **唯一选择优于多选** | 同类问题只有一种标准做法 |
| 3 | **可预测优于聪明** | 命名/位置/模式必须可预测 |
| 4 | **类型即文档** | 用 TypeScript 类型表达意图 |
| 5 | **模板可复制** | 每个场景必须提供可复制模板 |
| 6 | **决策可追溯** | 每个文件位置必须有决策依据 |
| 7 | **边界要硬** | 跨模块边界硬约束 |

## 4 script setup（继承通用规范）

`script setup` 10 步骤顺序、TypeScript 泛型 Props/Emits、命名规范——全部继承 `profiles/frontend/vue/vue3/script-setup.md`，本文件不重复。

MDM 额外约束：

| 规则 | 说明 |
|------|------|
| **MDF-FE-GS-006** | `defineOptions({ name })` 中的组件名 MUST 与文件名一致，且 MUST 以 `Tp-` 前缀（封装组件）或业务实体名（页面/普通组件）。 |
| **MDF-FE-GS-007** | 封装组件文件名（如 `TpTable.vue`、`TpLeftTreeLayout.vue`） MUST 与组件名一致。 |