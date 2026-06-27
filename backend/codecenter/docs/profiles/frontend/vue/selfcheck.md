| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-checklist |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/` |
| 适用版本 | Vue 3 |

# AI 编码自检清单

> 本文档是 `frontend-vue` 包的 AI 编码自检清单。编码完成后 MUST 逐项 ✓ 检查。
> 每条检查项标注了对应的主文档章节，便于追溯完整规则。

---

## 1. 命名合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 文件名 PascalCase + 4 后缀之一（List / Detail / Editor / Index） | `common/architecture §8` |
| ☐ | 组件名（defineOptions）与文件名一致 | `vue3/script-setup §6` |
| ☐ | API 函数是 5 个固化名之一（list / get / create / update / delete） | `common/api-conventions §1` |
| ☐ | 变量/方法使用 camelCase | `vue3/script-setup §6` |
| ☐ | 常量使用 SCREAMING_SNAKE | `vue3/script-setup §6` |
| ☐ | 无拼音、无缩写、无 `data`/`info`/`handleClick` 等模糊名 | `vue3/component §3` |

---

## 2. 类型合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 业务实体 5 件套全导出（Entity / VO / CreateDTO / UpdateDTO / Query） | `common/type-system §2` |
| ☐ | 跨模块类型放 `common/types/` | `common/type-system §4` |
| ☐ | DTO 不含 id / createdAt | `common/type-system §3` |
| ☐ | VO 不直接传给 API | `common/type-system §3` |
| ☐ | 无 `any`（必要时用 `unknown`） | `common/structure §8` |

---

## 3. 文件位置合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 视图放 `views/`，组件放 `components/` | `common/architecture §4` |
| ☐ | API 放 `api/`，类型放 `types/` | `common/architecture §4` |
| ☐ | 通用组件放 `common/components/{category}/` | `common/architecture §3` |
| ☐ | 全局 store 放 `common/stores/` | `common/architecture §3` |
| ☐ | 跨模块引用走 `index.ts` 公开面 | `common/architecture §9` |

---

## 4. API 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 5 个函数齐全（list / get / create / update / delete） | `common/api-conventions §1` |
| ☐ | 顺序固定（list → get → create → update → delete） | `common/api-conventions §1` |
| ☐ | 路径前缀 `/api/v1/{module}/{entity}` | `common/api-conventions §2` |
| ☐ | 使用 `http` 实例（非 axios 原生，非 get/post 解构导入） | `common/api-conventions §2` |
| ☐ | 异步操作有 try/catch/finally | `common/api-conventions §4` |
| ☐ | 异步操作有 loading 状态 | `common/api-conventions §4` |
| ☐ | 错误用 `{EncapsulatedMessage}.error` 提示（非 console.log） | `common/api-conventions §4` |

---

## 5. 跨模块合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 跨模块引用走 `index.ts` 公开面 | `common/architecture §9` |
| ☐ | 无循环依赖 | `common/architecture §9` |
| ☐ | 无深路径引用（如 `@/modules/*/components/*`） | `common/architecture §9` |

---

## 6. 路由合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 路由声明在 `modules/{m}/routes.ts` | `common/architecture §7` |
| ☐ | 路由 name 格式 `{module}-{type}` | `common/architecture §7` |
| ☐ | 路由权限码在 `meta.permission` 中 | `common/architecture §7` |
| ☐ | 路由模块名常量（MODULE_NAME / MODULE_DISPLAY_NAME）已填写 | `common/architecture §7` |

---

## 7. i18n 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | Key 4 段式命名空间（`{module}.{page}.{element}.{action}`） | `common/i18n §1` |
| ☐ | zh-CN.ts 与 en-US.ts 同步 | `common/i18n §2` |
| ☐ | 无通用 key（`common.save`） | `common/i18n §3` |
| ☐ | 嵌套层级 ≤ 4 层 | `common/i18n §3` |
| ☐ | 动态值用 `{name}` 占位符（无字符串拼接） | `common/i18n §4` |

---

## 8. UI 组件合规（Vue 3 + Element Plus）

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 所有表单类组件有 `size="default"` | `vue3/ui-element-plus §1.1` |
| ☐ | 所有 `el-form` 有 `label-width` | `vue3/ui-element-plus §1.2` |
| ☐ | 所有 `el-table` 有 `#empty` 槽位（用 `{EncapsulatedEmpty}`） | `vue3/ui-element-plus §1.3` |
| ☐ | 所有分页场景用 `{EncapsulatedPagination}`（或 `{EncapsulatedTable}` 内置），受控模式 | `vue3/encapsulated §4.1-4.2` |
| ☐ | 所有 `el-dialog` 有 `append-to-body` + `:close-on-click-modal="false"` | `vue3/ui-element-plus §2` |
| ☐ | 所有 `el-tag` 有 `size="default"` | `vue3/ui-element-plus §1.1` |
| ☐ | 所有必填字段标 `required` | `vue3/ui-element-plus §3` |
| ☐ | 列表场景用 `{EncapsulatedTable}`（非 el-table） | `vue3/encapsulated §4.1` |
| ☐ | `<{EncapsulatedTable}>` 节点带 `class="flex-1 min-h-0"` | `vue3/encapsulated §4.1` |
| ☐ | 表格工具栏左 = 操作，右 = 搜索；无操作/搜索时整条工具栏不写 | `vue3/page-patterns §2` |
| ☐ | 左树右表时工具栏在 `#right` 插槽内，与树搜索框水平对齐 | `vue3/page-patterns §5` |
| ☐ | 确认操作用 `{EncapsulatedConfirm}`（非 ElMessageBox） | `vue3/encapsulated §4.3` |
| ☐ | 消息提示用 `{EncapsulatedMessage}`（非 ElMessage） | `vue3/encapsulated §4.4` |
| ☐ | 空状态用 `{EncapsulatedEmpty}`（非 el-empty） | `vue3/encapsulated §4.5` |
| ☐ | 详情分组用 `{EncapsulatedSectionTitle}`（非 el-divider） | `vue3/encapsulated §4.6` |
| ☐ | 操作列按钮是 `link` 类型 | `vue3/ui-element-plus §4` |
| ☐ | 列宽用 `min-width`（非 width） | `vue3/ui-element-plus §4` |
| ☐ | 长字段加 `show-overflow-tooltip` | `vue3/ui-element-plus §4` |
| ☐ | 弹窗包 `el-scrollbar`（长内容场景） | `vue3/ui-element-plus §2` |
| ☐ | 异步操作按钮有 `:loading` | `vue3/ui-element-plus §6` |
| ☐ | 图标用组件 import（非 string 名） | `vue3/ui-element-plus §6` |
| ☐ | 状态徽标用 `el-tag` + `:type` 三态映射 | `vue3/ui-element-plus §4` |
| ☐ | 「新增」按钮带 `:icon="Plus"` | `vue3/ui-element-plus §6` |
| ☐ | 「批量删除」按钮带 `:icon="Delete"` | `vue3/ui-element-plus §6` |
| ☐ | 其他按钮（编辑/删除/查询/重置/确定/取消/导出/导入等）不携带图标 | `vue3/ui-element-plus §6` |
| ☐ | 详情页用 `{EncapsulatedSectionTitle}` 分组（不用 el-divider） | `vue3/encapsulated §4.6` |
| ☐ | 详情字段用 `el-descriptions` + `:column="2"` 或 `"3"` | `vue3/ui-element-plus §5` |
| ☐ | 状态字段用 `el-tag` | `vue3/ui-element-plus §4` |
| ☐ | 有底部操作栏的页面用 flex 三段式布局 | `vue3/page-patterns §1` |
| ☐ | 编辑页和详情页使用 `<PageHeader>` 组件（非手写标题 div） | `vue3/encapsulated §6` |
| ☐ | 左树右表页面使用 `{EncapsulatedTreeLayout}`，工具栏在 `#right` 插槽内 | `vue3/page-patterns §5` |
| ☐ | 树形控件使用 `{EncapsulatedTree}` / `{EncapsulatedTreeLazy}`（非直接使用 el-tree） | `vue3/encapsulated §4.8` |
| ☐ | 顶层路由页面使用 `{EncapsulatedPageFrame}` 作为根容器 | `vue3/encapsulated §4.7` |
| ☐ | 卡片视图列表页使用两行式工具栏 | `vue3/page-patterns §6` |
| ☐ | 视图切换使用文字按钮，当前模式 `type="primary"` | `vue3/encapsulated §5` |
| ☐ | 分页控件统一圆角（具体值由项目设计 Token 决定） | `vue3/encapsulated §4.2` |
| ☐ | 分页上一页/下一页背景色（具体值由项目设计 Token 决定） | `vue3/encapsulated §4.2` |
| ☐ | 分页容器白色背景 + 顶部 1px 边框 + 48px 高度 | `vue3/encapsulated §4.2` |

---

## 9. 设计 Token 合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 颜色引用设计 Token（无硬编码色值） | `vue3/design-tokens §1` |
| ☐ | 间距引用 Tailwind 原子类（无硬编码 px 值） | `vue3/design-tokens §3` |
| ☐ | 字号引用设计 Token | `vue3/design-tokens §2` |
| ☐ | 圆角引用设计 Token | `vue3/design-tokens §4` |
| ☐ | 无 Tailwind 默认调色板类名（`bg-gray-*` / `text-blue-*`） | `vue3/design-tokens §1.3` |
| ☐ | 默认内边距 `p-3`（无 `px-4 py-3` 分写） | `vue3/design-tokens §3.5` |
| ☐ | 仅一行工具栏不加 `border-b` | `vue3/design-tokens §3.6` |

---

## 10. 体量与结构合规

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | `.vue` 文件块顺序为 `<script setup>` → `<template>` → `<style scoped>` | `vue3/script-setup §2` |
| ☐ | 单文件 `.vue` ≤ 600 行 | `vue3/component §2` |
| ☐ | 组件 Props ≤ 10 个 | `vue3/script-setup §3` |
| ☐ | 函数 ≤ 50 行 | `common/structure §10` |
| ☐ | `routes.ts` 路由完整（列表/新增/详情/编辑/特殊页） | `common/architecture §7` |
| ☐ | `index.ts` 与公开 exports 对应 | `common/architecture §9` |

---

## 11. script setup 合规（Vue 3 特有）

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 使用 `<script setup lang="ts">` | `vue3/script-setup §1` |
| ☐ | 块内 10 步骤顺序正确 | `vue3/script-setup §1` |
| ☐ | Props 使用 TypeScript 泛型（非 runtime declaration） | `vue3/script-setup §3` |
| ☐ | Emits 使用 TypeScript 泛型 | `vue3/script-setup §4` |
| ☐ | defineExpose 暴露的方法有 JSDoc 注释 | `vue3/script-setup §5` |
| ☐ | 列表渲染使用稳定唯一的 `key`（非 index） | `vue3/script-setup §7` |
| ☐ | 交互元素用语义化 HTML（`button` / `a`，非 `div`） | `vue3/script-setup §8` |
| ☐ | 图标按钮有 `aria-label` | `vue3/script-setup §8` |

---

## 12. 状态管理合规（Vue 3 特有）

| ✓ | 检查项 | 详见 |
|---|--------|------|
| ☐ | 本地状态用 `ref` / `reactive`（不提升到全局 Store） | `vue3/state §2` |
| ☐ | 服务端数据通过业务模块 API 函数获取 | `vue3/state §4` |
| ☐ | 派生状态用 `computed`（不用 `ref` 存储） | `vue3/state §5` |
| ☐ | Store ID 格式 `{module-name}-{entity}` | `common/architecture §6` |
| ☐ | 跨组件通信按优先级：Props > Slot > Composable > Store > Provide/Inject | `vue3/state §7` |

---

## 自检完成确认

```
✅ 全部 ☐ 已勾选
✅ 任何不通过项已修复并重新自检
✅ 完成时间：____
✅ 提交人：____
```

> **自检未通过 → 禁止提交**。

---

*本文件是 `frontend-vue` 包的 AI 编码自检清单。版本与各主文档保持同步。*
