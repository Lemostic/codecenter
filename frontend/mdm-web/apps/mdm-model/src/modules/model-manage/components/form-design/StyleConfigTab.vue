<script setup lang="ts">
/**
 * StyleConfigTab - 属性样式配置 Tab (§25)
 * 控件类型、输入方式、宽高、属性提示、提示效果、公式字段
 */
import { computed, ref } from 'vue';
import type { FieldStyle, ControlType, InputMethod, TooltipEffect } from '@/modules/model-design/types/form-design';
import { CONTROL_TYPE_LABEL, INPUT_METHOD_LABEL, TOOLTIP_EFFECT_LABEL } from '@/modules/model-design/types/form-design';

defineOptions({ name: 'StyleConfigTab' });

interface Props {
  fieldStyle: FieldStyle | null;
  /** 属性名称(只读显示) */
  attributeName?: string;
  /** 数据类型(用于判断可选输入方式) */
  dataType?: string;
  /** 是否有关联对象 */
  hasRelatedObject?: boolean;
  /** 是否文件字段 */
  isFileField?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  attributeName: '',
  dataType: 'VARCHAR',
  hasRelatedObject: false,
  isFileField: false,
});

const emit = defineEmits<{
  change: [style: Partial<FieldStyle>];
}>();

const controlTypeOptions = computed(() =>
  Object.entries(CONTROL_TYPE_LABEL).map(([value, label]) => ({
    value: value as ControlType,
    label,
  })),
);

// §25.4 根据数据类型和关联对象计算可用输入方式
const inputMethodOptions = computed(() => {
  const opts: { value: InputMethod; label: string; disabled?: boolean }[] = [];
  const dt = props.dataType?.toUpperCase() || '';

  if (props.isFileField) {
    // 文件类型 → 文件选择器(不可改)
    opts.push({ value: 'file-selector', label: '文件选择器', disabled: true });
    return opts;
  }

  if (dt.includes('TEXT') || dt.includes('CLOB')) {
    // 大文本 → 多行(不可改) + 宽高配置
    opts.push({ value: 'multi-line', label: '多行输入框', disabled: true });
    return opts;
  }

  if (dt.includes('DATE') || dt.includes('TIME')) {
    // 日期型 → 自动
    opts.push({ value: 'auto', label: '自动', disabled: true });
    return opts;
  }

  if (dt.includes('INT') || dt.includes('DECIMAL') || dt.includes('NUM') || dt.includes('FLOAT')) {
    // 数值型 → 单行(不可改)
    opts.push({ value: 'single-line', label: '单行输入框', disabled: true });
    return opts;
  }

  // 字符型
  opts.push({ value: 'single-line', label: '单行输入框' });
  opts.push({ value: 'multi-line', label: '多行输入框' });

  if (props.hasRelatedObject) {
    opts.push({ value: 'dropdown', label: '下拉框' });
    opts.push({ value: 'button-group', label: '按钮组' });
    opts.push({ value: 'dialog', label: '弹窗' });
  }

  return opts;
});

// §25.3 公式字段：大文本类型+非文件字段时显示
const showFormulaField = computed(() => {
  const dt = props.dataType?.toUpperCase() || '';
  return (dt.includes('TEXT') || dt.includes('CLOB')) && !props.isFileField;
});

// 是否显示高度配置(大文本/多行时)
const showHeightConfig = computed(() => {
  const method = props.fieldStyle?.inputMethod;
  return method === 'multi-line' || props.fieldStyle?.controlType === 'textarea';
});

const tooltipEffectOptions = computed(() =>
  Object.entries(TOOLTIP_EFFECT_LABEL).map(([value, label]) => ({
    value: value as TooltipEffect,
    label,
  })),
);

const handleChange = (key: keyof FieldStyle, value: unknown) => {
  const updates: Partial<FieldStyle> = { [key]: value };

  // §25.3 公式字段选中后 → 文本域+独占一行
  if (key === 'isFormula' && value === true) {
    updates.controlType = 'textarea';
    updates.fullRow = true;
  }

  emit('change', updates);
};
</script>

