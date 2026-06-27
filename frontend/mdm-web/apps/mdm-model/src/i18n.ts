/**
 * apps/mdm-model/src/i18n.ts
 */
import type { I18nModuleEntry } from '@mdm/core/i18n';
import { createAppI18n } from '@mdm/core/i18n';
import zhCommon from '@mdm/common/locales/zh-CN';
import enCommon from '@mdm/common/locales/en-US';

const zhModules = import.meta.glob<Record<string, unknown>>(
  './modules/*/locales/zh-CN.ts',
  { eager: true },
);
const enModules = import.meta.glob<Record<string, unknown>>(
  './modules/*/locales/en-US.ts',
  { eager: true },
);

const toCamelCase = (s: string) =>
  s.replace(/-([a-z])/g, (_, c) => c.toUpperCase());

// 模块名映射：model-manage 目录使用 modelDesign 作为翻译键前缀
const MODULE_NAME_MAP: Record<string, string> = {
  modelManage: 'modelDesign',
};

const modules: I18nModuleEntry[] = Object.entries(zhModules).map(([path, mod]) => {
  const dir = path.match(/\.\/modules\/([^/]+)\//)?.[1] ?? '';
  const enMod = enModules[path.replace('zh-CN', 'en-US')] ?? {};
  const originalName = toCamelCase(dir);
  const name = MODULE_NAME_MAP[originalName] ?? originalName;
  return {
    name,
    zhCN: (mod as any).default ?? {},
    enUS: (enMod as any).default ?? {},
  };
});

const i18n = createAppI18n({
  commonLocales: { zhCN: zhCommon as any, enUS: enCommon as any },
  modules,
});

export default i18n;
