<script setup lang="ts">
/**
 * TpSectionTitle - 分组标题组件
 *
 * 用于详情页或表单页的分组标题展示，替代 el-divider + el-text 组合。
 * 支持通过 #actions 插槽在标题右侧放置操作按钮。
 */

defineOptions({ name: 'TpSectionTitle' });

withDefaults(defineProps<{
  /** 标题文案 */
  title: string;
  /** 是否显示底部分隔线 */
  divider?: boolean;
}>(), {
  divider: true,
});
</script>

<template>
  <div
    class="flex items-center justify-between mb-4"
    :class="{ 'pb-2 border-b border-[var(--el-border-color-light)]': divider }"
  >
    <div class="flex items-center gap-2">
      <span class="inline-block w-[3px] h-4 rounded-sm bg-[var(--el-color-primary)]" />
      <span class="text-sm font-semibold text-[var(--el-text-color-primary)]">
        {{ title }}
      </span>
    </div>
    <div v-if="$slots.actions" class="flex items-center gap-2">
      <slot name="actions" />
    </div>
  </div>
</template>
