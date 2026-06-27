<script setup lang="ts">
/**
 * SimilarityRulePanel - 相似规则 Tab 面板（§33-41）
 *
 * 相似规则列表、新增/编辑规则、权重设置、保存时检查开关
 */
import { ref, reactive, computed, onMounted, watch } from 'vue';
import type { AxiosResponse } from 'axios';
import { Plus, Delete, Search, Refresh, Setting } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import {
  listSimilarityRule, createSimilarityRule, updateSimilarityRule,
  deleteSimilarityRule,
} from '@/modules/model-design/api/similarity-rule';
import type {
  SimilarityRuleVO, SimilarityRuleCreateDTO, SimilarityRuleUpdateDTO,
  SimilarityRuleQuery, AttributeWeight,
} from '@/modules/model-design/types/similarity-rule';
import {
  SIMILARITY_ALGORITHM_OPTIONS, SIMILARITY_ALGORITHM_LABEL,
  COMBINATION_MODE_OPTIONS,
} from '@/modules/model-design/types/similarity-rule';
import type { SimilarityAlgorithm, CombinationMode } from '@/modules/model-design/types/similarity-rule';
import type { ApiResponse, PaginatedResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'SimilarityRulePanel' });

const props = defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();

// ========== 状态 ==========
const loading = ref(false);
const rules = ref<SimilarityRuleVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);
const keyword = ref('');

/** 选中行 */
const selectedIds = ref<ID[]>([]);

/** 编辑弹窗 */
const dialogVisible = ref(false);
const dialogTitle = ref('新增相似规则');
const isEditing = ref(false);
const editingId = ref<ID | null>(null);

/** 权重设置抽屉 */
const weightDrawerVisible = ref(false);
const weightRuleId = ref<ID | null>(null);
const weightList = ref<AttributeWeight[]>([]);

/** 表单 */
const formRef = ref();
const formData = reactive({
  name: '',
  combinationMode: 'weighted' as CombinationMode,
  threshold: 80,
  checkOnSave: true,
  attributeWeights: [] as Omit<AttributeWeight, 'attributeName'>[],
});

/** Mock 属性列表（用于相似属性选择） */
const availableAttributes = ref<{ id: string; name: string }[]>([
  { id: 'attr-001', name: '姓名' },
  { id: 'attr-002', name: '工号' },
  { id: 'attr-003', name: '手机号' },
  { id: 'attr-004', name: '邮箱' },
  { id: 'attr-005', name: '身份证号' },
]);

// ========== 列配置 ==========
const columns: TpTableColumn[] = [
  { prop: 'name', label: '相似规则', minWidth: 160, showOverflowTooltip: true },
  { prop: 'combinationModeLabel', label: '组合模式', minWidth: 120 },
  { prop: 'attributeCount', label: '相似属性数', minWidth: 100 },
  { prop: 'threshold', label: '相似度阈值', minWidth: 100 },
  { prop: 'checkOnSave', label: '实时检查', minWidth: 100 },
  { prop: 'actions', label: '操作', minWidth: 200, fixed: 'right' },
];

// ========== 计算属性 ==========
const isModelLocked = computed(() => props.modelStatus !== 'draft');
const hasSelected = computed(() => selectedIds.value.length > 0);

// ========== 方法 ==========

/** 加载规则列表 */
const loadRules = async () => {
  loading.value = true;
  try {
    const query: SimilarityRuleQuery = {
      modelId: props.modelId,
      keyword: keyword.value,
      page: currentPage.value,
      pageSize: pageSize.value,
    };
    const res = (await listSimilarityRule(query)) as unknown as AxiosResponse<ApiResponse<PaginatedResponse<SimilarityRuleVO>>>;
    const pageData = res.data?.data;
    if (pageData) {
      rules.value = pageData.rows ?? [];
      total.value = pageData.total ?? 0;
    }
  } catch (error) {
    console.error('[SimilarityRulePanel] load error', error);
  } finally {
    loading.value = false;
  }
};

/** 重置表单 */
const resetForm = () => {
  formData.name = '';
  formData.combinationMode = 'weighted';
  formData.threshold = 80;
  formData.checkOnSave = true;
  formData.attributeWeights = [];
  editingId.value = null;
  isEditing.value = false;
};

/** 打开新增弹窗 */
const handleAdd = () => {
  resetForm();
  dialogTitle.value = '新增相似规则';
  dialogVisible.value = true;
};

