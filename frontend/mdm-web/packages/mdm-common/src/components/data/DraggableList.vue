<script setup lang="ts">
import { ref, watch } from 'vue';

/**
 * DraggableList - 可拖拽排序列表
 *
 * 用于填报设计的属性分组拖拽排序。使用原生 HTML5 拖拽 API 实现，不引入额外依赖。
 *
 * 用法：
 *   <DraggableList v-model="items" :columns="columns" row-key="id" @reorder="onReorder" />
 *
 *   const columns = [
 *     { key: 'name', label: '名称' },
 *     { key: 'sort', label: '排序' },
 *   ]
 */
defineOptions({ name: 'DraggableList' });

interface Column {
  key: string;
  label: string;
  width?: string;
}

interface Props {
  /** 数据列表 */
  modelValue: Record<string, any>[];
  /** 列配置 */
  columns: Column[];
  /** 每行唯一标识字段 */
  rowKey?: string;
  /** 是否禁用拖拽 */
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  rowKey: 'id',
  disabled: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: Record<string, any>[]): void;
  (e: 'reorder', v: Record<string, any>[]): void;
}>();

const list = ref<Record<string, any>[]>([...props.modelValue]);
const draggingIndex = ref<number | null>(null);

watch(
  () => props.modelValue,
  (v) => {
    list.value = [...v];
  },
  { deep: true },
);

const handleDragStart = (index: number) => {
  if (props.disabled) return;
  draggingIndex.value = index;
};

const handleDragOver = (event: DragEvent, index: number) => {
  event.preventDefault();
};

const handleDrop = (event: DragEvent, targetIndex: number) => {
  event.preventDefault();
  if (draggingIndex.value === null || draggingIndex.value === targetIndex) return;
  const newList = [...list.value];
  const [dragged] = newList.splice(draggingIndex.value, 1);
  newList.splice(targetIndex, 0, dragged);
  list.value = newList;
  draggingIndex.value = null;
  emit('update:modelValue', newList);
  emit('reorder', newList);
};

const handleDragEnd = () => {
  draggingIndex.value = null;
};

const getCellValue = (row: Record<string, any>, col: Column) => {
  return row[col.key] ?? '';
};
</script>

<template>
  <div class="draggable-list" :class="{ 'is-disabled': disabled }">
    <!-- 表头 -->
    <div class="draggable-list__header flex items-center px-3 py-2 bg-[var(--el-fill-color-light)] border border-[var(--el-border-color-lighter)] rounded-t">
      <div class="w-6 flex-shrink-0" />
      <div
        v-for="col in columns"
        :key="col.key"
        class="flex-1 text-sm font-medium text-[var(--el-text-color-primary)]"
        :style="col.width ? { width: col.width } : {}"
      >
        {{ col.label }}
      </div>
    </div>

    <!-- 列表行 -->
    <div
      v-for="(row, index) in list"
      :key="row[rowKey] ?? index"
      draggable="true"
      class="draggable-list__row flex items-center px-3 py-2 border-b border-[var(--el-border-color-lighter)] cursor-grab"
      :class="{ 'is-dragging': draggingIndex === index }"
      @dragstart="handleDragStart(index)"
      @dragover="handleDragOver($event, index)"
      @drop="handleDrop($event, index)"
      @dragend="handleDragEnd"
    >
      <!-- 拖拽手柄 -->
      <div class="w-6 flex-shrink-0 text-[var(--el-text-color-placeholder)] cursor-grab">
        <el-icon><Rank /></el-icon>
      </div>
      <div
        v-for="col in columns"
        :key="col.key"
        class="flex-1 text-sm text-[var(--el-text-color-regular)] truncate pr-2"
        :style="col.width ? { width: col.width } : {}"
        :title="String(getCellValue(row, col))"
      >
        {{ getCellValue(row, col) }}
      </div>
    </div>

    <!-- 空状态 -->
    <div v-if="!list.length" class="py-8 text-center text-sm text-[var(--el-text-color-placeholder)]">
      暂无数据
    </div>
  </div>
</template>

<style scoped>
.draggable-list.is-disabled {
  pointer-events: none;
  opacity: 0.6;
}

.draggable-list__row:hover {
  background-color: var(--el-fill-color-light);
}

.draggable-list__row.is-dragging {
  opacity: 0.5;
  background-color: var(--el-fill-color);
}
</style>
