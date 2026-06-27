/**
 * common/components/feedback/TpConfirm.ts
 *
 * 全局确认弹窗工具（§13 6 大强制场景之一，feedback 分类）。
 * 封装 ElMessageBox.confirm，提供删除/通用确认两个入口。
 * 用法：
 *   import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';
 *   await TpConfirm.delete('确定要删除"{name}"吗？', { name: row.name });
 *   await TpConfirm.confirm({ message: '...', type: 'warning' });
 */
import { ElMessageBox } from 'element-plus';
import type { MessageBoxData, ElMessageBoxOptions } from 'element-plus';

export interface TpConfirmOptions {
  /** 弹窗消息（支持 {var} 占位符） */
  message: string;
  /** 弹窗标题，默认 "提示" */
  title?: string;
  /** 图标类型，默认 warning */
  type?: 'warning' | 'info' | 'success' | 'error';
  /** 占位符变量（替换 message 里的 {var}） */
  params?: Record<string, string>;
  /** 确认按钮文案，默认"确定" */
  confirmText?: string;
  /** 取消按钮文案，默认"取消" */
  cancelText?: string;
}

/** 替换 {var} 占位符 */
const interpolate = (template: string, params?: Record<string, string>): string => {
  if (!params) return template;
  return template.replace(/\{(\w+)\}/g, (_, key) => params[key] ?? `{${key}}`);
};

/** 通用确认 */
const confirm = (opts: TpConfirmOptions): Promise<MessageBoxData> => {
  const message = interpolate(opts.message, opts.params);
  const options: ElMessageBoxOptions = {
    type: opts.type ?? 'warning',
    confirmButtonText: opts.confirmText ?? '确定',
    cancelButtonText: opts.cancelText ?? '取消',
  };
  return ElMessageBox.confirm(message, opts.title ?? '提示', options);
};

/** 删除快捷方法（warn 图标 + "删除"按钮文案） */
const deleteConfirm = (
  message: string,
  params?: Record<string, string>,
): Promise<MessageBoxData> => {
  return confirm({
    message,
    params,
    type: 'warning',
    confirmText: '删除',
    title: '提示',
  });
};

export const TpConfirm = {
  confirm,
  delete: deleteConfirm,
};

export default TpConfirm;
