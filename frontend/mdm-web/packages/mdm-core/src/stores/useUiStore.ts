/**
 * core/stores/useUiStore.ts
 *
 * 框架级 UI 状态：仅 sidebarCollapsed。
 *
 * 设计原因：core 不能依赖 @mdm/common（架构硬约束），
 * 因此放在 core 内部。useAppStore（业务 store）在 common，
 * 持有 config / locale / permissions / hasPermission 等。
 */
import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useUiStore = defineStore('mdm-ui', () => {
  const sidebarCollapsed = ref(false);

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
  }

  function setSidebarCollapsed(v: boolean) {
    sidebarCollapsed.value = v;
  }

  return {
    sidebarCollapsed,
    toggleSidebar,
    setSidebarCollapsed,
  };
});
