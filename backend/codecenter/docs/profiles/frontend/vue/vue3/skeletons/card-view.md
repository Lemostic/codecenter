# 卡片视图骨架

> 来源：`frontend_ai_coding_rules.md §11.16.x` / `vue3/page-patterns §6`
>
> 适用：卡片视图列表页

---

## 卡片视图完整示例

使用 `{EncapsulatedCardList}` 作为网格容器，通过作用域插槽 `#item` 渲染自定义卡片。

```vue
<template>
  <{EncapsulatedPageFrame}>
    <!-- 第 1 行：操作按钮行 -->
    <div class="flex items-center gap-2 mb-3 flex-shrink-0">
      <el-button type="primary" :icon="Plus" @click="handleAdd">新增</el-button>
      <el-button type="danger" :icon="Delete" @click="handleBatchDelete">批量删除</el-button>
      <el-divider direction="vertical" />
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
        <el-input v-model="query.keyword" placeholder="请输入关键字" clearable style="width: 220px;" @keyup.enter="loadData" />
        <el-button type="primary" @click="loadData">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-button size="small" :type="viewMode === 'card' ? 'primary' : ''" @click="viewMode = 'card'">卡片视图</el-button>
        <el-button size="small" :type="viewMode === 'list' ? 'primary' : ''" @click="viewMode = 'list'">列表视图</el-button>
      </div>
    </div>

    <!-- 卡片视图 -->
    <{EncapsulatedCardList}
      v-show="viewMode === 'card'"
      class="flex-1 min-h-0"
      :data="models"
      v-model:current-page="query.page"
      v-model:page-size="query.pageSize"
      :total="total"
      :loading="loading"
      :empty-text="t('modelDesign.list.empty')"
      @page-change="loadData"
      @size-change="handleSizeChange"
    >
      <template #item="{ item }">
        <ModelCard
          :model="item"
          :selected="selectedIds.has(item.id)"
          @click="handleCardClick"
          @select="handleCardSelect"
        />
      </template>
    </{EncapsulatedCardList}>

    <!-- 列表视图 -->
    <{EncapsulatedTable}
      v-show="viewMode === 'list'"
      class="flex-1 min-h-0"
      :data="models"
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
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import { Plus, Delete } from '@element-plus/icons-vue';
import ModelCard from './components/ModelCard.vue';
import { listModel } from '@/modules/modelDesign/api/model';
import { {EncapsulatedMessage} } from '@/common/components/feedback';
import type { Model, ModelQuery } from '@/modules/modelDesign/types/model';

defineOptions({ name: 'ModelList' });

const { t } = useI18n();

// 视图模式
const viewMode = ref<'card' | 'list'>('card');

// 查询参数
const query = ref<ModelQuery>({ page: 1, pageSize: 20, keyword: '', status: undefined });

// 表格/卡片数据
const models = ref<Model[]>([]);
const total = ref(0);
const loading = ref(false);

// 选中的卡片
const selectedIds = ref<Set<string>>(new Set());

// 表格列定义
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
    const res = await listModel(query.value);
    models.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    console.error('[loadData]', error);
    {EncapsulatedMessage}.error('加载失败');
  } finally {
    loading.value = false;
  }
};

const handleSizeChange = (size: number) => {
  query.value.pageSize = size;
  query.value.page = 1;
  loadData();
};

const handleReset = () => {
  query.value = { page: 1, pageSize: 20, keyword: '', status: undefined };
  loadData();
};

const handleAdd = () => { /* ... */ };
const handleBatchDelete = () => { /* ... */ };
const handleCardClick = (model: Model) => { /* ... */ };
const handleCardSelect = (model: Model) => {
  if (selectedIds.value.has(model.id)) {
    selectedIds.value.delete(model.id);
  } else {
    selectedIds.value.add(model.id);
  }
};

onMounted(loadData);
</script>
```

---

## ModelCard 设计规格

| 属性 | 值 |
|------|-----|
| border-radius | `4px` |
| border | `1px solid #dcdfe6` |
| background | `#ffffff` |
| cursor | `pointer` |
| 默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| 选中态 | `ring-2 ring-[var(--el-color-primary)]` |
| 图标框 | 36x36px，`#337bff` 背景 |
| 复选框 | 右上角 `absolute top-4 right-4 z-10` |

```vue
<!-- ModelCard.vue -->
<template>
  <div
    class="rounded-sm border border-[#dcdfe6] p-4 cursor-pointer transition-all duration-200 ease-in-out relative"
    :class="[selected ? 'ring-2 ring-[var(--el-color-primary)]' : '']"
    :style="cardStyle"
    @click="$emit('click', model)"
  >
    <el-checkbox
      v-if="selectable"
      :model-value="selected"
      class="absolute top-4 right-4 z-10"
      @change="$emit('select', model)"
      @click.stop
    />
    <div class="flex items-center gap-3 mb-3">
      <div
        class="flex items-center justify-center rounded-sm"
        :style="{ width: '36px', height: '36px', backgroundColor: '#337bff' }"
      >
        <el-icon :size="20" color="#fff"><Document /></el-icon>
      </div>
      <div class="flex-1 min-w-0">
        <div class="text-sm font-medium text-[#3d4247] truncate">{{ model.name }}</div>
        <div class="text-xs text-[#666666] truncate">{{ model.code }}</div>
      </div>
    </div>
    <div class="text-[13px] text-[#666666]" :style="{ lineHeight: '30px' }">
      <div>状态：<el-tag :type="statusTagType(model.status)" size="default">{{ model.status }}</el-tag></div>
      <div>创建时间：{{ formatDate(model.createdAt) }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { Document } from '@element-plus/icons-vue';
import { formatDate, statusTagType } from '@/common/utils';

defineOptions({ name: 'ModelCard' });

interface Props {
  model: Model;
  selected?: boolean;
  selectable?: boolean;
}

withDefaults(defineProps<Props>(), {
  selected: false,
  selectable: true,
});

defineEmits<{
  (e: 'click', model: Model): void;
  (e: 'select', model: Model): void;
}>();

const cardStyle = {
  backgroundColor: '#ffffff',
};
</script>
```

---

*本文件为骨架代码参考。具体封装组件名由项目配置决定。*
