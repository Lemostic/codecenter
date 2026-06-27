/**
 * 框架内置默认配置。
 *
 * 当前阶段所有配置项使用框架内置固定值。
 * 后续接入外部配置来源后，此固定值自动降级为兜底值。
 */
import type { PlatformConfig } from './types';

export const DEFAULT_CONFIG: PlatformConfig = {
  theme: {
    primaryColor: '#409EFF',
    backgroundColor: '#FFFFFF',
  },
  font: {
    fontSize: 12,
  },
  i18n: {
    locale: 'zh-CN',
    availableLocales: ['zh-CN'],
    fallbackLocale: 'zh-CN',
  },
  appTitle: '主数据管理平台',
};
