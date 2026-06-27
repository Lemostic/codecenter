<script setup lang="ts">
/**
 * VersionCompareDialog - 版本对比弹窗 (§13)
 * 选择两个版本，对比属性差异
 */
import { ref, computed, onMounted } from 'vue';

defineOptions({ name: 'VersionCompareDialog' });

const props = defineProps<{
  modelId: string;
  modelName: string;
  visible: boolean;
}>();

const emit = defineEmits<{
  'update:visible': [val: boolean];
}>();

// Mock 版本数据
const versions = ref([
  { version: 1, label: 'V1', status: 'published', createdAt: '2024-01-15' },
  { version: 2, label: 'V2', status: 'published', createdAt: '2024-03-20' },
  { version: 3, label: 'V3', status: 'draft', createdAt: '2024-05-10' },
]);

// Mock 属性数据(不同版本)
const versionAttributes: Record<number, { id: string; name: string; englishName: string; dataType: string; length: number; status: string }[]> = {
  1: [
    { id: 'a1', name: '供应商名称', englishName: 'M_NAME', dataType: 'VARCHAR', length: 200, status: 'enabled' },
    { id: 'a2', name: '供应商编码', englishName: 'M_CODE', dataType: 'VARCHAR', length: 50, status: 'enabled' },
    { id: 'a3', name: '联系电话', englishName: 'M_PHONE', dataType: 'VARCHAR', length: 20, status: 'enabled' },
  ],
  2: [
    { id: 'a1', name: '供应商名称', englishName: 'M_NAME', dataType: 'VARCHAR', length: 200, status: 'enabled' },
    { id: 'a2', name: '供应商编码', englishName: 'M_CODE', dataType: 'VARCHAR', length: 100, status: 'enabled' },
    { id: 'a3', name: '联系电话', englishName: 'M_PHONE', dataType: 'VARCHAR', length: 30, status: 'enabled' },
    { id: 'a4', name: '联系地址', englishName: 'M_ADDRESS', dataType: 'VARCHAR', length: 500, status: 'enabled' },
  ],
  3: [
    { id: 'a1', name: '供应商名称', englishName: 'M_NAME', dataType: 'VARCHAR', length: 200, status: 'enabled' },
    { id: 'a2', name: '供应商编码', englishName: 'M_CODE', dataType: 'VARCHAR', length: 100, status: 'enabled' },
    { id: 'a3', name: '联系电话', englishName: 'M_PHONE', dataType: 'VARCHAR', length: 30, status: 'enabled' },
    { id: 'a4', name: '联系地址', englishName: 'M_ADDRESS', dataType: 'VARCHAR', length: 500, status: 'enabled' },
    { id: 'a5', name: '注册资本', englishName: 'M_CAPITAL', dataType: 'DECIMAL', length: 18, status: 'enabled' },
  ],
};

// 选择的对比版本
const leftVersion = ref(2);
const rightVersion = ref(3);

// §13.4 模式切换
const compareMode = ref<'all' | 'diff'>('all');

// 对比结果
const compareResult = computed(() => {
  const leftAttrs = versionAttributes[leftVersion.value] || [];
  const rightAttrs = versionAttributes[rightVersion.value] || [];

  const allKeys = new Set([...leftAttrs.map(a => a.id), ...rightAttrs.map(a => a.id)]);
  const result: {
    id: string;
    name: string;
    leftAttr: typeof leftAttrs[0] | null;
    rightAttr: typeof rightAttrs[0] | null;
    diffs: string[];
    isNew: boolean;
    isRemoved: boolean;
  }[] = [];

  allKeys.forEach(id => {
    const left = leftAttrs.find(a => a.id === id) || null;
    const right = rightAttrs.find(a => a.id === id) || null;
    const diffs: string[] = [];

    if (left && right) {
      // §13.5 以高版本为参考，比较差异
      if (left.name !== right.name) diffs.push('name');
      if (left.englishName !== right.englishName) diffs.push('englishName');
      if (left.dataType !== right.dataType) diffs.push('dataType');
      if (left.length !== right.length) diffs.push('length');
      if (left.status !== right.status) diffs.push('status');
    }

    result.push({
      id,
      name: right?.name || left?.name || '',
      leftAttr: left,
      rightAttr: right,
      diffs,
      isNew: !left,
      isRemoved: !right,
    });
  });

  return result;
});

