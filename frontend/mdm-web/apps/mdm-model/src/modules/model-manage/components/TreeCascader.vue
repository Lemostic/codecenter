<script setup lang="ts">
/**
 * TreeCascader - 树形级联选择器
 *
 * 用于主题域树等场景，支持层级级联选择
 *
 * 用法：
 *   <TreeCascader
 *     v-model="value"
 *     :data="treeData"
 *     :props="{ label: 'name', children: 'children' }"
 *     placeholder="请选择"
 *   />
 */
defineOptions({ name: 'TreeCascader' });

interface Props {
  modelValue?: string | string[];
  /** 树数据 */
  data: Record<string, any>[];
  /** el-tree 的 props 配置 */
  props?: { label?: string; children?: string; disabled?: string };
  /** 占位文本 */
  placeholder?: string;
  /** 是否多选 */
  multiple?: boolean;
  /** 是否显示完整路径 */
  showAllLevels?: boolean;
  /** 分隔符（multiple 时有效） */
  separator?: string;
}

withDefaults(defineProps<Props>(), {
  modelValue: undefined,
  data: () => [],
  props: () => ({ label: 'name', children: 'children' }),
  placeholder: '请选择',
  multiple: false,
  showAllLevels: true,
  separator: '/',
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: any): void;
  (e: 'change', v: any): void;
}>();

// TODO: 实现树形级联选择器
// - 使用 el-cascader 或 el-tree-select
// - 支持单选/多选
// - 支持 show-all-levels 显示完整路径
// - 支持搜索过滤
</script>

<template>
  <el-cascader
    :model-value="modelValue"
    :options="data"
    :props="props"
    :placeholder="placeholder"
    :show-all-levels="showAllLevels"
    :separator="separator"
    clearable
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="emit('change', $event)"
  />
</template>
