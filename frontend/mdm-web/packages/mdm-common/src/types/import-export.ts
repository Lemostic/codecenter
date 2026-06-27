/**
 * common/types/import-export.ts - 导入/导出通用类型
 *
 * 各模块的导入/导出功能复用。
 */
import type { ID } from './base';

/** 导入任务状态 */
export type ImportTaskStatus = 'pending' | 'processing' | 'completed' | 'failed';

/** 导入任务状态标签映射 */
export const IMPORT_TASK_STATUS_LABEL: Record<ImportTaskStatus, string> = {
  pending: '等待中',
  processing: '处理中',
  completed: '已完成',
  failed: '已失败',
};

/** 导入任务状态 Tag 类型映射 */
export const IMPORT_TASK_STATUS_TAG: Record<ImportTaskStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  pending: 'info',
  processing: 'warning',
  completed: 'success',
  failed: 'danger',
};

/** 导入结果摘要 */
export interface ImportResult {
  /** 总条数 */
  total: number;
  /** 成功条数 */
  success: number;
  /** 失败条数 */
  failed: number;
  /** 跳过条数（如完全重复数据） */
  skipped?: number;
  /** 失败记录下载链接 */
  failRecordUrl?: string;
}

/** 导入进度 */
export interface ImportProgress {
  /** 当前进度百分比 0~100 */
  percent: number;
  /** 当前状态描述 */
  statusText: string;
}

/** 重名处理策略 */
export type DuplicateStrategy = 'overwrite' | 'rename';

/** 重名处理策略标签映射 */
export const DUPLICATE_STRATEGY_LABEL: Record<DuplicateStrategy, string> = {
  overwrite: '覆盖',
  rename: '重命名',
};

/** 导入任务实体 */
export interface ImportTaskEntity {
  id: ID;
  /** 任务名称 */
  name: string;
  /** 导入类型（如 model / attribute / form-design） */
  type: string;
  /** 任务状态 */
  status: ImportTaskStatus;
  /** 导入结果 */
  result?: ImportResult;
  /** 创建时间 */
  createdAt: string;
  /** 创建人 */
  createdBy: string;
}

/** 导出参数 */
export interface ExportParams {
  /** 导出文件名前缀 */
  fileNamePrefix: string;
  /** 选中的 ID 列表（空则导出全部/按搜索条件） */
  ids?: ID[];
  /** 搜索条件（按条件导出时传入） */
  query?: Record<string, unknown>;
}
