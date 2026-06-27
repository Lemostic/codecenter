<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import type { UploadUserFile, UploadProps, UploadFile } from 'element-plus';
import { TpMessage } from './TpMessage';

/**
 * ImportDialog - 通用导入弹窗
 *
 * 功能：
 * - 文件上传（支持 xlsx/csv）
 * - 导入进度条展示
 * - 导入结果统计（成功/失败条数）
 * - 失败记录下载
 *
 * 用法：
 *   <ImportDialog v-model="dialogVisible" :import-api="handleImport" @success="onSuccess" />
 */
defineOptions({ name: 'ImportDialog' });

interface Props {
  modelValue: boolean;
  /** 导入 API，返回 { success: number; failed: number; failedFileUrl?: string } */
  importApi: (file: File) => Promise<{ success: number; failed: number; failedFileUrl?: string }>;
  /** 导入模板下载 URL（可选） */
  templateUrl?: string;
  /** 模板文件名前缀 */
  templateName?: string;
  /** 允许的文件类型 */
  accept?: string;
}

const props = withDefaults(defineProps<Props>(), {
  templateUrl: '',
  templateName: '导入模板',
  accept: '.xlsx,.xls',
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void;
  (e: 'success'): void;
}>();

const dialogVisible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
});

const fileList = ref<UploadUserFile[]>([]);
const uploading = ref(false);
const uploadProgress = ref(0);
const result = ref<{ success: number; failed: number; failedFileUrl?: string } | null>(null);
const errorMsg = ref('');

const uploadRef = ref<{ clearFiles: () => void } | null>(null);

const handleClose = () => {
  if (uploading.value) return;
  dialogVisible.value = false;
};

const handleExceed: UploadProps['onExceed'] = (_files, _fileList) => {
  TpMessage.warning('只能上传一个文件');
};

const handleChange: UploadProps['onChange'] = (uploadFile: UploadFile) => {
  errorMsg.value = '';
  result.value = null;
};

const handleError: UploadProps['onError'] = (err: Error) => {
  uploading.value = false;
  uploadProgress.value = 0;
  TpMessage.error('上传失败');
  console.error('[ImportDialog] upload error', err);
};

const handleSuccess: UploadProps['onSuccess'] = (_res, _file) => {
  // 上传成功但导入结果在确定按钮点击后显示
};

const handleRemove = () => {
  result.value = null;
  errorMsg.value = '';
};

const handleConfirm = async () => {
  if (!fileList.value.length) {
    TpMessage.warning('请先选择文件');
    return;
  }
  const file = fileList.value[0].raw;
  if (!file) return;

  uploading.value = true;
  errorMsg.value = '';
  result.value = null;

  try {
    const res = await props.importApi(file);
    result.value = res;
    if (res.failed > 0) {
      TpMessage.warning(`导入完成，成功 ${res.success} 条，失败 ${res.failed} 条`);
    } else {
      TpMessage.success('导入成功');
      emit('success');
    }
  } catch (err: any) {
    errorMsg.value = err?.message ?? '导入失败';
    TpMessage.error(errorMsg.value || '导入失败');
  } finally {
    uploading.value = false;
  }
};

const handleDownloadFailed = () => {
  if (!result.value?.failedFileUrl) return;
  const a = document.createElement('a');
  a.href = result.value.failedFileUrl;
  a.download = '导入失败记录.xlsx';
  a.click();
};

watch(dialogVisible, (v) => {
  if (!v) {
    // 关闭时重置状态
    fileList.value = [];
    uploading.value = false;
    uploadProgress.value = 0;
    result.value = null;
    errorMsg.value = '';
  }
});
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="导入"
    width="500px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="!uploading"
    destroy-on-close
    @close="handleClose"
  >
    <!-- 提示信息 -->
    <el-alert type="info" :closable="false" show-icon class="mb-3">
      <template #title>提示</template>
      <div class="text-sm">
        <template v-if="templateUrl">
          <span>请先
            <a :href="templateUrl" class="text-primary">下载导入模板</a>
            ，填写完成后上传文件导入。
          </span>
        </template>
        <template v-else>
          请选择文件后点击确定导入。
        </template>
      </div>
    </el-alert>

    <!-- 文件上传 -->
    <el-upload
      ref="uploadRef"
      v-model:file-list="fileList"
      :accept="accept"
      :auto-upload="false"
      :limit="1"
      :on-exceed="handleExceed"
      :on-change="handleChange"
      :on-error="handleError"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      class="w-full mb-3"
      drag
    >
      <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
      <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
      <template #tip>
        <div class="el-upload__tip text-xs">支持 xlsx/xls 格式</div>
      </template>
    </el-upload>

    <!-- 错误信息 -->
    <div v-if="errorMsg" class="mb-2 text-sm text-danger">
      {{ errorMsg }}
    </div>

    <!-- 导入结果 -->
    <template v-if="result">
      <el-divider />
      <div class="text-sm">
        <div class="flex items-center gap-2 mb-2">
          <el-icon color="#67c23a"><SuccessFilled /></el-icon>
          <span>成功 {{ result.success }} 条</span>
          <template v-if="result.failed > 0">
            <el-icon color="#f56c6c" class="ml-2"><CircleCloseFilled /></el-icon>
            <span class="text-danger">失败 {{ result.failed }} 条</span>
          </template>
        </div>
        <div v-if="result.failed > 0 && result.failedFileUrl">
          <el-button type="primary" link class="p-0" @click="handleDownloadFailed">
            下载失败记录
          </el-button>
        </div>
      </div>
    </template>

    <template #footer>
      <el-button :disabled="uploading" @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="handleConfirm">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>
