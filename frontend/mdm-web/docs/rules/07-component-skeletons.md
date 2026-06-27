---
description: 组件骨架代码参考 — 列表页/左树右表/flex三段式/TpPageFrame/TpLeftTreeLayout/卡片视图/两行式工具栏/p-3用法/按钮图标/ModelCard规格 等完整模板。
paths:
  - "**/*.vue"
---

## 1. 列表页完整骨架

来源: §11.3

列表页无标题，工具栏即页面顶部，左操作右搜索。

```vue
<div class="page-list p-4 flex flex-col h-full bg-white rounded-lg">
  <!-- 表格工具栏：左 = 操作，右 = 搜索 -->
  <div class="mb-4 flex justify-between items-center flex-shrink-0">
    <div class="flex items-center gap-2">
      <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
      <el-button @click="handleBatchDelete">批量删除</el-button>
      <el-button @click="handleImport">导入</el-button>
      <el-button @click="handleExport">导出</el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-input v-model="query.name" placeholder="请输入模型名称" clearable />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
  </div>

  <TpTable class="flex-1 min-h-0" ... />
</div>
```

---

## 2. 列表页最小骨架

来源: §11.3

与 §4.2 决策树呼应的最小可用列表页。

```vue
<template>
  <div class="page-list p-4 flex flex-col h-full bg-white rounded-lg">
    <!-- 表格工具栏：左 = 操作，右 = 搜索 -->
    <div class="mb-4 flex justify-between items-center flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-button type="primary" @click="handleCreate">新增</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input v-model="query.name" placeholder="请输入名称" clearable />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>
    <!-- 表格 + 分页（关键：flex-1 min-h-0 透传） -->
    <TpTable
      class="flex-1 min-h-0"
      :columns="columns" :data="tableData" :loading="loading" :total="total"
      v-model:pageNum="query.pageNum" v-model:pageSize="query.pageSize"
      @page-change="loadData"
    />
  </div>
</template>
```

---

## 3. 表格禁忌写法

来源: §11.3

以下为列表页中常见的错误写法。

```vue
<!-- ❌ 列表页手写标题（一级页面不写标题，需要标题的页面用 TpPageHeader） -->
<h1>模型列表</h1>
<div class="page-list ..."> ... </div>

<!-- ❌ 表格工具栏只放标题或单按钮，搜索区另起一行——视觉割裂、浪费高度 -->
<div class="mb-4">...</div>
<div class="mb-4 flex items-center gap-2">...</div>

<!-- ❌ 空表格工具栏（既无操作也无搜索）—— 直接不写 -->
<div class="mb-4 flex justify-between items-center"></div>
```

---

## 4. 左树右表骨架

来源: §11.3

表格工具栏在右侧面板内，紧贴 TpTable 上方，与树搜索框水平对齐。

```vue
<div class="page-list p-4 flex flex-col h-full bg-white rounded-lg">
  <!-- 左树右表（列表页无标题） -->
  <TpLeftTreeLayout class="flex-1 min-h-0" :default-width="280">
    <template #left>
      <TpTreeLazy ... />
    </template>

    <template #right>
      <div class="h-full flex flex-col pt-2">
        <!-- 表格工具栏：紧贴 TpTable 上方，与树搜索框对齐（pt-2 / pb-2 border-b 匹配 TpTreeLazy 的 py-2 + border-b） -->
        <div class="pb-2 flex justify-between items-center flex-shrink-0 border-b border-[#e4e7ed]">
          <div class="flex items-center gap-2">
            <el-button type="primary" :icon="Plus" @click="handleCreate">新增分类</el-button>
          </div>
          <div class="flex items-center gap-2">
            <el-input v-model="query.name" placeholder="请输入" clearable />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>

        <!-- 表格（pt-4 留出工具栏与表格间距） -->
        <TpTable class="flex-1 min-h-0 pt-4" ... />
      </div>
    </template>
  </TpLeftTreeLayout>
</div>
```

---

