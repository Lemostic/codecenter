<script setup lang="ts">
/**
 * SimilarityRuleList - 相似规则列表页（占位）
 *
 * 后续实现：相似规则配置列表，支持新增/编辑/删除规则，
 * 配置相似算法、属性权重、相似度阈值等。
 */
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'SimilarityRuleList' });

defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();
</script>

<template>
  <div class="p-4">
    <el-empty description="相似规则列表页将在后续阶段实现" :image-size="80" />
  </div>
</template>
