<script setup lang="ts">
/**
 * AttributeConfigList - 属性字段管理列表页
 *
 * 需求 §14：属性列表展示、新增/编辑/删除（单个+批量）、启用/停用
 * 需求 §16：系统字段重名 Tooltip + 异常提示
 */
import { ref, reactive, computed, onMounted, watch } from 'vue';
import { useI18n } from 'vue-i18n';
import type { AxiosResponse } from 'axios';
import { Plus, Delete, Search, Refresh } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import TpSectionTitle from '@mdm/common/components/layout/TpSectionTitle.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import {
  listAttribute, createAttribute, updateAttribute, deleteAttribute,
  batchDeleteAttribute, enableAttribute, disableAttribute,
  checkAttributeNameUnique, checkAttributeEnglishNameUnique,
  checkAttributeReferences,
} from '@/modules/model-design/api/attribute';
import type {
  AttributeVO, AttributeCreateDTO, AttributeUpdateDTO, AttributeQuery,
  PhysicalDataType, DataCategory, AttributeStatus,
} from '@/modules/model-design/types/attribute';
import {
  ATTRIBUTE_STATUS_OPTIONS, ATTRIBUTE_STATUS_LABEL,
  DATA_CATEGORY_CONFIG_SUPPORT,
} from '@/modules/model-design/types/attribute';
import type { ApiResponse, PaginatedResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'AttributeConfigList' });

const props = defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();

const { t } = useI18n();

// ========== 系统字段列表（§16.1） ==========
const SYSTEM_FIELDS: { name: string; code: string }[] = [
  { name: '数据ID', code: 'M_SYS_ID' },
  { name: '数据编码', code: 'M_SYS_CODE' },
  { name: '数据状态', code: 'M_SYS_STATUS' },
  { name: '数据版本', code: 'M_SYS_VERSION' },
  { name: '数据创建人ID', code: 'M_SYS_CREATE_USER_ID' },
  { name: '数据创建人', code: 'M_SYS_CREATE_USER' },
  { name: '数据创建时间', code: 'M_SYS_CREATE_TIME' },
  { name: '数据修改人ID', code: 'M_SYS_UPDATE_USER_ID' },
  { name: '数据修改人', code: 'M_SYS_UPDATE_USER' },
  { name: '数据修改时间', code: 'M_SYS_UPDATE_TIME' },
  { name: '数据所属组织ID', code: 'M_SYS_ORG_ID' },
  { name: '数据所属组织', code: 'M_SYS_ORG_NAME' },
  { name: '数据所属部门ID', code: 'M_SYS_DEPT_ID' },
  { name: '数据所属部门', code: 'M_SYS_DEPT_NAME' },
  { name: '数据排序号', code: 'M_SYS_SORT_ORDER' },
  { name: '数据备注', code: 'M_SYS_REMARK' },
  { name: '数据来源', code: 'M_SYS_SOURCE' },
  { name: '数据时间戳', code: 'M_SYS_TIMESTAMP' },
];

// ========== 数据类型选项 ==========
const DATA_TYPE_OPTIONS: { value: PhysicalDataType; label: string; category: DataCategory }[] = [
  { value: 'VARCHAR', label: 'VARCHAR', category: 'string' },
  { value: 'CHAR', label: 'CHAR', category: 'string' },
  { value: 'TEXT', label: 'TEXT', category: 'clob' },
  { value: 'CLOB', label: 'CLOB', category: 'clob' },
  { value: 'INT', label: 'INT', category: 'number' },
  { value: 'BIGINT', label: 'BIGINT', category: 'number' },
  { value: 'DECIMAL', label: 'DECIMAL', category: 'number' },
  { value: 'FLOAT', label: 'FLOAT', category: 'number' },
  { value: 'DOUBLE', label: 'DOUBLE', category: 'number' },
  { value: 'DATE', label: 'DATE', category: 'date' },
  { value: 'DATETIME', label: 'DATETIME', category: 'date' },
  { value: 'TIMESTAMP', label: 'TIMESTAMP', category: 'date' },
  { value: 'BLOB', label: 'BLOB', category: 'file' },
];

const DATA_TYPE_CATEGORY: Record<PhysicalDataType, DataCategory> = {
  VARCHAR: 'string', CHAR: 'string', TEXT: 'clob', CLOB: 'clob',
  INT: 'number', BIGINT: 'number', DECIMAL: 'number', FLOAT: 'number', DOUBLE: 'number',
  DATE: 'date', DATETIME: 'date', TIMESTAMP: 'date', BLOB: 'file',
};

