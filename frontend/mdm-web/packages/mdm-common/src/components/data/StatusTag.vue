<script setup lang="ts">
import { computed } from 'vue';

/**
 * StatusTag - 通用状态标签
 *
 * 根据不同状态值显示对应颜色标签。提供常用状态映射默认值。
 *
 * 用法：
 *   <StatusTag :value="status" :map="statusMap" />
 *
 * 常用 map 示例：
 *   const statusMap: StatusMap = {
 *     draft: { label: '编辑中', type: 'info' },
 *     published: { label: '生效', type: 'success' },
 *     disabled: { label: '停用', type: 'danger' },
 *   }
 */
defineOptions({ name: 'StatusTag' });

export interface StatusConfig {
  label: string;
  type?: 'success' | 'warning' | 'danger' | 'info' | '';
  effect?: 'dark' | 'light' | 'plain';
}

export interface StatusMap {
  [key: string]: StatusConfig;
}

interface Props {
  /** 状态值 */
  value?: string | null;
  /** 状态映射配置，默认提供常见状态映射 */
  map?: StatusMap;
}

const props = withDefaults(defineProps<Props>(), {
  value: null,
  map: () => ({}),
});

/** 默认状态映射（可被 props.map 覆盖） */
const defaultMap: StatusMap = {
  draft: { label: '编辑中', type: 'info', effect: 'light' },
  editing: { label: '编辑中', type: 'info', effect: 'light' },
  published: { label: '生效', type: 'success', effect: 'light' },
  active: { label: '生效', type: 'success', effect: 'light' },
  enabled: { label: '启用', type: 'success', effect: 'light' },
  disabled: { label: '停用', type: 'danger', effect: 'light' },
  stopped: { label: '停用', type: 'danger', effect: 'light' },
  archived: { label: '归档', type: 'warning', effect: 'light' },
  deleted: { label: '已删除', type: 'info', effect: 'plain' },
  pending: { label: '待处理', type: 'warning', effect: 'light' },
  reviewing: { label: '审核中', type: 'warning', effect: 'light' },
  approved: { label: '已通过', type: 'success', effect: 'light' },
  rejected: { label: '已驳回', type: 'danger', effect: 'light' },
  success: { label: '成功', type: 'success', effect: 'light' },
  error: { label: '失败', type: 'danger', effect: 'light' },
  warning: { label: '警告', type: 'warning', effect: 'light' },
  info: { label: '提示', type: 'info', effect: 'light' },
};

const mergedMap = computed<StatusMap>(() => ({ ...defaultMap, ...props.map }));

const config = computed<StatusConfig>(() => mergedMap.value[props.value ?? ''] ?? {
  label: props.value ?? '',
  type: 'info',
  effect: 'light',
});
</script>

<template>
  <el-tag
    v-if="value != null"
    :type="config.type ?? 'info'"
    :effect="config.effect ?? 'light'"
    size="small"
    :hit="false"
    :closable="false"
    disable-transitions
  >
    {{ config.label }}
  </el-tag>
</template>
