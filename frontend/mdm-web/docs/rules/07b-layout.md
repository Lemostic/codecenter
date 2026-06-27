---
description: 布局组件骨架与规则 — flex三段式/TpPageFrame/TpLeftTreeLayout/高度自适应。
paths:
  - "**/*.vue"
---

## 1. flex 三段式布局示例

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

## 2. TpPageFrame 骨架示例

来源: §11.14

### 2a. 纯表格列表页

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

### 2b. 左树右表

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

## 3. TpLeftTreeLayout 骨架示例

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

## 4. 页面高度自适应布局详细规则

来源: §11.13

**适用范围**：编辑页、表单页、详情页（有底部操作栏时）等。列表页的左树右表场景见 §3。

- ✅ 任何包含底部操作按钮的页面，**必须**使用 flex 纵向三段式布局
- ✅ 根容器：`<TpPageFrame>`（提供 `bg-white p-4 rounded-lg flex flex-col h-full overflow-hidden`）
- ✅ 页头/工具栏：`flex-shrink-0`（固定不压缩）
- ✅ 内容区：`flex-1 min-h-0 overflow-auto`（自适应撑开，内容多时可滚动）
- ✅ 底部操作栏：`flex-shrink-0`（始终贴底）
- ✅ **编辑页和详情页的页头必须使用 `<TpPageHeader>` 组件**（`@mdm/common/components/structure/TpPageHeader.vue`），自带返回箭头和标题。`backTo` prop 指定返回路由（通常为列表页路由 name），不传则 `router.back()`。右侧操作按钮用 `#actions` 插槽。**禁止**手写页头标题 div
- ❌ 禁止根容器手写 `bg-white p-4 flex flex-col h-full` 等价外壳（用 `TpPageFrame` 替代）
- ❌ 禁止底部操作按钮仅靠 `margin-top` 定位（内容少时按钮悬空）

---

## 5. TpPageFrame 规则

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

## 6. TpLeftTreeLayout 规则

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
