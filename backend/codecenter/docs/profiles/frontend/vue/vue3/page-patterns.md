| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `vue3/encapsulated.md`、`common/architecture.md` |

# 页面布局模式

> 本文件定义页面级布局模式：列表页/详情页/编辑页/左树右表/卡片视图的布局结构。
> 本文件规则**仅适用于 Vue 3**。

---

## 1 高度自适应布局原则

**PROF-FE-951** 任何包含底部操作按钮的页面，MUST 使用 flex 纵向三段式布局。 [MUST]

```
根容器（flex flex-col h-full）
├── 页头/工具栏（flex-shrink-0）
├── 内容区（flex-1 min-h-0 overflow-auto）
└── 底部操作栏（flex-shrink-0）
```

| 规则 | 说明 |
|------|------|
| MUST | 根容器使用 `{EncapsulatedPageFrame}`（详见 `vue3/encapsulated.md §4.7`） |
| MUST | 页头/工具栏 `flex-shrink-0`（固定不压缩） |
| MUST | 内容区 `flex-1 min-h-0 overflow-auto` |
| MUST | 底部操作栏 `flex-shrink-0`（始终贴底） |
| MUST NOT | 底部操作按钮仅靠 `margin-top` 定位（内容少时按钮悬空） |
| MUST NOT | 用 `h-full` 替代 `flex-1 min-h-0`（在 flex item 上会与内容撑高形成循环依赖） |

---

## 2 列表页布局

**PROF-FE-952** 列表页（一级页面） MUST 使用 flex 纵向布局 + 工具栏 + 表格。 [MUST]

| 规则 | 说明 |
|------|------|
| MUST | 根容器：`{EncapsulatedPageFrame}` |
| MUST | 表格工具栏紧贴 `{EncapsulatedTable}` 上方（属于表格，不属于页面） |
| MUST | 表格工具栏分左右两部分：`flex justify-between` |
| MUST | 左侧 = 操作按钮（新增/删除/导入/导出/批量删除） |
| MUST | 右侧 = 搜索区（输入字段 + 查询/重置），**不使用 `el-form`** |
| MUST | `<{EncapsulatedTable}>` 节点 MUST 带 `class="flex-1 min-h-0"` |
| MUST NOT | 列表页（一级页面）不写页面标题（面包屑由全局布局提供） |
| MUST NOT | 既无操作也无搜索时不写工具栏 |

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 表格工具栏：左 = 操作，右 = 搜索 -->
    <div class="flex justify-between items-center p-3 flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
        <el-button @click="handleImport">导入</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input v-model="query.name" placeholder="请输入名称" clearable @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <{EncapsulatedTable}
      class="flex-1 min-h-0"
      :data="tableData"
      :loading="loading"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :columns="columns"
      @page-change="loadData"
      @size-change="loadData"
    />
  </{EncapsulatedPageFrame}>
</template>
```

---

## 3 详情页布局

**PROF-FE-953** 详情页 MUST 使用 `<PageHeader>` + 内容区 + 描述列表。 [MUST]

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 页头：返回 + 标题 + 操作 -->
    <PageHeader title="用户详情" :back-to="{ name: 'user-list' }">
      <template #actions>
        <el-button type="primary" @click="handleEdit">编辑</el-button>
      </template>
    </PageHeader>

    <!-- 内容区：分组展示 -->
    <div class="flex-1 min-h-0 overflow-auto p-3 flex flex-col gap-3">
      <{EncapsulatedSectionTitle} title="基本信息" />
      <el-descriptions :column="2" border>
        <el-descriptions-item label="用户名">{{ user.username }}</el-descriptions-item>
        <el-descriptions-item label="邮箱">{{ user.email }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(user.status)" size="default">{{ user.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDate(user.createdAt) }}</el-descriptions-item>
      </el-descriptions>
    </div>
  </{EncapsulatedPageFrame}>
</template>
```

---

## 4 编辑页布局

**PROF-FE-954** 编辑页 MUST 使用 `<PageHeader>` + flex 三段式布局 + 表单。 [MUST]

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 页头 -->
    <PageHeader title="编辑用户" :back-to="{ name: 'user-list' }">
      <template #actions>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </PageHeader>

    <!-- 内容区（flex-1 自适应撑开，内容多时可滚动） -->
    <div class="flex-1 min-h-0 overflow-auto p-3">
      <el-form :model="formData" label-width="100px" size="default">
        <el-form-item label="用户名" prop="username" required>
          <el-input v-model="formData.username" clearable maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱" prop="email" required>
          <el-input v-model="formData.email" clearable />
        </el-form-item>
        <!-- ... -->
      </el-form>
    </div>
  </{EncapsulatedPageFrame}>
