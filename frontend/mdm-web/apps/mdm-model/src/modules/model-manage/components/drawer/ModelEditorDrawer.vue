<script setup lang="ts">
import { ref, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import { ElMessage, type FormInstance, type FormRules } from 'element-plus';
import type { ModelType } from '@/modules/model-design/types/model';
import { MODEL_TYPE_OPTIONS } from '@/modules/model-design/types/model';
import {
  getModel,
  createModel,
  updateModel,
  checkModelNameUnique,
  checkModelCodeUnique,
  checkTableNameUnique,
} from '@/modules/model-design/api/model';

defineOptions({ name: 'ModelEditorDrawer' });

const { t } = useI18n();

const props = defineProps<{
  visible: boolean;
  modelId?: string;
}>();

const emit = defineEmits<{
  'update:visible': [val: boolean];
  'success': [];
}>();

// ========== Mock DataSource Options ==========
const DATASOURCE_OPTIONS = [
  { value: 'ds-1', label: 'MySQL数据源' },
  { value: 'ds-2', label: 'PostgreSQL数据源' },
  { value: 'ds-3', label: 'Oracle数据源' },
];

// ========== Form State ==========
const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const formData = ref({
  datasourceId: '',
  name: '',
  code: '',
  tableName: '',
  modelType: 'normal' as ModelType,
  description: '',
  standardFile: null as File | null,
});

// ========== Form Rules ==========
const formRules: FormRules = {
  datasourceId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  name: [
    { required: true, message: '请输入模型名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_一-龥]*$/, message: '仅允许中文、字母、数字和下划线', trigger: 'blur' },
    { max: 300, message: '不超过300字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入模型编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_一-龥]*$/, message: '仅允许中文、字母、数字和下划线', trigger: 'blur' },
    { max: 300, message: '不超过300字符', trigger: 'blur' },
  ],
  tableName: [
    { required: true, message: '请输入表名称', trigger: 'blur' },
    { pattern: /^[一-龥a-zA-Z][一-龥a-zA-Z0-9_]*$/, message: '须以字母或中文开头，仅允许中文、字母、数字和下划线', trigger: 'blur' },
    { max: 300, message: '不超过300字符', trigger: 'blur' },
  ],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  description: [{ max: 1200, message: '不超过1200字符', trigger: 'blur' }],
};

// ========== File Upload (Simple Mode) ==========
const fileInputRef = ref<HTMLInputElement>();
const uploadedFileName = ref('');

const handleFileChange = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (file) {
    formData.value.standardFile = file;
    uploadedFileName.value = file.name;
  }
};

const handleRemoveFile = () => {
  formData.value.standardFile = null;
  uploadedFileName.value = '';
  if (fileInputRef.value) {
    fileInputRef.value.value = '';
  }
};

// ========== Watch visible to reset form ==========
watch(() => props.visible, (val) => {
  if (val) {
    formRef.value?.resetFields();
    formData.value = {
      datasourceId: '',
      name: '',
      code: '',
      tableName: '',
      modelType: 'normal',
      description: '',
      standardFile: null,
    };
    uploadedFileName.value = '';
    loading.value = false;
    saving.value = false;
  }
});

// ========== Load model data when editing ==========
const loadData = async () => {
  if (!props.modelId) return;
  loading.value = true;
  try {
    const res = await getModel(props.modelId);
    const d = res.data?.data;
    if (d) {
      formData.value = {
        datasourceId: d.datasourceId ?? '',
        name: d.name,
        code: d.code,
        tableName: d.tableName,
        modelType: d.modelType,
        description: d.description ?? '',
        standardFile: null,
      };
    }
  } catch (error) {
    ElMessage.error(t('modelDesign.editor.message.loadFailed'));
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};

// Watch modelId to load data
watch(() => props.modelId, (val) => {
  if (val && props.visible) {
    loadData();
  }
}, { immediate: true });

// ========== Submit Handler ==========
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    saving.value = true;
    // Unique checks
    await checkModelNameUnique(formData.value.name, props.modelId);
    await checkModelCodeUnique(formData.value.code, props.modelId);
    await checkTableNameUnique(formData.value.tableName, formData.value.datasourceId, props.modelId);
    // Prepare data (exclude standardFile which is not part of DTO)
    const { standardFile: _omitted, ...submitData } = formData.value;
    // Note: topicId is intentionally excluded - the form does not include topic field
    const saveData = { ...submitData, topicId: '' as const };
    // Save
    if (props.modelId) {
      await updateModel({ id: props.modelId, ...saveData });
    } else {
      await createModel(saveData);
    }
    ElMessage.success(t('modelDesign.editor.message.saveSuccess'));
    emit('success');
    emit('update:visible', false);
  } catch (error) {
    if (error instanceof Error) {
      ElMessage.error(error.message);
    }
    console.error('[handleSubmit]', error);
  } finally {
    saving.value = false;
  }
};

// ========== Close Handler ==========
const handleClose = () => {
  emit('update:visible', false);
};
</script>

<template>
  <el-drawer
    :model-value="visible"
    direction="rtl"
    :size="560"
    :close-on-click-modal="false"
    destroy-on-close
    @update:model-value="emit('update:visible', $event)"
  >
    <template #header>{{ props.modelId ? t('modelDesign.editor.title') : t('modelDesign.editor.titleCreate') }}</template>
    <div v-loading="loading" class="flex flex-col h-full">
      <el-form
        ref="formRef"
        size="default"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        class="flex-1 overflow-auto"
      >
        <el-card shadow="never" class="mb-4">
          <div class="mb-4 text-base font-semibold" style="color: #1d1d1f;">{{ t('modelDesign.editor.section.basic') }}</div>
          <el-form-item :label="t('modelDesign.editor.label.datasource')" prop="datasourceId" required>
            <el-select v-model="formData.datasourceId" :placeholder="t('modelDesign.editor.placeholder.datasource')" size="default" clearable>
              <el-option v-for="opt in DATASOURCE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('modelDesign.editor.label.name')" prop="name" required>
            <el-input v-model="formData.name" :placeholder="t('modelDesign.editor.placeholder.name')" size="default" clearable maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item :label="t('modelDesign.editor.label.code')" prop="code" required>
            <el-input v-model="formData.code" :placeholder="t('modelDesign.editor.placeholder.code')" size="default" clearable maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item :label="t('modelDesign.editor.label.tableName')" prop="tableName" required>
            <el-input v-model="formData.tableName" :placeholder="t('modelDesign.editor.placeholder.tableName')" size="default" clearable maxlength="300" show-word-limit />
          </el-form-item>
          <el-form-item :label="t('modelDesign.editor.label.modelType')" prop="modelType" required>
            <el-select v-model="formData.modelType" :placeholder="t('modelDesign.editor.placeholder.modelType')" size="default">
              <el-option v-for="opt in MODEL_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item :label="t('modelDesign.editor.label.description')" prop="description">
            <el-input v-model="formData.description" type="textarea" :rows="3" :placeholder="t('modelDesign.editor.placeholder.description')" size="default" maxlength="1200" show-word-limit />
          </el-form-item>
        </el-card>
        <el-card shadow="never">
          <div class="mb-4 text-base font-semibold" style="color: #1d1d1f;">{{ t('modelDesign.editor.section.standardFile') }}</div>
          <el-button size="default" @click="fileInputRef?.click()">{{ t('modelDesign.editor.btn.upload') }}</el-button>
          <input ref="fileInputRef" type="file" class="hidden" @change="handleFileChange" />
          <div v-if="uploadedFileName" class="mt-2 flex items-center gap-2">
            <span class="text-sm text-gray-600">{{ uploadedFileName }}</span>
            <el-button size="small" link type="danger" @click="handleRemoveFile">{{ t('modelDesign.editor.btn.removeFile') }}</el-button>
          </div>
        </el-card>
      </el-form>
      <div class="flex-shrink-0 flex justify-end gap-2 pt-4 border-t border-[#e9e9e9]">
        <el-button size="default" @click="handleClose">{{ t('modelDesign.editor.btn.cancel') }}</el-button>
        <el-button size="default" type="primary" :loading="saving" @click="handleSubmit">{{ t('modelDesign.editor.btn.confirm') }}</el-button>
      </div>
    </div>
  </el-drawer>
</template>
