<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { AxiosResponse } from 'axios';
import { Plus, Delete, Download, Upload, ArrowLeft } from '@element-plus/icons-vue';
import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';
import TpSectionTitle from '@mdm/common/components/layout/TpSectionTitle.vue';
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import type { ApiResponse } from '@mdm/common/types/api';
import { getModel } from '../api/model';
import { listAttribute, deleteAttribute, batchDeleteAttribute, enableAttribute, disableAttribute } from '../api/attribute';
import type { ModelVO } from '../types/model';
import type { AttributeVO, AttributeQuery } from '../types/attribute';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'ModelAttributeEditor' });

// ========== 路由 ==========
const route = useRoute();
const router = useRouter();
const modelId = computed(() => route.params.id as string);

// ========== i18n ==========
const { t } = useI18n();

// ========== 状态 ==========
const loading = ref(false);
const model = ref<ModelVO | null>(null);
const activeTab = ref('model-manage');

// 子模型
const selectedSubModelId = ref<string>('');

// 属性表格
const attrLoading = ref(false);
const attrList = ref<AttributeVO[]>([]);
const selectedAttrIds = ref<ID[]>([]);
const query = ref<AttributeQuery>({
  modelId: modelId.value,
  page: 1,
  pageSize: 20,
});
const total = ref(0);

// ========== tabs ==========
const tabs = [
  { key: 'model-manage', label: t('modelDesign.detail.tabs.modelManage'), icon: '/images/model-detail/tab_model_manage.svg' },
  { key: 'attr-config', label: t('modelDesign.detail.tabs.attrConfig'), icon: '/images/model-detail/tab_attr_config.svg' },
  { key: 'form-design', label: t('modelDesign.detail.tabs.formDesign'), icon: '/images/model-detail/tab_form_design.svg' },
  { key: 'data-display', label: t('modelDesign.detail.tabs.dataDisplay'), icon: '/images/model-detail/tab_data_display.svg' },
  { key: 'coding-rule', label: t('modelDesign.detail.tabs.codeRule'), icon: '/images/model-detail/tab_code_rule.svg' },
  { key: 'similar-rule', label: t('modelDesign.detail.tabs.similarRule'), icon: '/images/model-detail/tab_similar_rule.svg' },
  { key: 'quality-rule', label: t('modelDesign.detail.tabs.qualityRule'), icon: '/images/model-detail/tab_quality_rule.svg' },
];

// ========== 计算属性 ==========
const statusBadge = computed(() => {
  if (!model.value) return null;
  const map: Record<string, { bg: string; dot: string; text: string }> = {
    draft: { bg: '#f0f9ff', dot: '#409EFF', text: model.value.statusLabel },
    active: { bg: '#f0f9eb', dot: '#67C23A', text: model.value.statusLabel },
    disabled: { bg: '#fdf6ec', dot: '#E6A23C', text: model.value.statusLabel },
    reviewing: { bg: '#f0f9ff', dot: '#409EFF', text: model.value.statusLabel },
  };
  return map[model.value.status] || { bg: '#f4f4f5', dot: '#909399', text: model.value.statusLabel };
});

const subModels = computed(() => model.value?.subModels || []);

// ========== 方法 ==========
const fetchModel = async () => {
  loading.value = true;
  try {
    const res = (await getModel(modelId.value)) as unknown as AxiosResponse<ApiResponse<ModelVO>>;
    if (res.data?.success && res.data?.data) {
      model.value = res.data.data;
    }
  } catch {
    TpMessage.error(t('common.loadFailed'));
  } finally {
    loading.value = false;
  }
};

const fetchAttributes = async () => {
  attrLoading.value = true;
  try {
    const res = (await listAttribute({ ...query.value, modelId: modelId.value })) as unknown as AxiosResponse<{ success: boolean; data?: { rows: AttributeVO[]; total: number } }>;
    attrList.value = res.data?.data?.rows || [];
    total.value = res.data?.data?.total || 0;
  } catch {
    attrList.value = [];
    total.value = 0;
  } finally {
    attrLoading.value = false;
  }
};

const handleTabClick = (key: string) => {
  activeTab.value = key;
};

const handleSubModelClick = (sub: { id: string }) => {
  selectedSubModelId.value = sub.id;
  query.value.page = 1;
  fetchAttributes();
};

const handleSizeChange = (size: number) => {
  query.value.pageSize = size;
  query.value.page = 1;
  fetchAttributes();
};

const handlePageChange = (page: number) => {
  query.value.page = page;
  fetchAttributes();
};

const handleSelectionChange = (rows: unknown) => {
  selectedAttrIds.value = (rows as AttributeVO[]).map((r: AttributeVO) => r.id);
};

