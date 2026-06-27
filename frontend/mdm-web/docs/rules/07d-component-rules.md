---
description: 组件详细规则 — statusTagType/el-dialog/el-form/el-descriptions/按钮图标/TpTable布局/选择器/树形控件/标签页卡片。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1. statusTagType 标准映射

来源: §11.5

```typescript
const statusTagType = (status: string) => {
  const map: Record<string, string> = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
    enabled: 'success',
    disabled: 'info',
    error: 'danger',
  };
  return map[status] ?? 'info';
};
```

---

## 2. 弹窗 el-dialog 详细规则

来源: §11.1

- ✅ 必须 `append-to-body`（防 overflow 裁剪）
- ✅ 必须 `:close-on-click-modal="false"`（防误关）
- ✅ 长内容必须 `<el-scrollbar max-height="60vh">`
- ✅ footer 按钮顺序：取消 → 确定
- ✅ 保存按钮必须 `:loading="saving"`

**弹窗宽度选择**：

| 复杂度 | 字段数 | 宽度 |
|--------|--------|------|
| 简单 | 1-3 个字段 | `width="500px"` |
| 中等 | 4-8 个字段 | `width="800px"` |
| 复杂 | > 8 个字段 | `width="1000px"` + el-scrollbar |

---

## 3. 表单 el-form 详细规则

来源: §11.2

- ✅ 必填字段必须 `required` 红星
- ✅ `prop` 必须对应 `formData` 的字段
- ✅ 输入框必须 `clearable`（除密码等敏感字段）
- ✅ 长文本必须 `type="textarea" :rows="3"`

---

## 4. 描述列表 el-descriptions 规则

来源: §11.4

- ✅ 详情页分组用 `TpSectionTitle`（非 `title` 属性）
- ✅ `:column` 设为 `2` 或 `3`（避免单列过长）
- ✅ 状态字段同样用 `el-tag`
- ❌ el-descriptions 在 Element Plus 2.13 中无 `size` 属性（如看到示例有 `size="default"` 是冗余的，删掉）

---

## 5. 按钮与图标详细规则

来源: §11.5

**按钮类型规则**：

| 场景 | 类型 | 示例 |
|------|------|------|
| 表格操作列 | 必须 `link` | `<el-button size="default" link type="primary">编辑</el-button>` |
| 主操作 | 必须 `type="primary"` | `<el-button type="primary">确定</el-button>` |
| 危险操作 | 必须 `type="danger"` | `<el-button type="danger">删除</el-button>` |
| 异步操作 | 必须 `:loading` | `<el-button :loading="saving">保存</el-button>` |

**图标规则（核心硬约束）**：

| 按钮 | 是否带图标 | 图标 |
|------|----------|------|
| 新增 | ✅ | `:icon="Plus"` |
| 批量删除 | ✅ | `:icon="Delete"` |
| 编辑 / 单条删除 / 查询 / 重置 / 确定 / 取消 / 导出 / 导入 | ❌ 不带图标 | — |

**图标引入规则**：
- ✅ 必须从 `@element-plus/icons-vue` 显式 import（不全局注册）
- ✅ 加载状态用 `<el-icon class="is-loading"><Loading /></el-icon>`
- ❌ 禁止用 string 名（如 `icon="plus"` 在 2.13 已废弃）
- ❌ 禁止用 `type="text"`（已废弃，用 `text` 属性）

---

## 6. TpTable 详细布局规则

来源: §11.3

### 表格基础规则

- ✅ 列表渲染必须 `:key`（`el-table-column` 已自动）
- ✅ 长字段必须 `show-overflow-tooltip`（如 `name` / `description`）
- ✅ 状态列用 `el-tag` + `:type` 三态映射（success/warning/info/danger）
- ✅ 操作列必须 `link` 类型 + `fixed="right"`
- ✅ 必写 `#empty` 槽位（用 `TpEmpty`）

### 高度自适应（关键）

- ✅ **列表页必须使用 flex 纵向布局实现高度自适应**：搜索区/表格工具栏 `flex-shrink-0`、表格区 `flex-1 min-h-0` + `height="100%"`、分页区 `flex-shrink-0` 固定底部
- ✅ **`<TpTable>` 节点必须带 `class="flex-1 min-h-0"`**（透传到组件根 div），使其在 `.page-list`（`flex flex-col h-full`）中撑开剩余空间。**`h-full` 在 flex item 上会与内容撑高形成循环依赖**，实测会让 TpTable 节点超出 page-list 剩余空间、被 `el-main` 的 `overflow:hidden` 裁掉尾部，导致分页器不可见。**必须用 `flex-1` 而非 `h-full`**

