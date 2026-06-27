<script setup lang="ts">
/**
 * TpTable - 通用数据表格组件（内置分页）
 *
 * 封装 el-table + el-pagination，提供统一的列表页表格体验。
 * 自定义列内容通过 `#col-{prop}` 具名插槽实现。
 *
 * 操作列自动测量：
 * - 当列 `prop === 'actions'` 且未指定 `width` / `minWidth` 时，
 *   TpTable 会自动测量所有行中该列槽位内容的最大宽度（含按钮文字、间距），
 *   并将该宽度（含 8px 视觉缓冲）作为该列的固定列宽。
 * - 其他列如需自动测量，可显式声明 `autoSize: true`。
 */

import { ref, onMounted, nextTick, watch } from 'vue';
import type { TableInstance } from 'element-plus';
import TpPagination from './TpPagination.vue';

// ========== 2. defineOptions ==========
defineOptions({ name: 'TpTable' });

// ========== 3. Props 定义 ==========

/** 列配置 */
export interface TpTableColumn {
  /** 字段名（对应 row 中的 key） */
  prop?: string;
  /** 列标题 */
  label: string;
  /** 最小宽度 */
  minWidth?: string | number;
  /** 固定宽度 */
  width?: string | number;
  /** 超出显示 tooltip */
  showOverflowTooltip?: boolean;
  /** 固定列位置 */
  fixed?: boolean | 'left' | 'right';
  /**
   * 是否按内容自动计算列宽。
   * 默认约定：`prop === 'actions'` 且未指定 width / minWidth 时启用。
   * 其他列可显式声明 `autoSize: true` 启用自动测量。
   */
  autoSize?: boolean;
}

interface Props {
  /** 列配置数组 */
  columns: TpTableColumn[];
  /** 表格数据 */
  data: Record<string, any>[];
  /** 加载状态 */
  loading?: boolean;
  /** 数据总条数（用于分页，0 则不显示分页） */
  total?: number;
  /** 可选分页大小 */
  pageSizes?: number[];
  /** 是否显示边框 */
  border?: boolean;
  /** 是否斑马纹 */
  stripe?: boolean;
}

const props = withDefaults(defineProps<Props & {
  /** 当前页码（v-model:currentPage） */
  currentPage?: number;
  /** 每页条数（v-model:pageSize） */
  pageSize?: number;
}>(), {
  loading: false,
  total: 0,
  currentPage: 1,
  pageSize: 20,
  pageSizes: () => [10, 20, 50, 100],
  stripe: false,
});

// ========== 4. Emits 定义 ==========
const emit = defineEmits<{
  (e: 'update:currentPage', value: number): void;
  (e: 'update:pageSize', value: number): void;
  (e: 'page-change', page: number): void;
  (e: 'size-change', size: number): void;
}>();

// ========== 5. 响应式数据 ==========

/** el-table 实例引用（用于 doLayout 重布局） */
const tableRef = ref<TableInstance>();

/**
 * 自动测量得到的列宽：列索引 → 像素值（含 8px 视觉缓冲）。
 * 仅对 `shouldAutoSize(col) === true` 的列生效。
 */
const autoSizeWidths = ref<Record<number, number>>({});

/** 操作列自动测量前的占位列宽（避免首屏抖动） */
const DEFAULT_AUTO_SIZE_WIDTH = 120;

// ========== 6. Computed 计算属性 ==========
// ========== 7. 方法 ==========

/** 判断某列是否需要自动按内容计算宽度 */
const shouldAutoSize = (col: TpTableColumn): boolean => {
  // 显式声明优先
  if (col.autoSize) return true;
  // 默认约定：操作列（prop='actions'）未指定 width/minWidth 时启用
  if (col.prop === 'actions' && col.width == null && col.minWidth == null) return true;
  return false;
};

/**
 * 测量指定列在所有数据行中槽位内容的自然宽度（像素）。
 *
 * 关键：脱离 el-table 的列宽约束。
 * 把 cell 的 .cell 容器临时移动到 body 下的隐藏 measurer（无宽度限制），
 * 读取 scrollWidth 后立即移回原位。
 * 由于移动和移回在同一同步代码块内完成，浏览器不会重绘，用户看不到闪烁。
 */
const measureColumnContentWidth = (colIndex: number): number => {
  const el = tableRef.value?.$el as HTMLElement | undefined;
  if (!el) return 0;
  const rows = el.querySelectorAll('.el-table__body tr');
  if (!rows.length) return 0;

  // 创建隐藏的测量容器（无宽度限制）
  const measurer = document.createElement('div');
  measurer.style.cssText = [
    'position: absolute',
    'top: -9999px',
    'left: -9999px',
    'visibility: hidden',
    'pointer-events: none',
    'z-index: -1',
    'white-space: nowrap',
  ].join(';');

  // 取首个 .cell 的计算样式，把 padding/font 等继承不到的属性同步到 measurer
  //（el-table 的 .cell padding 是上下文 CSS 规则，移出后丢失）
  const firstCellInner = rows[0].querySelectorAll('td')[colIndex]?.querySelector('.cell') as HTMLElement | null;
  if (firstCellInner) {
    const cs = getComputedStyle(firstCellInner);
    measurer.style.padding = cs.padding;
  }
  document.body.appendChild(measurer);

  let max = 0;
  rows.forEach((row) => {
    const cell = row.querySelectorAll('td')[colIndex] as HTMLElement | undefined;
    if (!cell) return;
    const inner = cell.querySelector('.cell') as HTMLElement | null;
    if (!inner) return;
    const originalParent = inner.parentElement;
    if (!originalParent) return;

    // 同步：移出 → 临时改 inline-block → 测量 → 还原 → 移回
    measurer.appendChild(inner);
    const prevDisplay = inner.style.display;
    inner.style.display = 'inline-block';
    const w = measurer.scrollWidth;
    inner.style.display = prevDisplay;
    if (w > max) max = w;
    originalParent.appendChild(inner);
  });

  document.body.removeChild(measurer);
  return max;
};

