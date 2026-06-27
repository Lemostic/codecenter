<script setup lang="ts">
/**
 * ModelDetail - 模型管理页面（Tab 容器页）
 *
 * 点击卡片后进入的模型详情页，包含 7 个页签：
 * 模型管理、属性配置、填报设计、数据展现、相似规则、编码规则、质量规则
 *
 * 对应 Figma 设计稿：
 * - Frame 552 (node 437:65323)：顶部导航栏
 * - Frame 547 (node 437:43016)：模型信息卡片 + 操作按钮 + 版本 + 数据表格
 */

// ========== 1. 外部 import（vue → 第三方 → 业务模块）==========
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useI18n } from 'vue-i18n';
import type { AxiosResponse } from 'axios';
import { ArrowLeft, ArrowUp, Close, Document } from '@element-plus/icons-vue';
import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';
import TpEmpty from '@mdm/common/components/data/TpEmpty.vue';
import { getModel, disableModel, enableModel } from '@/modules/model-design/api/model';
import type { ModelVO, ModelStatus, StandardFile } from '@/modules/model-design/types/model';
import { MODEL_STATUS_DOT_COLOR } from '@/modules/model-design/types/model';
import type { ApiResponse } from '@mdm/common/types/api';
import AttributeConfigPanel from '@/modules/model-design/components/attribute/AttributeConfigPanel.vue';
import FormDesignPanel from '@/modules/model-design/components/form-design/FormDesignPanel.vue';
import SimilarityRulePanel from '@/modules/model-design/components/similarity-rule/SimilarityRulePanel.vue';
import CodingRulePanel from '@/modules/model-design/components/coding-rule/CodingRulePanel.vue';
import QualityRulePanel from '@/modules/model-design/components/quality-rule/QualityRulePanel.vue';

// ========== 2. defineOptions ==========
defineOptions({ name: 'ModelDetail' });

// ========== 5. 响应式数据 ==========
const { t } = useI18n();
const router = useRouter();
const route = useRoute();
const loading = ref(false);
const model = ref<ModelVO | null>(null);
const activeTab = ref('model-manage');

/** 基本信息区域收起/展开 */
const basicInfoCollapsed = ref(false);

/** 停用/启用操作 loading 状态（§14.5 异步按钮必须有 :loading） */
const togglingStatus = ref(false);

/** 当前选中版本 */
const currentVersion = ref('v2');

/** 版本列表 */
const versionOptions = ref([
  { value: 'v2', label: 'v2' },
  { value: 'v1', label: 'v1' },
]);

/** 属性表格行类型（§17.2 禁止 any） */
interface AttributeRow {
  id: number;
  sortNum: number;
  attrName: string;
  attrCode: string;
  dataType: string;
  required: boolean;
  description: string;
}

/** 数据表格选中行 */
const selectedRows = ref<AttributeRow[]>([]);

/** 属性字段表格数据（模型管理Tab展示，后续替换为真实 API 数据） */
const attributeTableData = ref<AttributeRow[]>([
  { id: 1, sortNum: 1, attrName: '项目进度信息', attrCode: 'project_progress', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 2, sortNum: 2, attrName: '部门信息', attrCode: 'department', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 3, sortNum: 3, attrName: '销售合同信息', attrCode: 'sales_contract', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 4, sortNum: 4, attrName: '年度培训计划', attrCode: 'training_plan', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 5, sortNum: 5, attrName: '项目台账信息', attrCode: 'project_ledger', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 6, sortNum: 6, attrName: '同步省份信息', attrCode: 'sync_province', dataType: 'VARCHAR(100)', required: false, description: '' },
  { id: 7, sortNum: 7, attrName: '工基本信息', attrCode: 'work_basic', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 8, sortNum: 8, attrName: 'DW销售机会信息', attrCode: 'dw_sales_opportunity', dataType: 'VARCHAR(200)', required: false, description: '' },
  { id: 9, sortNum: 9, attrName: '人员基本信息', attrCode: 'person_basic', dataType: 'VARCHAR(200)', required: true, description: '' },
  { id: 10, sortNum: 10, attrName: '岗位信息', attrCode: 'position', dataType: 'VARCHAR(100)', required: false, description: '' },
  { id: 11, sortNum: 11, attrName: '组织机构', attrCode: 'organization', dataType: 'VARCHAR(200)', required: true, description: '' },
  { id: 12, sortNum: 12, attrName: '学历信息', attrCode: 'education', dataType: 'VARCHAR(50)', required: false, description: '' },
  { id: 13, sortNum: 13, attrName: '联系方式', attrCode: 'contact', dataType: 'VARCHAR(100)', required: false, description: '' },
]);

