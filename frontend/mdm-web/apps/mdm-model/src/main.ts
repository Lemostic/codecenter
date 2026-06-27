/**
 * apps/mdm-model/src/main.ts — 应用入口
 *
 * Bootstrap 流程：
 *   1. 加载并应用 PlatformConfig（当前用 StaticProvider 固定值）
 *   2. 根据 config.i18n.locale 动态导入 Element Plus locale
 *   3. 创建 i18n（用 config 中解析过的 locale）
 *   4. 创建 Vue 实例，依次 use(pinia / router / i18n / ElementPlus)
 *   5. useAppStore().init(config) 注入已解析配置
 *   6. 挂载 + 设置 document.title
 */
import { createApp } from 'vue';
import { createPinia } from 'pinia';
import ElementPlus from 'element-plus';
import 'element-plus/dist/index.css';
import '@mdm/common/styles/index.css';
import './style.css';

import App from './App.vue';
import router from './router';
import i18n from './i18n';

import { loadAndApplyConfig } from '@mdm/core/config';
import { StaticProvider } from '@mdm/core/config/providers/static';
import { useAppStore } from '@mdm/common/stores/useAppStore';

async function bootstrap() {
  // 1. 加载 + 应用配置（当前用 StaticProvider 返回固定值）
  const config = await loadAndApplyConfig(new StaticProvider());

  // 2. 根据 locale 动态导入 Element Plus 语言包
  const epLocale = config.i18n.locale.startsWith('zh')
    ? (await import('element-plus/es/locale/lang/zh-cn')).default
    : (await import('element-plus/es/locale/lang/en')).default;

  // 3. 创建 Vue 实例
  const app = createApp(App);
  app.use(createPinia());
  app.use(router);
  app.use(i18n);
  app.use(ElementPlus, { locale: epLocale });

  // 4. 注入已解析配置到全局 store
  const appStore = useAppStore();
  appStore.init(config);

  // 5. 挂载
  app.mount('#app');

  // 6. 设置浏览器标签
  document.title = config.appTitle ?? '主数据管理平台';
}

bootstrap();
