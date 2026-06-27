---
description: AI 编码自检清单 — 编码完成后逐项检查：命名/类型/文件位置/API/跨模块/路由/i18n/UI组件/设计Token/体量结构。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1. 命名合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 文件名 PascalCase + 4 后缀之一（List / Detail / Editor / Index） | [PO] §5 页面命名铁律 |
| ☐ | 组件名（defineOptions）与文件名一致 | [CS] §3.6 命名规范 |
| ☐ | API 函数是 5 个固化名之一（list / get / create / update / delete） | [CS] §4.1 命名铁律 |
| ☐ | 变量/方法使用 camelCase | [CS] §3.6 命名规范 |
| ☐ | 常量使用 SCREAMING_SNAKE | [CS] §3.6 命名规范 |
| ☐ | 无拼音、无缩写、无 `data`/`info`/`handleClick` 等模糊名 | [CS] §3.6 命名规范 |

---

## 2. 类型合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 业务实体 5 件套全导出（Entity / VO / CreateDTO / UpdateDTO / Query） | [CS] §5.2 业务实体 5 件套 |
| ☐ | 跨模块基座类型放 `@mdm/core/types/`，`@mdm/common/types/` 重新导出 | [CS] §5.1 基座类型模式 |
| ☐ | DTO 不含 id / createdAt | [CS] §5.2 业务实体 5 件套 |
| ☐ | VO 不直接传给 API | [CS] §5.2 业务实体 5 件套 |
| ☐ | 无 `any`（必要时用 `unknown`） | [CS] §2 技术栈基线 |

---

## 3. 文件位置合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 视图放 `views/`，组件放 `components/` | [AT] §4 业务模块结构 |
| ☐ | API 放 `api/`，类型放 `types/` | [AT] §4 业务模块结构 |
| ☐ | 通用组件放 `@mdm/common/components/{category}/` | [PO] §4 @mdm/common 子目录 |
| ☐ | 全局 store 放 `@mdm/common/stores/` | [PO] §7 状态管理补充 |

---

## 4. API 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 5 个函数齐全（list / get / create / update / delete） | [CS] §4.1 命名铁律 |
| ☐ | 顺序固定（list → get → create → update → delete） | [CS] §4.2 铁律 |
| ☐ | 路径前缀 `/api/v1/{module}/{entity}` | [CS] §4.2 铁律 |
| ☐ | 使用 `@mdm/core/http` 的 `http` 实例（非 axios 原生，非 get/post 解构导入） | [CS] §4.2 铁律 |
| ☐ | 异步操作有 try/catch/finally | [CS] §4.3 错误处理标准模式 |
| ☐ | 异步操作有 loading 状态 | [CS] §4.3 错误处理标准模式 |
| ☐ | 错误用 `TpMessage.error` 提示（非 console.log） | [CS] §4.3 错误处理标准模式 |

---

## 5. 跨模块合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 跨模块引用走 `index.ts` 公开面 | [AT] §1 分层架构 |
| ☐ | 无循环依赖 | [AT] §1 分层架构 |
| ☐ | 无深路径引用（如 `@/modules/*/components/*`） | [AT] §1 分层架构 |

---

## 6. 路由合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 路由声明在 `modules/{m}/routes.ts` | [PO] §6 路由规范补充 |
| ☐ | 路由 name 格式 `{module}-{type}` | [PO] §6 路由规范补充 |
| ☐ | 路由权限码在 `meta.permission` 中 | [PO] §6 路由规范补充 |
| ☐ | 路由模块名常量（MODULE_NAME / MODULE_DISPLAY_NAME）已填写 | [PO] §6 路由规范补充 |

---

## 7. i18n 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | Key 4 段式命名空间（`{module}.{page}.{element}.{action}`） | [CS] §6.1 Key 命名 |
| ☐ | zh-CN.ts 与 en-US.ts 同步 | [CS] §6.2 语言包结构 |
| ☐ | 无通用 key（`common.save`） | [CS] §6.3 铁律 |
| ☐ | 嵌套层级 ≤ 4 层 | [CS] §6.3 铁律 |

---

