---
description: 卡片视图/分页/详情页组件骨架 — TpCardList/两行式工具栏/视图切换/TpPageHeader/TpPagination/ModelCard规格。
paths:
  - "**/*.vue"
---

## 1. 卡片视图完整示例

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

## 2. 两行式工具栏规则说明和示例

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

## 3. 视图切换代码示例

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

## 4. TpPageHeader 示例

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

## 5. TpPagination 用法示例

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

## 6. ModelCard 设计规格参考

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

---

## 7. TpCardList 详细规则

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
