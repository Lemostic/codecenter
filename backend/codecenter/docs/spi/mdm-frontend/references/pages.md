description: MDM 列表页骨架、左树右表骨架、flex 三段式编辑/详情页骨架、卡片视图、TpPageFrame/TpLeftTreeLayout 完整规则。
---

# 页面模板（TpPageFrame + TpLeftTreeLayout）

> 页面命名铁律、列表页/详情页/编辑页通用骨架、flex 三段式布局原则继承 `profiles/frontend/vue/vue3/page-patterns.md`。本文件给出 MDM 项目使用 `Tp-*` 封装组件后的具体骨架代码。

## 1 页面命名铁律（继承通用规范）

| 页面类型 | 强制后缀 | 文件名示例 |
|---------|---------|-----------|
| 列表页 | `List` | `ModelList.vue` |
| 详情页 | `Detail` | `ModelDetail.vue` |
| 编辑页 | `Editor` | `ModelEditor.vue` |
| 主页面 | `Index` | `ModelVersionIndex.vue` |
| 特殊业务页 | 业务名（需评审） | `ModelDesigner.vue` |

路由 name 格式：`{moduleName}-{type}`（如 `model-design-list`）。

## 2 列表页骨架

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { Plus, Delete } from '@element-plus/icons-vue';
import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';
import TpTable from '@mdm/common/components/data/TpTable.vue';
import TpEmpty from '@mdm/common/components/feedback/TpEmpty.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { useCrudList } from '@mdm/common/composables/useCrudList';
import { listModel, deleteModel } from '~model-design/modules/modelDesign/api/model';
import type { ModelEntity, ModelQuery } from '~model-design/modules/modelDesign/types/model';

const tableData = ref<ModelEntity[]>([]);

const {
  data, total, loading, pageNum, pageSize,
  fetchData, handleDelete, handlePageChange, handleSearch, handleReset,
} = useCrudList<ModelEntity>({
  listApi: listModel,
  deleteApi: deleteModel,
  defaultPageSize: 20,
});
</script>

<template>
  <TpPageFrame>
    <!-- 表格工具栏：左 = 操作，右 = 搜索 -->
    <div class="flex justify-between items-center p-3 flex-shrink-0">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
        <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input v-model="filters.name" placeholder="请输入名称" clearable @keyup.enter="fetchData" />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <TpTable
      class="flex-1 min-h-0"
      :data="data"
      :loading="loading"
      :total="total"
      v-model:pageNum="pageNum"
      v-model:pageSize="pageSize"
      :columns="columns"
      @page-change="handlePageChange"
      @size-change="handlePageChange"
    >
      <template #empty>
        <TpEmpty description="暂无数据" />
      </template>
    </TpTable>
  </TpPageFrame>
</template>
```

**关键规则**：

- `TpTable` MUST 带 `class="flex-1 min-h-0"`（禁止 `h-full`）
- 工具栏不放标题（面包屑由全局布局提供）
- 列表页（一级页面）不写页面标题

## 3 左树右表骨架

```vue
<TpPageFrame>
  <TpLeftTreeLayout
    class="flex-1 min-h-0"
    :default-width="280"
    storage-key="model-design-category-tree-width"
  >
    <template #left>
      <TpTreeLazy
        :load="loadTreeNode"
        :field-map="{ label: 'name', isLeaf: 'isLeaf' }"
        node-key="id"
        search-placeholder="搜索分类"
        @node-click="handleNodeClick"
      />
    </template>

    <template #right>
      <div class="h-full flex flex-col pt-2">
        <!-- 表格工具栏：紧贴 TpTable 上方，与树搜索框水平对齐 -->
        <div class="pb-2 flex justify-between items-center flex-shrink-0 border-b border-[#e4e7ed]">
          <div class="flex items-center gap-2">
            <el-button type="primary" :icon="Plus" @click="handleCreate">新增分类</el-button>
          </div>
          <div class="flex items-center gap-2">
            <el-input v-model="filters.name" placeholder="请输入" clearable @keyup.enter="fetchData" />
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>

        <TpTable class="flex-1 min-h-0 pt-4" ... />
      </div>
    </template>
  </TpLeftTreeLayout>