// ========== 响应式状态 ==========
const loading = ref(false);
const attributes = ref<AttributeVO[]>([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(20);

/** 搜索条件 */
const keyword = ref('');
const statusFilter = ref<AttributeStatus | ''>('');

/** 选中行 */
const selectedIds = ref<ID[]>([]);

/** 编辑弹窗 */
const dialogVisible = ref(false);
const dialogTitle = ref('新增属性');
const isEditing = ref(false);
const editingId = ref<ID | null>(null);

/** 表单 */
const formRef = ref();
const formData = reactive<{
  name: string;
  englishName: string;
  dataType: PhysicalDataType | '';
  length: number | undefined;
  precision: number | undefined;
  positiveOnly: boolean;
  required: boolean;
  unique: boolean;
  comment: string;
  sortOrder: number;
}>({
  name: '',
  englishName: '',
  dataType: '',
  length: undefined,
  precision: undefined,
  positiveOnly: false,
  required: true,
  unique: false,
  comment: '',
  sortOrder: 1,
});

// ========== 列配置 ==========
const columns: TpTableColumn[] = [
  { prop: 'sortOrder', label: '排序号', minWidth: 70 },
  { prop: 'name', label: '属性名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'englishName', label: '英文名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'dataType', label: '数据类型', minWidth: 110 },
  { prop: 'length', label: '长度', minWidth: 70 },
  { prop: 'precision', label: '精度', minWidth: 70 },
  { prop: 'positiveOnly', label: '仅为正数', minWidth: 80 },
  { prop: 'required', label: '是否必填', minWidth: 80 },
  { prop: 'unique', label: '是否唯一', minWidth: 80 },
  { prop: 'comment', label: '注释', minWidth: 140, showOverflowTooltip: true },
  { prop: 'status', label: '状态', minWidth: 80 },
  { prop: 'actions', label: '操作', minWidth: 160, fixed: 'right' },
];

// ========== 计算属性 ==========
const isModelEditable = computed(() => props.modelStatus === 'draft');

const hasSelected = computed(() => selectedIds.value.length > 0);

const filteredAttributes = computed(() => {
  let list = [...attributes.value];
  if (keyword.value) {
    const kw = keyword.value.toLowerCase();
    list = list.filter(a =>
      a.name.toLowerCase().includes(kw) ||
      a.englishName.toLowerCase().includes(kw)
    );
  }
  if (statusFilter.value) {
    list = list.filter(a => a.status === statusFilter.value);
  }
  return list;
});

/** 数据类型是否支持长度 */
const supportsLength = computed(() => {
  if (!formData.dataType) return false;
  const cat = DATA_TYPE_CATEGORY[formData.dataType];
  return cat === 'string' || cat === 'number';
});

/** 数据类型是否支持精度 */
const supportsPrecision = computed(() => {
  if (!formData.dataType) return false;
  return formData.dataType === 'DECIMAL' || formData.dataType === 'FLOAT' || formData.dataType === 'DOUBLE';
});

/** 数据类型是否支持仅为正数 */
const supportsPositive = computed(() => {
  if (!formData.dataType) return false;
  return DATA_TYPE_CATEGORY[formData.dataType] === 'number';
});

// ========== 表单校验 ==========
const formRules = computed(() => ({
  name: [
    { required: true, message: '请输入属性名称', trigger: 'blur' },
    { max: 200, message: '属性名称不超过200字符', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string, callback: any) => {
        if (!value) return callback();
        try {
          const res = await checkAttributeNameUnique(props.modelId, value, editingId.value ?? undefined);
          const data = (res as any)?.data?.data ?? (res as any)?.data;
          if (data === true) callback(new Error('属性名称已存在'));
          else callback();
        } catch { callback(); }
      },
      trigger: 'blur',
    },
  ],
  englishName: [
    { required: true, message: '请输入英文名称', trigger: 'blur' },
    { max: 200, message: '英文名称不超过200字符', trigger: 'blur' },
    { pattern: /^[a-zA-Z_][a-zA-Z0-9_]*$/, message: '以字母或下划线开头，仅允许字母数字下划线', trigger: 'blur' },
    {
      validator: async (_rule: any, value: string, callback: any) => {
        if (!value) return callback();
        // §16.2 系统字段重名检测
        const sysMatch = SYSTEM_FIELDS.find(sf => sf.code.toLowerCase() === value.toLowerCase());
        if (sysMatch) {
          return callback(new Error(`英文名称 '${value}' 与系统属性 '${sysMatch.code}' 重复`));
        }
        try {
          const res = await checkAttributeEnglishNameUnique(props.modelId, value, editingId.value ?? undefined);
          const data = (res as any)?.data?.data ?? (res as any)?.data;
          if (data === true) callback(new Error('英文名称已存在'));
          else callback();
        } catch { callback(); }
      },
      trigger: 'blur',
    },
  ],
  dataType: [
    { required: true, message: '请选择数据类型', trigger: 'change' },
  ],
  length: [
    {
      validator: (_rule: any, value: number | undefined, callback: any) => {
        if (!supportsLength.value || value == null) return callback();
        const cat = DATA_TYPE_CATEGORY[formData.dataType as PhysicalDataType];
        if (cat === 'string' && value > 4000) return callback(new Error('字符类型长度不超过4000'));
        if (cat === 'number' && value > 38) return callback(new Error('数值类型长度不超过38'));
        callback();
      },
      trigger: 'blur',
    },
  ],
}));

// ========== 方法 ==========

/** 加载属性列表 */
const loadAttributes = async () => {
  loading.value = true;
  try {
    const query: AttributeQuery = {
      modelId: props.modelId,
      version: props.modelVersion,
      page: currentPage.value,
      pageSize: pageSize.value,
    };
    const res = (await listAttribute(query)) as unknown as AxiosResponse<ApiResponse<PaginatedResponse<AttributeVO>>>;
    const pageData = res.data?.data;
    if (pageData) {
      attributes.value = pageData.rows ?? [];
      total.value = pageData.total ?? 0;
    }
  } catch (error) {
    console.error('[AttributeConfigList] load error', error);
  } finally {
    loading.value = false;
  }
};

/** 重置表单 */
const resetForm = () => {
  formData.name = '';
  formData.englishName = '';
  formData.dataType = '';
  formData.length = undefined;
  formData.precision = undefined;
  formData.positiveOnly = false;
  formData.required = true;
  formData.unique = false;
  formData.comment = '';
  formData.sortOrder = attributes.value.length + 1;
  editingId.value = null;
  isEditing.value = false;
};

/** 打开新增弹窗 */
const handleAdd = () => {
  resetForm();
  dialogTitle.value = '新增属性';
  dialogVisible.value = true;
};

/** 打开编辑弹窗 */
const handleEdit = (row: AttributeVO) => {
  resetForm();
  dialogTitle.value = '编辑属性';
  isEditing.value = true;
  editingId.value = row.id;
  formData.name = row.name;
  formData.englishName = row.englishName;
  formData.dataType = row.dataType;
  formData.length = row.length;
  formData.precision = row.precision;
  formData.positiveOnly = row.positiveOnly;
  formData.required = row.required;
  formData.unique = row.unique;
  formData.comment = row.comment;
  formData.sortOrder = row.sortOrder;
  dialogVisible.value = true;
};

/** 保存属性 */
const handleSave = async () => {
  try {
    await formRef.value?.validate();
  } catch { return; }

  try {
    if (isEditing.value && editingId.value) {
      const dto: AttributeUpdateDTO = {
        id: editingId.value,
        name: formData.name,
        englishName: formData.englishName,
        dataType: formData.dataType as PhysicalDataType,
        length: formData.length,
        precision: formData.precision,
        positiveOnly: formData.positiveOnly,
        required: formData.required,
        unique: formData.unique,
        comment: formData.comment,
        sortOrder: formData.sortOrder,
      };
      await updateAttribute(dto);
      TpMessage.success('属性更新成功');
    } else {
      const dto: AttributeCreateDTO = {
        modelId: props.modelId,
        name: formData.name,
        englishName: formData.englishName,
        dataType: formData.dataType as PhysicalDataType,
        length: formData.length,
        precision: formData.precision,
        positiveOnly: formData.positiveOnly,
        required: formData.required,
        unique: formData.unique,
        comment: formData.comment,
        sortOrder: formData.sortOrder,
      };
      await createAttribute(dto);
      TpMessage.success('属性创建成功');
    }
    dialogVisible.value = false;
    await loadAttributes();
  } catch (error) {
    console.error('[AttributeConfigList] save error', error);
  }
};

/** 删除单个属性 */
const handleDelete = async (row: AttributeVO) => {
  try {
    await TpConfirm.delete(`确定要删除属性"${row.name}"吗？`);
  } catch { return; }

  try {
    // §14.5 删除前引用检查
    if (row.hasBeenActive) {
      const refRes = await checkAttributeReferences(row.id);
      const refData = (refRes as any)?.data?.data;
      if (refData?.isReferenced) {
        TpMessage.warning('该属性被引用，无法直接删除');
        return;
      }
    }
    await deleteAttribute(row.id);
    TpMessage.success('删除成功');
    await loadAttributes();
  } catch (error) {
    console.error('[AttributeConfigList] delete error', error);
  }
};

/** 批量删除 */
const handleBatchDelete = async () => {
  if (!hasSelected.value) {
    TpMessage.warning('请至少选择一条记录');
    return;
  }
  try {
    await TpConfirm.delete(`确定要删除选中的 ${selectedIds.value.length} 个属性吗？`);
  } catch { return; }

  try {
    await batchDeleteAttribute(selectedIds.value);
    TpMessage.success('批量删除成功');
    selectedIds.value = [];
    await loadAttributes();
  } catch (error) {
    console.error('[AttributeConfigList] batch delete error', error);
  }
};

/** 启用/停用属性 */
const handleToggleStatus = async (row: AttributeVO) => {
  try {
    if (row.status === 'enabled') {
      await disableAttribute(row.id);
      TpMessage.success('属性已停用');
    } else {
      await enableAttribute(row.id);
      TpMessage.success('属性已启用');
    }
    await loadAttributes();
  } catch (error) {
    console.error('[AttributeConfigList] toggle status error', error);
  }
};

/** 表格选中变化 */
const handleSelectionChange = (rows: AttributeVO[]) => {
  selectedIds.value = rows.map(r => r.id);
};

/** 分页变化 */
const handlePageChange = () => { loadAttributes(); };
const handleSizeChange = () => { currentPage.value = 1; loadAttributes(); };

/** 搜索 */
const handleSearch = () => { currentPage.value = 1; loadAttributes(); };
const handleReset = () => {
  keyword.value = '';
  statusFilter.value = '';
  handleSearch();
};

/** 获取系统字段 Tooltip 内容（§16.1） */
const getSystemFieldTooltip = () => {
  return SYSTEM_FIELDS.map(f => `${f.name} (${f.code})`).join('\n');
};

// ========== 生命周期 ==========
onMounted(() => { loadAttributes(); });
watch(() => [props.modelId, props.modelVersion], () => { loadAttributes(); });
</script>

<template>
  <div class="attribute-config-list flex flex-col h-full">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <div class="flex items-center gap-2">
        <el-button type="primary" :icon="Plus" :disabled="!isModelEditable" @click="handleAdd">
          新增属性
        </el-button>
        <el-button :icon="Delete" :disabled="!hasSelected || !isModelEditable" @click="handleBatchDelete">
          批量删除
        </el-button>
      </div>
      <div class="flex items-center gap-2">
        <el-input
          v-model="keyword"
          placeholder="搜索属性名称/英文名称"
          :prefix-icon="Search"
          clearable
          style="width: 240px"
          @clear="handleSearch"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width: 120px" @change="handleSearch">
          <el-option
            v-for="opt in ATTRIBUTE_STATUS_OPTIONS"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
        <el-button :icon="Search" @click="handleSearch">搜索</el-button>
        <el-button :icon="Refresh" @click="handleReset">重置</el-button>
      </div>
    </div>

    <!-- 系统字段提示（§16.1） -->
    <div class="px-4 py-2">
      <el-tooltip :content="getSystemFieldTooltip()" placement="bottom" :show-after="300">
        <span class="text-xs text-[var(--el-text-color-secondary)] cursor-help">
          系统字段列表（hover 查看）
        </span>
      </el-tooltip>
    </div>

    <!-- 属性表格 -->
    <div class="flex-1 min-h-0 px-4">
      <TpTable
        :columns="columns"
        :data="filteredAttributes"
        :loading="loading"
        :total="total"
        :current-page="currentPage"
        :page-size="pageSize"
        border
        stripe
        @update:current-page="handlePageChange"
        @update:page-size="handleSizeChange"
      >
        <!-- 排序号 -->
        <template #col-sortOrder="{ row }">
          {{ row.sortOrder }}
        </template>

        <!-- 属性名称 -->
        <template #col-name="{ row }">
          <span>{{ row.name }}</span>
          <span v-if="row.status === 'disabled'" class="text-red-500 ml-1 text-xs">(已停用)</span>
        </template>

        <!-- 数据类型 -->
        <template #col-dataType="{ row }">
          <span>{{ row.dataType }}<template v-if="row.length">({{ row.length }}<template v-if="row.precision">,{{ row.precision }}</template>)</template></span>
        </template>

        <!-- 长度 -->
        <template #col-length="{ row }">
          {{ row.length ?? '-' }}
        </template>

        <!-- 精度 -->
        <template #col-precision="{ row }">
          {{ row.precision ?? '-' }}
        </template>

        <!-- 仅为正数 -->
        <template #col-positiveOnly="{ row }">
          <el-tag v-if="row.positiveOnly" size="small" type="success">是</el-tag>
          <span v-else class="text-[var(--el-text-color-placeholder)]">否</span>
        </template>

        <!-- 是否必填 -->
        <template #col-required="{ row }">
          <el-tag v-if="row.required" size="small" type="danger">是</el-tag>
          <span v-else class="text-[var(--el-text-color-placeholder)]">否</span>
        </template>

        <!-- 是否唯一 -->
        <template #col-unique="{ row }">
          <el-tag v-if="row.unique" size="small" type="warning">是</el-tag>
          <span v-else class="text-[var(--el-text-color-placeholder)]">否</span>
        </template>

        <!-- 注释 -->
        <template #col-comment="{ row }">
          {{ row.comment || '-' }}
        </template>

        <!-- 状态 -->
        <template #col-status="{ row }">
          <el-tag :type="row.status === 'enabled' ? 'success' : 'info'" size="small">
            {{ row.statusLabel }}
          </el-tag>
        </template>

        <!-- 操作列 -->
        <template #col-actions="{ row }">
          <el-button size="small" link type="primary" :disabled="!isModelEditable" @click="handleEdit(row as AttributeVO)">编辑</el-button>
          <el-button size="small" link :type="row.status === 'enabled' ? 'warning' : 'success'" @click="handleToggleStatus(row as AttributeVO)">
            {{ row.status === 'enabled' ? '停用' : '启用' }}
          </el-button>
          <el-button size="small" link type="danger" :disabled="!isModelEditable" @click="handleDelete(row as AttributeVO)">删除</el-button>
        </template>

        <!-- 空状态 -->
        <template #empty>
          <TpEmpty state="no-data" description="暂无属性数据，请点击新增属性" />
        </template>
      </TpTable>
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="640px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
        label-position="right"
      >
        <el-form-item label="属性名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入属性名称" maxlength="200" />
        </el-form-item>
        <el-form-item label="英文名称" prop="englishName">
          <el-input v-model="formData.englishName" placeholder="请输入英文名称（字段名）" maxlength="200" />
        </el-form-item>
        <el-form-item label="数据类型" prop="dataType">
          <el-select v-model="formData.dataType" placeholder="请选择数据类型" style="width: 100%">
            <el-option-group label="字符类型">
              <el-option v-for="dt in DATA_TYPE_OPTIONS.filter(d => d.category === 'string')" :key="dt.value" :label="dt.label" :value="dt.value" />
            </el-option-group>
            <el-option-group label="数值类型">
              <el-option v-for="dt in DATA_TYPE_OPTIONS.filter(d => d.category === 'number')" :key="dt.value" :label="dt.label" :value="dt.value" />
            </el-option-group>
            <el-option-group label="日期类型">
              <el-option v-for="dt in DATA_TYPE_OPTIONS.filter(d => d.category === 'date')" :key="dt.value" :label="dt.label" :value="dt.value" />
            </el-option-group>
            <el-option-group label="文件/大文本">
              <el-option v-for="dt in DATA_TYPE_OPTIONS.filter(d => d.category === 'file' || d.category === 'clob')" :key="dt.value" :label="dt.label" :value="dt.value" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item v-if="supportsLength" label="长度" prop="length">
          <el-input-number v-model="formData.length" :min="1" :max="4000" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="supportsPrecision" label="精度" prop="precision">
          <el-input-number v-model="formData.precision" :min="0" :max="38" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item v-if="supportsPositive" label="仅为正数">
          <el-switch v-model="formData.positiveOnly" />
        </el-form-item>
        <el-form-item label="是否必填">
          <el-switch v-model="formData.required" />
        </el-form-item>
        <el-form-item label="是否唯一">
          <el-switch v-model="formData.unique" />
        </el-form-item>
        <el-form-item label="注释">
          <el-input v-model="formData.comment" type="textarea" :rows="3" placeholder="请输入注释" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="formData.sortOrder" :min="1" controls-position="right" style="width: 200px" />
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
.attribute-config-list {
  background: var(--el-bg-color);
}
</style>
