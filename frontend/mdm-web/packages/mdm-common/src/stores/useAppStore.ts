/**
 * common/stores/useAppStore.ts - 全局应用状态
 *
 * 全局业务 store：跨页面共享，可在任意组件中直接引用。
 * 持有 PlatformConfig、locale、permissions（业务相关）。
 *
 * **框架级 UI 状态**（如 sidebarCollapsed）由 `@mdm/core/stores/useUiStore` 持有，
 * 因为 core 不能依赖 common。两者职责清晰，互不重复。
 *
 * 提供 init() / setLocale() / setPrimaryColor() / hasPermission() 等方法。
 */
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { PlatformConfig } from '@mdm/core/config/types';
import { applyConfig } from '@mdm/core/config/apply';

export const useAppStore = defineStore('app', () => {
  // ========== 状态 ==========
  /** 已解析的平台配置（main.ts bootstrap 中由 init() 注入） */
  const config = ref<PlatformConfig | null>(null);
  /** 当前语言（响应式，i18n 模块内部 watch 同步 i18n.global.locale） */
  const locale = ref<'zh-CN' | 'en-US'>('zh-CN');
  /** 用户权限码列表 */
  const permissions = ref<string[]>([]);

  // ========== 计算属性 ==========
  /** 检查是否拥有指定权限码 */
  const hasPermission = computed(() => (permission: string) =>
    permissions.value.includes(permission),
  );

  // ========== Actions ==========

  /**
   * main.ts 中配置加载完成后调用，注入已解析的 PlatformConfig。
   * 同步刷新 locale 状态。
   */
  function init(resolvedConfig: PlatformConfig) {
    config.value = resolvedConfig;
    locale.value = (resolvedConfig.i18n.locale as 'zh-CN' | 'en-US') ?? 'zh-CN';
  }

  /** 切换语言：更新 store，i18n 模块内部通过 watch 同步 i18n.global.locale */
  function setLocale(newLocale: 'zh-CN' | 'en-US') {
    locale.value = newLocale;
    if (config.value) {
      config.value.i18n.locale = newLocale;
    }
  }

  /** 切换主色：运行时修改 CSS 变量（需先 init 过） */
  function setPrimaryColor(color: string) {
    if (!config.value) return;
    config.value.theme.primaryColor = color;
    applyConfig(config.value);
  }

  /** 设置权限码列表 */
  function setPermissions(p: string[]) {
    permissions.value = p;
  }

  return {
    // state
    config,
    locale,
    permissions,
    // getters
    hasPermission,
    // actions
    init,
    setLocale,
    setPrimaryColor,
    setPermissions,
  };
});
