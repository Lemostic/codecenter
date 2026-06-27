<script setup lang="ts">
/**
 * AttributeSelector - 属性选择器
 *
 * 用于相似规则/质量规则，选择模型的属性
 *
 * 用法：
 *   <AttributeSelector
 *     v-model="visible"
 *     :model-id="modelId"
 *     :multiple="true"
 *     @select="handleSelect"
 *   />
 */
defineOptions({ name: 'AttributeSelector' });

interface Props {
  modelValue: boolean;
  /** 模型 ID */
  modelId: string;
  /** 是否多选 */
  multiple?: boolean;
}

withDefaults(defineProps<Props>(), {
  multiple: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'select', attrs: any[]): void;
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

// TODO: 实现属性选择器
// - 弹窗显示模型的有效属性列表
// - 支持单选/多选
// - 支持搜索过滤
// - 选中后触发 select 事件
</script>

<template>
  <el-dialog v-model="dialogVisible" title="选择属性" width="480px" destroy-on-close>
    <div class="attribute-selector__placeholder">
      <TpEmpty description="AttributeSelector 组件占位，功能开发中" />
    </div>
  </el-dialog>
</template>

<style scoped>
.attribute-selector__placeholder {
  min-height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