/** 打开编辑弹窗 */
const handleEdit = (row: SimilarityRuleVO) => {
  resetForm();
  dialogTitle.value = '编辑相似规则';
  isEditing.value = true;
  editingId.value = row.id;
  formData.name = row.name;
  formData.combinationMode = row.combinationMode;
  formData.threshold = row.threshold;
  formData.checkOnSave = row.checkOnSave;
  formData.attributeWeights = row.attributeWeights.map(w => ({
    attributeId: w.attributeId,
    algorithm: w.algorithm,
    weight: w.weight,
  }));
  dialogVisible.value = true;
};

/** 保存规则 */
const handleSave = async () => {
  try {
    await formRef.value?.validate();
  } catch { return; }

  try {
    if (isEditing.value && editingId.value) {
      const dto: SimilarityRuleUpdateDTO = {
        id: editingId.value,
        name: formData.name,
        combinationMode: formData.combinationMode,
        threshold: formData.threshold,
        checkOnSave: formData.checkOnSave,
        attributeWeights: formData.attributeWeights,
      };
      await updateSimilarityRule(dto);
      TpMessage.success('规则更新成功');
    } else {
      const dto: SimilarityRuleCreateDTO = {
        modelId: props.modelId,
        name: formData.name,
        combinationMode: formData.combinationMode,
        threshold: formData.threshold,
        checkOnSave: formData.checkOnSave,
        attributeWeights: formData.attributeWeights,
      };
      await createSimilarityRule(dto);
      TpMessage.success('规则创建成功');
    }
    dialogVisible.value = false;
    await loadRules();
  } catch (error) {
    console.error('[SimilarityRulePanel] save error', error);
  }
};

/** 删除规则 */
const handleDelete = async (row: SimilarityRuleVO) => {
  try {
    await TpConfirm.delete(`确定要删除规则"${row.name}"吗？`);
  } catch { return; }

  try {
    await deleteSimilarityRule(row.id);
    TpMessage.success('删除成功');
    await loadRules();
  } catch (error) {
    console.error('[SimilarityRulePanel] delete error', error);
  }
};

/** 批量删除 */
const handleBatchDelete = async () => {
  if (!hasSelected.value) {
    TpMessage.warning('请至少选择一条记录');
    return;
  }
  try {
    await TpConfirm.delete(`确定要删除选中的 ${selectedIds.value.length} 条规则吗？`);
  } catch { return; }

  try {
    for (const id of selectedIds.value) {
      await deleteSimilarityRule(id);
    }
    TpMessage.success('批量删除成功');
    selectedIds.value = [];
    await loadRules();
  } catch (error) {
    console.error('[SimilarityRulePanel] batch delete error', error);
  }
};

/** 打开权重设置抽屉（§35.1） */
const handleOpenWeight = (row: SimilarityRuleVO) => {
  weightRuleId.value = row.id;
  weightList.value = row.attributeWeights.map(w => ({ ...w }));
  weightDrawerVisible.value = true;
};

/** 保存权重（§35.5） */
const handleSaveWeight = async () => {
  if (!weightRuleId.value) return;
  try {
    await updateSimilarityRule({
      id: weightRuleId.value,
      attributeWeights: weightList.value,
    });
    TpMessage.success('权重保存成功');
    weightDrawerVisible.value = false;
    await loadRules();
  } catch (error) {
    console.error('[SimilarityRulePanel] save weight error', error);
  }
};

/** 切换保存时检查（§33.3） */
const handleToggleCheckOnSave = async (row: SimilarityRuleVO, value: boolean) => {
  try {
    await updateSimilarityRule({ id: row.id, checkOnSave: value });
    row.checkOnSave = value;
    TpMessage.success(value ? '已开启保存时检查' : '已关闭保存时检查');
  } catch (error) {
    console.error('[SimilarityRulePanel] toggle check error', error);
  }
};

/** 添加相似属性到规则 */
const handleAddAttribute = () => {
  formData.attributeWeights.push({
    attributeId: '',
    algorithm: 'editDistance',
    weight: 0,
  });
};

/** 移除相似属性 */
const handleRemoveAttribute = (index: number) => {
  formData.attributeWeights.splice(index, 1);
};

/** 表格选中变化 */
const handleSelectionChange = (rows: SimilarityRuleVO[]) => {
  selectedIds.value = rows.map(r => r.id);
};

/** 分页 */
const handlePageChange = () => { loadRules(); };
const handleSizeChange = () => { currentPage.value = 1; loadRules(); };
const handleSearch = () => { currentPage.value = 1; loadRules(); };

// ========== 表单校验 ==========
const formRules = {
  name: [
    { required: true, message: '请输入规则名称', trigger: 'blur' },
    { max: 200, message: '规则名称不超过200字符', trigger: 'blur' },
  ],
  threshold: [
    { required: true, message: '请输入相似度阈值', trigger: 'blur' },
  ],
};