// Tab配置（对应Figma设计稿中的7个功能Tab）
const tabs = [
  { name: 'model-manage', label: '模型管理', icon: '/images/model-detail/tab_model_manage.svg' },
  { name: 'attr-config', label: '属性配置', icon: '/images/model-detail/tab_attr_config.svg' },
  { name: 'form-design', label: '填报设计', icon: '/images/model-detail/tab_form_design.svg' },
  { name: 'data-display', label: '数据展现', icon: '/images/model-detail/tab_data_display.svg' },
  { name: 'code-rule', label: '编码规则', icon: '/images/model-detail/tab_code_rule.svg' },
  { name: 'similar-rule', label: '相似规则', icon: '/images/model-detail/tab_similar_rule.svg' },
  { name: 'quality-rule', label: '质量规则', icon: '/images/model-detail/tab_quality_rule.svg' },
];

/** 未知/默认状态圆点颜色（对应 EP --el-color-info，JS 侧无法引用 CSS 变量） */
const DEFAULT_STATUS_DOT = '#909399';

// ========== 6. Computed 计算属性 ==========

/** 关联标准文件标签（从 model.standardFiles 派生） */
const standardTags = computed(() => {
  if (!model.value?.standardFiles) return [];
  return model.value.standardFiles.map((f: StandardFile) => ({
    id: f.id,
    name: f.name,
  }));
});

/** 模型状态圆点颜色 */
const statusDotColor = computed(() => {
  if (!model.value) return DEFAULT_STATUS_DOT;
  return MODEL_STATUS_DOT_COLOR[model.value.status as ModelStatus] || DEFAULT_STATUS_DOT;
});

/** 状态徽标背景色（根据状态动态变化） */
const statusBadgeBg = computed(() => {
  const color = statusDotColor.value;
  return `${color}1a`;
});

/** 是否为停用状态 */
const isDisabled = computed(() => model.value?.status === 'disabled');

// ========== 7. 方法 ==========

const handleTabClick = (name: string) => {
  activeTab.value = name;
};

const handleBack = () => {
  router.push({ name: 'model-design-list' });
};

/** 修订 → 跳转编辑页 */
const handleRevise = () => {
  if (!model.value) return;
  router.push({ name: 'model-design-editor', params: { id: model.value.id } });
};

/** 停用/启用切换（§7.3 try/catch/finally + TpMessage + console.error + :loading） */
const handleToggleStatus = async () => {
  if (!model.value || togglingStatus.value) return;
  togglingStatus.value = true;
  try {
    if (isDisabled.value) {
      await enableModel(model.value.id);
      model.value.status = 'active';
      model.value.statusLabel = '生效';
    } else {
      await disableModel(model.value.id);
      model.value.status = 'disabled';
      model.value.statusLabel = '停用';
    }
  } catch (error) {
    // 错误由 core/http 拦截器统一处理提示
    console.error('[handleToggleStatus]', error);
  } finally {
    togglingStatus.value = false;
  }
};

/** 数据规范 */
const handleDataSpec = () => {
  // TODO: 下一阶段实现
};

/** 版本对比 */
const handleVersionCompare = () => {
  // TODO: 下一阶段实现
};

/** 移除关联标准文件标签 */
const handleRemoveTag = (tagId: string | number) => {
  if (!model.value?.standardFiles) return;
  model.value.standardFiles = model.value.standardFiles.filter((f: StandardFile) => f.id !== tagId);
};

