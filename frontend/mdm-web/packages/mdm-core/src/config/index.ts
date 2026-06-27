/**
 * @mdm/core/config 统一导出面
 */
export type { PlatformConfig, ThemeConfig, FontConfig, I18nConfig, ConfigProvider } from './types';
export { DEFAULT_CONFIG } from './defaults';
export { applyConfig } from './apply';
export { loadAndApplyConfig } from './loader';
export { deepMerge, mixColor, generateColorVariants } from './utils';
export { StaticProvider } from './providers/static';
