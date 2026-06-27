/**
 * 将 PlatformConfig 应用到运行时：
 * - 写入 Element Plus 主题色 CSS 变量
 * - 写入项目自定义 CSS 变量
 * - 设置 body 背景色与基础字号
 */
import type { PlatformConfig } from './types';
import { generateColorVariants } from './utils';

export function applyConfig(config: PlatformConfig): void {
  if (typeof document === 'undefined') return;
  const root = document.documentElement;

  // ── 主题 ──
  root.style.setProperty('--el-color-primary', config.theme.primaryColor);
  root.style.setProperty('--mdm-bg-color', config.theme.backgroundColor);
  document.body.style.backgroundColor = config.theme.backgroundColor;

  // 派生色阶（--el-color-primary-light-3/5/7/8/9、--el-color-primary-dark-2）
  generateColorVariants(config.theme.primaryColor);

  // ── 字体 ──
  root.style.setProperty('--mdm-font-size', `${config.font.fontSize}px`);
  document.body.style.fontSize = `${config.font.fontSize}px`;
}
