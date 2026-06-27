<script setup lang="ts">
/**
 * QualityRulePanel - 质量规则 Tab 面板（§47-50）
 *
 * 质量规则列表、新增/编辑/删除规则、数据入库校验、导入导出
 */
import { ref, reactive, computed, onMounted, watch } from 'vue';
import type { AxiosResponse } from 'axios';
import { Plus, Delete, Search, Download, Upload } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import {
  listQualityRule, createQualityRule, updateQualityRule,
  deleteQualityRule, batchDeleteQualityRule, setDataStorage,
} from '@/modules/model-design/api/quality-rule';
import type {
  QualityRuleVO, QualityRuleCreateDTO, QualityRuleUpdateDTO,
  QualityRuleQuery, RuleCondition,
} from '@/modules/model-design/types/quality-rule';
import {
  QUALITY_RULE_TYPE_OPTIONS, QUALITY_RULE_TYPE_LABEL,
  QUALITY_RULE_STATUS_LABEL,
} from '@/modules/model-design/types/quality-rule';
import type { QualityRuleType, CheckTiming } from '@/modules/model-design/types/quality-rule';
import type { ApiResponse, PaginatedResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'QualityRulePanel' });

const props = defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();

// ========== 状态 ==========
const loading = ref(false);
const rules = ref<QualityRuleVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const keyword = ref('');
const typeFilter = ref<QualityRuleType | ''>('');

const selectedIds = ref<ID[]>([]);

/** 编辑弹窗 */
const dialogVisible = ref(false);
const dialogTitle = ref('新增质量规则');
const isEditing = ref(false);
const editingId = ref<ID | null>(null);

/** Mock 属性列表 */
const availableAttributes = ref<{ id: string; name: string }[]>([
  { id: 'attr-001', name: '姓名' },
  { id: 'attr-002', name: '工号' },
  { id: 'attr-003', name: '手机号' },
  { id: 'attr-004', name: '邮箱' },
  { id: 'attr-005', name: '身份证号' },
]);

/** 表单 */
const formRef = ref();
const formData = reactive({
  name: '',
  ruleType: 'notNull' as QualityRuleType,
  description: '',
  conditions: [] as Omit<RuleCondition, 'attributeName'>[],
  checkTiming: ['onSave'] as CheckTiming[],
  dataStorage: true,
});

// ========== 质量六性分类 ==========
const QUALITY_DIMENSIONS: Record<QualityRuleType, string> = {
  notNull: '完整性',
  unique: '唯一性',
  format: '有效性',
  range: '有效性',
  regex: '有效性',
  custom: '一致性',
  crossTable: '一致性',
};

// ========== 列配置 ==========
const columns: TpTableColumn[] = [
  { prop: 'name', label: '规则名称', minWidth: 160, showOverflowTooltip: true },
  { prop: 'attributeNames', label: '属性名称', minWidth: 140 },
  { prop: 'description', label: '规则描述', minWidth: 200, showOverflowTooltip: true },
  { prop: 'ruleTypeLabel', label: '规则类型', minWidth: 100 },
  { prop: 'qualityDimension', label: '质量六性', minWidth: 80 },
  { prop: 'dataStorage', label: '数据入库校验', minWidth: 100 },
  { prop: 'actions', label: '操作', minWidth: 140, fixed: 'right' },
];

// ========== 计算属性 ==========
const isModelLocked = computed(() => props.modelStatus !== 'draft');
const hasSelected = computed(() => selectedIds.value.length > 0);

// ========== 方法 ==========

const loadRules = async () => {
  loading.value = true;
  try {
    const query: QualityRuleQuery = {
      modelId: props.modelId,
      keyword: keyword.value,
      ruleType: typeFilter.value || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    };
    const res = (await listQualityRule(query)) as unknown as AxiosResponse<ApiResponse<PaginatedResponse<QualityRuleVO>>>;
    const pageData = res.data?.data;
    if (pageData) {
      rules.value = pageData.rows ?? [];
      total.value = pageData.total ?? 0;
    }
  } catch (error) {
    console.error('[QualityRulePanel] load error', error);
  } finally {
    loading.value = false;
  }
};

