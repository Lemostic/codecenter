/**
 * common/components/feedback/TpMessage.ts
 *
 * 全局消息提示工具（§13 6 大强制场景之一，feedback 分类）。
 * 封装 ElMessage，提供统一的 API 和默认配置。
 *
 * 用法：
 *   import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
 *   TpMessage.success('操作成功');
 *   TpMessage.error('操作失败');
 *   TpMessage.warning('请先选择一条记录');
 */
import { ElMessage } from 'element-plus';

const DEFAULT_DURATION = 3000;

/** 成功提示 */
const success = (message: string, duration = DEFAULT_DURATION) => {
  ElMessage({ type: 'success', message, duration });
};

/** 错误提示 */
const error = (message: string, duration = DEFAULT_DURATION) => {
  ElMessage({ type: 'error', message, duration });
};

/** 警告提示 */
const warning = (message: string, duration = DEFAULT_DURATION) => {
  ElMessage({ type: 'warning', message, duration });
};

/** 信息提示 */
const info = (message: string, duration = DEFAULT_DURATION) => {
  ElMessage({ type: 'info', message, duration });
};

export const TpMessage = {
  success,
  error,
  warning,
  info,
};

export default TpMessage;