## 5. 左树右表禁忌写法

来源: §11.3

表格工具栏不应放在 TpLeftTreeLayout 外侧（页面级别）。

```vue
<!-- ❌ 表格工具栏放在 TpLeftTreeLayout 外侧（页面级别）—— 工具栏属于表格，应紧贴 TpTable -->
<div class="page-list p-4 flex flex-col h-full">
  <div class="mb-4 flex justify-between">  <!-- 表格工具栏不应在这里 -->
    <el-button>新增</el-button>
    <el-form>...</el-form>
  </div>
  <TpLeftTreeLayout>
    <template #right>
      <TpTable ... />
    </template>
  </TpLeftTreeLayout>
</div>
```

---

## 6. flex 三段式布局示例

来源: §11.13

编辑页/详情页带底部操作栏的 flex 纵向三段式布局，使用 TpPageFrame + TpPageHeader。

```vue
<!-- ✅ 正确：flex 三段式布局 + TpPageFrame + TpPageHeader -->
<template>
  <TpPageFrame>
    <!-- 页头（返回 + 标题） -->
    <TpPageHeader title="编辑模型" :back-to="{ name: 'model-design-list' }" />

    <!-- 内容区（flex-1 自适应撑开，内容多时可滚动） -->
    <div class="flex-1 min-h-0 overflow-auto">
      <!-- 表单 / 详情 / 卡片等 -->
    </div>

    <!-- 底部操作栏（flex-shrink-0 始终贴底） -->
    <div class="flex justify-end gap-2 flex-shrink-0 pt-4 mt-4 border-t border-[#e4e7ed]">
      <el-button size="default">取消</el-button>
      <el-button size="default" type="primary">确定</el-button>
    </div>
  </TpPageFrame>
</template>
```

---

## 7. TpPageFrame 骨架示例

来源: §11.14

### 7a. 纯表格列表页

```vue
<!-- ✅ 正确：纯表格的列表页 -->
<template>
  <TpPageFrame>
    <!-- 工具栏 -->
    <div class="flex justify-between items-center pb-2 flex-shrink-0">...</div>
    <!-- 表格 -->
    <TpTable class="flex-1 min-h-0" ... />
  </TpPageFrame>
</template>
```

### 7b. 左树右表

```vue
<!-- ✅ 正确：左树右表的列表页 -->
<template>
  <TpPageFrame>
    <TpLeftTreeLayout class="flex-1 min-h-0">
      <template #left>...</template>
      <template #right>
        <div class="h-full flex flex-col">
          <!-- 工具栏 + TpTable -->
        </div>
      </template>
    </TpLeftTreeLayout>
  </TpPageFrame>
</template>
```

---

## 8. TpLeftTreeLayout 骨架示例

来源: §11.15

配合 TpPageFrame 使用，TpLeftTreeLayout 在内层并加 `class="flex-1 min-h-0"`。

```vue
<TpPageFrame>
  <TpLeftTreeLayout class="flex-1 min-h-0" :default-width="280" storage-key="model-tree-width">
    <template #left>
      <TpTreeLazy :load="loadTreeNode" search-placeholder="搜索分类" @node-click="handleNodeClick" />
    </template>
    <template #right>
      <div class="h-full flex flex-col pt-2">
        <!-- 工具栏 + TpTable -->
      </div>
    </template>
  </TpLeftTreeLayout>
</TpPageFrame>
```

---

## 9. 卡片视图完整示例

来源: §11.16.x

使用 TpCardList 作为网格容器，通过作用域插槽 `#item` 渲染 ModelCard。

```vue
<!-- 调用方 -->
<TpCardList
  class="flex-1 min-h-0"
  :data="models"
  v-model:pageNum="currentPage"
  v-model:pageSize="pageSize"
  :total="total"
  :loading="loading"
  empty-text="t('modelDesign.list.empty')"
  @page-change="loadModels"
>
  <template #item="{ item }">
    <ModelCard
      :model="item"
      :selected="selectedIds.has(item.id)"
      @click="handleCardClick"
      @select="handleCardSelect"
    />
  </template>
</TpCardList>
```

