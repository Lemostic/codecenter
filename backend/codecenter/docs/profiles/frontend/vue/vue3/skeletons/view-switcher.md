# 视图切换骨架

> 来源：`frontend_ai_coding_rules.md §11.18` / `vue3/encapsulated §5`
>
> 适用：卡片/列表视图文字按钮切换

---

## 视图切换代码示例

卡片/列表视图通过文字按钮切换，不带图标，当前选中模式使用 `type="primary"`。

```vue
<template>
  <!-- 视图切换 -->
  <div class="flex items-center gap-2">
    <el-button
      size="small"
      :type="viewMode === 'card' ? 'primary' : ''"
      @click="viewMode = 'card'"
    >
      卡片视图
    </el-button>
    <el-button
      size="small"
      :type="viewMode === 'list' ? 'primary' : ''"
      @click="viewMode = 'list'"
    >
      列表视图
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

const viewMode = ref<'card' | 'list'>('card');
</script>
```

---

## 规则

| 规则 | 说明 |
|------|------|
| MUST | 文字按钮切换（不带图标） |
| MUST | 当前选中 `type="primary"` |
| MUST | `size="small"`，放工具栏右侧 |
| MUST | 状态：`ref<'card' \| 'list'>('card')`，默认卡片视图 |
| MAY | 用 `el-divider direction="vertical"` 与筛选区分隔 |
| MUST NOT | 图标按钮切换视图 |

---

## 完整集成示例（卡片视图列表页）

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 第 1 行：操作按钮行 -->
    <div class="flex items-center gap-2 mb-3 flex-shrink-0">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <!-- 第 2 行：筛选搜索行（含视图切换） -->
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
        <!-- 视图切换 -->
        <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
        <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <{EncapsulatedCardList}
      v-show="viewMode === 'card'"
      class="flex-1 min-h-0"
      :data="items"
      :total="total"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      @page-change="loadData"
    >
      <template #item="{ item }">
        <ItemCard :item="item" />
      </template>
    </{EncapsulatedCardList}>

    <{EncapsulatedTable}
      v-show="viewMode === 'list'"
      class="flex-1 min-h-0"
      :data="items"
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

## 禁忌写法

```vue
<!-- ❌ 禁止：图标按钮切换视图 -->
<el-button :icon="Grid" @click="viewMode = 'card'" />
<el-button :icon="List" @click="viewMode = 'list'" />

<!-- ❌ 禁止：未指定 type="primary" 区分选中态 -->
<el-button @click="viewMode = 'card'">卡片视图</el-button>
<el-button @click="viewMode = 'list'">列表视图</el-button>

<!-- ❌ 禁止：size 错误（必须 size="small"） -->
<el-button size="default" :type="viewMode === 'card' ? 'primary' : ''">卡片视图</el-button>
```

---

*本文件为骨架代码参考。*
