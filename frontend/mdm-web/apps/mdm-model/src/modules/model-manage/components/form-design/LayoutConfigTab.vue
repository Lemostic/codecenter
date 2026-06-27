<script setup lang="ts">
/**
 * LayoutConfigTab - 布局样式配置 Tab (§27)
 * 页面布局(列数) + 字段布局(上下/左右)
 */
import type { LayoutColumns, FieldLayout } from '@/modules/model-design/types/form-design';
import { FIELD_LAYOUT_LABEL } from '@/modules/model-design/types/form-design';

defineOptions({ name: 'LayoutConfigTab' });

interface Props {
  layoutColumns: LayoutColumns;
  fieldLayout?: FieldLayout;
}

const props = withDefaults(defineProps<Props>(), {
  fieldLayout: 'horizontal',
});

const emit = defineEmits<{
  'update:layoutColumns': [val: LayoutColumns];
  'update:fieldLayout': [val: FieldLayout];
}>();
</script>

<template>
  <div class="layout-config-tab">
    <!-- §27.2 顶部提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-xs">
        在主数据查看/维护页面，将按外观布局设置渲染页面；默认设置为：表单布局：双列，字段布局：左右
      </div>
    </el-alert>

    <el-form label-position="top" size="small">
      <!-- §27.3 页面布局 -->
      <el-form-item label="页面布局">
        <el-radio-group
          :model-value="layoutColumns"
          @update:model-value="emit('update:layoutColumns', $event)"
        >
          <el-radio :value="1">单列布局</el-radio>
          <el-radio :value="2">双列布局</el-radio>
          <el-radio :value="3">三列布局</el-radio>
          <el-radio :value="4">四列布局</el-radio>
        </el-radio-group>
        <div class="mt-2 flex gap-1 w-full">
          <div
            v-for="i in layoutColumns"
            :key="i"
            class="h-6 bg-[var(--el-color-primary-light-5)] rounded"
            :style="{ flex: 1 }"
          />
        </div>
      </el-form-item>

      <!-- §27.4 字段布局 -->
      <el-form-item label="字段布局">
        <el-radio-group
          :model-value="fieldLayout"
          @update:model-value="emit('update:fieldLayout', $event)"
        >
          <el-radio value="horizontal">左右</el-radio>
          <el-radio value="vertical">上下</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <!-- §27.5 布局说明 -->
    <div class="mt-4 p-3 bg-[var(--el-fill-color-light)] rounded">
      <div class="text-sm font-medium mb-2">布局说明</div>
      <div class="text-xs text-[var(--el-text-color-secondary)] space-y-1">
        <div>· 页面布局：控制每行控件个数</div>
        <div>· 字段布局：控制标题/输入框排列方式</div>
        <div class="mt-2">
          当前：{{ layoutColumns }}列 / {{ FIELD_LAYOUT_LABEL[fieldLayout as FieldLayout] || '左右' }}
        </div>
      </div>
      <!-- 字段布局预览 -->
      <div class="mt-3 p-2 border border-dashed border-[var(--el-border-color)] rounded">
        <div
          class="grid gap-2"
          :style="{ gridTemplateColumns: `repeat(${layoutColumns}, 1fr)` }"
        >
          <div
            v-for="i in layoutColumns * 2"
            :key="i"
            class="flex items-center gap-1"
            :class="fieldLayout === 'vertical' ? 'flex-col' : 'flex-row'"
          >
            <div class="bg-[var(--el-color-info-light-7)] rounded text-xs px-1 py-0.5 whitespace-nowrap">
              字段{{ i }}
            </div>
            <div class="flex-1 h-5 bg-[var(--el-fill-color)] rounded min-w-0" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.layout-config-tab {
  width: 100%;
}
</style>