---

## 10. 两行式工具栏规则说明和示例

来源: §11.17

卡片视图列表页使用两行工具栏（区别于纯表格列表页的单行工具栏）。

- **第 1 行 - 操作按钮行**：`flex items-center gap-2 mb-3 flex-shrink-0`
  - 仅"新增"按钮使用 `type="primary"` + `:icon="Plus"`
  - 仅"删除"（批量）按钮可带 `:icon="Delete"`
  - 其余按钮为 `size="default"` 无图标
  - 可用 `el-divider direction="vertical"` 分隔不同功能组的按钮

- **第 2 行 - 筛选搜索行**：`flex items-center justify-between mb-3 flex-shrink-0`
  - 左侧：筛选下拉（`el-select` 120px，`clearable`）+ 关键字输入（`el-input` 220px，`clearable`）+ 搜索/重置按钮
  - 右侧：视图切换按钮 + 可选附加控件（如"级联子分类"复选框）

- 操作按钮禁用逻辑：新增/复制/导入在未选中末级树节点时 `disabled`；移动/删除在未选中模型时 `disabled`

---

## 11. 视图切换代码示例

来源: §11.18

卡片/列表视图通过文字按钮切换，不带图标，当前选中模式使用 `type="primary"`。

```vue
<!-- 视图切换 -->
<div class="flex items-center gap-2">
  <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''">卡片视图</el-button>
  <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''">列表视图</el-button>
</div>
```

---

## 12. TpPageHeader 示例

来源: §11.19

编辑页和详情页的页头必须使用 TpPageHeader 组件，自带返回箭头和标题。

```vue
<!-- 编辑页页头 -->
<TpPageHeader title="编辑模型" :back-to="{ name: 'model-design-list' }">
  <template #actions>
    <el-button>取消</el-button>
    <el-button type="primary" :loading="saving">保存</el-button>
  </template>
</TpPageHeader>
```

---

## 13. TpPagination 用法示例

来源: §11.20

TpTable 已内置 TpPagination，通常无需单独引用。独立使用场景如下。

```vue
<!-- ✅ 正确：TpTable 内置分页（最常见） -->
<TpTable
  :data="items"
  :total="total"
  v-model:pageNum="currentPage"
  v-model:pageSize="pageSize"
/>

<!-- ✅ 正确：独立使用 TpPagination -->
<TpPagination
  v-model:pageNum="page"
  v-model:pageSize="size"
  :total="total"
  @page-change="loadData"
/>

<!-- ❌ 错误：直接使用 el-pagination -->
<el-pagination v-model:current-page="page" :total="total" />
```

---

## 14. p-3 典型用法速查

来源: §11.21

### 14a. 工具栏

仅一行时不加 border-b。

```vue
<!-- 工具栏：左 = 操作按钮，右 = 筛选/搜索（仅一行时不加 border-b） -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <div class="flex items-center gap-2">
    <el-button>新增</el-button>
    <el-button>编辑</el-button>
  </div>
  <div class="flex items-center gap-2">
    <el-input placeholder="搜索" />
  </div>
</div>
```

### 14b. 内容区 wrapper

包住 TpTable / TpCardList，顶部间距由上方工具栏提供，底部由分页栏提供，wrapper 仅需左右间距。

```vue
<!-- 内容区 wrapper：包住 TpTable / TpCardList -->
<div class="flex-1 min-h-0 flex flex-col px-3">
  <TpTable ... />
</div>
```

### 14c. 详情页分组

```vue
<!-- 详情页分组：标题 + 内容 -->
<div class="flex flex-col gap-3 p-3">
  <TpSectionTitle title="基本信息" />
  <el-form ... />
</div>
```

### 14d. 树搜索框

```vue
<!-- 树搜索框（dm-tree-search） -->
<div class="dm-tree-search p-3 flex-shrink-0">
  <el-input ... />
</div>
```

