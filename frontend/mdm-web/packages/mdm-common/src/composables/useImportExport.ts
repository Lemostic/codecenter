/**
 * common/composables/useImportExport.ts
 *
 * 导入/导出通用逻辑。
 * - 导出：触发文件下载
 * - 导入：文件上传 + 进度管理 + 结果展示
 *
 * 用法：
 *   const { handleExport, exportLoading } = useExport({ exportApi, fileNamePrefix });
 *   const { handleImport, importDialogVisible, importLoading,
 *           importProgress, importResult, handleFileChange,
 *           resetImport } = useImport({ importApi });
 */
import { ref } from 'vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import type { ImportResult, ImportProgress } from '@mdm/common/types/import-export';

// ========== 导出 ==========

interface UseExportOptions {
  /** 导出 API（返回 Blob 或直接下载） */
  exportApi: (params?: Record<string, unknown>) => Promise<Blob | void>;
  /** 导出文件名前缀 */
  fileNamePrefix: string;
}

export function useExport(options: UseExportOptions) {
  const { exportApi, fileNamePrefix } = options;
  const exportLoading = ref(false);

  /** 生成带时间戳的文件名 */
  const generateFileName = (ext = 'xlsx') => {
    const now = new Date();
    const timestamp = `${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}${String(now.getDate()).padStart(2, '0')}_${String(now.getHours()).padStart(2, '0')}${String(now.getMinutes()).padStart(2, '0')}`;
    return `${fileNamePrefix}_${timestamp}.${ext}`;
  };

  /** 触发导出 */
  const handleExport = async (params?: Record<string, unknown>) => {
    exportLoading.value = true;
    try {
      const result = await exportApi(params);
      if (result instanceof Blob) {
        const url = URL.createObjectURL(result);
        const a = document.createElement('a');
        a.href = url;
        a.download = generateFileName();
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
        TpMessage.success('导出成功');
      }
    } catch {
      TpMessage.error('导出失败');
    } finally {
      exportLoading.value = false;
    }
  };

  return { handleExport, exportLoading, generateFileName };
}

// ========== 导入 ==========

interface UseImportOptions {
  /** 导入 API（上传文件，返回导入结果） */
  importApi: (file: File, extra?: Record<string, unknown>) => Promise<ImportResult>;
  /** 导入成功回调 */
  onSuccess?: () => void;
}

export function useImport(options: UseImportOptions) {
  const { importApi, onSuccess } = options;

  const importDialogVisible = ref(false);
  const importLoading = ref(false);
  const importProgress = ref<ImportProgress>({ percent: 0, statusText: '' });
  const importResult = ref<ImportResult | null>(null);
  const selectedFile = ref<File | null>(null);

  /** 打开导入弹窗 */
  const openImport = () => {
    resetImport();
    importDialogVisible.value = true;
  };

  /** 关闭导入弹窗 */
  const closeImport = () => {
    importDialogVisible.value = false;
    resetImport();
  };

  /** 文件选择变更 */
  const handleFileChange = (file: File | null) => {
    selectedFile.value = file;
  };

  /** 执行导入 */
  const handleImport = async (extra?: Record<string, unknown>) => {
    if (!selectedFile.value) {
      TpMessage.warning('请先选择要导入的文件');
      return;
    }
    importLoading.value = true;
    importProgress.value = { percent: 0, statusText: '正在上传...' };
    try {
      importProgress.value = { percent: 50, statusText: '正在解析...' };
      const result = await importApi(selectedFile.value, extra);
      importResult.value = result;
      importProgress.value = { percent: 100, statusText: '导入完成' };
      if (result.failed === 0) {
        TpMessage.success(`导入成功，共 ${result.success} 条`);
        onSuccess?.();
      }
    } catch {
      importProgress.value = { percent: 0, statusText: '导入失败' };
      TpMessage.error('导入失败，请检查文件格式');
    } finally {
      importLoading.value = false;
    }
  };

  /** 重置导入状态 */
  const resetImport = () => {
    selectedFile.value = null;
    importLoading.value = false;
    importProgress.value = { percent: 0, statusText: '' };
    importResult.value = null;
  };

  return {
    importDialogVisible,
    importLoading,
    importProgress,
    importResult,
    selectedFile,
    openImport,
    closeImport,
    handleFileChange,
    handleImport,
    resetImport,
  };
}
