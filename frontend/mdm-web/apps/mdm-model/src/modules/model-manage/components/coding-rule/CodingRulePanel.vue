<script setup lang="ts">
/**
 * CodingRulePanel - 编码规则 Tab 面板（§42-46）
 *
 * 布局：顶部标题栏 + 筛选栏 + 表格 + 分页
 * - 顶部：标题 + 新增按钮
 * - 筛选：规则类型 + 状态 + 关键词搜索
 * - 表格：序号/编码字段/规则名称/规则描述/版本/状态/修改人/修改时间/操作
 * - 操作按钮根据状态动态显示
 */
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import type { AxiosResponse } from 'axios';
import { Plus, Delete, Search, View, Edit, Check, Close } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import TpMessage from '@mdm/common/components/feedback/TpMessage';
import TpConfirm from '@mdm/common/components/feedback/TpConfirm';
import CodeEditor from '@mdm/common/components/form/CodeEditor.vue';
import {
  listCodingRule, getCodingRule, getCodingRuleSegments, createCodingRule, updateCodingRule, deleteCodingRule,
  activateCodingRule, disableCodingRule, enableCodingRule, reviseCodingRule,
  validateGroovyScript, getAvailableCodeAttributes, saveCodingRuleSegments,
} from '@/modules/model-design/api/coding-rule';
import { listSegment } from '@/modules/model-design/api/segment';
import type {
  CodingRuleVO, CodingRuleCreateDTO, CodingRuleUpdateDTO, CodingRuleQuery,
  CodingRuleStatus,
} from '@/modules/model-manage/types/coding-rule';
import {
  CODING_RULE_STATUS_OPTIONS, CODING_RULE_STATUS_LABEL, CODING_RULE_STATUS_TAG,
  CODING_GENERATION_TIMING_OPTIONS, RULE_DEFINITION_TYPE_OPTIONS,
} from '@/modules/model-manage/types/coding-rule';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'CodingRulePanel' });

const router = useRouter();

const props = defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();

// ========== 状态 ==========

/** 列表加载 */
const loading = ref(false);

/** 规则列表 */
const tableData = ref<CodingRuleVO[]>([]);

/** 总数 */
const total = ref(0);

/** 分页 */
const currentPage = ref(1);
const pageSize = ref(20);

/** 筛选 */
const filterData = reactive({
  ruleType: '' as '' | 'segment' | 'script',
  status: '' as '' | CodingRuleStatus,
  keyword: '',
});

/** 编辑弹窗 */
const dialogVisible = ref(false);
const dialogTitle = ref('新增编码规则');
const dialogMode = ref<'create' | 'edit' | 'view'>('create');
const editingRuleId = ref<ID | null>(null);

/** 表单 */
const formRef = ref();
const saving = ref(false);
const formData = reactive({
  attributeId: '' as ID | '',
  name: '',
  ruleDefinitionType: 'segment' as 'segment' | 'script',
  generationTiming: 'button' as 'button' | 'onSave' | 'onActive',
  script: '',
  prefix: '',
});

/** 已选码段（含 segmentId 和 sortOrder） */
const selectedSegments = ref<Array<{ segmentId: string; segmentCode: string; segmentType: string; sortOrder: number }>>([]);

/** 加载码段下拉选项（按 modelId 过滤） */
const segmentOptions = ref<Array<{ id: string; code: string; name: string; type: string }>>([]);
const loadAvailableSegments = async () => {
  if (!props.modelId) return;
  const res = await listSegment({ modelId: props.modelId, page: 1, size: 200 }) as any;
  if (res.data?.data?.rows) {
    segmentOptions.value = res.data.data.rows.map((r: any) => ({
      id: r.id, code: r.segmentCode || r.code, name: r.segmentName || r.name, type: r.segmentType || r.type
    }));
  }
};

/** 加载规则的码段（编辑时回显） */
const loadRuleSegmentsForEdit = async (ruleId: ID) => {
  const res = await getCodingRuleSegments(ruleId) as any;
  const segList = res.data?.data ?? res.data ?? [];
  if (Array.isArray(segList)) {
    selectedSegments.value = segList.map((s: any, idx: number) => ({
      segmentId: s.segmentId || s.id || '',
      segmentCode: s.segmentCode || s.code || '',
      segmentType: s.segmentType || '',
      sortOrder: s.sortOrder ?? idx + 1,
    }));
  }
};