## 8. UI 组件合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 所有表单类组件有 `size="default"` | [UI] §1.1 尺寸属性 |
| ☐ | 所有 `el-form` 有 `label-width` | [UI] §1.2 表单必填 |
| ☐ | 所有 `el-table` 有 `#empty` 槽位（用 `TpEmpty`） | [UI] §1.3 表格必填 |
| ☐ | 所有分页场景用 `TpPagination`（或 `TpTable` 内置），受控模式 | [CC] §1 强制使用场景 |
| ☐ | 所有 `el-dialog` 有 `append-to-body` + `:close-on-click-modal="false"` | [UI] §4 弹窗规则 |
| ☐ | 所有 `el-tag` 有 `size="default"` | [UI] §1.1 尺寸属性 |
| ☐ | 所有必填字段标 `required` | [UI] §5 表单规则 |
| ☐ | 列表场景用 `TpTable`（非 el-table） | [CC] §1 强制使用场景 |
| ☐ | `<TpTable>` 节点带 `class="flex-1 min-h-0"` | [SK] §24 TpTable 详细布局规则 |
| ☐ | 表格工具栏左 = 操作，右 = 搜索；无操作/搜索时整条工具栏不写；左树右表时工具栏须与树搜索框水平对齐 | [SK] §24 TpTable 详细布局规则 |
| ☐ | 确认操作用 `TpConfirm`（非 ElMessageBox） | [CC] §1 强制使用场景 |
| ☐ | 消息提示用 `TpMessage`（非 ElMessage） | [CC] §1 强制使用场景 |
| ☐ | 空状态用 `TpEmpty`（非 el-empty） | [CC] §1 强制使用场景 |
| ☐ | 详情分组用 `TpSectionTitle`（非 el-divider） | [CC] §1 强制使用场景 |
| ☐ | 操作列按钮是 `link` 类型 | [UI] §6 表格规则 |
| ☐ | 列宽用 `min-width`（非 width） | [UI] §6 表格规则 |
| ☐ | 长字段加 `show-overflow-tooltip` | [UI] §6 表格规则 |
| ☐ | 弹窗包 `el-scrollbar`（长内容场景） | [UI] §4 弹窗规则 |
| ☐ | 异步操作按钮有 `:loading` | [UI] §4 弹窗规则, §8 按钮规则 |
| ☐ | 图标用组件 import（非 string 名） | [UI] §3 强制行为清单 |
| ☐ | 状态徽标用 `el-tag` + `:type` 三态映射 | [UI] §6 表格规则 |
| ☐ | 「新增」按钮带 `:icon="Plus"` | [UI] §8 按钮规则 |
| ☐ | 「批量删除」按钮带 `:icon="Delete"` | [UI] §8 按钮规则 |
| ☐ | 其他按钮（编辑/删除/查询/重置/确定/取消/导出/导入等）不携带图标 | [UI] §8 按钮规则 |
| ☐ | 详情页用 `TpSectionTitle` 分组（不用 el-divider） | [SK] §22 描述列表规则 |
| ☐ | 详情字段用 `el-descriptions` + `:column="2"` 或 `"3"` | [UI] §7 描述列表规则 |
| ☐ | 状态字段用 `el-tag` | [UI] §7 描述列表规则 |
| ☐ | 有底部操作栏的页面用 flex 三段式布局 | [UI] §11 高度自适应布局原则 |
| ☐ | 编辑页和详情页使用 `<TpPageHeader>` 组件（非手写标题 div） | [SK] §12 TpPageHeader 示例, §28 页面高度自适应 |
| ☐ | 左树右表页面使用 `TpLeftTreeLayout`，工具栏在 `#right` 插槽内 | [SK] §8 TpLeftTreeLayout, §30 TpLeftTreeLayout 规则 |
| ☐ | 树形控件使用 `TpTree` / `TpTreeLazy`（非直接使用 el-tree） | [CC] §1 强制使用场景 |
| ☐ | 顶层路由页面使用 `TpPageFrame` 作为根容器 | [CC] §1 强制使用场景 |
| ☐ | 卡片视图列表页使用两行式工具栏 | [SK] §10 两行式工具栏规则 |
| ☐ | 视图切换使用文字按钮，当前模式 `type="primary"` | [SK] §11 视图切换代码示例 |
| ☐ | 卡片选中态使用 `ring-2 ring-[var(--el-color-primary)]` | [SK] §18 ModelCard 设计规格 |
| ☐ | 卡片图标框 36x36 蓝色 `#337bff`，复选框在右上角 | [SK] §18 ModelCard 设计规格 |
| ☐ | 卡片圆角 `4px`，阴影 `0px 1px 4px rgba(0,0,0,0.08)` | [SK] §18 ModelCard 设计规格 |
| ☐ | 分页控件统一 `border-radius: 2px` | [SK] §18 TpPagination 设计规格 |
| ☐ | 分页上一页/下一页 `#F5F7FA` 背景；激活页码 `#337BFF` 描边 | [SK] §18 TpPagination 设计规格 |
| ☐ | 分页容器白色背景 + 顶部 `1px solid #E1E9F0` 边框 + 48px 高度 | [SK] §18 TpPagination 设计规格 |

---

## 9. 设计 Token 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 颜色引用设计 Token（无硬编码色值） | [DS] §2 颜色系统 |
| ☐ | 间距引用 Tailwind 原子类（无硬编码 px 值） | [DS] §4 间距系统 |
| ☐ | 字号引用 `--mdm-font-size` 或 Element Plus 字体变量 | [DS] §3 字体系统 |
| ☐ | 圆角引用设计 Token | [DS] §5 圆角系统 |
| ☐ | 无 Tailwind 默认调色板类名（`bg-gray-*` / `text-blue-*`） | [DS] §2 使用规则 |

---

## 10. 体量与结构合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | `.vue` 文件块顺序为 `<script setup>` → `<template>` → `<style scoped>` | [CS] §3.2 块顺序 |
| ☐ | 单文件 `.vue` ≤ 600 行（软建议） | [CS] §7 组件拆分 |
| ☐ | 组件 Props ≤ 10 个 | [CS] §3.3 defineProps |
| ☐ | 函数 ≤ 50 行 | — |
| ☐ | `routes.ts` 路由完整（列表/新增/详情/编辑/特殊页） | [PO] §6 路由规范补充 |
| ☐ | `index.ts` 与公开 exports 对应 | [AT] §4 业务模块结构 |
