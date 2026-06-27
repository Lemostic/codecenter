<script setup lang="ts">
/**
 * VisibilityConfigPanel - 显隐条件配置面板
 *
 * 用于填报设计-显隐条件，配置 IF 表达式控制字段显隐
 *
 * 用法：
 *   <VisibilityConfigPanel
 *     v-model="conditions"
 *     :available-fields="fields"
 *     @change="handleChange"
 *   />
 */
defineOptions({ name: 'VisibilityConfigPanel' });

interface VisibilityCondition {
  /** 条件表达式，如 IF(GYS.GJ='中国') */
  expression: string;
  /** 隐藏的属性名列表 */
  hiddenFields: string[];
}

interface Props {
  modelValue?: VisibilityCondition[];
  /** 可用的字段列表 */
  availableFields?: { name: string; label: string }[];
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  availableFields: () => [],
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: VisibilityCondition[]): void;
  (e: 'change', v: VisibilityCondition[]): void;
}>();

// TODO: 实现显隐条件配置
// - 显示条件列表（按创建时间倒序）
// - 新增/编辑条件（IF 表达式编辑器）
// - 选择隐藏的字段
// - 删除条件
</script>

<template>
  <div class="visibility-config-panel">
    <div class="visibility-config-panel__header">
      <el-button type="primary" size="small">添加显隐规则</el-button>
    </div>
    <div class="visibility-config-panel__list">
      <!-- TODO: 条件列表 -->
    </div>
    <div class="visibility-config-panel__placeholder">
      <TpEmpty description="VisibilityConfigPanel 组件占位，功能开发中" />
    </div>
  </div>
</template>

<style scoped>
.visibility-config-panel {
  width: 100%;
}

.visibility-config-panel__header {
  margin-bottom: 12px;
}

.visibility-config-panel__placeholder {
  min-height: 150px;
  display: flex;
  align-items: center;
  justify-content: center;
}
</style>