---

## 15. p-3 旧代码迁移对照表

来源: §11.21

| 旧 class | 新 class | 出现位置 |
|----------|----------|----------|
| `px-4 py-3` | `p-3` | 工具栏（ModelList / CategoryList / DataItemList） |
| `pr-3 pl-3` | `p-3` | 内容区 wrapper（ModelList / CategoryList / DataItemList） |
| `h-[60px] px-4` | `p-3`（去掉 `h-[60px]`） | 工具栏首行（ModelList 第 1 行） |
| `pt-2` / `pt-4`（仅顶部留白时） | `p-3`（统一为四方向） | 工具栏与 TpTable 之间的局部间距 |

> 迁移原则：迁移后所有列表页工具栏 / 内容区 / 树搜索框的 class 完全一致（`p-3 flex-shrink-0 border-b border-[#e9e9e9]` 等），便于后续维护与样式统一。

---

## 16. 工具栏 border 规则示例

来源: §11.21

仅一行工具栏不加 `border-b`；多行工具栏叠放时行与行之间加 `border-b`，但最下面一行（紧邻内容区）不加。

```vue
<!-- ✅ 仅一行工具栏：不加 border-b -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <div class="flex items-center gap-2">
    <el-button>新增</el-button>
    <el-button>编辑</el-button>
  </div>
  <div class="flex items-center gap-2">
    <el-input placeholder="搜索" />
  </div>
</div>

<!-- ✅ 多行工具栏：行间用 border-b 分隔，最后一行不加 -->
<!-- 第1行：筛选/排序 -->
<div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
  <!-- 排序/筛选控件 -->
</div>
<!-- 第2行：操作按钮（紧邻内容区，不加 border-b） -->
<div class="flex items-center justify-between p-3 flex-shrink-0">
  <!-- 操作按钮 -->
</div>
```

---

## 17. 按钮图标反模式

来源: §11.5

只有「新增」按钮和「批量删除」按钮携带图标，其他按钮一律不携带图标。

```vue
<!-- ❌ 禁止：非"新增/批量删除"按钮带图标 -->
<el-button :icon="Search" @click="handleSearch">查询</el-button>     <!-- ❌ 禁止 -->
<el-button :icon="Refresh" @click="handleReset">重置</el-button>    <!-- ❌ 禁止 -->
<el-button :icon="Edit" link type="primary">编辑</el-button>         <!-- ❌ 禁止 -->
<el-button :icon="Delete" link type="danger">删除</el-button>       <!-- ❌ 禁止（单条删除不是批量删除） -->
<el-button :icon="Download" @click="handleExport">导出</el-button>   <!-- ❌ 禁止 -->

<!-- ✅ 正确：只有"新增"和"批量删除"带图标 -->
<el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
<el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
```

---

## 18. ModelCard 设计规格参考

来源: §11.16

以下为 ModelCard 组件的 Figma 设计规格数值，实现时必须严格遵循。

### 卡片容器

| 属性 | 值 |
|------|-----|
| border-radius | `4px`（非 rounded-lg） |
| border | `1px solid #dcdfe6` |
| background | `#ffffff` |
| cursor | `pointer` |
| 默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| 选中态 | `ring-2 ring-[var(--el-color-primary)]` |
| hover border-color | `var(--el-color-primary-light-3)` |
| 过渡效果 | `transition-all duration-200 ease-in-out` |

### 卡片头部

| 属性 | 值 |
|------|-----|
| 图标框尺寸 | 36x36px（非 40x40） |
| 图标框 border-radius | `4px` |
| 图标框 background | `#337bff`（蓝色，非 emerald-500） |
| 图标尺寸 | 20px，白色 |
| 复选框位置 | `absolute top-4 right-4 z-10`（右上角，非左上角） |
| 标题 font-size | 14px |
| 标题 font-weight | 500（medium，非 semibold） |
| 标题 color | `#3d4247`（非 #303133） |
| 编码 font-size | 12px |
| 编码 color | `#666666` |
| 图标与文字间距 | `gap: 15px` |
| 头部区域 padding | `px-4 pt-4 pb-2` |

