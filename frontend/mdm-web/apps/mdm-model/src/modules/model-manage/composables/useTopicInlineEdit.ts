import { ref, computed } from 'vue';
import type { TopicVO, TopicCreateDTO } from '@/modules/model-design/types/topic';
import { createTopic, updateTopic, checkTopicNameUnique } from '@/modules/model-design/api/topic';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { useI18n } from 'vue-i18n';

const NAME_REGEX = /^[一-龥a-zA-Z0-9][一-龥a-zA-Z0-9_-]*$/;
const MAX_NAME_LEN = 50;
const MAX_DESC_LEN = 200;

/** 内联编辑扩展字段（UI 状态，不在 TopicVO 上） */
type TopicEditRow = Partial<TopicVO> & { isNew?: boolean; isEditing?: boolean };

export interface UseTopicInlineEditOptions {
  getMaxSortOrder: () => number;
  onRefresh: () => void;
}

export function useTopicInlineEdit(options: UseTopicInlineEditOptions) {
  const { t } = useI18n();
  const { getMaxSortOrder, onRefresh } = options;

  const editingRow = ref<TopicEditRow | null>(null);
  const newRowData = ref<TopicEditRow | null>(null);

  const isEditing = computed(() => editingRow.value !== null || newRowData.value !== null);

  function validateName(name: string, parentId: string | null, excludeId?: string): string | null {
    if (!name || name.trim() === '') {
      return t('modelDesign.topic.editor.rule.nameRequired') ?? '该输入项为必填项';
    }
    if (name.length > MAX_NAME_LEN) {
      return t('modelDesign.topic.editor.rule.nameMax') ?? '请最多输入50个字符';
    }
    if (!NAME_REGEX.test(name)) {
      return t('modelDesign.topic.editor.rule.nameFormat') ?? "请最多输入50个字符，字符支持汉字、英文字母、数字、'-'、'_'，且符号不能位于首位。";
    }
    return null;
  }

  function validateSortOrder(sortOrder: number): string | null {
    if (!sortOrder || sortOrder < 1) {
      return t('modelDesign.topic.editor.rule.sortOrderRequired') ?? '该输入项为必填项';
    }
    if (sortOrder > 9999) {
      return '排序号不能超过9999';
    }
    return null;
  }

  function validateDescription(desc: string): string | null {
    if (desc && desc.length > MAX_DESC_LEN) {
      return t('modelDesign.topic.editor.rule.descriptionMax') ?? '请最多输入200个字。';
    }
    return null;
  }

  function startNew(parentId: string | null): Partial<TopicVO> {
    const newRow: TopicEditRow = {
      id: '',
      name: '',
      parentId,
      sortOrder: getMaxSortOrder() + 1,
      description: '',
      isLeaf: true,
      level: 0,
      isNew: true,
      isEditing: true,
    };
    newRowData.value = newRow;
    return newRow;
  }

  function startEdit(row: TopicVO): void {
    editingRow.value = { ...row, isEditing: true };
  }

  function cancel(): void {
    editingRow.value = null;
    newRowData.value = null;
  }

  async function saveNew(row: Partial<TopicVO>): Promise<boolean> {
    const name = (row.name ?? '').trim();
    const sortOrder = row.sortOrder ?? 0;
    const description = row.description ?? '';

    const nameErr = validateName(name, row.parentId ?? null);
    if (nameErr) { TpMessage.error(nameErr); return false; }
    const sortErr = validateSortOrder(sortOrder);
    if (sortErr) { TpMessage.error(sortErr); return false; }
    const descErr = validateDescription(description);
    if (descErr) { TpMessage.error(descErr); return false; }

    try {
      const uniqueRes = await checkTopicNameUnique(name, row.parentId ?? null);
      if (uniqueRes.data?.data === false) {
        TpMessage.error(t('modelDesign.topic.editor.rule.nameDuplicate') ?? '该名称已存在');
        return false;
      }
    } catch {
      // 唯一性校验失败不影响保存
    }

    const dto: TopicCreateDTO = {
      name,
      parentId: row.parentId ?? null,
      sortOrder,
      description,
    };
    try {
      await createTopic(dto);
      TpMessage.success(t('modelDesign.topic.message.createSuccess') ?? '创建成功');
      newRowData.value = null;
      onRefresh();
      return true;
    } catch {
      TpMessage.error(t('modelDesign.topic.message.createFailed') ?? '创建失败');
      return false;
    }
  }

  async function saveEdit(row: TopicVO): Promise<boolean> {
    const name = (row.name ?? '').trim();
    const sortOrder = row.sortOrder ?? 0;
    const description = row.description ?? '';

    const nameErr = validateName(name, row.parentId ?? null, row.id);
    if (nameErr) { TpMessage.error(nameErr); return false; }
    const sortErr = validateSortOrder(sortOrder);
    if (sortErr) { TpMessage.error(sortErr); return false; }
    const descErr = validateDescription(description);
    if (descErr) { TpMessage.error(descErr); return false; }

    try {
      await updateTopic({ id: row.id, name, parentId: row.parentId, sortOrder, description });
      TpMessage.success(t('modelDesign.topic.message.updateSuccess') ?? '更新成功');
      editingRow.value = null;
      onRefresh();
      return true;
    } catch {
      TpMessage.error(t('modelDesign.topic.message.updateFailed') ?? '更新失败');
      return false;
    }
  }

  return {
    editingRow,
    newRowData,
    isEditing,
    startNew,
    startEdit,
    cancel,
    saveNew,
    saveEdit,
  };
}
