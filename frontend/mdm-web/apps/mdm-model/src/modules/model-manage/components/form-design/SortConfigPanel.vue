<script setup lang="ts">
/**
 * SortConfigPanel - 排序字段配置 (§29)
 * 排序列表、新增/删除排序字段、优先级、升降序
 */
import { ref, computed } from 'vue';
import type { SortFieldConfig } from '@/modules/model-design/types/form-design';

defineOptions({ name: 'SortConfigPanel' });

interface Props {
  /** 排序字段列表 */
  sortFields?: SortFieldConfig[];
  /** 可用属性列表 */
  availableAttributes?: { id: string; name: string; englishName: string; dataType: string }[];
}

const props = withDefaults(defineProps<Props>(), {
  sortFields: () => [],
  availableAttributes: () => [],
});

const emit = defineEmits<{
  'update:sortFields': [val: SortFieldConfig[]];
}>();

// 搜索过滤
const searchKeyword = ref('');

const filteredAttributes = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase();
  if (!kw) return props.availableAttributes;
  return props.availableAttributes.filter(
    a => a.name.toLowerCase().includes(kw) || a.englishName.toLowerCase().includes(kw),
  );
});

// 已排序的属性 ID
const sortedAttributeIds = computed(() => new Set(props.sortFields.map(f => f.attributeId)));

// 新增排序弹窗
const addDialogVisible = ref(false);
const selectedAttrIds = ref<string[]>([]);

// §29.3 新增排序字段
const handleAddSort = () => {
  selectedAttrIds.value = [];
  addDialogVisible.value = true;
};

const confirmAddSort = () => {
  const currentMaxPriority = props.sortFields.reduce((max, f) => Math.max(max, f.priority), 0);
  const newFields: SortFieldConfig[] = selectedAttrIds.value.map((id, i) => ({
    attributeId: id,
    direction: 'asc' as const,
    priority: currentMaxPriority + i + 1,
  }));
  emit('update:sortFields', [...props.sortFields, ...newFields]);
  addDialogVisible.value = false;
};

// §29.5 删除排序字段
const handleRemoveSort = (attributeId: string) => {
  const filtered = props.sortFields.filter(f => f.attributeId !== attributeId);
  // 重新编号优先级
  const reordered = filtered.map((f, i) => ({ ...f, priority: i + 1 }));
  emit('update:sortFields', reordered);
};

// 批量删除
const selectedRows = ref<string[]>([]);
const handleBatchRemove = () => {
  if (selectedRows.value.length === 0) return;
  const removeSet = new Set(selectedRows.value);
  const filtered = props.sortFields.filter(f => !removeSet.has(f.attributeId));
  const reordered = filtered.map((f, i) => ({ ...f, priority: i + 1 }));
  emit('update:sortFields', reordered);
  selectedRows.value = [];
};

// §29.4 更新排序方向
const handleDirectionChange = (attributeId: string, direction: 'asc' | 'desc') => {
  emit('update:sortFields', props.sortFields.map(f =>
    f.attributeId === attributeId ? { ...f, direction } : f,
  ));
};

// 获取属性信息
const getAttrInfo = (attrId: string) => {
  return props.availableAttributes.find(a => a.id === attrId);
};

// 选择行
const handleSelectionChange = (rows: SortFieldConfig[]) => {
  selectedRows.value = rows.map(r => r.attributeId);
};
</script>

<template>
  <div class="sort-config-panel">
    <!-- 提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-xs">
        设置排序字段和优先级，未设置时系统按 状态→创建时间倒序→修改时间倒序→ID升序 排列
      </div>
    </el-alert>

    <!-- 操作栏 -->
    <div class="flex items-center justify-between mb-3">
      <el-input
        v-model="searchKeyword"
        size="small"
        placeholder="搜索属性名称/英文名称"
        clearable
        class="w-48"
      />
      <div class="flex gap-2">
        <el-button size="small" type="primary" @click="handleAddSort">新增排序字段</el-button>
        <el-button size="small" type="danger" :disabled="selectedRows.length === 0" @click="handleBatchRemove">
          批量删除
        </el-button>
      </div>
    </div>

    <!-- §29.1 排序列表 -->
    <el-table
      :data="sortFields"
      size="small"
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="40" />
      <el-table-column label="优先级" width="70" align="center">
        <template #default="{ row }">
          <el-tag size="small" type="info">{{ row.priority }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="属性名称" min-width="120">
        <template #default="{ row }">
          {{ getAttrInfo(row.attributeId)?.name || row.attributeId }}
        </template>
      </el-table-column>
      <el-table-column label="英文名称" min-width="120">
        <template #default="{ row }">
          {{ getAttrInfo(row.attributeId)?.englishName || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="数据类型" width="100">
        <template #default="{ row }">
          {{ getAttrInfo(row.attributeId)?.dataType || '-' }}
        </template>
      </el-table-column>
      <!-- §29.4 排序方向 -->
      <el-table-column label="排序方向" width="120" align="center">
        <template #default="{ row }">
          <el-radio-group
            :model-value="row.direction"
            size="small"
            @update:model-value="handleDirectionChange(row.attributeId, $event)"
          >
            <el-radio-button value="asc">升序</el-radio-button>
            <el-radio-button value="desc">降序</el-radio-button>
          </el-radio-group>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="70" align="center">
        <template #default="{ row }">
          <el-button size="small" text type="danger" @click="handleRemoveSort(row.attributeId)">
            删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- §29.6 默认排序说明 -->
    <div v-if="sortFields.length === 0" class="mt-3 p-3 bg-[var(--el-fill-color-light)] rounded text-xs text-[var(--el-text-color-secondary)]">
      未设置排序字段，系统将按默认排序：状态排序 → 创建时间倒序 → 修改时间倒序 → ID升序
    </div>

    <!-- §29.3 新增排序字段弹窗 -->
    <el-dialog v-model="addDialogVisible" title="选择排序属性" width="500px">
      <el-table
        ref="addTableRef"
        :data="filteredAttributes.filter(a => !sortedAttributeIds.has(a.id))"
        size="small"
        max-height="300"
        @selection-change="(rows: any[]) => selectedAttrIds = rows.map((r: any) => r.id)"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column prop="name" label="属性名称" />
        <el-table-column prop="englishName" label="英文名称" />
        <el-table-column prop="dataType" label="数据类型" width="100" />
      </el-table>

      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :disabled="selectedAttrIds.length === 0" @click="confirmAddSort">
          确定 ({{ selectedAttrIds.length }})
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.sort-config-panel {
  width: 100%;
}
</style>
