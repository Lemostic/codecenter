# 左树右表骨架

> 来源：`frontend_ai_coding_rules.md §11.3/§11.15` / `vue3/page-patterns §5`
>
> 适用：左树右表 / 左分类右列表

---

## 左树右表骨架（正确）

表格工具栏在右侧面板内，紧贴 `{EncapsulatedTable}` 上方，与树搜索框水平对齐。

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
          <!-- pt-2 / pb-2 border-b 匹配 {EncapsulatedTreeLazy} 的 py-2 + border-b -->
          <div class="pb-2 flex justify-between items-center flex-shrink-0 border-b border-[#e4e7ed]">
            <div class="flex items-center gap-2">
              <el-button type="primary" :icon="Plus" @click="handleCreate">新增分类</el-button>
            </div>
            <div class="flex items-center gap-2">
              <el-input v-model="query.name" placeholder="请输入" clearable @keyup.enter="loadData" />
              <el-button type="primary" @click="loadData">查询</el-button>
              <el-button @click="handleReset">重置</el-button>
            </div>
          </div>

          <!-- 表格（pt-4 留出工具栏与表格间距） -->
          <{EncapsulatedTable}
            class="flex-1 min-h-0 pt-4"
            :data="tableData"
            :loading="loading"
            :total="total"
            :columns="columns"
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            @page-change="loadData"
            @size-change="loadData"
          />
        </div>
      </template>
    </{EncapsulatedTreeLayout}>
  </{EncapsulatedPageFrame}>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { Plus } from '@element-plus/icons-vue';
import { {EncapsulatedMessage} } from '@/common/components/feedback';

defineOptions({ name: 'ModelCategoryList' });

// 当前选中的节点
const currentNode = ref<{ id: string; name: string } | null>(null);

// 查询参数
const query = ref({ page: 1, pageSize: 20, name: '', categoryId: '' });

// 表格数据
const tableData = ref([]);
const total = ref(0);
const loading = ref(false);

const columns = [
  { prop: 'name', label: '名称', minWidth: 120, showOverflowTooltip: true },
  { prop: 'code', label: '编码', minWidth: 120, showOverflowTooltip: true },
  { prop: 'createdAt', label: '创建时间', minWidth: 160 },
  { prop: 'actions', label: '操作', minWidth: 180, fixed: 'right' },
];

// 加载数据
const loadData = async () => {
  loading.value = true;
  try {
    // 传入当前选中分类的 id
    // const res = await listModel({ ...query.value, categoryId: currentNode.value?.id });
    // tableData.value = res.data?.rows ?? [];
    // total.value = res.data?.total ?? 0;
  } catch (error) {
    console.error('[loadData]', error);
    {EncapsulatedMessage}.error('加载失败');
  } finally {
    loading.value = false;
  }
};

// 树节点点击
const handleNodeClick = (node: { id: string; name: string }) => {
  currentNode.value = node;
  query.value.categoryId = node.id;
  query.value.page = 1;
  loadData();
};

// 懒加载树节点
const loadTreeNode = async (node: unknown, resolve: (data: unknown[]) => void) => {
  // 调用分类树 API
  // const res = await listCategoryTree({ parentId: node?.id });
  // resolve(res.data);
  resolve([]);
};

const handleCreate = () => { /* ... */ };
const handleReset = () => {
  query.value = { page: 1, pageSize: 20, name: '', categoryId: currentNode.value?.id ?? '' };
  loadData();
};
</script>
```

---

## 左树右表禁忌写法

表格工具栏不应放在 `{EncapsulatedTreeLayout}` 外侧（页面级别）。

```vue
<!-- ❌ 错误：表格工具栏放在 {EncapsulatedTreeLayout} 外侧（页面级别） -->
<template>
  <{EncapsulatedPageFrame}>
    <div class="mb-4 flex justify-between">
      <el-button>新增</el-button>
      <el-form>...</el-form>
    </div>
    <{EncapsulatedTreeLayout}>
      <template #right>
        <{EncapsulatedTable} ... />
      </template>
    </{EncapsulatedTreeLayout}>
  </{EncapsulatedPageFrame}>
</template>
```

```vue
<!-- ❌ 错误：工具栏用 mb-4 分隔（导致与树工具栏不对齐） -->
<template>
  <{EncapsulatedTreeLayout}>
    <template #right>
      <div>
        <div class="mb-4 flex justify-between">
          <el-button>新增</el-button>
        </div>
        <{EncapsulatedTable} ... />
      </div>
    </template>
  </{EncapsulatedTreeLayout}>
</template>
```

---

## 关键点

| 规则 | 原因 |
|------|------|
| 表格工具栏放在 `#right` 插槽内 | 工具栏属于表格，不属于页面 |
| 右侧面板根容器 `pt-2` | 匹配 `{EncapsulatedTreeLazy}` 搜索框的 `py-2` 顶部间距 |
| 工具栏 `pb-2 border-b border-[#e4e7ed]` | 匹配树搜索框的 `py-2` 底部间距 + 边框 |
| `{EncapsulatedTable}` 加 `pt-4` | 留出工具栏与表格的间距 |
| 工具栏不用 `mb-4` | 会导致与树搜索框不对齐 |

---

*本文件为骨架代码参考。具体封装组件名由项目配置决定。*