/** 重置选中码段 */
const resetSelectedSegments = () => {
  selectedSegments.value = [];
};

/** 码段下拉变化时同步 segmentId 和 segmentType */
const onSegmentSelectChange = (idx: number, val: string) => {
  const opt = segmentOptions.value.find(o => o.code === val);
  if (opt) {
    selectedSegments.value[idx].segmentId = opt.id;
    selectedSegments.value[idx].segmentType = opt.type;
  }
  // 按 sortOrder 重新排序
  selectedSegments.value.forEach((s, i) => s.sortOrder = i + 1);
};

/** 可选编码属性 */
const availableAttributes = ref<{ id: ID; name: string }[]>([]);

/** 编码预览 */
const codePreview = ref('');

/** 查看脚本弹窗 */
const scriptDialogVisible = ref(false);
const scriptDialogContent = ref('');

/** Groovy 脚本校验 */
const scriptValidating = ref(false);
const scriptValidationResult = ref<{ valid: boolean; errors: string[] } | null>(null);

// ========== 列配置 ==========

const columns = computed<TpTableColumn[]>(() => [
  { prop: 'index', label: '序号', width: 60, formatter: (_row, _col, cellValue, $index) => (currentPage.value - 1) * pageSize.value + $index + 1 },
  { prop: 'targetAttributeName', label: '编码字段', minWidth: 120 },
  { prop: 'name', label: '规则名称', minWidth: 180, showOverflowTooltip: true },
  {
    prop: 'segments',
    label: '码段组合',
    minWidth: 200,
    formatter: (row: CodingRuleVO & { ruleSegments?: Array<{ segmentCode: string; segmentType?: string }> }) => {
      const segs = row.ruleSegments || [];
      if (segs.length === 0) {
        return row.segmentCodes || '-';
      }
      return segs.map(s => {
        const t = (s.segmentType || '').substring(0, 3);
        return `${s.segmentCode}${t ? '(' + t + ')' : ''}`;
      }).join(' → ');
    },
  },
  {
    prop: 'ruleDescription',
    label: '规则描述',
    minWidth: 200,
    formatter: (row: CodingRuleVO) => {
      if (row.ruleDefinitionType === 'script') {
        return '查看规则';
      }
      return row.ruleDescription || '';
    },
  },
  { prop: 'version', label: '版本', width: 80, formatter: (row: CodingRuleVO) => `V${row.version}.0` },
  {
    prop: 'status',
    label: '状态',
    width: 100,
    formatter: (row: CodingRuleVO & { statusLabel?: string }) => row.statusLabel || row.status || '',
    cellClass: (row: CodingRuleVO) => `status-${row.status}`,
  },
  { prop: 'updater', label: '修改人', minWidth: 100 },
  { prop: 'updateTime', label: '修改时间', minWidth: 160 },
  { prop: 'actions', label: '操作', width: 200, fixed: 'right' },
]);

// ========== 计算属性 ==========

/** 是否模型锁定（只有编辑中状态才能操作） */
/** 是否为草稿（编辑中）—— 草稿状态下锁定不允许编辑规则 */
const isModelDraft = computed(() => {
  const s = (props.modelStatus || '').toLowerCase();
  return s === 'edit' || s === 'draft' || s === '';
});
/** 是否完全锁定（草稿 / 停用 / 历史） */
const isModelLocked = computed(() => {
  const s = (props.modelStatus || '').toLowerCase();
  return s === 'edit' || s === 'draft' || s === 'disabled' || s === 'history' || s === '';
});

/** 获取操作按钮 */
const getActions = (row: CodingRuleVO) => {
  const actions: { label: string; type: string; icon: any; handler: () => void; disabled?: boolean }[] = [];
  switch (row.status) {
    case 'draft':
      actions.push(
        { label: '编辑', type: 'primary', icon: Edit, handler: () => handleEdit(row), disabled: isModelLocked.value },
        { label: '删除', type: 'danger', icon: Delete, handler: () => handleDelete(row), disabled: isModelLocked.value },
        { label: '生效', type: 'success', icon: Check, handler: () => handleActivate(row) },
        { label: '查看', type: 'default', icon: View, handler: () => handleView(row) },
      );
      break;
    case 'active':
      actions.push(
        { label: '修订', type: 'warning', icon: Edit, handler: () => handleRevise(row) },
        { label: '停用', type: 'info', icon: Close, handler: () => handleDisable(row) },
        { label: '查看', type: 'default', icon: View, handler: () => handleView(row) },
      );
      break;
    case 'disabled':
      actions.push(
        { label: '启用', type: 'success', icon: Check, handler: () => handleEnable(row) },
        { label: '查看', type: 'default', icon: View, handler: () => handleView(row) },
      );
      break;
    case 'history':
      actions.push({ label: '查看', type: 'default', icon: View, handler: () => handleView(row) });
      break;
  }
  return actions;
};

