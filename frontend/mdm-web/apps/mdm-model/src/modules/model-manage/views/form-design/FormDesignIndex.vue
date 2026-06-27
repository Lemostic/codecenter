<script setup lang="ts">
/**
 * FormDesignIndex - 填报设计主页（占位）
 *
 * 后续实现：表单设计器，包含左侧属性分组面板、
 * 右侧预览区域、显隐条件配置、布局样式等。
 */
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'FormDesignIndex' });

defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();
</script>

<template>
  <div class="p-4">
    <el-empty description="填报设计页面将在后续阶段实现" :image-size="80" />
  </div>
</template>
