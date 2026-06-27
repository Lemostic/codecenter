<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';

defineOptions({ name: 'TpLeftTreeLayout' });

const props = withDefaults(
  defineProps<{
    /** 左侧默认宽度（px） */
    defaultWidth?: number;
    /** 拖动时可在 defaultWidth 基础上增加的最大距离（px），实际最大 = defaultWidth + maxExtraWidth */
    maxExtraWidth?: number;
    /** 拖动时的最小宽度（px），防止拖到不可用 */
    minWidth?: number;
    /** 是否折叠左侧，支持 v-model:collapsed */
    collapsed?: boolean;
    /** 持久化宽度的 localStorage key；不传则不持久化 */
    storageKey?: string;
    /** 折叠按钮 tooltip */
    collapseTitle?: string;
    /** 展开按钮 tooltip */
    expandTitle?: string;
  }>(),
  {
    defaultWidth: 300,
    maxExtraWidth: 200,
    minWidth: 200,
    collapsed: false,
    storageKey: '',
    collapseTitle: '收起',
    expandTitle: '展开',
  },
);

const emit = defineEmits<{
  (e: 'update:collapsed', value: boolean): void;
  (e: 'resize', width: number): void;
}>();

// ========== State ==========
const width = ref(props.defaultWidth);
const isDragging = ref(false);
const startX = ref(0);
const startWidth = ref(0);

// ========== Computed ==========
const maxWidth = computed(() => props.defaultWidth + props.maxExtraWidth);
const displayWidth = computed(() =>
  Math.max(props.minWidth, Math.min(maxWidth.value, width.value)),
);

// ========== Methods ==========
const handleMouseDown = (e: MouseEvent): void => {
  if (props.collapsed) return;
  isDragging.value = true;
  startX.value = e.clientX;
  startWidth.value = width.value;
  document.addEventListener('mousemove', handleMouseMove);
  document.addEventListener('mouseup', handleMouseUp);
};

const handleMouseMove = (e: MouseEvent): void => {
  if (!isDragging.value) return;
  const delta = e.clientX - startX.value;
  width.value = Math.max(
    props.minWidth,
    Math.min(maxWidth.value, startWidth.value + delta),
  );
};

const handleMouseUp = (): void => {
  if (!isDragging.value) return;
  isDragging.value = false;
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
  if (props.storageKey) {
    localStorage.setItem(props.storageKey, String(width.value));
  }
  emit('resize', width.value);
};

const handleToggle = (): void => {
  emit('update:collapsed', !props.collapsed);
};

// ========== Lifecycle ==========
onMounted(() => {
  if (!props.storageKey) return;
  const saved = localStorage.getItem(props.storageKey);
  if (!saved) return;
  const savedWidth = Number(saved);
  if (
    !Number.isNaN(savedWidth) &&
    savedWidth >= props.minWidth &&
    savedWidth <= maxWidth.value
  ) {
    width.value = savedWidth;
  }
});

onBeforeUnmount(() => {
  document.removeEventListener('mousemove', handleMouseMove);
  document.removeEventListener('mouseup', handleMouseUp);
});

defineExpose({
  /** 切换折叠/展开 */
  toggle: handleToggle,
});
</script>

<template>
  <div class="left-tree-layout flex h-full w-full overflow-hidden relative">
    <!-- 左侧面板（树） -->
    <div
      v-show="!collapsed"
      class="layout-left flex-shrink-0 h-full overflow-hidden bg-white"
      :style="{ width: `${displayWidth}px` }"
    >
      <slot name="left" />
    </div>

    <!-- 拖拽分隔条（仅作拖拽热区，按钮已上提至根容器） -->
    <div
      v-show="!collapsed"
      class="layout-divider h-full flex-shrink-0"
      :class="{ 'is-dragging': isDragging }"
      @mousedown="handleMouseDown"
    />

    <!-- 右侧面板（列表） -->
    <div class="layout-right flex-1 min-w-0 h-full relative overflow-hidden">
      <slot name="right" />
    </div>

    <!-- 折叠/展开按钮：根容器的直接子元素，z-index 高于一切子内容，避免被遮挡 -->
    <button
      v-if="!collapsed"
      class="layout-toggle-btn"
      type="button"
      :title="collapseTitle"
      :style="{ left: `${displayWidth}px` }"
      @mousedown.stop
      @click="handleToggle"
    >
      <el-icon><ArrowLeft /></el-icon>
    </button>
    <button
      v-else
      class="layout-expand-btn"
      type="button"
      :title="expandTitle"
      style="left: 0"
      @click="handleToggle"
    >
      <el-icon><ArrowRight /></el-icon>
    </button>
  </div>
</template>

<style scoped>
.layout-divider {
  width: 1px;
  background: var(--el-border-color);
  cursor: col-resize;
  transition: background 0.2s;
}

.layout-divider:hover,
.layout-divider.is-dragging {
  background: var(--el-color-primary);
}

.layout-toggle-btn,
.layout-expand-btn {
  position: absolute;
  top: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 48px;
  padding: 0;
  border: 1px solid var(--el-border-color);
  background: var(--el-color-white);
  color: var(--el-text-color-regular);
  cursor: pointer;
  z-index: 10;
  transform: translate(-50%, -50%);
  border-radius: 4px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
  transition: all 0.2s;
}

.layout-toggle-btn:hover,
.layout-expand-btn:hover {
  color: var(--el-color-primary);
  border-color: var(--el-color-primary);
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}
</style>