// ========== 工具方法 ==========

/** 获取规则描述 */
const getRuleDescription = (row: CodingRuleVO): string => {
  if (row.ruleDefinitionType === 'script') {
    return '查看规则';
  }
  if (row.ruleDescription) {
    return row.ruleDescription;
  }
  return row.segmentTypeList?.map(t => {
    const labels: Record<string, string> = {
      fixed: '固定码', serial: '流水码', date: '日期码', feature: '特征码',
      rangeSerial: '区间流水码', ref: '引用码', dynamicSerial: '动态流水码',
      dateSerial: '日期流水码', refSerial: '引用流水码',
    };
    return labels[t] || t;
  }).join('+') || '';
};

// ========== 数据加载 ==========

/** 加载规则列表 */
const loadRules = async () => {
  loading.value = true;
  try {
    const query: CodingRuleQuery = {
      modelId: props.modelId,
      keyword: filterData.keyword || undefined,
      page: currentPage.value,
      pageSize: pageSize.value,
    };
    const res = (await listCodingRule(query)) as unknown as AxiosResponse<any>;
    const pageData = res.data?.data;
    if (pageData) {
      const rows = pageData.rows ?? [];
      // 异步加载每个规则的码段组合
      await Promise.all(rows.map(async (row: any) => {
        try {
          const segRes = await getCodingRuleSegments(row.id) as any;
          const segList = segRes.data?.data ?? segRes.data ?? [];
          if (Array.isArray(segList)) {
            row.ruleSegments = segList;
            row.segmentCodes = segList.map((s: any) => s.segmentCode || s.code).join('+');
          }
        } catch { /* ignore */ }
      }));
      tableData.value = rows;
      total.value = pageData.total ?? 0;
    }
  } catch (error) {
    console.error('[CodingRulePanel] load error', error);
  } finally {
    loading.value = false;
  }
};

/** 加载可选编码属性 */
const loadAvailableAttributes = async () => {
  try {
    const res = await getAvailableCodeAttributes(props.modelId);
    availableAttributes.value = res.data?.data ?? [];
  } catch (error) {
    console.error('[CodingRulePanel] load attributes error', error);
  }
};

// ========== 操作处理 ==========

/** 新增 */
const handleAdd = async () => {
  dialogTitle.value = '新增编码规则';
  dialogMode.value = 'create';
  editingRuleId.value = null;
  resetForm();
  resetSelectedSegments();
  await loadAvailableAttributes();
  await loadAvailableSegments();
  dialogVisible.value = true;
};

/** 码段管理 —— 跳转到独立码段管理页面 */
const handleSegmentManage = () => {
  router.push({ name: 'model-design-segment' });
};

/** 编辑 */
const handleEdit = async (row: CodingRuleVO) => {
  dialogTitle.value = '编辑编码规则';
  dialogMode.value = 'edit';
  editingRuleId.value = row.id;
  resetForm();
  resetSelectedSegments();
  await loadAvailableAttributes();
  await loadAvailableSegments();
  await loadRuleSegmentsForEdit(row.id);
  try {
    const res = await getCodingRule(row.id);
    const data = res.data?.data;
    if (data) {
      // ponytail: 详情接口字段名兼容 (encodeFieldId/ruleName/ruleMode/triggerType)
      formData.attributeId = data.encodeFieldId || data.attributeId || row.targetAttributeId || '';
      formData.name = data.ruleName || data.name || row.name || '';
      const mode = (data.ruleMode || data.ruleDefinitionType || row.ruleDefinitionType || '').toLowerCase();
      formData.ruleDefinitionType = (mode === 'groovy' ? 'script' : 'segment') as 'segment' | 'script';
      formData.generationTiming = data.triggerType || data.generationTiming || row.generationTiming || 'BUTTON';
      formData.script = data.groovyScript || data.script || '';
      formData.prefix = data.prefix || '';
      // ponytail: 跳过 dsl 预览回填 (此组件未声明 dslTemplate ref, 赋值会抛 ReferenceError)
    }
  } catch (error) {
    TpMessage.error('加载规则详情失败');
    console.error(error);
  }
  dialogVisible.value = true;
};

