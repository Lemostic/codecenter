/**
 * @mdm/core 统一导出面
 *
 * 注意：所有内部 re-export 路径加 `.js` 后缀——这是 ESM 规范要求，
 * 同时也是 esbuild 编译后 dist/ 加载所必需。
 */
export * from './types/index.js';
export { createAppRouter } from './router/index.js';
export type { CreateRouterOptions } from './router/index.js';
export { createAppI18n } from './i18n/index.js';
export type { I18nModuleEntry, CommonLocales, CreateI18nOptions } from './i18n/index.js';
