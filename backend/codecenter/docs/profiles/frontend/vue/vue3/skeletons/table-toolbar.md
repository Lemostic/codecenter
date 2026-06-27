# 表格工具栏骨架

> 来源：`frontend_ai_coding_rules.md §11.3/§11.17` / `vue3/page-patterns §2/§6`
>
> 适用：单行工具栏 / 两行工具栏（卡片视图）

---

## 1. 单行工具栏（普通列表页）

仅一行工具栏 → 不加 `border-b`。`p-3` 底部 12px 间距已足够呼吸空间。

```vue
<template>
  <!-- 工具栏：左 = 操作按钮，右 = 筛选/搜索（仅一行时不加 border-b） -->
  <div class="flex items-center justify-between p-3 flex-shrink-0">
    <div class="flex items-center gap-2">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button @click="handleEdit">编辑</el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-input v-model="query.keyword" placeholder="搜索" clearable @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
  </div>
</template>
```

---

## 2. 多行工具栏（多行叠放）

多行工具栏叠放 → 行间用 `border-b` 分隔，**最下面一行（紧邻内容区）不加**。

```vue
<template>
  <!-- 第 1 行：排序/筛选 -->
  <div class="flex items-center justify-between p-3 flex-shrink-0 border-b border-[#e9e9e9]">
    <div class="flex items-center gap-2">
      <el-select v-model="query.sort" placeholder="排序" style="width: 120px;">
        <el-option label="最新" value="newest" />
        <el-option label="最旧" value="oldest" />
      </el-select>
      <el-select v-model="query.filter" placeholder="筛选" style="width: 120px;">
        <el-option label="全部" value="" />
        <el-option label="启用" value="enabled" />
        <el-option label="禁用" value="disabled" />
      </el-select>
    </div>
  </div>
  <!-- 第 2 行：操作按钮（紧邻内容区，不加 border-b） -->
  <div class="flex items-center justify-between p-3 flex-shrink-0">
    <div class="flex items-center gap-2">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button @click="handleExport">导出</el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-input v-model="query.keyword" placeholder="搜索" clearable />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
  </div>
</template>
```

---

## 3. 两行式工具栏（卡片视图列表页）

两行工具栏（区别于纯表格列表页的单行工具栏）。

### 第 1 行 - 操作按钮行

- `flex items-center gap-2 mb-3 flex-shrink-0`
- 仅"新增"用 `type="primary"` + `:icon="Plus"`
- 仅"批量删除"可带 `:icon="Delete"`
- 其余 `size="default"` 无图标
- 可用 `el-divider direction="vertical"` 分隔

```vue
<template>
  <div class="flex items-center gap-2 mb-3 flex-shrink-0">
    <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
    <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
    <el-divider direction="vertical" />
    <el-button @click="handleImport">导入</el-button>
    <el-button @click="handleExport">导出</el-button>
  </div>
</template>
```

### 第 2 行 - 筛选搜索行

- `flex items-center justify-between mb-3 flex-shrink-0`
- 左侧：筛选下拉（`el-select` 120px，`clearable`）+ 关键字输入（`el-input` 220px，`clearable`）+ 搜索/重置按钮
- 右侧：视图切换按钮 + 可选附加控件（如"级联子分类"复选框）

```vue
<template>
  <div class="flex items-center justify-between mb-3 flex-shrink-0">
    <div class="flex items-center gap-2">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 120px;">
        <el-option label="启用" value="enabled" />
        <el-option label="禁用" value="disabled" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="请输入关键字" clearable style="width: 220px;" @keyup.enter="loadData" />
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-checkbox v-model="cascadeSubCategory">级联子分类</el-checkbox>
      <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
      <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
    </div>
  </div>
</template>
```

---

## 4. 禁忌写法

```vue
<!-- ❌ 禁止：工具栏用 mb-4 与下方表格分隔 -->
<template>
  <div>
    <div class="mb-4 flex justify-between">
      <el-button>新增</el-button>
    </div>
    <{EncapsulatedTable} ... />
  </div>
</template>

<!-- ❌ 禁止：px-4 py-3 分写（必须用 p-3） -->
<div class="px-4 py-3 flex justify-between">
  <!-- ... -->
</div>

<!-- ❌ 禁止：h-[60px] px-4 与 padding 双重控制（去掉 h-[60px]） -->
<div class="h-[60px] px-4 flex items-center justify-between">
  <!-- ... -->
</div>

<!-- ❌ 禁止：p-4 直接写工具栏（工具栏属于"基础间距"档位） -->
<div class="p-4 flex justify-between">
  <!-- ... -->
</div>

<!-- ❌ 禁止：包 DmTable 的 wrapper 写 p-3（紧邻工具栏时只保留 px-3） -->
<div class="flex-1 min-h-0 flex flex-col p-3">
  <{EncapsulatedTable} ... />
</div>
```

---

## 5. 旧代码迁移对照表

| 旧 class | 新 class | 出现位置 |
|----------|----------|----------|
| `px-4 py-3` | `p-3` | 工具栏 |
| `pr-3 pl-3` | `p-3` | 内容区 wrapper |
| `h-[60px] px-4` | `p-3`（去掉 `h-[60px]`） | 工具栏首行 |
| `pt-2` / `pt-4`（仅顶部留白时） | `p-3`（统一为四方向） | 工具栏与 DmTable 之间的局部间距 |

---

*本文件为骨架代码参考。*
