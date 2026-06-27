<script setup lang="ts">
/**
 * TpDynamicForm - 通用动态表单组件
 *
 * 基于 Formily + Element Plus，通过 JSON Schema 驱动渲染。
 * 支持常用字段类型：Input/InputNumber/Select/DatePicker/Radio/
 * Checkbox/Switch/Upload/Cascader 等
 *
 * 特性：
 * - v-model 数据双向绑定
 * - 暴露 validate() / resetFields() / getValues() 方法
 * - 支持自定义提交/取消按钮（showActions）
 */

import { ref, watch } from 'vue';
import type { ISchema } from '@formily/vue';
import { createForm, onFormValuesChange } from '@formily/core';
import { FormProvider, createSchemaField } from '@formily/vue';
import {
  FormItem,
  Input,
  InputNumber,
  Select,
  Checkbox,
  Switch,
  DatePicker,
  Upload,
  Radio,
  Cascader,
  FormLayout,
} from '@formily/element-plus';
import type { Form } from '@formily/core';

// ========== 2. defineOptions ==========
defineOptions({ name: 'TpDynamicForm' });

// ========== 3. Props 定义 ==========
interface Props {
  /** Formily Schema，描述所有字段的配置 */
  schema: ISchema;
  /** 表单数据（v-model） */
  modelValue?: Record<string, any>;
  /** 提交按钮文字 */
  submitText?: string;
  /** 取消按钮文字 */
  cancelText?: string;
  /** 是否显示底部的提交/取消按钮 */
  showActions?: boolean;
  /** 是否禁用整个表单 */
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  schema: () => ({ type: 'object', properties: {} }),
  modelValue: () => ({}),
  submitText: '提交',
  cancelText: '取消',
  showActions: false,
  disabled: false,
});

// ========== 4. Emits 定义 ==========
const emit = defineEmits<{
  (e: 'update:modelValue', values: Record<string, any>): void;
  (e: 'change', values: Record<string, any>): void;
  (e: 'submit', values: Record<string, any>): void;
}>();

// ========== 5. SchemaField 配置 ==========
const SchemaField = createSchemaField({
  components: {
    FormItem,
    Input,
    InputNumber,
    Select,
    Checkbox,
    Switch,
    DatePicker,
    Upload,
    Radio,
    Cascader,
    FormLayout,
  },
});

// ========== 6. Formily Form 实例 ==========
const form = createForm({
  initialValues: props.modelValue,
  effects: () => {
    // 监听表单值变化，同步到 v-model
    onFormValuesChange((formInst: Form) => {
      const values = formInst.values;
      emit('update:modelValue', values);
      emit('change', values);
    });
  },
});

// ========== 7. v-model 实现 ==========
// 外部传入 modelValue 变化时，同步到 form
watch(
  () => props.modelValue,
  (newVal) => {
    if (newVal && Object.keys(newVal).length > 0) {
      form.setValues(newVal);
    }
  },
  { immediate: true },
);

// ========== 8. 暴露的方法 ==========
/**
 * 校验整个表单
 * @returns Promise<{ valid: boolean; errors: Record<string, string> }>
 */
const validate = async () => {
  const result = await form.validate();
  const errors: Record<string, string> = {};
  let valid = true;

  if (result.errors) {
    valid = false;
    Object.keys(result.errors).forEach((key) => {
      errors[key] = result.errors[key]?.[0]?.messages?.[0] || '校验失败';
    });
  }

  return { valid, errors };
};

/** 重置表单到初始状态 */
const resetFields = () => {
  form.reset();
};

/** 获取当前表单值 */
const getValues = () => {
  return form.values;
};

// ========== 9. 按钮事件 ==========
const handleSubmit = async () => {
  const result = await validate();
  if (result.valid) {
    emit('submit', form.values);
  }
};

const handleCancel = () => {
  resetFields();
};

// ========== 10. defineExpose ==========
defineExpose({
  validate,
  resetFields,
  getValues,
});
</script>

<template>
  <div class="dm-dynamic-form">
    <FormProvider :schema="props.schema">
      <FormLayout :layout="'vertical'" :labelWidth="'100px'">
        <SchemaField :schema="props.schema" />
      </FormLayout>
    </FormProvider>

    <!-- 底部操作按钮 -->
    <div v-if="props.showActions" class="dm-dynamic-form__actions">
      <el-button @click="handleCancel">{{ props.cancelText }}</el-button>
      <el-button type="primary" @click="handleSubmit">{{ props.submitText }}</el-button>
    </div>
  </div>
</template>

<style scoped>
.dm-dynamic-form {
  width: 100%;
}

.dm-dynamic-form__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
