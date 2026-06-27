# 列表页骨架

> 来源：`frontend_ai_coding_rules.md §11.3` / `vue3/page-patterns §2` / `vue3/encapsulated §4.1`
>
> 适用：数据列表页（带分页、搜索、操作工具栏）
> 占位符说明：`{EncapsulatedXxx}` 由项目配置定义（如 `DmTable` / `ProTable` / `AppTable`）

---

## 1. 列表页完整骨架

列表页无标题，工具栏即页面顶部，左操作右搜索。

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 表格工具栏：左 = 操作，右 = 搜索 -->
    <div class="flex justify-between items-center p-3 flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
        <el-button @click="handleBatchDelete">批量删除</el-button>
        <el-button @click="handleImport">导入</el-button>
        <el-button @click="handleExport">导出</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input v-model="query.name" placeholder="请输入模型名称" clearable @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <{EncapsulatedTable}
      class="flex-1 min-h-0"
      :data="tableData"
      :loading="loading"
      :total="total"
      :columns="columns"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      @page-change="loadData"
      @size-change="loadData"
    />
  </{EncapsulatedPageFrame}>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { listUser, deleteUser } from '@/modules/user/api/user';
import type { User, UserQuery } from '@/modules/user/types/user';
import { {EncapsulatedMessage}, {EncapsulatedConfirm} } from '@/common/components/feedback';

defineOptions({ name: 'UserList' });

// 查询参数
const query = ref<UserQuery>({ page: 1, pageSize: 20, name: '' });

// 表格数据
const tableData = ref<User[]>([]);
const total = ref(0);
const loading = ref(false);

// 选中的行
const selectedIds = ref<Set<string>>(new Set());

// 列定义
const columns = [
  { type: 'selection', width: 50, fixed: 'left' },
  { prop: 'name', label: '名称', minWidth: 120, showOverflowTooltip: true },
  { prop: 'code', label: '编码', minWidth: 120, showOverflowTooltip: true },
  { prop: 'status', label: '状态', minWidth: 100 },
  { prop: 'createdAt', label: '创建时间', minWidth: 160 },
  { prop: 'actions', label: '操作', minWidth: 180, fixed: 'right' },
];

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listUser(query.value);
    tableData.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    {EncapsulatedMessage}.error('加载失败');
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};

// 重置
const handleReset = () => {
  query.value = { page: 1, pageSize: 20, name: '' };
  loadData();
};

// 新增
const handleCreate = () => {
  // 跳转到编辑页或打开弹窗
};

// 批量删除
const handleBatchDelete = async () => {
  if (selectedIds.value.size === 0) {
    {EncapsulatedMessage}.warning('请先选择要删除的项');
    return;
  }
  await {EncapsulatedConfirm}(`确定要删除已选中的 ${selectedIds.value.size} 项吗？`, '批量删除');
  // 调用批量删除 API
};

onMounted(loadData);
</script>
```

---

## 2. 列表页最小骨架

与决策树呼应的最小可用列表页。

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 表格工具栏 -->
    <div class="flex justify-between items-center p-3 flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
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

## 3. 表格禁忌写法

```vue
<!-- ❌ 列表页手写标题（一级页面不写标题，需要标题的页面用 PageHeader） -->
<h1>模型列表</h1>
<div class="page-list ..."> ... </div>

<!-- ❌ 表格工具栏只放标题或单按钮，搜索区另起一行——视觉割裂、浪费高度 -->
<div class="mb-4">...</div>
<div class="mb-4 flex items-center gap-2">...</div>

<!-- ❌ 空表格工具栏（既无操作也无搜索）—— 直接不写 -->
<div class="mb-4 flex justify-between items-center"></div>
```

---

*本文件为骨架代码参考。具体封装组件名由项目配置 `project_config/component_prefix.md` 决定。*
