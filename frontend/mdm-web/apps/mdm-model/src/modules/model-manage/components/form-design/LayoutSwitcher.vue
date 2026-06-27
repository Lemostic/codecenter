<script setup lang="ts">
/**
 * LayoutSwitcher - 布局列数切换
 */
defineOptions({ name: 'LayoutSwitcher' });

import { useI18n } from 'vue-i18n';

interface Props {
  modelValue: 1 | 2 | 3 | 4;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'update:modelValue': [val: 1 | 2 | 3 | 4];
}>();

const { t } = useI18n();

const options = [
  { value: 1 as const, label: t('modelDesign.formDesign.layoutColumns.one') },
  { value: 2 as const, label: t('modelDesign.formDesign.layoutColumns.two') },
  { value: 3 as const, label: t('modelDesign.formDesign.layoutColumns.three') },
  { value: 4 as const, label: t('modelDesign.formDesign.layoutColumns.four') },
];

const handleSelect = (val: 1 | 2 | 3 | 4) => {
  emit('update:modelValue', val);
};
</script>

<template>
  <div class="layout-switcher flex items-center gap-2">
    <span class="text-sm text-[var(--el-text-color-secondary)]">表单布局：</span>
    <el-radio-group :model-value="modelValue" @update:model-value="handleSelect">
      <el-radio-button v-for="opt in options" :key="opt.value" :value="opt.value">
        {{ opt.label }}
      </el-radio-button>
    </el-radio-group>
  </div>
</template>
