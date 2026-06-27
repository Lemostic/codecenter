| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `vue3/ui-element-plus.md`、`common/architecture.md` |

# 强制封装组件规范

> 本文件定义前端项目的"强制封装场景"——即禁止直接使用 Element Plus 底层组件、必须使用项目封装的统一组件的场景清单。
>
> **重要**：本文件**不锁定组件前缀**（如 `Dm-` / `Pro-` / `App-` / `Custom-`）。具体前缀由项目配置定义，本规范仅规定"哪些场景必须封装"。下文统一用 `{EncapsulatedXxx}` 占位符表示项目实际封装组件。

---

## 1 强制使用场景（10 类）

**PROF-FE-931** 以下 10 类场景 MUST 使用项目封装的统一组件，MUST NOT 直接使用 Element Plus 底层组件。 [MUST]

| # | 场景 | 首选组件 | 禁用（底层可用） | 触发条件 |
|---|------|---------|----------------|---------|
| 1 | 列表/表格 | `{EncapsulatedTable}` | `el-table` | 任何"数据列表"场景 |
| 2 | 分页器 | `{EncapsulatedPagination}` | `el-pagination` | 任何"分页"场景 |
| 3 | 操作确认 | `{EncapsulatedConfirm}` | `el-popconfirm` / `ElMessageBox` | 删除/批量操作/重要提交 |
| 4 | 全局消息 | `{EncapsulatedMessage}` | `ElMessage` / `ElNotification` | 成功/失败/警告提示 |
| 5 | 空状态 | `{EncapsulatedEmpty}` | `el-empty` | "无数据/无权限/加载失败"三态 |
| 6 | 详情分组 | `{EncapsulatedSectionTitle}` | `el-divider` + `el-text` | 详情页分组 |
| 7 | 主页面外壳 | `{EncapsulatedPageFrame}` | 手写 `bg-white p-4 rounded-lg` | List/Detail/Editor 顶层路由页面 |
| 8 | 树形控件 | `{EncapsulatedTree}` / `{EncapsulatedTreeLazy}` | `el-tree`（直接使用） | 左侧分类树、选择树 |
| 9 | 左右分栏布局 | `{EncapsulatedTreeLayout}` | 手写 flex 分栏 | 所有"左树右表"页面 |
| 10 | 卡片视图容器 | `{EncapsulatedCardList}` | 手写 `<div class="grid">` + 拼装 | 列表页"卡片视图"展示 |

> **占位符说明**：本规范统一用 `{EncapsulatedXxx}` 表示项目实际封装的组件名。实际项目中可能是 `DmTable` / `ProTable` / `AppTable` / `CustomTable` / `<项目前缀>Table` 等。具体命名由项目配置文件（如 `project_config/component_prefix.md`）定义。

---

## 2 组件选用决策树

### 2.1 数据列表展示

```
要展示一组数据？
├─ 标准列表（带分页/排序/筛选）→ {EncapsulatedTable}（内置分页）
├─ 极简（≤3 列，无分页）→ el-descriptions
└─ 仅展示单一对象属性 → {EncapsulatedSectionTitle} + el-descriptions
```

### 2.2 详情页布局

```
详情页展示对象属性？
├─ 有分组 → {EncapsulatedSectionTitle} + el-descriptions（column=2/3）
├─ 无分组 → el-descriptions（单组）
└─ 无数据 → {EncapsulatedEmpty} state="no-data" 或 "no-permission"
```

### 2.3 弹窗与表单

```
弹窗内嵌表单？
├─ 是 → 宽度：简单=500px / 中等=800px / 复杂=1000px+el-scrollbar
│       必填：append-to-body + :close-on-click-modal="false"
└─ 否 → 抽屉用 el-drawer / 新页面用 vue-router
```

### 2.4 确认操作

```
需要用户确认操作？
├─ 重要操作（删除/批量/发布）→ {EncapsulatedConfirm}（Promise 风格）
├─ 表格行内轻量确认 → el-popconfirm（仅在 {EncapsulatedConfirm} 不可用时）
└─ 普通二次确认 → {EncapsulatedConfirm} 或 {EncapsulatedMessage}.warning
```

### 2.5 全局消息

| 场景 | 组件 |
|------|------|
| 成功 | `{EncapsulatedMessage}.success` |
| 失败 | `{EncapsulatedMessage}.error` |
| 警告 | `{EncapsulatedMessage}.warning` |
| 信息 | `{EncapsulatedMessage}.info` |
| 重要通知 | `{EncapsulatedNotification}` |

### 2.6 空状态

| 场景 | 组件 |
|------|------|
| 正常无数据 | `{EncapsulatedEmpty} state="no-data"` |
| 无权限 | `{EncapsulatedEmpty} state="no-permission"` |
| 加载失败 | `{EncapsulatedEmpty} state="load-failed" @action="loadData"` |

