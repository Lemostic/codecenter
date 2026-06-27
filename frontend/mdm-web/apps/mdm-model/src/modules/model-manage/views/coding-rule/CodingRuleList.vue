<script setup lang="ts">
/**
 * CodingRuleList - 编码规则列表页（占位）
 *
 * 后续实现：编码规则列表，支持新增编码规则、
 * 码段管理（9种码段类型配置）等。
 */
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'CodingRuleList' });

defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();
</script>

<template>
  <div class="p-4">
    <el-empty description="编码规则列表页将在后续阶段实现" :image-size="80" />
  </div>
</template>