// ========== 生命周期 ==========
onMounted(() => { loadRules(); });
watch(() => [props.modelId, props.modelVersion], () => { loadRules(); });
</script>

<template>
  <div class="similarity-rule-panel flex flex-col h-full">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" :disabled="isModelLocked" @click="handleAdd">新增规则</el-button>
        <el-button :icon="Delete" :disabled="!hasSelected || isModelLocked" @click="handleBatchDelete">批量删除</el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input v-model="keyword" placeholder="搜索规则名称" :prefix-icon="Search" clearable style="width: 200px" @clear="handleSearch" @keyup.enter="handleSearch" />
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
        <!-- 相似度阈值 -->
        <template #col-threshold="{ row }">
          {{ row.threshold }}%
        </template>

        <!-- 实时检查（§33.3 开关按钮） -->
        <template #col-checkOnSave="{ row }">
          <el-switch
            :model-value="row.checkOnSave"
            :disabled="isModelLocked"
            @change="(val: boolean) => handleToggleCheckOnSave(row as SimilarityRuleVO, val)"
          />
        </template>

        <!-- 操作列 -->
        <template #col-actions="{ row }">
          <el-button size="small" link type="primary" :disabled="isModelLocked" @click="handleEdit(row as SimilarityRuleVO)">编辑</el-button>
          <el-button size="small" link type="primary" @click="handleOpenWeight(row as SimilarityRuleVO)">权重设置</el-button>
          <el-button size="small" link type="danger" :disabled="isModelLocked" @click="handleDelete(row as SimilarityRuleVO)">删除</el-button>
        </template>

        <template #empty>
          <TpEmpty state="no-data" description="暂无相似规则，请点击新增规则" />
        </template>
      </TpTable>
    </div>

    <!-- 新增/编辑弹窗（§34） -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="700px" destroy-on-close :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="组合模式">
          <el-select v-model="formData.combinationMode" style="width: 100%">
            <el-option v-for="opt in COMBINATION_MODE_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="相似度阈值" prop="threshold">
          <el-input-number v-model="formData.threshold" :min="0" :max="100" controls-position="right" style="width: 200px" />
          <span class="ml-2 text-xs text-[var(--el-text-color-secondary)]">%</span>
        </el-form-item>
        <el-form-item label="保存时检查">
          <el-switch v-model="formData.checkOnSave" />
          <span class="ml-2 text-xs text-[var(--el-text-color-secondary)]">开启后，新增保存数据时自动检查相似数据</span>
        </el-form-item>

        <!-- 相似属性配置（§34.2） -->
        <el-form-item label="相似属性">
          <div class="w-full">
            <div v-for="(item, index) in formData.attributeWeights" :key="index" class="flex items-center gap-2 mb-2">
              <el-select v-model="item.attributeId" placeholder="选择属性" style="width: 180px">
                <el-option v-for="attr in availableAttributes" :key="attr.id" :label="attr.name" :value="attr.id" />
              </el-select>
              <el-select v-model="item.algorithm" placeholder="选择算法" style="width: 150px">
                <el-option v-for="alg in SIMILARITY_ALGORITHM_OPTIONS" :key="alg.value" :label="alg.label" :value="alg.value" />
              </el-select>
              <el-input-number v-model="item.weight" :min="0" :max="100" placeholder="权重" controls-position="right" style="width: 120px" />
              <el-button :icon="Delete" circle size="small" @click="handleRemoveAttribute(index)" />
            </div>
            <el-button type="primary" link @click="handleAddAttribute">+ 添加属性</el-button>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>

    <!-- 权重设置抽屉（§35） -->
    <el-drawer v-model="weightDrawerVisible" title="权重设置" size="500px" direction="rtl">
      <div class="mb-4 text-xs text-[var(--el-text-color-secondary)]">
        <p>最终生效权重 = 该规则权重值 / 所有规则权重值之和 x 100%</p>
        <p>全部为空则平均加权；有任一规则设置权重后其他自动填充0</p>
      </div>
      <el-table :data="weightList" border size="small">
        <el-table-column prop="attributeName" label="属性名称" min-width="120">
          <template #default="{ row }">
            {{ availableAttributes.find(a => a.id === row.attributeId)?.name || row.attributeId }}
          </template>
        </el-table-column>
        <el-table-column label="规则权重" min-width="140">
          <template #default="{ row }">
            <el-input-number v-model="row.weight" :min="0" :max="100" size="small" controls-position="right" style="width: 100px" />
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="weightDrawerVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSaveWeight">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped>
.similarity-rule-panel {
  background: var(--el-bg-color);
}
</style>
