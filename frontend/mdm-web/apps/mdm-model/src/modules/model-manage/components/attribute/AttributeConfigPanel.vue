<script setup lang="ts">
/**
 * AttributeConfigPanel - 属性配置 Tab 面板（§17 属性配置主页）
 *
 * 属性配置列表：属性名称/英文名称/数据类型 + 配置项列
 * （流程字段/匹配字段/文件字段/关联对象/计算表达式）
 *
 * §17.2 草稿态显示当前编辑中版本的启用属性
 * §17.3 默认选中第一行
 * §17.4 数据类型不支持配置项时复选框置灰
 * §17.5 配置项提示图标 hover 说明
 */
import { ref, computed, onMounted, watch } from 'vue';
import type { AxiosResponse } from 'axios';
import { QuestionFilled } from '@element-plus/icons-vue';
import TpTable, { type TpTableColumn } from '@mdm/common/components/data/TpTable.vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import {
  listAttribute, saveMatchFieldConfig, saveProcessFieldConfig,
} from '@/modules/model-design/api/attribute';
import type {
  AttributeVO, AttributeQuery, DataCategory,
} from '@/modules/model-design/types/attribute';
import { DATA_CATEGORY_CONFIG_SUPPORT } from '@/modules/model-design/types/attribute';
import type { ApiResponse, PaginatedResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import RelatedObjectConfig from './RelatedObjectConfig.vue';
import ExpressionEditor from './ExpressionEditor.vue';

defineOptions({ name: 'AttributeConfigPanel' });

const props = defineProps<{
  modelId: ID;
  modelStatus: string;
  modelVersion: number;
}>();

// ========== 状态 ==========
const loading = ref(false);
const attributes = ref<AttributeVO[]>([]);
const selectedRowId = ref<ID | null>(null);

/** §21.2 匹配字段校验状态 */
const matchFieldValidation = ref<Record<string, 'checking' | 'success' | 'error' | null>>({});

// §22 关联对象配置弹窗
const relationDialogVisible = ref(false);
const relationAttribute = ref<AttributeVO | null>(null);

// §23 计算表达式弹窗
const expressionDialogVisible = ref(false);
const expressionAttribute = ref<AttributeVO | null>(null);

/** 配置项说明 */
const CONFIG_TOOLTIPS: Record<string, string> = {
  processField: '流程关键字段：标记该字段用于流程类型匹配，支持多字段指定',
  matchField: '匹配字段：用于数据唯一性校验，勾选后系统实时校验组合值是否唯一',
  fileField: '文件字段：配置为文件字段后，数据维护页面该字段独占一行显示',
  relationConfig: '关联对象：配置该字段关联其他模型或字典，支持带出字段和过滤规则',
  expressionConfig: '计算表达式：配置该字段的自动计算规则，支持函数和字段运算',
};

// ========== 列配置 ==========
const columns: TpTableColumn[] = [
  { prop: 'name', label: '属性名称', minWidth: 140, showOverflowTooltip: true },
  { prop: 'englishName', label: '英文名称', minWidth: 130, showOverflowTooltip: true },
  { prop: 'dataType', label: '数据类型', minWidth: 100 },
  { prop: 'processField', label: '流程关键字段', minWidth: 110 },
  { prop: 'matchField', label: '匹配字段', minWidth: 90 },
  { prop: 'fileField', label: '文件字段', minWidth: 90 },
  { prop: 'relationConfig', label: '关联对象', minWidth: 110 },
  { prop: 'expressionConfig', label: '计算表达式', minWidth: 110 },
];

// ========== 计算属性 ==========
const isModelLocked = computed(() => props.modelStatus !== 'draft');

/** 获取属性的数据分类 */
const getDataCategory = (row: AttributeVO): DataCategory => row.dataCategory;

/** 检查某配置项是否支持 */
const isConfigSupported = (row: AttributeVO, configKey: keyof typeof DATA_CATEGORY_CONFIG_SUPPORT['string']): boolean => {
  const cat = getDataCategory(row);
  return DATA_CATEGORY_CONFIG_SUPPORT[cat]?.[configKey] ?? false;
};

// ========== 方法 ==========

/** 加载属性列表 */
const loadAttributes = async () => {
  loading.value = true;
  try {
    const query: AttributeQuery = {
      modelId: props.modelId,
      version: props.modelVersion,
      page: 1,
      pageSize: 500,
      status: 'enabled',
    };
    const res = (await listAttribute(query)) as unknown as AxiosResponse<ApiResponse<PaginatedResponse<AttributeVO>>>;
    const pageData = res.data?.data;
    if (pageData) {
      attributes.value = pageData.rows ?? [];
      // §17.3 默认选中第一行
      if (attributes.value.length > 0 && !selectedRowId.value) {
        selectedRowId.value = attributes.value[0].id;
      }
    }
  } catch (error) {
    console.error('[AttributeConfigPanel] load error', error);
  } finally {
    loading.value = false;
  }
};

/** 流程字段切换（§21.1 实时保存） */
const handleProcessFieldChange = async (row: AttributeVO, value: boolean) => {
  if (!isConfigSupported(row, 'processField')) return;
  try {
    await saveProcessFieldConfig(row.id, value);
    row.processField = value;
    TpMessage.success('流程字段配置已保存');
  } catch (error) {
    console.error('[AttributeConfigPanel] save process field error', error);
  }
};

/** 匹配字段切换（§20.2 + §21.2 唯一性校验 + §21.3 互斥校验） */
const handleMatchFieldChange = async (row: AttributeVO, value: boolean) => {
  if (!isConfigSupported(row, 'matchField')) return;

  // §21.3 互斥校验：匹配字段与计算表达式不能同时配置
  if (value && row.expressionConfig) {
    TpMessage.warning('匹配字段与计算表达式不能同时配置，请先取消计算表达式');
    return;
  }

  // §21.2 唯一性校验
  matchFieldValidation.value[row.id] = 'checking';
  try {
    await saveMatchFieldConfig(row.id, value);
    row.matchField = value;
    // Mock 校验结果（实际应调用后端接口）
    matchFieldValidation.value[row.id] = 'success';
    TpMessage.success('匹配字段配置已保存');
  } catch (error) {
    matchFieldValidation.value[row.id] = 'error';
    TpMessage.error('匹配字段校验失败，组合值不唯一');
  }
};

/** 获取匹配字段校验状态 */
const getMatchFieldStatus = (row: AttributeVO) => {
  return matchFieldValidation.value[row.id] || null;
};

/** 文件字段切换 */
const handleFileFieldChange = async (row: AttributeVO, value: boolean) => {
  if (!isConfigSupported(row, 'fileField')) return;
  row.fileField = value;
  TpMessage.success('文件字段配置已保存');
};

/** 打开关联对象配置 */
const handleOpenRelation = (row: AttributeVO) => {
  if (!isConfigSupported(row, 'relationConfig')) return;
  relationAttribute.value = row;
  relationDialogVisible.value = true;
};

/** 关闭关联对象配置 */
const handleCloseRelation = () => {
  relationDialogVisible.value = false;
  relationAttribute.value = null;
};

/** 打开计算表达式配置 */
const handleOpenExpression = (row: AttributeVO) => {
  if (!isConfigSupported(row, 'expressionConfig')) return;

  // §21.3 互斥校验：计算表达式与匹配字段不能同时配置
  if (row.matchField) {
    TpMessage.warning('计算表达式与匹配字段不能同时配置，请先取消匹配字段');
    return;
  }

  expressionAttribute.value = row;
  expressionDialogVisible.value = true;
};

/** 关闭计算表达式 */
const handleCloseExpression = () => {
  expressionDialogVisible.value = false;
  expressionAttribute.value = null;
};

/** 确认表达式 */
const handleConfirmExpression = (expr: string) => {
  if (expressionAttribute.value) {
    expressionAttribute.value.expressionSummary = expr.length > 30 ? expr.slice(0, 30) + '...' : expr;
    TpMessage.success('表达式已保存');
  }
  handleCloseExpression();
};

// ========== 导入导出 ==========
const importDialogVisible = ref(false);
const importProgress = ref(0);
const importResult = ref<{ success: number; fail: number } | null>(null);

/** §18 导出属性配置 */
const handleExport = () => {
  TpMessage.success('属性配置导出功能（Mock：实际使用 xlsx 库生成 Excel）');
  // Mock: 实际应调用 exportAttributeConfig API
};

/** §19.2 下载导入模板 */
const handleDownloadTemplate = () => {
  TpMessage.success('导入模板下载（Mock：实际生成 Excel 模板）');
};

/** §19.4 导入文件处理 */
const handleImportFile = (file: File) => {
  importProgress.value = 0;
  importResult.value = null;
  // Mock: 模拟导入进度
  const timer = setInterval(() => {
    importProgress.value += 20;
    if (importProgress.value >= 100) {
      clearInterval(timer);
      importResult.value = { success: attributes.value.length, fail: 0 };
      TpMessage.success('导入成功');
    }
  }, 300);
  return false; // 阻止默认上传
};

// ========== 生命周期 ==========
onMounted(() => { loadAttributes(); });
watch(() => [props.modelId, props.modelVersion], () => {
  selectedRowId.value = null;
  loadAttributes();
});
</script>

<template>
  <div class="attribute-config-panel flex flex-col h-full">
    <!-- 工具栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <div class="flex items-center gap-2">
        <span class="text-sm font-medium">属性配置</span>
        <span class="text-xs text-[var(--el-text-color-secondary)]">共 {{ attributes.length }} 个启用属性</span>
      </div>
      <div class="flex gap-2">
        <!-- §18.1 导出按钮 -->
        <el-button size="small" @click="handleExport">导出</el-button>
        <!-- §19 导入按钮 -->
        <el-button size="small" @click="importDialogVisible = true">导入</el-button>
      </div>
    </div>

    <!-- 配置项说明提示行（§17.5） -->
    <div class="flex items-center gap-4 px-4 py-2 bg-[var(--el-fill-color-lighter)] text-xs text-[var(--el-text-color-secondary)]">
      <template v-for="(tip, key) in CONFIG_TOOLTIPS" :key="key">
        <el-tooltip :content="tip" placement="bottom" :show-after="300">
          <span class="inline-flex items-center gap-1 cursor-help">
            <el-icon :size="12"><QuestionFilled /></el-icon>
            <span>{{ { processField: '流程字段', matchField: '匹配字段', fileField: '文件字段', relationConfig: '关联对象', expressionConfig: '计算表达式' }[key] }}</span>
          </span>
        </el-tooltip>
      </template>
    </div>

    <!-- 属性配置表格 -->
    <div class="flex-1 min-h-0 px-4">
      <TpTable
        :columns="columns"
        :data="attributes"
        :loading="loading"
        :total="attributes.length"
        :page-size="500"
        border
        stripe
      >
        <!-- 属性名称 -->
        <template #col-name="{ row }">
          <span>{{ row.name }}</span>
          <span v-if="row.status === 'disabled'" class="text-red-500 ml-1 text-xs">(已停用)</span>
        </template>

        <!-- 数据类型 -->
        <template #col-dataType="{ row }">
          <span>{{ row.dataType }}<template v-if="row.length">({{ row.length }}<template v-if="row.precision">,{{ row.precision }}</template>)</template></span>
        </template>

        <!-- 流程关键字段（§20.1 复选框形式） -->
        <template #col-processField="{ row }">
          <el-checkbox
            :model-value="row.processField"
            :disabled="!isConfigSupported(row, 'processField') || isModelLocked"
            @change="(val: boolean) => handleProcessFieldChange(row as AttributeVO, val)"
          />
        </template>

        <!-- 匹配字段（§20.2 + §21.2 校验状态） -->
        <template #col-matchField="{ row }">
          <div class="flex items-center gap-1">
            <el-checkbox
              :model-value="row.matchField"
              :disabled="!isConfigSupported(row, 'matchField') || isModelLocked"
              @change="(val: boolean) => handleMatchFieldChange(row as AttributeVO, val)"
            />
            <el-icon v-if="getMatchFieldStatus(row) === 'checking'" class="is-loading text-[var(--el-color-primary)]">
              <i class="el-icon-loading" />
            </el-icon>
            <el-tooltip v-if="getMatchFieldStatus(row) === 'success'" content="唯一性校验通过" placement="top">
              <el-icon class="text-[var(--el-color-success)]"><i class="el-icon-circle-check-filled" /></el-icon>
            </el-tooltip>
            <el-tooltip v-if="getMatchFieldStatus(row) === 'error'" content="组合值不唯一" placement="top">
              <el-icon class="text-[var(--el-color-danger)]"><i class="el-icon-circle-close-filled" /></el-icon>
            </el-tooltip>
          </div>
        </template>

        <!-- 文件字段（§20.6） -->
        <template #col-fileField="{ row }">
          <el-checkbox
            :model-value="row.fileField"
            :disabled="!isConfigSupported(row, 'fileField') || isModelLocked"
            @change="(val: boolean) => handleFileFieldChange(row as AttributeVO, val)"
          />
        </template>

        <!-- 关联对象 -->
        <template #col-relationConfig="{ row }">
          <el-button
            v-if="isConfigSupported(row, 'relationConfig')"
            size="small"
            link
            type="primary"
            :disabled="isModelLocked"
            @click="handleOpenRelation(row as AttributeVO)"
          >
            {{ row.relationDisplay || '配置' }}
          </el-button>
          <span v-else class="text-[var(--el-text-color-placeholder)] text-xs">不支持</span>
        </template>

        <!-- 计算表达式 -->
        <template #col-expressionConfig="{ row }">
          <el-button
            v-if="isConfigSupported(row, 'expressionConfig')"
            size="small"
            link
            type="primary"
            :disabled="isModelLocked"
            @click="handleOpenExpression(row as AttributeVO)"
          >
            {{ row.expressionSummary || '配置' }}
          </el-button>
          <span v-else class="text-[var(--el-text-color-placeholder)] text-xs">不支持</span>
        </template>
      </TpTable>
    </div>

    <!-- §22 关联对象配置弹窗 -->
    <el-dialog v-model="relationDialogVisible" title="关联对象配置" width="800px" destroy-on-close>
      <RelatedObjectConfig
        v-if="relationAttribute"
        :model-id="props.modelId"
        :attribute-id="relationAttribute.id"
        :attribute-name="relationAttribute.name"
        @close="handleCloseRelation"
      />
    </el-dialog>

    <!-- §23 计算表达式弹窗 -->
    <el-dialog v-model="expressionDialogVisible" title="计算表达式" width="90%" top="5vh" destroy-on-close>
      <ExpressionEditor
        v-if="expressionAttribute"
        :model-id="props.modelId"
        :attribute-name="expressionAttribute.name"
        :initial-expression="expressionAttribute.expressionSummary"
        @close="handleCloseExpression"
        @confirm="handleConfirmExpression"
      />
    </el-dialog>

    <!-- §19 导入弹窗 -->
    <el-dialog v-model="importDialogVisible" title="属性配置导入" width="500px">
      <div class="space-y-4">
        <!-- §19.1 导入模式 -->
        <div class="flex items-center gap-2">
          <span class="text-sm">导入模式：</span>
          <el-radio-group model-value="realtime" size="small">
            <el-radio value="realtime">实时</el-radio>
            <el-radio value="async">异步</el-radio>
          </el-radio-group>
        </div>

        <!-- §19.2 下载模板 -->
        <el-button size="small" @click="handleDownloadTemplate">下载导入模板</el-button>

        <!-- 上传区域 -->
        <el-upload
          drag
          accept=".xlsx,.xls"
          :auto-upload="false"
          :show-file-list="false"
          :on-change="(file: any) => handleImportFile(file.raw)"
        >
          <div class="el-upload__text">
            拖拽文件到此处或 <em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">支持 .xlsx / .xls 格式</div>
          </template>
        </el-upload>

        <!-- §19.5 导入进度 -->
        <div v-if="importProgress > 0" class="space-y-2">
          <el-progress :percentage="importProgress" :status="importProgress >= 100 ? 'success' : ''" />
          <div v-if="importResult" class="text-sm">
            <span class="text-[var(--el-color-success)]">成功：{{ importResult.success }} 条</span>
            <span v-if="importResult.fail > 0" class="text-[var(--el-color-danger)] ml-4">失败：{{ importResult.fail }} 条</span>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.attribute-config-panel {
  background: var(--el-bg-color);
}
</style>