/** 表格选中行变化 */
const handleSelectionChange = (rows: AttributeRow[]) => {
  selectedRows.value = rows;
};

/** 编辑属性（TODO: 下一阶段实现） */
const handleEditAttribute = (_row: AttributeRow) => {
  // TODO: 下一阶段实现
};

/** 删除属性（TODO: 下一阶段实现） */
const handleDeleteAttribute = (_row: AttributeRow) => {
  // TODO: 下一阶段实现
};

// ========== 9. 生命周期 ==========

/** 加载模型详情（§7.3 try/catch/finally + console.error） */
const loadModel = async () => {
  const id = route.params.id as string;
  loading.value = true;
  try {
    const res = (await getModel(id)) as unknown as AxiosResponse<ApiResponse<ModelVO>>;
    if (res.data?.success && res.data?.data) {
      model.value = res.data.data;
    }
  } catch (error) {
    model.value = null;
    console.error('[loadModel]', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadModel();
});
</script>

<template>
  <TpPageFrame>
    <!-- ========== 模型信息导航栏（Figma Frame 552）========== -->
    <div class="model-detail-header flex-shrink-0">
      <div class="header-left">
        <el-icon class="back-arrow" @click="handleBack"><ArrowLeft /></el-icon>
        <div class="model-icon-wrap">
          <img src="/images/model-detail/model_icon.svg" alt="" class="model-icon" />
        </div>
        <div class="model-meta">
          <span class="model-name">{{ model?.name || t('modelDesign.detail.loading') }}</span>
          <span class="model-code">{{ model?.code || '' }}</span>
        </div>
      </div>
      <div class="header-tabs">
        <div
          v-for="tab in tabs"
          :key="tab.name"
          class="header-tab"
          :class="{ 'is-active': activeTab === tab.name }"
          @click="handleTabClick(tab.name)"
        >
          <img :src="tab.icon" :alt="tab.label" class="tab-icon-img" />
          <span class="tab-label-text">{{ tab.label }}</span>
        </div>
      </div>
    </div>

    <!-- ========== Tab内容区 ========== -->
    <div v-loading="loading" class="flex-1 min-h-0 overflow-auto">
      <template v-if="model">
        <el-tabs v-model="activeTab" class="model-detail-tabs">
          <!-- ===== 模型管理 Tab ===== -->
          <el-tab-pane name="model-manage">
            <!-- 模型信息卡片（Figma Frame 547） -->
            <div class="py-4 px-6 border-b border-[var(--el-border-color-lighter)]">
              <div class="flex items-center justify-between mb-2">
                <span class="text-sm font-medium text-[var(--el-text-color-primary)]">{{ t('modelDesign.detail.section.basic') }}</span>
                <el-icon
                  class="cursor-pointer text-[var(--el-text-color-secondary)] hover:text-[var(--el-text-color-primary)] transition-transform duration-200"
                  :class="{ 'rotate-180': basicInfoCollapsed }"
                  @click="basicInfoCollapsed = !basicInfoCollapsed"
                >
                  <ArrowUp />
                </el-icon>
              </div>
              <div v-show="!basicInfoCollapsed" class="grid grid-cols-2 gap-y-2 gap-x-8">
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.name') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.name }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.topic') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.topicName || '-' }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.code') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.code }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.creator') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.creatorName }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.tableName') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.tableName }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.createdAt') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.createdAt }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.modelType') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.modelTypeLabel }}</span>
                </div>
                <div class="flex items-center gap-1 text-sm">
                  <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.description') }}：</span>
                  <span class="text-[var(--el-text-color-primary)]">{{ model.description || '-' }}</span>
                </div>
                <div v-if="standardTags.length" class="col-span-2 flex items-start gap-2 flex-wrap">
                  <span class="text-sm text-[var(--el-text-color-secondary)] shrink-0 pt-0.5">{{ t('modelDesign.detail.label.standardFile') }}：</span>
                  <div class="flex flex-wrap gap-2">
                    <div
                      v-for="tag in standardTags"
                      :key="tag.id"
                      class="inline-flex items-center gap-1 px-2 py-0.5 border border-[var(--el-border-color-lighter)] rounded text-xs text-[var(--el-text-color-primary)] bg-white"
                    >
                      <span class="max-w-[240px] truncate">{{ tag.name }}</span>
                      <el-icon class="text-xs text-[var(--el-text-color-secondary)] cursor-pointer hover:text-[var(--el-text-color-primary)]" @click="handleRemoveTag(tag.id)">
                        <Close />
                      </el-icon>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 操作按钮栏 -->
            <div class="flex items-center gap-2 py-3 px-6">
              <el-button size="default" type="primary" @click="handleRevise">{{ t('modelDesign.detail.btn.revise') }}</el-button>
              <el-button
                size="default"
                :loading="togglingStatus"
                @click="handleToggleStatus"
              >
                {{ isDisabled ? t('modelDesign.detail.btn.enable') : t('modelDesign.detail.btn.disable') }}
              </el-button>
              <el-button size="default" @click="handleDataSpec">{{ t('modelDesign.detail.btn.dataSpec') }}</el-button>
              <el-button size="default" @click="handleVersionCompare">{{ t('modelDesign.detail.btn.versionCompare') }}</el-button>
            </div>

            <!-- 版本选择器 + 状态标识 -->
            <div class="flex items-center justify-between py-2 px-6">
              <div class="flex items-center gap-2">
                <span class="text-sm text-[var(--el-text-color-primary)]">{{ t('modelDesign.detail.label.version') }}：</span>
                <el-select v-model="currentVersion" size="default" style="width: 80px">
                  <el-option
                    v-for="v in versionOptions"
                    :key="v.value"
                    :value="v.value"
                    :label="v.label"
                  />
                </el-select>
              </div>
              <div
                class="inline-flex items-center gap-1.5 px-3 py-1 rounded"
                :style="{ backgroundColor: statusBadgeBg }"
              >
                <span class="w-1.5 h-1.5 rounded-full shrink-0" :style="{ backgroundColor: statusDotColor }" />
                <span class="text-xs">{{ model.statusLabel }}</span>
              </div>
            </div>

            <!-- 数据表格区域 -->
            <div class="px-6 pb-6">
              <TpSectionTitle :title="t('modelDesign.detail.attrTableTitle')" />
              <el-table
                :data="attributeTableData"
                border
                stripe
                size="default"
                class="model-attr-table"
                @selection-change="handleSelectionChange"
              >
                <el-table-column type="selection" min-width="48" align="center" />
                <el-table-column prop="sortNum" :label="t('modelDesign.detail.attrTableCol.sortNum')" min-width="70" align="center" sortable />
                <el-table-column prop="attrName" :label="t('modelDesign.detail.attrTableCol.attrName')" min-width="160" sortable show-overflow-tooltip />
                <el-table-column prop="attrCode" :label="t('modelDesign.detail.attrTableCol.attrCode')" min-width="160" show-overflow-tooltip />
                <el-table-column prop="dataType" :label="t('modelDesign.detail.attrTableCol.dataType')" min-width="120" />
                <el-table-column prop="required" :label="t('modelDesign.detail.attrTableCol.required')" min-width="90" align="center">
                  <template #default="{ row }">
                    <span>{{ row.required ? t('common.yes') : t('common.no') }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="description" :label="t('modelDesign.detail.attrTableCol.description')" min-width="160" show-overflow-tooltip>
                  <template #default="{ row }">
                    {{ row.description || '-' }}
                  </template>
                </el-table-column>
                <el-table-column :label="t('modelDesign.detail.attrTableCol.action')" min-width="120" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button size="default" link type="primary" @click="handleEditAttribute(row as AttributeRow)">{{ t('modelDesign.detail.attrTableAction.edit') }}</el-button>
                    <el-button size="default" link type="danger" @click="handleDeleteAttribute(row as AttributeRow)">{{ t('modelDesign.detail.attrTableAction.delete') }}</el-button>
                  </template>
                </el-table-column>
                <template #empty>
                  <TpEmpty state="no-data" :description="t('modelDesign.detail.emptyAttr')" />
                </template>
              </el-table>
            </div>
          </el-tab-pane>

          <!-- ===== 属性配置 Tab ===== -->
          <el-tab-pane name="attr-config">
            <AttributeConfigPanel
              :model-id="model.id"
              :model-status="model.status"
              :model-version="model.version"
            />
          </el-tab-pane>

          <!-- ===== 填报设计 Tab ===== -->
          <el-tab-pane name="form-design">
            <FormDesignPanel
              :model-id="model.id"
              :model-status="model.status"
              :model-version="model.version"
            />
          </el-tab-pane>

          <!-- ===== 数据展现 Tab ===== -->
          <el-tab-pane name="data-display">
            <!-- §13.1 使用 TpEmpty 替代 el-empty -->
            <TpEmpty state="no-data" description="数据展现功能将在下一阶段实现" />
          </el-tab-pane>

          <!-- ===== 相似规则 Tab ===== -->
          <el-tab-pane name="similar-rule">
            <SimilarityRulePanel
              :model-id="model.id"
              :model-status="model.status"
              :model-version="model.version"
            />
          </el-tab-pane>

          <!-- ===== 编码规则 Tab ===== -->
          <el-tab-pane name="code-rule">
            <CodingRulePanel
              :model-id="model.id"
              :model-status="model.status"
              :model-version="model.version"
            />
          </el-tab-pane>

          <!-- ===== 质量规则 Tab ===== -->
          <el-tab-pane name="quality-rule">
            <QualityRulePanel
              :model-id="model.id"
              :model-status="model.status"
              :model-version="model.version"
            />
          </el-tab-pane>
        </el-tabs>
      </template>
    </div>
  </TpPageFrame>
</template>

<style scoped>
/* ========== 模型详情导航栏（Figma Frame 552）========== */
.model-detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 70px;
  border-bottom: 1px solid var(--el-border-color-lighter);
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-arrow {
  font-size: 16px;
  color: var(--el-text-color-secondary);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.2s;
}

.back-arrow:hover {
  color: var(--el-color-primary);
}

.model-icon-wrap {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
}

.model-icon {
  width: 100%;
  height: 100%;
}

.model-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-name {
  font-family: 'PingFang SC', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: var(--el-color-black);
  line-height: 20px;
  letter-spacing: 0.07em;
}

.model-code {
  font-family: 'PingFang SC', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 20px;
  letter-spacing: 0.08em;
}

/* ========== 右侧Tab导航 ========== */
.header-tabs {
  display: flex;
  align-items: center;
}

.header-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  padding: 6px 16px;
  cursor: pointer;
  border-radius: 4px;
  transition: background-color 0.2s;
  user-select: none;
}

.header-tab:hover {
  background-color: var(--el-fill-color-light);
}

.header-tab .tab-icon-img {
  width: 20px;
  height: 20px;
  filter: grayscale(100%) brightness(0.6);
  transition: filter 0.2s;
}

.header-tab.is-active .tab-icon-img {
  filter: grayscale(0%) brightness(100%);
}

.header-tab .tab-label-text {
  font-family: 'PingFang SC', sans-serif;
  font-size: 12px;
  font-weight: 500;
  color: var(--el-text-color-primary);
  line-height: 20px;
  transition: color 0.2s;
}

.header-tab.is-active .tab-label-text {
  color: var(--el-color-primary);
  font-weight: 700;
}

/* ========== 隐藏el-tabs原生导航栏 ========== */
.model-detail-tabs :deep(.el-tabs__header) {
  display: none !important;
}

.model-detail-tabs :deep(.el-tabs__content) {
  padding: 0;
}

/* 表格样式覆盖 */
.model-attr-table {
  width: 100%;
}

.model-attr-table :deep(.el-table__header th) {
  background: #f5f7fa;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.model-attr-table :deep(.el-table__body td) {
  font-size: 12px;
  color: var(--el-text-color-primary);
}
</style>
