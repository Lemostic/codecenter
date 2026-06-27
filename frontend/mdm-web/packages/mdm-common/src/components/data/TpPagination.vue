<script setup lang="ts">
/**
 * TpPagination - 通用分页组件
 *
 * 封装 el-pagination，覆写样式以匹配 Figma 设计规范：
 * - 容器：白色背景、顶部 1px #E1E9F0 边框、48px 高度
 * - 统计区：「本页 X-Y 条」+「共 Z 条」
 * - 翻页按钮：28×28、borderRadius 2px
 *   - 上一页 / 下一页：#F5F7FA 浅蓝背景、无边框
 *   - 激活页码：#337BFF 描边 + 文字
 *   - 普通页码：#E9E9E9 描边、#585F66 文字
 * - 跳页输入框：56×28、2px 圆角、#E1E9F0 描边
 * - 每页条数选择器：100×28、2px 圆角、#E1E9F0 描边
 */

import { computed } from 'vue';

defineOptions({ name: 'TpPagination' });

// ========== Props ==========
const props = withDefaults(
  defineProps<{
    /** 当前页码 */
    currentPage?: number;
    /** 每页条数 */
    pageSize?: number;
    /** 数据总条数（0 则不渲染） */
    total?: number;
    /** 可选每页条数 */
    pageSizes?: number[];
  }>(),
  {
    currentPage: 1,
    pageSize: 20,
    total: 0,
    pageSizes: () => [10, 20, 50, 100],
  },
);

// ========== Emits ==========
const emit = defineEmits<{
  (e: 'update:currentPage', value: number): void;
  (e: 'update:pageSize', value: number): void;
  (e: 'page-change', page: number): void;
  (e: 'size-change', size: number): void;
}>();

// ========== Computed ==========

/** 当前页码双向绑定（受控模式，满足 el-pagination v-model 要求） */
const bindCurrentPage = computed({
  get: () => props.currentPage,
  set: (val: number) => {
    emit('update:currentPage', val);
    emit('page-change', val);
  },
});

/** 每页条数双向绑定（受控模式） */
const bindPageSize = computed({
  get: () => props.pageSize,
  set: (val: number) => {
    emit('update:pageSize', val);
    emit('size-change', val);
  },
});

/** 当前页起始序号 */
const pageStart = computed(() => {
  if (props.total === 0) return 0;
  return (props.currentPage - 1) * props.pageSize + 1;
});

/** 当前页结束序号 */
const pageEnd = computed(() => {
  return Math.min(props.currentPage * props.pageSize, props.total);
});
</script>

<template>
  <div v-if="props.total > 0" class="dm-pagination">
    <el-pagination
      v-model:current-page="bindCurrentPage"
      v-model:page-size="bindPageSize"
      :total="props.total"
      :page-sizes="props.pageSizes"
      layout="prev, pager, next, jumper, sizes"
    />
    <!-- 自定义统计区：本页 X-Y 条 + 共 Z 条 -->
    <div class="dm-pagination__stats">
      <span class="dm-pagination__stats-item">本页 {{ pageStart }}-{{ pageEnd }} 条</span>
      <span class="dm-pagination__stats-item">共 {{ props.total }} 条</span>
    </div>
  </div>
</template>

<style scoped>
.dm-pagination {
  width: 100%;
  height: 48px;
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: flex-end;
  padding: 4px 16px;
  background: var(--el-color-white);
  border-top: 1px solid #e1e9f0;
  box-sizing: border-box;
  flex-shrink: 0;
}

/* ===== el-pagination 根容器 ===== */
.dm-pagination :deep(.el-pagination) {
  display: flex;
  align-items: center;
  font-weight: 400;
  --el-pagination-button-width: 28px;
  --el-pagination-button-height: 28px;
  --el-pagination-button-bg-color: var(--el-color-white);
  --el-pagination-bg-color: var(--el-color-white);
  --el-pagination-text-color: #585f66;
  --el-pagination-button-color: #585f66;
  --el-pagination-hover-color: #337bff;
}

/* ===== 自定义统计区 ===== */
.dm-pagination__stats {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-shrink: 0;
}

.dm-pagination__stats-item {
  font-size: 12px;
  line-height: 24px;
  color: #585f66;
  white-space: nowrap;
}

/* ===== 每页条数选择器 (sizes) ===== */
.dm-pagination :deep(.el-pagination__sizes) {
  margin-left: 16px;
  flex-shrink: 0;
}