/**
 * 重新测量所有 autoSize 列的宽度，并触发表格重新布局。
 *
 * 实现策略：move-to-measurer（见 measureColumnContentWidth），不需 2-pass。
 * - 测量阶段不修改列宽，所以不会污染测量结果。
 * - 移动和移回在同一同步块内完成，浏览器不重绘，无视觉闪烁。
 * - 用 updateToken 防止旧调用覆盖新调用。
 */
let updateToken = 0;

const updateAutoSizeWidths = async () => {
  const hasAutoSize = props.columns.some(shouldAutoSize);
  if (!hasAutoSize) return;

  const myToken = ++updateToken;

  // 等待首屏 el-table 渲染完毕（不阻塞后续调用，仅令牌校验）
  await nextTick();
  if (myToken !== updateToken) return;
  await new Promise<void>((r) => requestAnimationFrame(() => r()));
  if (myToken !== updateToken) return;

  const updates: Record<number, number> = {};
  props.columns.forEach((col, idx) => {
    if (shouldAutoSize(col)) {
      const w = measureColumnContentWidth(idx);
      if (w > 0) {
        // 8px 右侧缓冲（按钮与 cell 边界的视觉间距）
        updates[idx] = w + 8;
      } else {
        // 测量失败（如暂无数据），回退到默认
        updates[idx] = DEFAULT_AUTO_SIZE_WIDTH;
      }
    }
  });

  autoSizeWidths.value = updates;
  await nextTick();
  tableRef.value?.doLayout();
};

/** 判断某列是否有自定义插槽 */
const hasSlot = (col: TpTableColumn): boolean => !!col.prop;

/** 获取列的插槽名 */
const getSlotName = (col: TpTableColumn): string => `col-${col.prop}`;

const handleCurrentChange = (page: number) => {
  emit('update:currentPage', page);
  emit('page-change', page);
};

const handleSizeChange = (size: number) => {
  emit('update:pageSize', size);
  emit('size-change', size);
};

// ========== 8. Watch 监听 ==========

// 数据变化时重新测量（flush: 'post' 等待 DOM 更新）
watch(() => props.data, () => {
  updateAutoSizeWidths();
}, { flush: 'post', deep: false });

// 列配置变化时重新测量
watch(() => props.columns, () => {
  updateAutoSizeWidths();
}, { flush: 'post', deep: true });

// ========== 9. 生命周期 ==========
onMounted(() => {
  updateAutoSizeWidths();
});

// ========== 10. defineExpose（显式暴露外部需要的方法）==========
/** 手动触发表格重新布局（如父容器尺寸变化时） */
defineExpose({
  /** 触发表格重布局 */
  doLayout: () => tableRef.value?.doLayout?.(),
});
</script>

<template>
  <div class="dm-table flex flex-col h-full">
    <!-- 表格区 -->
    <div class="flex-1 min-h-0 overflow-hidden">
      <el-table
        ref="tableRef"
        :data="props.data"
        v-loading="props.loading"
        :border="props.border"
        :stripe="props.stripe"
        height="100%"
        style="width: 100%"
      >
        <el-table-column
          v-for="(col, colIndex) in props.columns"
          :key="col.prop ?? col.label"
          :prop="col.prop"
          :label="col.label"
          :min-width="shouldAutoSize(col) ? undefined : col.minWidth"
          :width="shouldAutoSize(col) ? (autoSizeWidths[colIndex] ?? DEFAULT_AUTO_SIZE_WIDTH) : col.width"
          :show-overflow-tooltip="col.showOverflowTooltip"
          :fixed="col.fixed"
        >
          <!-- 自定义列内容插槽：#col-{prop} -->
          <template v-if="col.prop" #default="scope">
            <slot :name="getSlotName(col)" v-bind="scope">
              {{ col.prop ? scope.row[col.prop] : '' }}
            </slot>
          </template>
        </el-table-column>

        <template #empty>
          <el-empty description="暂无数据" />
        </template>
      </el-table>
    </div>

    <!-- 分页区 -->
    <TpPagination
      :current-page="props.currentPage"
      :page-size="props.pageSize"
      :total="props.total"
      :page-sizes="props.pageSizes"
      @update:current-page="handleCurrentChange"
      @update:page-size="handleSizeChange"
    />
  </div>
</template>
