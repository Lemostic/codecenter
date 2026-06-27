<script setup lang="ts">
/**
 * FieldTreePanel - 字段树（展示复合模型的层级结构）
 */
import { ref, onMounted } from 'vue';
import { useI18n } from 'vue-i18n';
import TpTree from '@mdm/common/components/data/TpTree.vue';

defineOptions({ name: 'FieldTreePanel' });

const { t } = useI18n();

interface TreeNode {
  id: string;
  name: string;
  isLeaf: boolean;
  children?: TreeNode[];
}

interface Props {
  modelId: string;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  'select': [fieldId: string];
}>();

const loading = ref(false);
const treeData = ref<TreeNode[]>([]);

const loadData = async () => {
  loading.value = true;
  try {
    // TODO: Replace with actual API call to fetch composite model structure
    // Expected API: GET /api/model-design/{modelId}/fields
    // The response should return a tree structure where parent nodes represent
    // groups/sub-models and leaf nodes represent individual fields
    treeData.value = [
      {
        id: 'main',
        name: '供应商_20583_文雪颖',
        isLeaf: false,
        children: [
          { id: 'sub1', name: '机构信息', isLeaf: true },
          { id: 'sub2', name: '附件信息', isLeaf: true },
        ],
      },
    ];
  } finally {
    loading.value = false;
  }
};

const handleNodeClick = (node: TreeNode) => {
  if (node.isLeaf) {
    emit('select', node.id);
  }
};

onMounted(() => {
  loadData();
});
</script>

<template>
  <div class="field-tree-panel flex flex-col h-full">
    <!-- 标题 -->
    <div class="p-3 border-b border-[var(--el-border-color-lighter)]">
      <span class="text-sm font-medium">{{ t('formDesign.fieldList') }}</span>
    </div>

    <!-- 树 -->
    <div class="flex-1 overflow-auto p-2">
      <TpTree
        v-loading="loading"
        :data="treeData"
        :field-map="{ label: 'name', children: 'children' }"
        :expand-on-click-node="false"
        :default-expand-all="true"
        @node-click="handleNodeClick"
      />
    </div>
  </div>
</template>

<style scoped>
.field-tree-panel {
  background: var(--el-fill-color-lighter);
}
</style>