### 信息行

| 属性 | 值 |
|------|-----|
| font-size | 13px（非 12px） |
| color | `#666666` |
| line-height | 30px |
| 布局 | 标签与值拼接为整行文本 |

### 底部标签区

| 属性 | 值 |
|------|-----|
| 布局 | `justify-between` |
| 内容 | 密级标签（可选）+ 版本/状态组合标签 + 授权按钮 |
| 版本左半 border-radius | `3px 0 0 3px`，描边，`color: #333` |
| 状态右半 border-radius | `0 3px 3px 0`，`background: rgba(227,77,89,0.1)`，`color: #e34d59` |

### TpPagination 设计规格

来源: §11.20

| 属性 | 值 |
|------|-----|
| 容器高度 | 48px |
| 容器 padding | `4px 16px` |
| 容器 border-top | `1px solid #E1E9F0` |
| 统计区 font-size | 12px |
| 统计区 color | `#585F66` |
| 统计区 line-height | 24px |
| 翻页按钮尺寸 | 28x28px |
| 翻页按钮 border-radius | `2px`（非 Element Plus 默认 4px） |
| 上一页/下一页背景 | `#F5F7FA`，无边框，箭头 `#585F66` |
| 激活页码 | `1px solid #337BFF` 描边，`#337BFF` 文字，白色背景 |
| 普通页码 | `1px solid #E9E9E9` 描边，`#585F66` 文字 |
| 跳页输入框 | 56x28px，`border-radius: 2px`，`1px solid #E1E9F0` |
| 每页条数选择器 | 100x28px，`border-radius: 2px`，`1px solid #E1E9F0`，12px 文字 |

---

## 19. statusTagType 标准映射

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

## 20. 弹窗 el-dialog 详细规则

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

## 21. 表单 el-form 详细规则

来源: §11.2

- ✅ 必填字段必须 `required` 红星
- ✅ `prop` 必须对应 `formData` 的字段
- ✅ 输入框必须 `clearable`（除密码等敏感字段）
- ✅ 长文本必须 `type="textarea" :rows="3"`

---

## 22. 描述列表 el-descriptions 规则

来源: §11.4

- ✅ 详情页分组用 `TpSectionTitle`（非 `title` 属性）
- ✅ `:column` 设为 `2` 或 `3`（避免单列过长）
- ✅ 状态字段同样用 `el-tag`
- ❌ el-descriptions 在 Element Plus 2.13 中无 `size` 属性（如看到示例有 `size="default"` 是冗余的，删掉）

---

## 23. 按钮与图标详细规则

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

## 24. TpTable 详细布局规则

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

## 25. 选择器 / 输入框 / 日期选择器规则

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

## 26. 树形控件详细规则

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

## 27. 标签页 / 卡片 规则

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

---

## 28. 页面高度自适应布局详细规则

来源: §11.13

**适用范围**：编辑页、表单页、详情页（有底部操作栏时）等。列表页的左树右表场景见 §8。

- ✅ 任何包含底部操作按钮的页面，**必须**使用 flex 纵向三段式布局
- ✅ 根容器：`<TpPageFrame>`（提供 `bg-white p-4 rounded-lg flex flex-col h-full overflow-hidden`）
- ✅ 页头/工具栏：`flex-shrink-0`（固定不压缩）
- ✅ 内容区：`flex-1 min-h-0 overflow-auto`（自适应撑开，内容多时可滚动）
- ✅ 底部操作栏：`flex-shrink-0`（始终贴底）
- ✅ **编辑页和详情页的页头必须使用 `<TpPageHeader>` 组件**（`@mdm/common/components/structure/TpPageHeader.vue`），自带返回箭头和标题。`backTo` prop 指定返回路由（通常为列表页路由 name），不传则 `router.back()`。右侧操作按钮用 `#actions` 插槽。**禁止**手写页头标题 div
- ❌ 禁止根容器手写 `bg-white p-4 flex flex-col h-full` 等价外壳（用 `TpPageFrame` 替代）
- ❌ 禁止底部操作按钮仅靠 `margin-top` 定位（内容少时按钮悬空）