/** 查看 */
const handleView = async (row: CodingRuleVO) => {
  dialogTitle.value = '查看编码规则';
  dialogMode.value = 'view';
  editingRuleId.value = row.id;
  resetForm();
  // 加载下拉选项供 select 选中匹配
  await loadAvailableAttributes();
  await loadAvailableSegments();
  try {
    // 后端详情接口返回 encodeFieldId/ruleName/ruleMode/triggerType;
    // 列表接口返回 name/targetAttributeName/ruleDefinitionType/generationTiming (兼容旧名)
    const res = await getCodingRule(row.id);
    const data = res.data?.data;
    if (data) {
      formData.attributeId = data.encodeFieldId || data.attributeId || row.targetAttributeId || '';
      formData.name = data.ruleName || data.name || row.name || '';
      const mode = (data.ruleMode || data.ruleDefinitionType || row.ruleDefinitionType || '').toLowerCase();
      formData.ruleDefinitionType = (mode === 'groovy' ? 'script' : 'segment') as 'segment' | 'script';
      formData.generationTiming = data.triggerType || data.generationTiming || row.generationTiming || 'BUTTON';
      formData.script = data.groovyScript || data.script || '';
      formData.prefix = data.prefix || '';
      // ponytail: 跳过 dsl 预览回填 (此组件未声明 dslTemplate ref, 赋值会抛 ReferenceError)
    }
    // 码段组合: 调用专用接口 (字段含 segmentCode + segmentType)
    const segRes = await getCodingRuleSegments(row.id) as any;
    const segList = segRes.data?.data ?? segRes.data ?? [];
    if (Array.isArray(segList)) {
      selectedSegments.value = segList.map((s: any, idx: number) => ({
        segmentId: s.segmentId || s.id,
        segmentCode: s.segmentCode || s.code || '',
        segmentType: s.segmentType || '',
        sortOrder: s.sortOrder ?? idx + 1,
      }));
    }
  } catch (error) {
    TpMessage.error('加载规则详情失败');
    console.error(error);
  }
  dialogVisible.value = true;
};

/** 删除 */
const handleDelete = async (row: CodingRuleVO) => {
  try {
    await TpConfirm.delete(`确定要删除规则"${row.name}"吗？`);
    await deleteCodingRule(row.id);
    TpMessage.success('删除成功');
    await loadRules();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error(error);
    }
  }
};

/** 生效 */
const handleActivate = async (row: CodingRuleVO) => {
  try {
    await TpConfirm.confirm('确定要让该编码规则生效吗？生效后将在数据维护时生成编码。');
    await activateCodingRule(row.id);
    TpMessage.success('规则已生效');
    await loadRules();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error(error);
    }
  }
};

/** 停用 */
const handleDisable = async (row: CodingRuleVO) => {
  try {
    await TpConfirm.confirm('确定要停用该编码规则吗？停用后该编码字段将不能由系统生成编码。');
    await disableCodingRule(row.id);
    TpMessage.success('规则已停用');
    await loadRules();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error(error);
    }
  }
};

/** 启用 */
const handleEnable = async (row: CodingRuleVO) => {
  try {
    await TpConfirm.confirm('确定要重新启用该编码规则吗？');
    await enableCodingRule(row.id);
    TpMessage.success('规则已启用');
    await loadRules();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error(error);
    }
  }
};

/** 修订 */
const handleRevise = async (row: CodingRuleVO) => {
  try {
    await TpConfirm.confirm('确定要修订该编码规则吗？将复制当前生效版本为新版本。');
    await reviseCodingRule(row.id);
    TpMessage.success('修订成功');
    await loadRules();
  } catch (error) {
    if ((error as any)?.message !== 'cancel') {
      console.error(error);
    }
  }
};

/** 查看脚本 */
const handleViewScript = () => {
  scriptDialogContent.value = formData.script;
  scriptDialogVisible.value = true;
};

