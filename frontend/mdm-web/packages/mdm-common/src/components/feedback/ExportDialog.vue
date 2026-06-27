<script setup lang="ts">
import { computed, ref } from 'vue';
import { TpMessage } from './TpMessage';

/**
 * ExportDialog - 通用导出弹窗
 *
 * 功能：
 * - 模板下载（可选）
 * - 触发导出并下载文件
 *
 * 用法：
 *   <ExportDialog v-model="dialogVisible" :export-api="handleExport" file-name="模型导出" @success="onSuccess" />
 */
defineOptions({ name: 'ExportDialog' });

interface Props {
  modelValue: boolean;
  /** 导出 API，返回 Blob */
  exportApi: (params?: Record<string, unknown>) => Promise<Blob>;
  /** 导出文件名（不含时间戳后缀） */
  fileName: string;
  /** 模板下载链接 */
  templateUrl?: string;
  /** 模板文件名 */
  templateName?: string;
}

const props = withDefaults(defineProps<Props>(), {
  templateUrl: '',
  templateName: '导出模板',
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'success'): void;
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const exporting = ref(false);
const filterParams = ref<Record<string, unknown>>({});

/** 生成带时间戳的文件名 */
const genFileName = (ext = 'xlsx') => {
  const now = new Date();
  const ts = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
  return `${props.fileName}_${ts}.${ext}`;
};

const handleExport = async () => {
  exporting.value = true;
  try {
    const blob = await props.exportApi(filterParams.value);
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = genFileName();
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
    TpMessage.success('导出成功');
    emit('success');
    dialogVisible.value = false;
  } catch (err) {
    TpMessage.error('导出失败');
    console.error('[ExportDialog] export error', err);
  } finally {
    exporting.value = false;
  }
};

const handleDownloadTemplate = () => {
  if (!props.templateUrl) return;
  const a = document.createElement('a');
  a.href = props.templateUrl;
  a.download = `${props.templateName}.xlsx`;
  a.click();
};

/** 设置过滤参数（外部调用） */
const setFilterParams = (params: Record<string, unknown>) => {
  filterParams.value = params;
};

defineExpose({ setFilterParams });
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="导出"
    width="480px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <!-- 模板下载 -->
    <div v-if="templateUrl" class="mb-3">
      <el-button type="primary" link @click="handleDownloadTemplate">
        <el-icon class="mr-1"><Download /></el-icon>
        下载导出模板
      </el-button>
    </div>

    <!-- 提示 -->
    <el-alert type="info" :closable="false" show-icon>
      <template #title>提示</template>
      <span class="text-sm">确定要导出当前数据吗？</span>
    </el-alert>

    <!-- 插槽：用于外部传入筛选条件等 -->
    <slot />

    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="exporting" @click="handleExport">确定导出</el-button>
    </template>
  </el-dialog>
</template>