---

## 29. TpPageFrame 规则

来源: §11.14

- ✅ 所有 List / Detail / Editor **顶层路由页面**的根容器必须使用 `TpPageFrame`（位于 `@mdm/common/components/layout/`）
- ✅ 组件**零 props**，约束所有页面的外层 padding（16px）、背景（白）、圆角（rounded-lg）、flex 撑高（h-full）、overflow-hidden 完全一致
- ✅ 必须**显式 import**：`import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';`（`unplugin-vue-components` 仅覆盖 Element Plus，**不会**自动注册 `@mdm/common/components/` 下的 Dm-*）
- ✅ 内部嵌套 `TpLeftTreeLayout` 时，必须给它加 `class="flex-1 min-h-0"`（否则会被压扁）
- ❌ 嵌入在 `el-tab-pane` / `el-dialog` / `el-drawer` 内的子内容**禁止**使用（会形成卡片嵌套）
- ❌ 禁止手写 `bg-white p-4 rounded-lg` / `bg-[#f5f7fa] p-4` 等等价外壳
- ❌ 子内容**禁止**在外壳同向上再加 padding（如根直接 `p-4`、顶部 `pt-2` 等），避免双层间距

**适用范围**：所有顶层路由页面（包括纯表格列表、左树右表、纯详情、编辑器等）。Tab/弹窗/抽屉内的子内容、占位页除外。

---

## 30. TpLeftTreeLayout 规则

来源: §11.15

- ✅ 所有"左树右表"/"左分类右列表"页面**必须**使用 `TpLeftTreeLayout`（位于 `@mdm/common/components/layout/`），禁止手写 flex 分栏或固定宽度侧边栏
- ✅ 组件使用两个命名插槽：`#left`（树/分类面板）和 `#right`（列表/卡片内容）
- ✅ 必须显式 import：`import TpLeftTreeLayout from '@mdm/common/components/layout/TpLeftTreeLayout.vue';`
- ✅ 必须配合 `TpPageFrame` 使用：`TpPageFrame` 在外层，`TpLeftTreeLayout` 在内层并加 `class="flex-1 min-h-0"`
- ✅ `defaultWidth` 默认 `300`，推荐 `280`；`minWidth` 默认 `200`；`maxExtraWidth` 默认 `200`
- ✅ 折叠/展开通过 `v-model:collapsed` 控制，折叠按钮绝对定位在分隔条上（16x48px，白色，4px 圆角）
- ✅ 拖拽分隔条可调整宽度，hover 时颜色变为 `var(--el-color-primary)`
- ✅ `storageKey` prop 用于 localStorage 持久化用户调整的宽度（推荐格式：`{module}-tree-width`）
- ❌ 禁止在 TpLeftTreeLayout 外侧放置表格工具栏（工具栏必须在 `#right` 插槽内，紧贴 `<TpTable>` 上方）
- ❌ 禁止子内容在 TpLeftTreeLayout 同向上再加 padding（避免双层间距）

---

## 31. TpCardList 详细规则

来源: §11.16.x