const resetForm = () => {
  formData.name = '';
  formData.ruleType = 'notNull';
  formData.description = '';
  formData.conditions = [];
  formData.checkTiming = ['onSave'];
  formData.dataStorage = true;
  editingId.value = null;
  isEditing.value = false;
};

const handleAdd = () => {
  resetForm();
  dialogTitle.value = '新增质量规则';
  dialogVisible.value = true;
};

const handleEdit = (row: QualityRuleVO) => {
  resetForm();
  dialogTitle.value = '编辑质量规则';
  isEditing.value = true;
  editingId.value = row.id;
  formData.name = row.name;
  formData.ruleType = row.ruleType;
  formData.description = row.description;
  formData.conditions = row.conditions.map(c => ({
    attributeId: c.attributeId,
    ruleType: c.ruleType,
    description: c.description,
    params: c.params,
  }));
  formData.checkTiming = [...row.checkTiming];
  formData.dataStorage = row.dataStorage;
  dialogVisible.value = true;
};

const handleSave = async () => {
  try { await formRef.value?.validate(); } catch { return; }

  try {
    if (isEditing.value && editingId.value) {
      const dto: QualityRuleUpdateDTO = {
        id: editingId.value,
        name: formData.name,
        ruleType: formData.ruleType,
        description: formData.description,
        conditions: formData.conditions,
        checkTiming: formData.checkTiming,
        dataStorage: formData.dataStorage,
      };
      await updateQualityRule(dto);
      TpMessage.success('规则更新成功');
    } else {
      const dto: QualityRuleCreateDTO = {
        modelId: props.modelId,
        name: formData.name,
        ruleType: formData.ruleType,
        description: formData.description,
        conditions: formData.conditions,
        checkTiming: formData.checkTiming,
        dataStorage: formData.dataStorage,
      };
      await createQualityRule(dto);
      TpMessage.success('规则创建成功');
    }
    dialogVisible.value = false;
    await loadRules();
  } catch (error) {
    console.error('[QualityRulePanel] save error', error);
  }
};

const handleDelete = async (row: QualityRuleVO) => {
  try { await TpConfirm.delete(`确定要删除规则"${row.name}"吗？删除不影响历史评估结果。`); } catch { return; }
  try {
    await deleteQualityRule(row.id);
    TpMessage.success('删除成功');
    await loadRules();
  } catch (error) {
    console.error('[QualityRulePanel] delete error', error);
  }
};

const handleBatchDelete = async () => {
  if (!hasSelected.value) { TpMessage.warning('请至少选择一条记录'); return; }
  try { await TpConfirm.delete(`确定要删除选中的 ${selectedIds.value.length} 条规则吗？`); } catch { return; }
  try {
    await batchDeleteQualityRule(selectedIds.value);
    TpMessage.success('批量删除成功');
    selectedIds.value = [];
    await loadRules();
  } catch (error) {
    console.error('[QualityRulePanel] batch delete error', error);
  }
};

/** 数据入库校验切换（§48.4 即时保存） */
const handleDataStorageChange = async (row: QualityRuleVO, value: boolean) => {
  try {
    await setDataStorage(row.id, value);
    row.dataStorage = value;
    TpMessage.success(value ? '已开启数据入库校验' : '已关闭数据入库校验');
  } catch (error) {
    console.error('[QualityRulePanel] data storage error', error);
  }
};

/** 添加条件 */
const handleAddCondition = () => {
  formData.conditions.push({
    attributeId: '',
    ruleType: formData.ruleType,
    description: '',
  });
};

const handleRemoveCondition = (index: number) => {
  formData.conditions.splice(index, 1);
};

const handleSelectionChange = (rows: QualityRuleVO[]) => {
  selectedIds.value = rows.map(r => r.id);
};

const handlePageChange = () => { loadRules(); };
const handleSizeChange = () => { currentPage.value = 1; loadRules(); };
const handleSearch = () => { currentPage.value = 1; loadRules(); };

const formRules = {
  name: [{ required: true, message: '请输入规则名称', trigger: 'blur' }],
  ruleType: [{ required: true, message: '请选择规则类型', trigger: 'change' }],
};