.dm-pagination :deep(.el-pagination__sizes .el-select) {
  width: 100px;
}

.dm-pagination :deep(.el-pagination__sizes .el-select .el-input) {
  --el-input-border-radius: 2px;
  --el-input-border-color: #e1e9f0;
  --el-input-text-color: #585f66;
  --el-input-font-size: 12px;
  height: 28px;
}

.dm-pagination :deep(.el-pagination__sizes .el-select .el-input .el-input__inner) {
  height: 28px;
  line-height: 28px;
  font-size: 12px;
  color: #585f66;
  border-radius: 2px;
  border-color: #e1e9f0;
}

.dm-pagination :deep(.el-pagination__sizes .el-select .el-input .el-input__wrapper) {
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e1e9f0 inset;
  height: 28px;
  padding: 0 8px;
}

/* ===== 翻页按钮容器 (prev / pager / next) ===== */
.dm-pagination :deep(.el-pager) {
  gap: 8px;
  margin: 0 8px;
}

/* 所有页码按钮（含数字、省略号） */
.dm-pagination :deep(.el-pager li) {
  min-width: 28px;
  width: 28px;
  height: 28px;
  line-height: 28px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 400;
  color: #585f66;
  background: var(--el-color-white);
  border: 1px solid #e9e9e9;
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

/* 普通页码 hover */
.dm-pagination :deep(.el-pager li:hover) {
  color: #337bff;
  border-color: #337bff;
}

/* 激活页码 */
.dm-pagination :deep(.el-pager li.is-active) {
  color: #337bff;
  background: var(--el-color-white);
  border: 1px solid #337bff;
  font-weight: 400;
}

/* 省略号按钮去掉边框 */
.dm-pagination :deep(.el-pager li.el-icon.more) {
  border-color: transparent;
  background: transparent;
}

.dm-pagination :deep(.el-pager li.el-icon.more:hover) {
  color: #337bff;
  border-color: transparent;
}

/* ===== 上一页 / 下一页按钮 ===== */
.dm-pagination :deep(.el-pagination__prev),
.dm-pagination :deep(.el-pagination__next) {
  width: 28px;
  height: 28px;
  min-width: 28px;
  border-radius: 2px;
  background: #f5f7fa;
  border: none;
  padding: 0;
}

.dm-pagination :deep(.el-pagination__prev .el-icon),
.dm-pagination :deep(.el-pagination__next .el-icon) {
  font-size: 12px;
  color: #585f66;
}

.dm-pagination :deep(.el-pagination__prev:hover),
.dm-pagination :deep(.el-pagination__next:hover) {
  background: #e8eaed;
}

.dm-pagination :deep(.el-pagination__prev:hover .el-icon),
.dm-pagination :deep(.el-pagination__next:hover .el-icon) {
  color: #337bff;
}

/* disabled 状态 */
.dm-pagination :deep(.el-pagination__prev.is-disabled),
.dm-pagination :deep(.el-pagination__next.is-disabled) {
  background: #f5f7fa;
  opacity: 0.5;
}

.dm-pagination :deep(.el-pagination__prev.is-disabled .el-icon),
.dm-pagination :deep(.el-pagination__next.is-disabled .el-icon) {
  color: #c0c4cc;
}

/* ===== 跳页区 (jumper) ===== */
.dm-pagination :deep(.el-pagination__jump) {
  font-size: 12px;
  line-height: 24px;
  color: #585f66;
  margin-left: 16px;
  flex-shrink: 0;
}

.dm-pagination :deep(.el-pagination__jump .el-input) {
  --el-input-border-radius: 2px;
  --el-input-border-color: #e1e9f0;
  --el-input-text-color: #585f66;
  --el-input-font-size: 14px;
  width: 56px;
  height: 28px;
  margin: 0 4px;
}

.dm-pagination :deep(.el-pagination__jump .el-input .el-input__wrapper) {
  border-radius: 2px;
  box-shadow: 0 0 0 1px #e1e9f0 inset;
  height: 28px;
  padding: 0 8px;
}

.dm-pagination :deep(.el-pagination__jump .el-input .el-input__inner) {
  height: 28px;
  line-height: 28px;
  font-size: 14px;
  color: #585f66;
  text-align: center;
  border-radius: 2px;
}
</style>
