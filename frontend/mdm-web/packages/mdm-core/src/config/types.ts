/**
 * 平台配置类型定义
 *
 * 与配置来源（API / 静态文件 / SDK / 框架内置）解耦，
 * 任何来源最终都转换为 `PlatformConfig` 传入应用层。
 */

/** 换肤 */
export interface ThemeConfig {
  /** 主色，如 '#409EFF' */
  primaryColor: string;
  /** 背景色，如 '#FFFFFF' */
  backgroundColor: string;
  /** 暗色模式（预留） */
  darkMode?: boolean;
}

/** 字体 */
export interface FontConfig {
  /** 全局基础字号（px） */
  fontSize: number;
}

/** 国际化 */
export interface I18nConfig {
  /** 默认语言，如 'zh-CN' */
  locale: string;
  /** 可用语言列表 */
  availableLocales: string[];
  /** 回退语言 */
  fallbackLocale: string;
}

/** 平台配置 */
export interface PlatformConfig {
  theme: ThemeConfig;
  font: FontConfig;
  i18n: I18nConfig;
  /** 应用标题（可选，显示在浏览器标签） */
  appTitle?: string;
}

/**
 * 配置来源提供者。
 * 实现 load() 返回部分配置，与 DEFAULT_CONFIG 合并后生效。
 */
export interface ConfigProvider {
  load(): Promise<Partial<PlatformConfig>>;
}