onMounted(() => { loadRules(); });
watch(() => [props.modelId, props.modelVersion], () => { loadRules(); });
</script>

<template>
  <div class="quality-rule-panel flex flex-col h-full">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" :disabled="isModelLocked" @click="handleAdd">新增规则</el-button>
        <el-button :icon="Delete" :disabled="!hasSelected || isModelLocked" @click="handleBatchDelete">批量删除</el-button>
        <el-button :icon="Download" @click="TpMessage.info('导出功能将在下一阶段实现')">导出</el-button>
        <el-button :icon="Upload" @click="TpMessage.info('导入功能将在下一阶段实现')">导入</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-select v-model="typeFilter" placeholder="规则类型" clearable style="width: 130px" @change="handleSearch">
          <el-option v-for="opt in QUALITY_RULE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索规则名称" :prefix-icon="Search" clearable style="width: 180px" @clear="handleSearch" @keyup.enter="handleSearch" />
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <!-- 规则列表 -->
    <div class="flex-1 min-h-0 px-4">
      <TpTable
        :columns="columns"
        :data="rules"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        border
        stripe
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
      >
        <!-- 属性名称 -->
        <template #col-attributeNames="{ row }">
          <span>{{ (row as QualityRuleVO).attributeNames?.join('、') || '-' }}</span>
        </template>

        <!-- 质量六性 -->
        <template #col-qualityDimension="{ row }">
          <el-tag size="small">{{ QUALITY_DIMENSIONS[(row as QualityRuleVO).ruleType] || '-' }}</el-tag>
        </template>

        <!-- 数据入库校验（§48.4 复选框即时保存） -->
        <template #col-dataStorage="{ row }">
          <el-checkbox
            :model-value="(row as QualityRuleVO).dataStorage"
            @change="(val: boolean) => handleDataStorageChange(row as QualityRuleVO, val)"
          />
        </template>

        <!-- 操作列 -->
        <template #col-actions="{ row }">
          <el-button size="small" link type="primary" :disabled="isModelLocked" @click="handleEdit(row as QualityRuleVO)">编辑</el-button>
          <el-button size="small" link type="danger" :disabled="isModelLocked" @click="handleDelete(row as QualityRuleVO)">删除</el-button>
        </template>

        <template #empty>
          <TpEmpty state="no-data" description="暂无质量规则，请点击新增规则" />
        </template>
      </TpTable>
    </div>

    <!-- 新增/编辑弹窗（§48.1） -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="650px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="规则类型" prop="ruleType">
          <el-select v-model="formData.ruleType" style="width: 100%">
            <el-option v-for="opt in QUALITY_RULE_TYPE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="规则描述">
          <el-input v-model="formData.description" type="textarea" :rows="2" placeholder="规则描述（可自动生成）" />
        </el-form-item>

        <!-- 规则条件 -->
        <el-form-item label="规则条件">
          <div class="w-full">
            <div v-for="(cond, index) in formData.conditions" :key="index" class="flex items-center gap-2 mb-2">
              <el-select v-model="cond.attributeId" placeholder="选择属性" style="width: 180px">
                <el-option v-for="attr in availableAttributes" :key="attr.id" :label="attr.name" :value="attr.id" />
              </el-select>
              <el-button :icon="Delete" circle size="small" @click="handleRemoveCondition(index)" />
            </div>
            <el-button type="primary" link @click="handleAddCondition">+ 添加属性</el-button>
          </div>
        </el-form-item>

        <!-- 校验时机 -->
        <el-form-item label="校验时机">
          <el-checkbox-group v-model="formData.checkTiming">
            <el-checkbox value="onSave">保存时</el-checkbox>
            <el-checkbox value="onSubmit">提交时</el-checkbox>
            <el-checkbox value="onImport">导入时</el-checkbox>
            <el-checkbox value="scheduled">定时检查</el-checkbox>
          </el-checkbox-group>
        </el-form-item>

        <!-- 数据入库校验 -->
        <el-form-item label="入库校验">
          <el-switch v-model="formData.dataStorage" />
          <span class="ml-2 text-xs text-[var(--el-text-color-secondary)]">开启后，数据入库时将按此规则校验</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.quality-rule-panel { background: var(--el-bg-color); }
</style>
