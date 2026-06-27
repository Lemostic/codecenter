<script setup lang="ts" generic="T extends Record<string, any> = Record<string, any>">
/**
 * TpCardList - 数据驱动卡片列表容器（内置分页）
 *
 * 替代手写「el-table 不适用，需卡片视图」场景下的 grid + v-loading + el-pagination 拼装。
 * 与 TpTable API 风格保持一致：data + 作用域插槽（`#item`），组件内部负责 grid 布局、
 * 加载态、空态、分页四大通用能力。
 *
 * 调用方**只**需要：
 * 1. 通过 `:data` 传数据数组（v-model:currentPage / v-model:pageSize / :total / :loading）
 * 2. 通过 `#item` 插槽告诉组件「每张卡片长什么样」
 *
 * 内置 grid 布局：`repeat(auto-fill, minmax(gridMinWidth px, 1fr))`。
 * 列宽 / 间距可通过 prop 调整，调用方无需手写 `class="grid"` 和 `grid-template-columns`。
 *
 * 详见 docs/frontend_ai_coding_rules.md §14.3。
 */

import { computed } from 'vue';
import TpPagination from './TpPagination.vue';
import TpEmpty from './TpEmpty.vue';

// ========== 2. defineOptions ==========
defineOptions({ name: 'TpCardList' });

// ========== 3. Props ==========
const props = withDefaults(defineProps<{
  /** 数据数组 */
  data?: T[];
  /** 加载状态（v-loading 蒙层覆盖整个滚动区） */
  loading?: boolean;
  /** 数据总条数（驱动分页，0 时分页不渲染） */
  total?: number;
  /** 是否空态；不传则由组件根据 `!loading && data.length === 0` 自动推算 */
  empty?: boolean;
  /** 空态描述文案 */
  emptyText?: string;
  /** 当前页码（v-model:currentPage） */
  currentPage?: number;
  /** 每页条数（v-model:pageSize） */
  pageSize?: number;
  /** 可选每页条数 */
  pageSizes?: number[];
  /** grid 卡片最小宽度（px） */
  gridMinWidth?: number;
  /** grid 间距档位（Tailwind gap 数值：2/3/4/6/8） */
  gridGap?: 2 | 3 | 4 | 6 | 8;
  /** v-for key 取值字段名（默认读取 `id`） */
  keyField?: keyof T | 'id';
}>(), {
  data: () => [] as T[],
  loading: false,
  total: 0,
  empty: undefined,
  emptyText: '暂无数据',
  currentPage: 1,
  pageSize: 20,
  pageSizes: () => [10, 20, 50, 100],
  gridMinWidth: 280,
  gridGap: 4,
  keyField: 'id',
});

// ========== 4. Emits ==========
const emit = defineEmits<{
  (e: 'update:currentPage', value: number): void;
  (e: 'update:pageSize', value: number): void;
  (e: 'page-change', page: number): void;
  (e: 'size-change', size: number): void;
}>();

// ========== 6. Computed ==========

/** 是否空态：prop 显式传入优先，否则根据 data + loading 自动推算 */
const isEmpty = computed<boolean>(() => {
  if (props.empty !== undefined) return props.empty;
  return !props.loading && props.data.length === 0;
});

/** grid 容器 style：列宽（auto-fill + minmax） */
const gridStyle = computed(() => ({
  gridTemplateColumns: `repeat(auto-fill, minmax(${props.gridMinWidth}px, 1fr))`,
}));

/**
 * grid 间距 class：switch-case 字面量映射，确保 Tailwind JIT 能扫到所有取值。
 * 取值集合：2/3/4/6/8（与 Tailwind 默认 gap 档位对齐）。
 */
const gapClass = computed<string>(() => {
  switch (props.gridGap) {
    case 2: return 'gap-2';
    case 3: return 'gap-3';
    case 4: return 'gap-4';
    case 6: return 'gap-6';
    case 8: return 'gap-8';
    default: return 'gap-4';
  }
});

/** v-for key：优先取 `item[keyField]`，缺省回退到 index */
const itemKey = (item: T, index: number): string | number => {
  const v = (item as Record<string, unknown>)[props.keyField as string];
  return (v ?? index) as string | number;
};

// ========== 7. 方法 ==========
const handleCurrentChange = (page: number) => {
  emit('update:currentPage', page);
  emit('page-change', page);
};

const handleSizeChange = (size: number) => {
  emit('update:pageSize', size);
  emit('size-change', size);
};
</script>

<template>
  <div class="dm-card-list flex flex-col h-full">
    <!-- 空态 -->
    <div
      v-if="isEmpty"
      class="flex-1 min-h-0 flex items-center justify-center"
    >
      <slot name="empty">
        <TpEmpty :description="props.emptyText" />
      </slot>
    </div>

    <!-- 卡片网格滚动区 -->
    <div
      v-else
      v-loading="props.loading"
      class="dm-card-list__grid flex-1 min-h-0 overflow-auto grid content-start"
      :class="gapClass"
      :style="gridStyle"
    >
      <template v-for="(item, index) in props.data" :key="itemKey(item, index)">
        <slot name="item" :item="item" :index="index" />
      </template>
    </div>

    <!-- 分页（空态时不渲染；TpPagination 内部 total=0 时也不渲染） -->
    <TpPagination
      v-if="!isEmpty"
      :current-page="props.currentPage"
      :page-size="props.pageSize"
      :total="props.total"
      :page-sizes="props.pageSizes"
      @update:current-page="handleCurrentChange"
      @update:page-size="handleSizeChange"
    />
  </div>
</template>