### 表格工具栏

- ✅ **表格工具栏分左右两部分**（`flex justify-between`），**表格工具栏里不放标题**：
  - **左侧** = **操作按钮**（新增 / 删除 / 导入 / 导出 / 批量删除 等）
  - **右侧** = **搜索区**（输入字段 + 查询 / 重置），**不使用 `el-form`**，可不使用 `label`（用 `placeholder` 代替）
  - **如果表格既无操作也无搜索，可不写表格工具栏**（避免出现空 div 浪费空间）
- ✅ **列表页（一级页面）不写页面标题**。面包屑由全局布局提供，列表页直接以"表格工具栏 → TpTable"开头。需要标题的页面只有编辑/详情等二级页面，**必须使用 `<TpPageHeader>` 组件**
- ✅ **表格工具栏必须紧贴 `<TpTable>` 上方**（表格工具栏属于表格，不属于页面）。左树右表布局（`TpLeftTreeLayout`）中，表格工具栏必须放在右侧面板内（`<TpTable>` 正上方），**禁止**放在 `TpLeftTreeLayout` 外侧（页面级别）
- ✅ **左树右表中，表格工具栏必须与树工具栏（`TpTreeLazy` 搜索框）水平对齐**。实现方式：右侧面板根容器加 `pt-2`（匹配树的 `py-2` 顶部间距），表格工具栏用 `pb-2 border-b border-[#e4e7ed]`（匹配树的 `py-2` 底部间距 + `border-b`），`TpTable` 加 `pt-4` 留出工具栏与表格的间距。**禁止**表格工具栏使用 `mb-4`（会导致与树工具栏不对齐）

---

## 7. 选择器 / 输入框 / 日期选择器规则

来源: §11.7, §11.8, §11.9

### el-select

- ✅ 长列表必须 `filterable`
- ✅ 必须 `clearable`（除非只读场景）
- ✅ 宽度必须 `style="width: 100%"`（在 form-item 内）
- ❌ 禁止 option 数量 > 100 时不虚拟滚动（`el-select-virtual`）

### el-input

- ✅ 必须 `clearable`（除密码字段）
- ✅ 有限字符数必须 `maxlength` + `show-word-limit`
- ✅ 描述/备注用 `type="textarea" :rows="3"`
- ✅ 搜索框加 `:suffix-icon="Search"` + `@keyup.enter`

### el-date-picker

- ✅ 必须显式 `format` 和 `value-format`（避免歧义）
- ✅ 日期范围使用 `type="daterange"` + 2 个 placeholder
- ❌ 禁止用 `type="datetime"` 又不显式时间格式

---

## 8. 树形控件详细规则

来源: §11.10

- ✅ 必须 `node-key`（用于高亮/选中）
- ✅ 必须 `:props` 映射字段名
- ✅ 懒加载用 `:load="loadNode"`（`TpTreeLazy` 已封装）
- ✅ **左侧分类树必须使用 `TpTree` 或 `TpTreeLazy`**（`@mdm/common/components/data/`），不直接使用 `el-tree`
- ✅ TpTree/TpTreeLazy 的 `fieldMap` prop 统一映射：`{ label: 'name', children: 'children', isLeaf: 'isLeaf' }`
- ✅ `nodeKey` 默认为 `'id'`，`searchable` 默认为 `true`，`highlightCurrent` 默认为 `true`
- ✅ 搜索框使用 `el-input` + `prefix-icon`（Search 图标），容器 `py-2`，底部 `border-b`
- ✅ 树节点选中态通过 `highlight-current` 实现，选中背景色 `rgba(64, 158, 255, 0.1)`
- ✅ 暴露方法：`setSearch(keyword)` / `clearSearch()`
- ❌ 禁止用未分页的递归树（> 100 节点用 `TpTreeLazy`）
- ❌ 禁止在业务组件中直接 import `el-tree` 绕过 TpTree 封装

---

## 9. 标签页 / 卡片 规则

来源: §11.11, §11.12

### el-tabs

- ✅ 必须 `v-model` 受控
- ✅ `name` 与 `v-model` 绑定的变量值一致
- ✅ 详情页用 `TpTabs`

### el-card

- ✅ 必须 `shadow="never"`（避免视觉噪音）
- ✅ header 必须用 `<template #header>`（非 prop）
- ✅ 卡片间距用 `mb-4`（非 `margin-bottom: 16px`、非 `mb-[16px]`）
- ✅ **列表页的卡片视图不使用 `el-card`**，使用自定义卡片组件（如 `ModelCard`）
- ❌ 禁止在资产卡片列表中使用 `el-card`（样式不匹配 Figma 设计）