</template>
```

---

## 5 左树右表布局

**PROF-FE-955** 左树右表页面 MUST 使用 `{EncapsulatedTreeLayout}` 组件。 [MUST]

| 规则 | 说明 |
|------|------|
| MUST | `{EncapsulatedTreeLayout}` 在 `{EncapsulatedPageFrame}` 内层，并加 `class="flex-1 min-h-0"` |
| MUST | 表格工具栏 MUST 放在 `#right` 插槽内（紧贴 `{EncapsulatedTable}` 上方） |
| MUST | 右侧面板根容器 `pt-2` |
| MUST | 工具栏 `pb-2 border-b border-[#e4e7ed]`（匹配树搜索框的 `py-2` 底部间距） |
| MUST | `{EncapsulatedTable}` 加 `pt-4` 留出工具栏与表格的间距 |
| MUST NOT | 工具栏使用 `mb-4`（会导致与树工具栏不对齐） |
| MUST NOT | 表格工具栏放在 `{EncapsulatedTreeLayout}` 外侧（页面级别） |

```vue
<template>
  <{EncapsulatedPageFrame}>
    <{EncapsulatedTreeLayout} class="flex-1 min-h-0" :default-width="280" storage-key="model-tree-width">
      <template #left>
        <{EncapsulatedTreeLazy}
          :load="loadTreeNode"
          search-placeholder="搜索分类"
          @node-click="handleNodeClick"
        />
      </template>

      <template #right>
        <div class="h-full flex flex-col pt-2">
          <!-- 表格工具栏：紧贴 {EncapsulatedTable} 上方，与树搜索框对齐 -->
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

          <{EncapsulatedTable} class="flex-1 min-h-0 pt-4" ... />
        </div>
      </template>
    </{EncapsulatedTreeLayout}>
  </{EncapsulatedPageFrame}>
</template>
```

---

## 6 卡片视图列表页

**PROF-FE-956** 卡片视图列表页 MUST 使用 `{EncapsulatedCardList}` 组件 + 两行式工具栏。 [MUST]

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 第 1 行：操作按钮行 -->
    <div class="flex items-center gap-2 mb-3 flex-shrink-0">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
      <el-button @click="handleImport">导入</el-button>
      <el-button @click="handleExport">导出</el-button>
    </div>

    <!-- 第 2 行：筛选搜索行 -->
    <div class="flex items-center justify-between mb-3 flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px;">
          <el-option label="启用" value="enabled" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-input v-model="query.keyword" placeholder="请输入关键字" clearable style="width: 220px;" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
        <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
      </div>
    </div>

    <!-- 卡片列表 -->
    <{EncapsulatedCardList}
      v-show="viewMode === 'card'"
      class="flex-1 min-h-0"
      :data="cardData"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :loading="loading"
      @page-change="loadData"
      @size-change="loadData"
    >
      <template #item="{ item }">
        <ModelCard :model="item" :selected="selectedIds.has(item.id)" @click="handleCardClick" />
      </template>
    </{EncapsulatedCardList}>

    <!-- 列表视图 -->
    <{EncapsulatedTable}
      v-show="viewMode === 'list'"
      class="flex-1 min-h-0"
      :data="tableData"
      :loading="loading"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :columns="columns"
      @page-change="loadData"
    />
  </{EncapsulatedPageFrame}>
</template>
```

---

## 7 工具栏 border 规则

**PROF-FE-957** 工具栏 border 规则： [MUST]

- **仅一行工具栏** → 不加 `border-b`（`p-3` 底部 12px 间距已足够呼吸空间）
- **多行工具栏叠放** → 行间加 `border-b` 分隔，但**最下面一行（紧邻内容区）不加**

```vue
<template>
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
  <div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
    <!-- 排序/筛选控件 -->
  </div>
  <div class="flex items-center justify-between p-3 flex-shrink-0">
    <!-- 操作按钮（紧邻内容区，不加 border-b） -->
  </div>
</template>
```

---

## 8 页面命名铁律

详见 `common/architecture.md §8`。

| 页面类型 | 强制后缀 | 文件名示例 | 组件名 |
|---------|---------|-----------|--------|
| 列表页 | `List` | `ModelList.vue` | `ModelList` |
| 详情页 | `Detail` | `ModelDetail.vue` | `ModelDetail` |
| 编辑页 | `Editor` | `ModelEditor.vue` | `ModelEditor` |
| 主页面 | `Index` | `ModelVersionIndex.vue` | `ModelVersionIndex` |
| 特殊业务页 | 业务名（需评审） | `ModelDesigner.vue` | `ModelDesigner` |

---

## 9 禁止行为清单

- ❌ 禁止用 `h-full` 替代 `flex-1 min-h-0`
- ❌ 禁止底部操作按钮仅靠 `margin-top` 定位
- ❌ 禁止列表页（一级页面）写页面标题
- ❌ 禁止既无操作也无搜索时还写空工具栏
- ❌ 禁止左树右表中工具栏放在 `{EncapsulatedTreeLayout}` 外侧
- ❌ 禁止手写页头标题 div（必须用 `<PageHeader>`）
- ❌ 禁止多行工具栏最下面一行加 `border-b`

---

*本文件规则仅适用于 Vue 3。*