### 2.7 树形展示

| 场景 | 组件 |
|------|------|
| 标准树（左侧菜单/分类） | `{EncapsulatedTree}` |
| 懒加载树（>100 节点） | `{EncapsulatedTreeLazy}` |
| 复杂树（版本对比/流程节点） | 自定义封装 |

### 2.8 卡片/列表双视图

```
列表页需要支持卡片/列表双视图？
├─ 是 → 卡片视图用 {EncapsulatedCardList} + #item 插槽
│       列表视图用 {EncapsulatedTable}
│       视图切换用文字按钮（当前模式 type="primary"）
└─ 否 → {EncapsulatedTable} + 单行工具栏
```

---

## 3 何时仍可用 el-*

以下场景不属于 10 类强制场景，可直接用 `el-*`：

- 弹窗（`el-dialog`）
- 表单（`el-form`、`el-input`、`el-select` 等）
- 标签页（`el-tabs`，除非走 `{EncapsulatedTabs}` 封装）
- 评分/颜色选择（`el-rate` / `el-color-picker`）
- 折叠面板（`el-collapse`）

---

## 4 强制封装组件规则模板

以下规则针对 10 类强制场景中的每个组件，由项目实施时填充具体实现。规范**只规定规则模板**，具体 API 由项目封装层定义。

### 4.1 {EncapsulatedTable}（封装的 el-table）

| 规则 | 说明 |
|------|------|
| MUST | 节点带 `class="flex-1 min-h-0"`（与 DmTable 一致） |
| MUST | 通过 `:data` 传入数组 |
| MUST | 必写 `#empty` 槽位（用 `{EncapsulatedEmpty}`） |
| MUST | 列表渲染必须 `:key` |
| MUST | 长字段必须 `show-overflow-tooltip` |
| MUST | 操作列必须 `link` 类型 + `fixed="right"` |
| MUST | 状态列用 `el-tag` + `:type` 映射 |
| MUST | 分页内置时使用 `{EncapsulatedPagination}`，禁止内部用 `el-pagination` |

### 4.2 {EncapsulatedPagination}（封装的 el-pagination）

| 规则 | 说明 |
|------|------|
| MUST | 所有分页控件统一圆角（具体值由项目设计 Token 决定） |
| MUST | 上一页/下一页背景色（具体值由项目设计 Token 决定） |
| MUST | 激活页码描边 + 文字色（具体值由项目设计 Token 决定） |
| MUST | 分页容器白色背景 + 顶部 1px 边框 + 48px 高度 |
| MUST NOT | 直接使用 `el-pagination`（必须通过 {EncapsulatedTable} 或 {EncapsulatedPagination} 间接使用） |

### 4.3 {EncapsulatedConfirm}（封装的操作确认）

| 规则 | 说明 |
|------|------|
| MUST | 重要操作（删除/批量/发布） MUST 使用 |
| MUST NOT | 表格行内轻量确认用 `el-popconfirm`（除非 `{EncapsulatedConfirm}` 不可用） |
| MUST NOT | 直接使用 `ElMessageBox` |

### 4.4 {EncapsulatedMessage}（封装的全局消息）

| 规则 | 说明 |
|------|------|
| MUST | 成功/失败/警告提示 MUST 使用 |
| MUST NOT | 直接使用 `ElMessage` / `ElNotification` |
| MUST | 异步操作失败时 MUST 调用 `{EncapsulatedMessage}.error` |

### 4.5 {EncapsulatedEmpty}（封装的空状态）

| 规则 | 说明 |
|------|------|
| MUST | "无数据/无权限/加载失败"三态 MUST 使用 |
| MUST | 必传 `state` 区分场景 |
| MAY | 加载失败场景可传 `@action="loadData"` 提供"重试"按钮 |
| MUST NOT | 直接使用 `el-empty` |

### 4.6 {EncapsulatedSectionTitle}（封装的详情分组）

| 规则 | 说明 |
|------|------|
| MUST | 详情页分组 MUST 使用 |
| MUST NOT | 详情分组用 `el-divider` + `el-text` 手写 |

### 4.7 {EncapsulatedPageFrame}（封装的页面外壳）

| 规则 | 说明 |
|------|------|
| MUST | 所有 List/Detail/Editor 顶层路由页面的根容器 MUST 使用 |
| MUST | 显式 import（具体路径由项目封装层定义） |
| MUST | 内部嵌套 `{EncapsulatedTreeLayout}` 时必须给后者加 `class="flex-1 min-h-0"` |
| MUST NOT | 嵌入在 `el-tab-pane` / `el-dialog` / `el-drawer` 内的子内容使用 |
| MUST NOT | 手写 `bg-white p-4 rounded-lg` / `bg-[#f5f7fa] p-4` 等等价外壳 |
| MUST NOT | 子内容在外壳同向上再加 padding（避免双层间距） |