</TpPageFrame>
```

**关键规则**：

- `TpPageFrame` 在外层，`TpLeftTreeLayout` 加 `class="flex-1 min-h-0"`
- 工具栏 MUST 放在 `#right` 插槽内，禁止放 `TpLeftTreeLayout` 外侧
- 右侧面板根容器 `pt-2`，工具栏 `pb-2 border-b border-[#e4e7ed]`
- `TpTable` 加 `pt-4` 留出工具栏与表格的间距
- `storageKey` MUST 用 `{module}-{tree-name}-width` 命名

## 4 flex 三段式布局（编辑页 / 详情页）

```vue
<TpPageFrame>
  <TpPageHeader title="编辑模型" :back-to="{ name: 'model-design-list' }">
    <template #actions>
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
    </template>
  </TpPageHeader>

  <div class="flex-1 min-h-0 overflow-auto p-3">
    <el-form :model="formData" label-width="100px" size="default">
      <el-form-item label="模型名称" prop="name" required>
        <el-input v-model="formData.name" clearable maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item label="模型编码" prop="code" required>
        <el-input v-model="formData.code" clearable />
      </el-form-item>
      <el-form-item label="描述" prop="description">
        <el-input v-model="formData.description" type="textarea" :rows="3" maxlength="200" show-word-limit />
      </el-form-item>
    </el-form>
  </div>
</TpPageFrame>
```

**关键规则**：

- `TpPageHeader` MUST 使用，编辑/详情页禁止手写标题 div
- 内容区 `flex-1 min-h-0 overflow-auto p-3`
- 表单组件 MUST 带 `size="default"`、必填 `required`

## 5 卡片视图列表页

```vue
<TpPageFrame>
  <!-- 第 1 行：操作按钮行 -->
  <div class="flex items-center gap-2 mb-3 flex-shrink-0">
    <el-button type="primary" :icon="Plus" @click="handleCreate">新增</el-button>
    <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
    <el-button @click="handleImport">导入</el-button>
    <el-button @click="handleExport">导出</el-button>
  </div>

  <!-- 第 2 行：筛选搜索行 + 视图切换 -->
  <div class="flex items-center justify-between mb-3 flex-shrink-0">
    <div class="flex items-center gap-2">
      <el-select v-model="filters.status" placeholder="状态" clearable style="width: 120px;">
        <el-option label="启用" value="enabled" />
        <el-option label="禁用" value="disabled" />
      </el-select>
      <el-input v-model="filters.keyword" placeholder="请输入关键字" clearable style="width: 220px;" @keyup.enter="fetchData" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="flex items-center gap-2">
      <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
      <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
    </div>
  </div>

  <!-- 卡片视图 -->
  <TpCardList
    v-show="viewMode === 'card'"
    class="flex-1 min-h-0"
    :data="data"
    v-model:pageNum="pageNum"
    v-model:pageSize="pageSize"
    :total="total"
    :loading="loading"
    @page-change="handlePageChange"
  >
    <template #item="{ item }">
      <ModelCard :model="item" :selected="selectedIds.has(item.id)" @click="handleCardClick" />
    </template>
  </TpCardList>

  <!-- 列表视图 -->
  <TpTable
    v-show="viewMode === 'list'"
    class="flex-1 min-h-0"
    ...
  />
</TpPageFrame>
```

`ModelCard` 完整视觉规格见 `references/design-tokens.md §2`。

## 6 视图切换规则

- 文字按钮切换（不带图标）
- `size="small"`，放工具栏右侧
- 当前选中 `type="primary"`
- 默认卡片视图

```vue
<el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
<el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
```

## 7 工具栏 border 规则

| 场景 | 规则 |
|------|------|
| 仅一行工具栏 | 不加 `border-b`（`p-3` 底部 12px 间距已足够呼吸空间） |
| 多行工具栏叠放 | 行间加 `border-b` 分隔，但**最下面一行不加** |