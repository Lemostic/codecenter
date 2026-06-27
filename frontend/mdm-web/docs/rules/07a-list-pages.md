---
description: 列表页骨架模板 — 纯表格列表/左树右表/禁忌写法/按钮图标约束。
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

## 6. 按钮图标反模式

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
