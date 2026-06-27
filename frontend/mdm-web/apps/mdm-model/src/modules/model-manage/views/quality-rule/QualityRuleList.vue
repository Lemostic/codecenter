<script setup lang="ts">
/**
 * QualityRuleList - 质量规则列表页（占位）
 *
 * 后续实现：质量规则配置列表，支持新增/编辑/删除规则，
 * 配置校验条件、阈值、核准规则、数据入库等。
 */
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'QualityRuleList' });

defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();
</script>

<template>
  <div class="p-4">
    <el-empty description="质量规则列表页将在后续阶段实现" :image-size="80" />
  </div>
</template>