// 过滤后的对比结果(差异模式)
const filteredResult = computed(() => {
  if (compareMode.value === 'diff') {
    return compareResult.value.filter(r => r.diffs.length > 0 || r.isNew || r.isRemoved);
  }
  return compareResult.value;
});

const hasNoDiff = computed(() =>
  compareMode.value === 'diff' && filteredResult.value.length === 0,
);

// 检查某字段是否有差异
const hasDiff = (row: typeof compareResult.value[0], field: string) => row.diffs.includes(field);

const handleClose = () => {
  emit('update:visible', false);
};
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="`版本对比 - ${modelName}`"
    width="90%"
    top="5vh"
    @update:model-value="emit('update:visible', $event)"
  >
    <!-- §13.3 版本选择 -->
    <div class="flex items-center gap-4 mb-4">
      <div class="flex items-center gap-2">
        <span class="text-sm">对比版本：</span>
        <el-select v-model="leftVersion" class="w-32" size="small">
          <el-option v-for="v in versions" :key="v.version" :label="v.label" :value="v.version" />
        </el-select>
        <span class="text-sm">vs</span>
        <el-select v-model="rightVersion" class="w-32" size="small">
          <el-option v-for="v in versions" :key="v.version" :label="v.label" :value="v.version" />
        </el-select>
      </div>

      <!-- §13.4 模式切换 -->
      <el-radio-group v-model="compareMode" size="small">
        <el-radio-button value="all">全部属性</el-radio-button>
        <el-radio-button value="diff">差异属性</el-radio-button>
      </el-radio-group>
    </div>

    <!-- §13.4 无差异提示 -->
    <el-alert v-if="hasNoDiff" type="success" :closable="false" class="mb-4">
      两个版本无差异
    </el-alert>

    <!-- §13.5 对比表格 -->
    <el-table :data="filteredResult" border size="small" max-height="500">
      <el-table-column label="属性名称" width="180">
        <template #default="{ row }">
          <span>{{ row.name }}</span>
          <el-tag v-if="row.isNew" type="success" size="small" class="ml-1">新增</el-tag>
          <el-tag v-if="row.isRemoved" type="danger" size="small" class="ml-1">已删除</el-tag>
        </template>
      </el-table-column>

      <!-- 高版本(参考) -->
      <el-table-column :label="`V${rightVersion} (参考)`" min-width="200">
        <template #default="{ row }">
          <template v-if="row.rightAttr">
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'englishName') }">
              {{ row.rightAttr.englishName }}
            </span>
            <span class="mx-2">|</span>
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'dataType') }">
              {{ row.rightAttr.dataType }}
            </span>
            <span class="mx-2">|</span>
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'length') }">
              {{ row.rightAttr.length }}
            </span>
          </template>
          <span v-else class="text-[var(--el-text-color-placeholder)]">-</span>
        </template>
      </el-table-column>

      <!-- 低版本 -->
      <el-table-column :label="`V${leftVersion}`" min-width="200">
        <template #default="{ row }">
          <template v-if="row.leftAttr">
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'englishName') }">
              {{ row.leftAttr.englishName }}
            </span>
            <span class="mx-2">|</span>
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'dataType') }">
              {{ row.leftAttr.dataType }}
            </span>
            <span class="mx-2">|</span>
            <span :class="{ 'bg-[var(--el-color-warning-light-7)]': hasDiff(row, 'length') }">
              {{ row.leftAttr.length }}
            </span>
          </template>
          <span v-else class="text-[var(--el-text-color-placeholder)]">-</span>
        </template>
      </el-table-column>
    </el-table>

    <template #footer>
      <el-button @click="handleClose">关闭</el-button>
    </template>
  </el-dialog>
</template>