### 4.8 {EncapsulatedTree} / {EncapsulatedTreeLazy}（封装的树形）

| 规则 | 说明 |
|------|------|
| MUST | 左侧分类树 / 选择树 MUST 使用项目封装版本 |
| MUST NOT | 业务组件中直接 import `el-tree` 绕过封装 |
| MUST | `{EncapsulatedTreeLazy}` 节点 > 100 时使用 |
| MUST | `fieldMap` prop 统一映射（具体字段名由项目封装层定义） |
| MUST | 选中态通过 `highlight-current` 实现，选中背景色使用 CSS 变量 |
| MUST | 暴露方法 `setSearch(keyword)` / `clearSearch()` |

### 4.9 {EncapsulatedTreeLayout}（封装的左右分栏）

| 规则 | 说明 |
|------|------|
| MUST | 所有"左树右表" / "左分类右列表"页面 MUST 使用 |
| MUST | 必须配合 `{EncapsulatedPageFrame}` 使用：`{EncapsulatedPageFrame}` 在外层，`{EncapsulatedTreeLayout}` 在内层 |
| MUST | 必须给 `{EncapsulatedTreeLayout}` 加 `class="flex-1 min-h-0"` |
| MUST | 表格工具栏 MUST 放在 `#right` 插槽内（紧贴 `{EncapsulatedTable}` 上方） |
| MUST NOT | 在 `{EncapsulatedTreeLayout}` 外侧放置表格工具栏 |
| MUST NOT | 子内容在 `{EncapsulatedTreeLayout}` 同向上再加 padding |

### 4.10 {EncapsulatedCardList}（封装的卡片列表）

| 规则 | 说明 |
|------|------|
| MUST | 卡片视图 MUST 使用作为网格容器 |
| MUST | 显式 import |
| MUST | 节点带 `class="flex-1 min-h-0"` |
| MUST | 数据驱动：通过 `:data` 传入数组 |
| MUST | 作用域插槽 `#item`（不用 default slot） |
| MUST | 分页：`v-model:currentPage` / `v-model:pageSize` + `:total` + `@page-change` / `@size-change` |
| MUST | 空态自动推算，可选 `:empty-text` |
| MUST NOT | 内部用 `el-pagination` 替换 `{EncapsulatedPagination}` |

---

## 5 视图切换规则（卡片/列表双视图）

| 规则 | 说明 |
|------|------|
| MUST | 文字按钮切换（不带图标），当前选中 `type="primary"` |
| MUST | `size="small"`，放工具栏右侧 |
| MUST | 状态：`ref<'card' \| 'list'>('card')`，默认卡片视图 |
| MUST NOT | 图标按钮切换视图 |

```vue
<template>
  <!-- 视图切换 -->
  <div class="flex items-center gap-2">
    <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''">卡片视图</el-button>
    <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''">列表视图</el-button>
  </div>
</template>
```

---

## 6 PageHeader 组件规则

**PROF-FE-932** 编辑页和详情页的页头 MUST 使用 `<PageHeader>` 组件。 [MUST]

| 规则 | 说明 |
|------|------|
| MUST | `title` prop 必填 |
| MUST | `backTo` prop 指定返回路由（通常为列表页路由 name），不传则 `router.back()` |
| MUST | 右侧操作按钮用 `#actions` 插槽 |
| MUST NOT | 手写页头标题 div |
| MUST NOT | 列表页使用 PageHeader（列表页用全局布局的标题） |

```vue
<template>
  <PageHeader title="编辑模型" :back-to="{ name: 'model-design-list' }">
    <template #actions>
      <el-button>取消</el-button>
      <el-button type="primary" :loading="saving">保存</el-button>
    </template>
  </PageHeader>
</template>
```

---

## 7 statusTagType 标准映射

```typescript
const statusTagType = (status: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
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

**PROF-FE-933** statusTagType 映射规则 MUST 在项目内统一定义。 [MUST]

---

## 8 项目配置说明

| 项 | 说明 |
|----|------|
| 组件前缀 | 由 `project_config/component_prefix.md` 定义（推荐 `Dm-` / `Pro-` / `App-` / `Custom-`） |
| 组件目录 | 由 `project_config/component_directory.md` 定义（推荐 `common/components/{category}/`） |
| 组件 API | 由项目封装层 `core/components/` 或 `common/components/` 实际实现 |

---

*本文件规则不锁定组件前缀——它规定"哪些场景必须封装"，"用什么前缀"由项目配置决定。*
