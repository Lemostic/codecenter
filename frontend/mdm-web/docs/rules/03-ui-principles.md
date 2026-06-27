---
description: Element Plus 组件使用原则。包含必填属性、禁止行为、强制行为、弹窗/表单/表格/树形/卡片/高度自适应布局规则。
paths:
  - "**/*.vue"
---

## 1 必填属性原则

Element Plus 组件存在大量"不报错但行为不一致"的隐性属性。以下原则确保跨项目的一致性。

### 1.1 尺寸属性

所有表单类组件必须显式声明 `size`（推荐 `size="default"`），避免继承上下文导致尺寸不一致：

| 组件 | 必填属性 |
|------|---------|
| `el-input` | `size` |
| `el-input-number` | `size` |
| `el-select` | `size` |
| `el-button` | `size` |
| `el-tag` | `size` |

### 1.2 表单必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-form` | `size`、`label-width` | 必须显式声明 label 宽度（如 80px/100px/120px），否则对齐错乱 |
| `el-form-item` | `label`、`prop` | `label` 必填；`prop` 在有校验时必填 |

### 1.3 表格必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-table` | `data`、`v-loading` | 必须绑定数据和加载状态 |
| `el-table` | `#empty` 槽位 | 表格无数据时必须展示空状态组件 |

### 1.4 弹窗必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-dialog` | `v-model`、`title`、`width` | 必须 `append-to-body`、`:close-on-click-modal="false"` |

### 1.5 其他组件必填

| 组件 | 必填属性 | 规则 |
|------|---------|------|
| `el-text` | `size` | `size="default"` |
| `el-tree` | `node-key`、`data` | `:data="treeData"`，`node-key="id"` |
| `el-dropdown` | `@command` | `@command="handleCommand"` 必须显式 |
| `el-tooltip` | `content` | `:content="提示文案"` |
| `el-page-header` | `@back` | `@back="goBack"` 或路由回退 |

### 1.6 受控模式

以下组件必须使用受控模式（`v-model`）：

`el-checkbox`、`el-radio`、`el-switch`、`el-tabs`、`el-date-picker`、`el-rate`

---

## 2 禁止行为清单

- ❌ 禁止省略 `size="default"`
- ❌ 禁止在视图中硬编码颜色（如 `color: #409eff`、`background: #fff`）。有 CSS 变量的颜色必须用变量（如 `var(--el-color-primary)`）；无 CSS 变量的项目色由项目设计系统定义引用方式
- ❌ 禁止在视图里硬编码间距数值（如 `margin: 16px`），必须用 Tailwind 原子类
- ❌ 禁止操作列按钮用普通类型（必须 `link`）
- ❌ 禁止弹窗不写 `append-to-body`（被父元素 overflow 裁剪）
- ❌ 禁止表单不写 `label-width`（对齐错乱）
- ❌ 禁止表格不写 `#empty` 槽位
- ❌ 禁止表格列硬编码 `width`（用 `min-width`）
- ❌ 禁止分页用非受控模式
- ❌ 禁止组件 props 超过 10 个（超出拆分）
- ❌ 禁止 `el-tag` 超过 4 种状态用同一组件（拆为多标签）

---

## 3 强制行为清单

- ✅ 所有 props 按"必填 → 常用 → 事件"顺序书写
- ✅ 所有事件 `@xxx` 必须显式（即使 handler 是 `() => {}`）
- ✅ 所有列表渲染必须 `:key`
- ✅ 所有异步操作必须 `try/catch/finally`
- ✅ 所有外链跳转必须 `target="_blank" rel="noopener noreferrer"`
- ✅ 图标必须从 `@element-plus/icons-vue` 显式 import（不全局注册）
- ✅ 加载状态用 `<el-icon class="is-loading"><Loading /></el-icon>`
- ❌ 禁止用 string 名引用图标（如 `icon="plus"` 在 EP 2.13 已废弃）
- ❌ 禁止用 `type="text"`（已废弃，用 `text` 属性替代）

---

## 4 弹窗规则

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

## 5 表单规则

- ✅ 必填字段必须 `required` 红星
- ✅ `prop` 必须对应 `formData` 的字段
- ✅ 输入框必须 `clearable`（除密码等敏感字段）
- ✅ 长文本必须 `type="textarea" :rows="3"`
- ✅ 有限字符数必须 `maxlength` + `show-word-limit`
- ✅ 搜索框加 `:suffix-icon="Search"` + `@keyup.enter`
- ✅ 选择器长列表必须 `filterable`、必须 `clearable`
- ✅ 日期选择器必须显式 `format` 和 `value-format`

---

## 6 表格规则

- ✅ 长字段必须 `show-overflow-tooltip`
- ✅ 状态列用 `el-tag` + `:type` 映射（success/warning/info/danger）
- ✅ 操作列必须 `link` 类型 + `fixed="right"`
- ✅ 必写 `#empty` 槽位
- ✅ 列宽用 `min-width`（非 `width`）

---

## 7 描述列表规则

- ✅ `:column` 设为 `2` 或 `3`（避免单列过长）
- ✅ 状态字段用 `el-tag`
- ❌ `el-descriptions` 在 EP 2.13 中无 `size` 属性（如看到示例有 `size="default"` 是冗余的，删掉）

---

## 8 按钮规则

- ✅ 表格操作列**必须** `link` 类型
- ✅ 主操作 `type="primary"`
- ✅ 危险操作 `type="danger"`
- ✅ 异步操作 `:loading="xxx"`

**图标规则**：项目的"新增"和"批量删除"按钮是否携带图标，由项目配置定义。其他按钮一律不携带图标。

---

## 9 卡片规则

- ✅ 必须 `shadow="never"`（避免视觉噪音）
- ✅ header 必须用 `<template #header>`（非 prop）
- ✅ 卡片间距用 Tailwind 原子类（非硬编码 px 值）

---

## 10 树形规则

- ✅ 必须 `node-key`（用于高亮/选中）
- ✅ 必须 `:props` 映射字段名
- ✅ 如项目封装了树形组件，优先使用项目封装版本

---

## 11 高度自适应布局原则

任何包含底部操作按钮的页面，**必须**使用 flex 纵向三段式布局：

```
根容器（flex flex-col h-full）
├── 页头/工具栏（flex-shrink-0）
├── 内容区（flex-1 min-h-0 overflow-auto）
└── 底部操作栏（flex-shrink-0）
```

- ❌ 禁止底部操作按钮仅靠 `margin-top` 定位（内容少时按钮悬空）
- ❌ 禁止用 `h-full` 替代 `flex-1 min-h-0`（在 flex item 上会与内容撑高形成循环依赖）

> 具体的根容器组件和页头组件由项目配置定义。

---

## 12 页面级布局原则

- ✅ 所有顶层路由页面的根容器应统一（通过项目封装的外壳组件）
- ❌ 禁止各页面自行手写 `bg-white p-4 rounded-lg` 等等价外壳
- ✅ Tab/弹窗/抽屉内的子内容不使用页面外壳组件（避免卡片嵌套）
- ❌ 禁止子内容在外壳同向上再加 padding（避免双层间距）

> 具体的外壳组件名称和样式参数由项目配置定义。
