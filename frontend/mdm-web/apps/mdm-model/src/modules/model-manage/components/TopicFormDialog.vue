<script setup lang="ts">
/**
 * TopicFormDialog - 主题域新增/编辑弹窗
 *
 * 模块内复用组件（仅 TopicList 调用），放 components/ 而非 common/
 * 模式：
 *   - 'create' 时，topicId 忽略，提交调 createTopic
 *   - 'edit'   时，按 topicId 调 getTopic 加载详情，提交调 updateTopic
 *
 * 父级选择：使用 el-tree-select（懒加载），用于新增时变更父级
 */
import { ref, watch, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import {
  getTopic,
  createTopic,
  updateTopic,
  getTopicRootTree,
  getTopicChildren,
} from '@/modules/model-design/api/topic';
import type { TopicCreateDTO, TopicTreeNode } from '@/modules/model-design/types/topic';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'TopicFormDialog' });

const { t } = useI18n();

const props = withDefaults(
  defineProps<{
    /** v-model 绑定：弹窗可见性 */
    modelValue?: boolean;
    /** 模式：create 新增 / edit 编辑 */
    mode?: 'create' | 'edit';
    /** 编辑模式的 id */
    topicId?: ID;
    /** 新增模式默认父级（用户可在对话框内调整） */
    defaultParentId?: ID | null;
  }>(),
  {
    modelValue: false,
    mode: 'create',
    topicId: '',
    defaultParentId: null,
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

// ========== 表单 ==========
const formRef = ref<FormInstance>();
const saving = ref(false);
const loadingDetail = ref(false);

interface FormState {
  name: string;
  parentId: ID | null;
  sortOrder: number;
  description: string;
}

const initialForm = (): FormState => ({
  name: '',
  parentId: props.defaultParentId,
  sortOrder: 1,
  description: '',
});

const formData = ref<FormState>(initialForm());

const rules: FormRules<FormState> = {
  name: [
    { required: true, message: t('modelDesign.topic.editor.rule.nameRequired'), trigger: 'blur' },
    { max: 50, message: t('modelDesign.topic.editor.rule.nameMax'), trigger: 'blur' },
  ],
  description: [
    { max: 200, message: t('modelDesign.topic.editor.rule.descriptionMax'), trigger: 'blur' },
  ],
  sortOrder: [
    { type: 'number', message: t('modelDesign.topic.editor.rule.sortOrderNumber'), trigger: 'blur' },
  ],
};

const dialogTitle = computed(() =>
  props.mode === 'create'
    ? t('modelDesign.topic.editor.titleCreate')
    : t('modelDesign.topic.editor.titleEdit'),
);

// ========== 父级树懒加载 ==========
// el-tree-select 的 LoadFunction 类型与项目实际使用的轻量签名不兼容，
// 这里使用宽松类型（与 metamanage-datatype/EditorDialog 保持一致）。
type TreeLoadResolve = (data: TopicTreeNode[]) => void;
const loadParentTree = (
  node: { level: number; data: TopicTreeNode },
  resolve: TreeLoadResolve,
) => {
  let res: Promise<unknown> | undefined;
  if (node.level === 0) {
    res = getTopicRootTree() as unknown as Promise<unknown>;
  } else {
    res = getTopicChildren(node.data.id) as unknown as Promise<unknown>;
  }
  res
    .then((r) => {
      const list = ((r as { data?: { data?: TopicTreeNode[] } })?.data?.data ?? []) as TopicTreeNode[];
      // 编辑模式下排除自己（避免循环引用）
      const filtered = props.mode === 'edit'
        ? list.filter((n) => String(n.id) !== String(props.topicId))
        : list;
      resolve(filtered);
    })
    .catch(() => resolve([]));
};

// ========== 弹窗打开时加载数据 ==========
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return;
    if (props.mode === 'create') {
      formData.value = initialForm();
    } else if (props.mode === 'edit' && props.topicId) {
      loadingDetail.value = true;
      try {
        const res = await getTopic(props.topicId);
        const t = res.data?.data;
        if (t) {
          formData.value = {
            name: t.name,
            parentId: t.parentId,
            sortOrder: t.sortOrder,
            description: t.description ?? '',
          };
        }
      } catch (error) {
        TpMessage.error(t('modelDesign.topic.message.loadDetailFailed'));
        console.error('[loadTopicDetail]', error);
      } finally {
        loadingDetail.value = false;
      }
    }
  },
);

// ========== 保存 ==========
const handleSave = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
  } catch {
    return;
  }
  saving.value = true;
  try {
    const dto: TopicCreateDTO = {
      name: formData.value.name,
      parentId: formData.value.parentId,
      sortOrder: formData.value.sortOrder,
      description: formData.value.description,
    };
    if (props.mode === 'create') {
      await createTopic(dto);
      TpMessage.success(t('modelDesign.topic.message.createSuccess'));
    } else {
      await updateTopic({ id: props.topicId, ...dto });
      TpMessage.success(t('modelDesign.topic.message.updateSuccess'));
    }
    emit('success');
    emit('update:modelValue', false);
  } catch (error) {
    TpMessage.error(props.mode === 'create' ? t('modelDesign.topic.message.createFailed') : t('modelDesign.topic.message.updateFailed'));
    console.error('[handleSave]', error);
  } finally {
    saving.value = false;
  }
};

const handleCancel = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="dialogTitle"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="!saving"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form
      v-if="modelValue"
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      v-loading="loadingDetail"
    >
      <el-form-item :label="t('modelDesign.topic.editor.label.name')" prop="name" required>
        <el-input
          v-model="formData.name"
          :placeholder="t('modelDesign.topic.editor.placeholder.name')"
          maxlength="50"
          show-word-limit
        />
      </el-form-item>

      <el-form-item :label="t('modelDesign.topic.editor.label.parent')" prop="parentId">
        <el-tree-select
          v-model="formData.parentId"
          :load="loadParentTree as any"
          :props="{ label: 'name', isLeaf: 'isLeaf' }"
          node-key="id"
          lazy
          check-strictly
          clearable
          :placeholder="t('modelDesign.topic.editor.placeholder.parent')"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item :label="t('modelDesign.topic.editor.label.sortOrder')" prop="sortOrder">
        <el-input-number
          v-model="formData.sortOrder"
          :min="1"
          :max="9999"
          :step="1"
          controls-position="right"
        />
      </el-form-item>

      <el-form-item :label="t('modelDesign.topic.editor.label.description')" prop="description">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="3"
          :placeholder="t('modelDesign.topic.editor.placeholder.description')"
          maxlength="200"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button size="default" :disabled="saving" @click="handleCancel">{{ t('common.cancel') }}</el-button>
      <el-button size="default" type="primary" :loading="saving" @click="handleSave">{{ t('common.confirm') }}</el-button>
    </template>
  </el-dialog>
</template>
