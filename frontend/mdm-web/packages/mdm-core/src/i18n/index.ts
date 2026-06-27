/**
 * core/i18n - i18n 工厂函数
 *
 * 由各 app 调用，传入本 app 的模块语言包 + 共享语言包。
 * 不在 packages 内部使用 import.meta.glob（设计文档 §12.1 硬约束）。
 */
import { createI18n } from 'vue-i18n';

export interface I18nModuleEntry {
  /** camelCase 模块名，如 modelDesign */
  name: string;
  /** zh-CN 语言包 */
  zhCN: Record<string, unknown>;
  /** en-US 语言包 */
  enUS: Record<string, unknown>;
}

export interface CommonLocales {
  zhCN: Record<string, unknown>;
  enUS: Record<string, unknown>;
}

export interface CreateI18nOptions {
  /** 共享语言包（common.* 兜底文案） */
  commonLocales: CommonLocales;
  /** 各业务模块语言包 */
  modules: I18nModuleEntry[];
  /** 其他 vue-i18n 配置（如 locale / fallbackLocale 覆盖） */
  locale?: string;
  fallbackLocale?: string;
  legacy?: boolean;
}

export function createAppI18n(options: CreateI18nOptions) {
  const { commonLocales, modules, ...rest } = options;

  const zhMessages: Record<string, unknown> = { common: commonLocales.zhCN };
  const enMessages: Record<string, unknown> = { common: commonLocales.enUS };

  for (const mod of modules) {
    zhMessages[mod.name] = mod.zhCN;
    enMessages[mod.name] = mod.enUS;
  }

  return createI18n({
    legacy: rest.legacy ?? false,
    locale: rest.locale ?? 'zh-CN',
    fallbackLocale: rest.fallbackLocale ?? 'zh-CN',
    messages: { 'zh-CN': zhMessages, 'en-US': enMessages } as never,
  });
}

export default createAppI18n;