<template>
  <div class="style-config-tab">
    <!-- §25.2 顶部提示 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-xs">数据维护/查看页面将按配置渲染页面</div>
    </el-alert>

    <el-form label-position="top" size="small">
      <!-- §25.1 属性名称(只读) -->
      <el-form-item v-if="attributeName" label="属性名称">
        <el-input :model-value="attributeName" disabled />
      </el-form-item>

      <!-- 控件类型 -->
      <el-form-item label="控件类型">
        <el-select
          :model-value="fieldStyle?.controlType || 'text'"
          class="w-full"
          @update:model-value="handleChange('controlType', $event)"
        >
          <el-option
            v-for="opt in controlTypeOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </el-form-item>

      <!-- §25.3 公式字段 -->
      <el-form-item v-if="showFormulaField" label="公式字段">
        <el-switch
          :model-value="fieldStyle?.isFormula || false"
          @update:model-value="handleChange('isFormula', $event)"
        />
        <span class="ml-2 text-xs text-[var(--el-text-color-placeholder)]">
          选中后字段添加为文本域、独占一行
        </span>
      </el-form-item>

      <!-- §25.4 输入方式 -->
      <el-form-item label="输入方式">
        <el-select
          :model-value="fieldStyle?.inputMethod || 'single-line'"
          class="w-full"
          @update:model-value="handleChange('inputMethod', $event)"
        >
          <el-option
            v-for="opt in inputMethodOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
            :disabled="opt.disabled"
          />
        </el-select>
      </el-form-item>

      <!-- §25.5 输入框宽度 -->
      <el-form-item label="输入框宽度">
        <el-input model-value="两端对齐" disabled />
      </el-form-item>

      <!-- §25.6 输入框高度(多行/文本域时显示) -->
      <el-form-item v-if="showHeightConfig" label="输入框高度(倍)">
        <el-input-number
          :model-value="fieldStyle?.heightMultiple || 2"
          :min="1"
          :max="10"
          class="w-full"
          @update:model-value="handleChange('heightMultiple', $event)"
        />
      </el-form-item>

      <!-- 占位列数 -->
      <el-form-item label="占位列数">
        <el-input-number
          :model-value="fieldStyle?.colSpan || 1"
          :min="1"
          :max="4"
          class="w-full"
          @update:model-value="handleChange('colSpan', $event)"
        />
      </el-form-item>

      <!-- 占一行 -->
      <el-form-item label="占一行">
        <el-switch
          :model-value="fieldStyle?.fullRow || false"
          @update:model-value="handleChange('fullRow', $event)"
        />
      </el-form-item>

      <!-- §25.7 属性提示 -->
      <el-form-item label="属性提示">
        <el-input
          :model-value="fieldStyle?.tooltip || ''"
          type="textarea"
          :maxlength="200"
          show-word-limit
          :rows="2"
          placeholder="请输入属性提示（最多200字符）"
          @update:model-value="handleChange('tooltip', $event)"
        />
      </el-form-item>

      <!-- §25.8 提示效果 -->
      <el-form-item label="提示效果">
        <el-radio-group
          :model-value="fieldStyle?.tooltipEffect || 'text'"
          @update:model-value="handleChange('tooltipEffect', $event)"
        >
          <el-radio v-for="opt in tooltipEffectOptions" :key="opt.value" :value="opt.value">
            {{ opt.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- 占位提示 -->
      <el-form-item label="占位提示">
        <el-input
          :model-value="fieldStyle?.placeholder || ''"
          placeholder="请输入占位提示"
          @update:model-value="handleChange('placeholder', $event)"
        />
      </el-form-item>

      <!-- 只读 -->
      <el-form-item label="只读">
        <el-switch
          :model-value="fieldStyle?.readonly || false"
          @update:model-value="handleChange('readonly', $event)"
        />
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.style-config-tab {
  width: 100%;
}
</style>