const handleDelete = async (row: AttributeVO) => {
  try {
    await TpConfirm.delete(t('modelDesign.detail.deleteConfirm', { name: row.name }));
    await deleteAttribute(row.id);
    TpMessage.success(t('common.deleteSuccess'));
    fetchAttributes();
  } catch {
    // cancelled
  }
};

const handleBatchDelete = async () => {
  if (!selectedAttrIds.value.length) {
    TpMessage.warning(t('modelDesign.detail.selectFirst'));
    return;
  }
  try {
    await TpConfirm.confirm({ message: t('modelDesign.detail.batchDeleteConfirm'), type: 'warning' });
    await batchDeleteAttribute(selectedAttrIds.value);
    TpMessage.success(t('common.deleteSuccess'));
    selectedAttrIds.value = [];
    fetchAttributes();
  } catch {
    // cancelled
  }
};

const handleEnable = async (row: AttributeVO) => {
  try {
    await enableAttribute(row.id);
    TpMessage.success(t('modelDesign.detail.enableSuccess'));
    fetchAttributes();
  } catch {
    // error handled by interceptor
  }
};

const handleDisable = async (row: AttributeVO) => {
  try {
    await disableAttribute(row.id);
    TpMessage.success(t('modelDesign.detail.disableSuccess'));
    fetchAttributes();
  } catch {
    // error handled by interceptor
  }
};

// ========== watch ==========
watch(modelId, () => {
  fetchModel();
  fetchAttributes();
});

// ========== 生命周期 ==========
onMounted(() => {
  fetchModel();
  fetchAttributes();
});
</script>