/** 保存 */
const handleSave = async () => {
  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  if (formData.ruleDefinitionType === 'script' && formData.script) {
    try {
      const res = await validateGroovyScript(formData.script);
      const result = res.data?.data;
      if (!result?.valid) {
        TpMessage.error(`脚本校验失败：${result?.errors?.join('；') || '语法错误'}`);
        return;
      }
    } catch {
      // 校验接口暂不可用，继续保存
    }
  }

  saving.value = true;
  try {
    let ruleId: string | undefined;
    if (dialogMode.value === 'edit' && editingRuleId.value) {
      const dto: CodingRuleUpdateDTO = {
        id: editingRuleId.value,
        attributeId: formData.attributeId as ID,
        name: formData.name,
        ruleDefinitionType: formData.ruleDefinitionType,
        generationTiming: formData.generationTiming,
        script: formData.script || undefined,
        prefix: formData.prefix || undefined,
      };
      await updateCodingRule(dto);
      ruleId = editingRuleId.value;
      TpMessage.success('规则更新成功');
    } else {
      const dto: CodingRuleCreateDTO = {
        modelId: props.modelId,
        attributeId: formData.attributeId as ID,
        name: formData.name,
        ruleDefinitionType: formData.ruleDefinitionType,
        generationTiming: formData.generationTiming,
        script: formData.script || undefined,
        prefix: formData.prefix || undefined,
      };
      const res = await createCodingRule(dto);
      ruleId = res.data?.data?.id;
      TpMessage.success('规则创建成功');
    }
    // 保存码段组合
    if (ruleId && selectedSegments.value.length > 0) {
      const segs = selectedSegments.value.map((s, idx) => ({
        segmentCode: s.segmentCode,
        sortOrder: idx + 1
      })) as any;
      await saveCodingRuleSegments(ruleId as ID, segs);
    }
    dialogVisible.value = false;
    await loadRules();
  } catch (error: any) {
    console.error('[CodingRulePanel] save error', error);
    const msg = error?.response?.data?.message || error?.message || '保存失败';
    TpMessage.error(msg);
  } finally {
    saving.value = false;
  }
};

// ========== 表单重置 ==========

const resetForm = () => {
  formData.attributeId = '';
  formData.name = '';
  formData.ruleDefinitionType = 'segment';
  formData.generationTiming = 'button';
  formData.script = '';
  formData.prefix = '';
  scriptValidationResult.value = null;
};

// ========== 筛选/搜索/分页 ==========

const handleSearch = () => {
  currentPage.value = 1;
  loadRules();
};

const handleReset = () => {
  filterData.ruleType = '';
  filterData.status = '';
  filterData.keyword = '';
  handleSearch();
};

const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadRules();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 1;
  loadRules();
};

// ========== 生命周期 ==========

onMounted(() => {
  loadRules();
});

watch(() => [props.modelId, props.modelVersion], () => {
  loadRules();
});
</script>

