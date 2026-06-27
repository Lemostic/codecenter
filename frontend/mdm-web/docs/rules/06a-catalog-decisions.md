---
description: Dm-* 组件目录与选用决策树 — 10类强制使用场景、组件选用决策树、何时仍可用el-*、TpTree规则、按钮图标规则、表格工具栏规则、两行式工具栏、视图切换。
paths:
  - "**/*.vue"
---

## 1 强制使用场景（10 类）

以下场景**必须**使用项目封装组件，**禁止**直接使用 Element Plus 底层组件。

| 场景 | 首选组件 | 禁用（底层可用） | 触发条件 |
|------|---------|----------------|---------|
| 列表/表格 | `TpTable` | `el-table` | 任何"数据列表"场景 |
| 分页器 | `TpPagination` | `el-pagination` | 任何"分页"场景 |
| 操作确认 | `TpConfirm` | `el-popconfirm` / `ElMessageBox` | 删除/批量操作/重要提交 |
| 全局消息 | `TpMessage` | `ElMessage` / `ElNotification` | 成功/失败/警告提示 |
| 空状态 | `TpEmpty` | `el-empty` | "无数据/无权限/加载失败"三态 |
| 详情分组 | `TpSectionTitle` | `el-divider` + `el-text` | 详情页分组 |
| 主页面外壳 | `TpPageFrame` | 手写 `bg-white p-4 rounded-lg` | List/Detail/Editor 顶层路由页面 |
| 树形控件 | `TpTree` / `TpTreeLazy` | `el-tree`（直接使用） | 左侧分类树、选择树 |
| 左右分栏布局 | `TpLeftTreeLayout` | 手写 flex 分栏 | 所有"左树右表"页面 |
| 卡片视图容器 | `TpCardList` | 手写 `<div class="grid">` + 拼装 | 列表页"卡片视图"展示 |

---

## 2 组件选用决策树

### 数据列表展示
```
要展示一组数据？
├─ 标准列表（带分页/排序/筛选）→ TpTable（内置分页）
├─ 极简（≤3 列，无分页）→ el-descriptions
└─ 仅展示单一对象属性 → TpSectionTitle + el-descriptions
```

### 详情页布局
```
详情页展示对象属性？
├─ 有分组 → TpSectionTitle + el-descriptions（column=2/3）
├─ 无分组 → el-descriptions（单组）
└─ 无数据 → TpEmpty state="no-data" 或 "no-permission"
```

### 弹窗与表单
```
弹窗内嵌表单？
├─ 是 → 宽度：简单=500px / 中等=800px / 复杂=1000px+el-scrollbar
│       必填：append-to-body + :close-on-click-modal="false"
└─ 否 → 抽屉用 el-drawer / 新页面用 vue-router
```

### 确认操作
```
需要用户确认操作？
├─ 重要操作（删除/批量/发布）→ TpConfirm（Promise 风格）
├─ 表格行内轻量确认 → el-popconfirm（仅在 TpConfirm 不可用时）
└─ 普通二次确认 → TpConfirm 或 TpMessage.warning
```

### 全局消息

| 场景 | 组件 |
|------|------|
| 成功 | `TpMessage.success` |
| 失败 | `TpMessage.error` |
| 警告 | `TpMessage.warning` |
| 信息 | `TpMessage.info` |
| 重要通知 | `TpNotification` |

### 空状态

| 场景 | 组件 |
|------|------|
| 正常无数据 | `TpEmpty state="no-data"` |
| 无权限 | `TpEmpty state="no-permission"` |
| 加载失败 | `TpEmpty state="load-failed" @action="loadData"` |

### 树形展示

| 场景 | 组件 |
|------|------|
| 标准树（左侧菜单/分类） | `TpTree` |
| 懒加载树（>100 节点） | `TpTreeLazy` |
| 复杂树（版本对比/流程节点） | 自定义封装 |

### 卡片/列表双视图
```
列表页需要支持卡片/列表双视图？
├─ 是 → 卡片视图用 TpCardList + #item 插槽
│       列表视图用 TpTable
│       视图切换用文字按钮（当前模式 type="primary"）
└─ 否 → TpTable + 单行工具栏
```

---

## 3 何时仍可用 el-*

以下场景不属于 10 类强制场景，可直接用 el-*：

- 弹窗（`el-dialog`）
- 表单（`el-form`、`el-input`、`el-select` 等）
- 标签页（`el-tabs`，除非走 `TpTabs` 封装）
- 评分/颜色选择（`el-rate` / `el-color-picker`）
- 折叠面板（`el-collapse`）

---

## 4 TpTree / TpTreeLazy 规则

- ✅ `fieldMap` prop：`{ label: 'name', children: 'children', isLeaf: 'isLeaf' }`
- ✅ `nodeKey` 默认 `'id'`，`searchable` 默认 `true`，`highlightCurrent` 默认 `true`
- ✅ 搜索框：`el-input` + `prefix-icon`（Search），容器 `py-2`，底部 `border-b`
- ✅ 选中态：`highlight-current`，选中背景色 `rgba(64, 158, 255, 0.1)`
- ✅ 暴露方法：`setSearch(keyword)` / `clearSearch()`
- ❌ 禁止 > 100 节点用非懒加载树
- ❌ 禁止在业务组件中直接 import `el-tree` 绕过封装

---

## 5 按钮图标规则（本项目特有）

| 按钮 | 是否带图标 | 图标 |
|------|----------|------|
| 新增 | ✅ | `:icon="Plus"` |
| 批量删除 | ✅ | `:icon="Delete"` |
| 编辑 / 单条删除 / 查询 / 重置 / 确定 / 取消 / 导出 / 导入 | ❌ | — |

---

## 6 表格工具栏规则（本项目特有）

- **表格工具栏分左右**：`flex justify-between`，不放标题
  - 左侧 = 操作按钮（新增 / 删除 / 导入 / 导出 / 批量删除）
  - 右侧 = 搜索区（输入字段 + 查询 / 重置），不使用 `el-form`
  - 无操作且无搜索时不写工具栏
- **列表页不写页面标题**（面包屑由全局布局提供）
- **工具栏紧贴 TpTable 上方**（属于表格，不属于页面）
- **左树右表中**，工具栏在 `#right` 插槽内，与树搜索框水平对齐：
  - 右侧面板根容器 `pt-2`
  - 工具栏 `pb-2 border-b border-[#e4e7ed]`
  - TpTable 加 `pt-4`
  - **禁止**工具栏用 `mb-4`

---

## 7 两行式工具栏（卡片/列表双视图列表页）

- **第 1 行 - 操作按钮行**：`flex items-center gap-2 mb-3 flex-shrink-0`
  - 仅"新增"用 `type="primary"` + `:icon="Plus"`
  - 仅"批量删除"可带 `:icon="Delete"`
  - 其余 `size="default"` 无图标
  - 可用 `el-divider direction="vertical"` 分隔
- **第 2 行 - 筛选搜索行**：`flex items-center justify-between mb-3 flex-shrink-0`
  - 左侧：筛选下拉（120px）+ 关键字输入（220px）+ 搜索/重置
  - 右侧：视图切换按钮 + 可选附加控件
- ❌ 禁止两行合并为一行
- ❌ 第 2 行不用 `el-form`

---

## 8 视图切换规则

- ✅ 文字按钮切换（不带图标），当前选中 `type="primary"`
- ✅ `size="small"`，放第 2 行工具栏右侧
- ✅ 状态：`ref<'card' | 'list'>('card')`，默认卡片视图
- ✅ 可用 `el-divider direction="vertical"` 与筛选区分隔
- ❌ 禁止图标按钮切换视图