- ✅ 卡片视图必须使用 `TpCardList`（位于 `@mdm/common/components/data/`）作为网格容器，**禁止**调用方手写 `<div class="grid">` + `v-loading` + `el-pagination` 拼装
- ✅ 必须**显式 import**：`import TpCardList from '@mdm/common/components/data/TpCardList.vue';`（与 TpPageFrame 同理，`unplugin-vue-components` 不会自动注册）
- ✅ 节点必须带 `class="flex-1 min-h-0"`（与 TpTable 一致，避免在 flex 父容器中被压扁）
- ✅ **数据驱动**：通过 `:data` 传入数组，**禁止**在 default slot 里手写 `v-for`
- ✅ **作用域插槽 `#item`**：调用方**只**通过 `#item="{ item, index }"` 告诉组件"每张卡片长什么样"。**不要**使用 default slot（不提供 default slot）
- ✅ **分页**：v-model:pageNum / v-model:pageSize + :total + @page-change
- ✅ **空态**：默认从 `!loading && data.length === 0` 自动推算，无须传 `:empty`；`:empty-text` 传描述文案
- ✅ 卡片最小宽度默认值 `280px`（与 ModelCard 内容宽度匹配），如需调整传 `:grid-min-width="320"`
- ✅ grid 间距档位 2/3/4/6/8（与 Tailwind gap 档位对齐），默认 4
- ❌ 禁止在 TpCardList 内部用 `el-pagination` 替换 `TpPagination`（破坏设计规范）
- ❌ 禁止在 `TpCardList` 的 default slot 写非卡片内容（这是它的唯一约束）

---

## 32. p-3 间距规则完整版

来源: §11.21

> **核心规则**：列表页 / 详情页 / 编辑器 / 对话框 / 卡片 / 工具栏 / 内容区 wrapper / 树搜索框 —— **默认内边距统一使用 `p-3`（12px）**。本规则优先级最高——除非下表"宽松间距"明确允许，否则一律 `p-3`。

### 间距档位（padding）

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| **基础（默认）** | `p-3` | **12px** | 工具栏、内容区 wrapper、卡片内边距、对话框 body、详情页分组、树搜索框 |
| 紧凑 | `p-2` | 8px | 徽标、tag、小型 badge、搜索框 prefix 间距 |
| 宽松（特例） | `p-4` | 16px | 详情页主容器、详情卡片、模型卡 header/padding 区域——**需评审通过** |

### gap 档位

| 档位 | Tailwind | 像素值 | 用途 |
|------|----------|--------|------|
| 按钮组 / 表单字段 | `gap-2` | 8px | 一行内并排的按钮、el-form-item 之间 |
| 通用垂直/水平 | `gap-3` | 12px | 卡片信息行、列表项之间、详情页字段行 |
| 顶层 section | `gap-4` | 16px | 详情页不同分组之间、TpPageFrame 内部顶层 section |

### TpTable/TpCardList 外层容器特例

当 wrapper 内嵌 `TpTable` / `TpCardList`（自带分页栏），且直接紧邻上方工具栏（已有 `p-3`）时，wrapper **只保留左右间距 `px-3`，顶部和底部间距均为 0**。顶部间距已由工具栏的 `p-3` 提供，底部间距由表格/卡片自带的分页栏提供。

### 禁止的反模式

| ❌ 反模式 | ✅ 正确写法 | 原因 |
|----------|------------|------|
| `px-4 py-3` / `pr-3 pl-3` 分写 | `p-3` | 避免双方向间距不一致 |
| `h-[60px] px-4` 与 padding 双重控制 | `p-3`（去掉 `h-[60px]`） | 高度由内容 + padding 决定 |
| `p-4` 直接写工具栏 | `p-3` | 工具栏属于"基础间距"档位 |
| 包 TpTable/TpCardList 的 wrapper 写 `p-3` | `px-3`（紧邻工具栏时） | 顶部间距由工具栏提供 |
| 工具栏用 `mb-4` 与下方表格分隔 | `p-3` wrapper + `border-b` | 边框 + 统一 padding 视觉更整齐 |
| `p-[12px]` `m-[16px]` 硬编码数值 | `p-3` `m-4` | 视图中禁止硬编码间距 |

### 工具栏 border 规则

- **仅一行工具栏** → 不加 `border-b`。工具栏 `p-3` 底部 12px 间距已提供足够的呼吸空间。
- **多行工具栏叠放** → 行与行之间加 `border-b` 分隔，但**最下面一行（紧邻内容区的行）不加**。