<template>
  <div class="coding-rule-panel h-full flex flex-col bg-white rounded-lg">
    <!-- 表格顶部操作栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <!-- 左侧操作按钮 -->
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" :disabled="isModelLocked" @click="handleAdd">
          新增编码规则
        </el-button>
        <el-button :icon="View" :disabled="isModelLocked" @click="handleSegmentManage">
          码段管理
        </el-button>
      </div>

      <!-- 右侧搜索框 -->
      <div class="flex items-center gap-3">
        <el-input
          v-model="filterData.keyword"
          placeholder="搜索规则名称"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">搜索</el-button>
      </div>
    </div>

    <!-- 筛选栏（规则类型 + 状态） -->
    <div class="flex items-center gap-4 px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <el-select v-model="filterData.ruleType" placeholder="规则类型" clearable style="width: 140px">
        <el-option label="码段组合" value="segment" />
        <el-option label="脚本自定义" value="script" />
      </el-select>

      <el-select v-model="filterData.status" placeholder="状态" clearable style="width: 120px">
        <el-option v-for="opt in CODING_RULE_STATUS_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
      </el-select>

      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 表格 -->
    <div class="flex-1 min-h-0 px-4 py-4 overflow-auto">
      <TpTable
        :columns="columns"
        :data="tableData"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        border
        stripe
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
      >
        <!-- 状态列自定义显示 -->
        <template #col-status="{ row }">
          <el-tag size="small" :type="CODING_RULE_STATUS_TAG[row.status as CodingRuleStatus]">
            {{ CODING_RULE_STATUS_LABEL[row.status as CodingRuleStatus] }}
          </el-tag>
        </template>

        <!-- 规则描述列自定义显示（脚本类型显示查看按钮） -->
        <template #col-ruleDescription="{ row }">
          <el-button
            v-if="row.ruleDefinitionType === 'script'"
            type="primary"
            link
            @click="handleView(row)"
          >
            查看规则
          </el-button>
          <span v-else>{{ getRuleDescription(row) }}</span>
        </template>

        <!-- 操作列 -->
        <template #col-actions="{ row }">
          <div class="flex items-center gap-1 flex-wrap">
            <template v-for="action in getActions(row)" :key="action.label">
              <el-button
                size="small"
                :type="action.type as any"
                link
                :disabled="action.disabled"
                @click="action.handler"
              >
                {{ action.label }}
              </el-button>
            </template>
          </div>
        </template>

        <!-- 空状态 -->
        <template #empty>
          <TpEmpty state="no-data" description="暂无编码规则，请点击新增规则" />
        </template>
      </TpTable>
    </div>

    <!-- 新增/编辑/查看弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        label-width="100px"
        :disabled="dialogMode === 'view'"
      >
        <el-form-item label="规则类型">
          <el-tag>{{ formData.ruleDefinitionType === 'segment' ? '码段组合' : '脚本自定义' }}</el-tag>
        </el-form-item>

        <el-form-item label="编码字段" prop="attributeId">
          <el-select
            v-model="formData.attributeId"
            filterable
            placeholder="请选择需要生成编码的字段"
            class="w-full"
            :disabled="dialogMode === 'view'"
          >
            <el-option
              v-for="opt in availableAttributes"
              :key="opt.id"
              :value="opt.id"
              :label="opt.name"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="规则名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入规则名称" maxlength="200" />
        </el-form-item>

        <el-form-item label="生成时机">
          <el-radio-group v-model="formData.generationTiming">
            <el-radio-button
              v-for="opt in CODING_GENERATION_TIMING_OPTIONS"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="formData.ruleDefinitionType === 'script'" label="Groovy脚本">
          <CodeEditor
            v-model="formData.script"
            language="groovy"
            :readonly="dialogMode === 'view'"
            height="200px"
          />
        </el-form-item>

        <el-form-item v-else label="码段组合">
          <div class="w-full space-y-2">
            <div v-if="selectedSegments.length === 0" class="text-[var(--el-text-color-secondary)] text-sm py-2">
              尚未选择码段（按"新增"按钮添加）
            </div>
            <div
              v-for="(seg, idx) in selectedSegments"
              :key="seg.segmentId || seg.segmentCode"
              class="flex items-center gap-2 p-2 border border-[var(--el-border-color-lighter)] rounded"
            >
              <el-tag size="small" type="info">#{{ idx + 1 }}</el-tag>
              <el-select
                v-model="seg.segmentCode"
                filterable
                placeholder="选择码段"
                class="flex-1"
                :disabled="dialogMode === 'view'"
                @change="(val: string) => onSegmentSelectChange(idx, val)"
              >
                <el-option
                  v-for="opt in segmentOptions"
                  :key="opt.id"
                  :value="opt.code"
                  :label="`${opt.code} (${opt.type || 'FIXED'}) - ${opt.name}`"
                />
              </el-select>
              <el-button
                size="small"
                type="danger"
                :disabled="dialogMode === 'view'"
                @click="selectedSegments.splice(idx, 1)"
              >删除</el-button>
            </div>
            <el-button
              v-if="dialogMode !== 'view'"
              type="primary"
              plain
              :icon="Plus"
              size="small"
              @click="selectedSegments.push({ segmentId: '', segmentCode: '', segmentType: 'FIXED', sortOrder: selectedSegments.length + 1 })"
            >新增码段</el-button>
          </div>
        </el-form-item>

        <el-form-item label="编码预览">
          <div class="p-3 bg-[var(--el-fill-color)] rounded font-mono text-sm w-full">
            {{ codePreview || '请配置后预览' }}
          </div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="dialogMode !== 'view'" type="primary" :loading="saving" @click="handleSave">
          保存
        </el-button>
      </template>
    </el-dialog>

    <!-- 查看脚本弹窗 -->
    <el-dialog v-model="scriptDialogVisible" title="查看脚本" width="700px" destroy-on-close>
      <CodeEditor v-model="scriptDialogContent" language="groovy" readonly height="400px" />
    </el-dialog>
  </div>
</template>

<style scoped>
.coding-rule-panel {
  background: var(--el-bg-color);
}

.status-draft { color: var(--el-color-warning); }
.status-active { color: var(--el-color-success); }
.status-disabled { color: var(--el-color-info); }
.status-history { color: var(--el-color-info); }
</style>
