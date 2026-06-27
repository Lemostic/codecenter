<script setup lang="ts">
/**
 * DataModelPicker - 数据模型选择弹窗
 *
 * 用于属性配置-关联对象，选择已生效的数据模型
 *
 * 用法：
 *   <DataModelPicker v-model="visible" @select="handleSelect" />
 */
defineOptions({ name: 'DataModelPicker' });

interface Props {
  modelValue: boolean;
}

const props = defineProps<Props>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'select', model: any): void;
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

// TODO: 实现数据模型选择弹窗
// - 弹窗显示已生效的模型列表
// - 支持搜索
// - 支持分页
// - 选中后触发 select 事件
</script>

<template>
  <el-dialog v-model="dialogVisible" title="选择数据模型" width="640px" destroy-on-close>
    <div class="data-model-picker__placeholder">
      <TpEmpty description="DataModelPicker 组件占位，功能开发中" />
    </div>
  </el-dialog>
</template>

<style scoped>
.data-model-picker__placeholder {
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