<template>
  <TpPageFrame>
    <!-- 头部区域 -->
    <div class="bg-white rounded-lg mb-4 overflow-hidden">
      <!-- 标题行 -->
      <div class="flex items-center gap-3 px-6 py-4 border-b border-[var(--el-border-color-lighter)]">
        <el-button text class="back-arrow" @click="router.back()">
          <el-icon><Arrow-Left /></el-icon>
        </el-button>
        <div class="w-10 h-10 rounded-lg bg-[#e6f0ff] flex items-center justify-center shrink-0">
          <img src="/images/model-detail/model_icon.svg" alt="" class="w-6 h-6" />
        </div>
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-3">
            <span class="text-base font-semibold text-[var(--el-text-color-primary)]">
              {{ model?.name || t('modelDesign.detail.loading') }}
            </span>
            <span class="text-sm text-[var(--el-text-color-secondary)]">{{ model?.code || '' }}</span>
          </div>
        </div>
        <!-- Tab 导航 -->
        <div class="flex items-center gap-1">
          <div
            v-for="tab in tabs"
            :key="tab.key"
            class="flex items-center gap-1.5 px-3 py-2 rounded cursor-pointer transition-colors text-sm"
            :class="activeTab === tab.key ? 'text-[var(--el-color-primary)] bg-[var(--el-color-primary-light-9)]' : 'text-[var(--el-text-color-secondary)] hover:text-[var(--el-text-color-primary)]'"
            @click="handleTabClick(tab.key)"
          >
            <img :src="tab.icon" alt="" class="w-4 h-4" />
            <span>{{ tab.label }}</span>
          </div>
        </div>
      </div>

      <!-- 状态标签 + 信息卡片 -->
      <div class="px-6 py-4 border-b border-[var(--el-border-color-lighter)]">
        <div class="flex items-center gap-4 mb-3">
          <div
            v-if="statusBadge"
            class="inline-flex items-center gap-1.5 px-3 py-1 rounded"
            :style="{ backgroundColor: statusBadge.bg }"
          >
            <span class="w-1.5 h-1.5 rounded-full shrink-0" :style="{ backgroundColor: statusBadge.dot }" />
            <span class="text-xs">{{ statusBadge.text }}</span>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-y-2 gap-x-8">
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.version') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.versionLabel || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.modelType') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.modelTypeLabel || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.name') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.name || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.topic') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.topicName || '-' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.code') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.code || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.creator') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.creatorName || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.tableName') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.tableName || '' }}</span>
          </div>
          <div class="flex items-center gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.createdAt') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.createdAt || '' }}</span>
          </div>
          <div class="col-span-2 flex items-start gap-1 text-sm">
            <span class="text-[var(--el-text-color-secondary)] shrink-0">{{ t('modelDesign.detail.label.description') }}：</span>
            <span class="text-[var(--el-text-color-primary)]">{{ model?.description || '-' }}</span>
          </div>
          <div v-if="model?.standardFiles?.length" class="col-span-2 flex items-start gap-2 flex-wrap">
            <span class="text-sm text-[var(--el-text-color-secondary)] shrink-0 pt-0.5">{{ t('modelDesign.detail.label.standardFile') }}：</span>
            <div class="flex flex-wrap gap-2">
              <el-tag v-for="file in model.standardFiles" :key="file.id" size="small" type="info">
                {{ file.name }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="px-6 py-3 border-b border-[var(--el-border-color-lighter)] flex items-center gap-2 flex-wrap">
        <el-button type="primary" size="default" :icon="Plus">
          {{ t('modelDesign.detail.btn.addAttr') }}
        </el-button>
        <el-button size="default" :icon="Delete" :disabled="!selectedAttrIds.length" @click="handleBatchDelete">
          {{ t('modelDesign.detail.btn.batchDelete') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.enable') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.disable') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.importAttr') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.exportAttr') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.addFromStandard') }}
        </el-button>
        <el-button size="default" @click="TpMessage.info('功能开发中')">
          {{ t('modelDesign.detail.btn.smartReference') }}
        </el-button>
      </div>
    </div>

    <!-- 主体区域：左侧子模型树 + 右侧表格 -->
    <div class="flex gap-4">
      <!-- 左侧子模型树 -->
      <div v-if="subModels.length" class="w-56 bg-white rounded-lg p-4 shrink-0">
        <TpSectionTitle :title="t('modelDesign.detail.subModelTitle')" />
        <div class="space-y-1">
          <div
            v-for="sub in subModels"
            :key="sub.id"
            class="flex items-center gap-2 px-3 py-2 rounded cursor-pointer text-sm transition-colors"
            :class="selectedSubModelId === sub.id ? 'bg-[var(--el-color-primary-light-9)] text-[var(--el-color-primary)]' : 'text-[var(--el-text-color-primary)] hover:bg-[var(--el-fill-color-light)]'"
            @click="handleSubModelClick(sub)"
          >
            <span class="truncate">{{ sub.name }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧属性表格 -->
      <div class="flex-1 bg-white rounded-lg p-4 min-w-0">
        <TpSectionTitle :title="t('modelDesign.detail.attrTableTitle')" />
        <el-table
          :data="attrList"
          v-loading="attrLoading"
          size="default"
          class="model-attr-table"
          @selection-change="handleSelectionChange"
        >
          <template #empty>
            <TpEmpty :description="t('modelDesign.detail.emptyAttr')" />
          </template>
          <el-table-column type="selection" width="50" />
          <el-table-column type="index" :label="t('modelDesign.detail.col.index')" width="60" />
          <el-table-column prop="name" :label="t('modelDesign.detail.col.attrName')" min-width="120" show-overflow-tooltip />
          <el-table-column prop="englishName" :label="t('modelDesign.detail.col.englishName')" min-width="120" show-overflow-tooltip />
          <el-table-column prop="relationDisplay" :label="t('modelDesign.detail.col.dataStandard')" min-width="120" show-overflow-tooltip />
          <el-table-column prop="dataType" :label="t('modelDesign.detail.col.dataType')" min-width="100" show-overflow-tooltip />
          <el-table-column prop="length" :label="t('modelDesign.detail.col.length')" min-width="80" />
          <el-table-column prop="precision" :label="t('modelDesign.detail.col.precision')" min-width="80" />
          <el-table-column prop="comment" :label="t('modelDesign.detail.col.comment')" min-width="120" show-overflow-tooltip />
          <el-table-column prop="sortOrder" :label="t('modelDesign.detail.col.sortOrder')" min-width="80" />
          <el-table-column prop="statusLabel" :label="t('modelDesign.detail.col.status')" min-width="80">
            <template #default="{ row }">
              <el-tag :type="(row as AttributeVO).status === 'enabled' ? 'success' : 'info'" size="small">
                {{ (row as AttributeVO).statusLabel }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column :label="t('modelDesign.detail.col.action')" min-width="140" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <el-button link type="primary" size="small" @click="TpMessage.info('功能开发中')">
                  {{ t('modelDesign.detail.attrTableAction.edit') }}
                </el-button>
                <el-button link type="danger" size="small" @click="handleDelete(row as AttributeVO)">
                  {{ t('modelDesign.detail.attrTableAction.delete') }}
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>

        <!-- 分页 -->
        <div class="flex justify-end mt-4">
          <el-pagination
            v-model:current-page="query.page"
            v-model:page-size="query.pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            size="default"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>
  </TpPageFrame>
</template>

<style scoped>
.back-arrow {
  padding: 4px;
  font-size: 16px;
  color: var(--el-text-color-secondary);
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
