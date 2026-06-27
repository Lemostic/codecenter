/**
 * core/router - 路由工厂函数
 *
 * 由各 app 调用，传入本 app 的模块路由列表。
 * 不在 packages 内部使用 import.meta.glob（设计文档 §12.1 硬约束）。
 */
/// <reference types="vite/client" />
import { createRouter, createWebHashHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';

/**
 * 模块路由的 meta 扩展约定：
 * - `menu: true` —— 该路由会出现在 AppLayout 侧边栏菜单中
 * - `menu: false` 或不填 —— 隐藏（如 `hidden: true` 的子路由）
 * - `icon: string` —— Element Plus 图标名（如 'Edit', 'Collection'）
 * - `order: number` —— 菜单排序（升序）
 * - `title: string` —— 菜单显示文本
 * - `permission: string` —— 权限码（可选，由业务层校验）
 */
export interface RouteMenuMeta {
  title?: string;
  icon?: string;
  /** true 才会出现在侧边栏 */
  menu?: boolean;
  hidden?: boolean;
  order?: number;
  permission?: string;
}

export interface CreateRouterOptions {
  /** 基础路由（如 / → /xxx 的重定向） */
  baseRoutes?: RouteRecordRaw[];
}

export function createAppRouter(
  moduleRoutes: RouteRecordRaw[],
  options: CreateRouterOptions = {},
) {
  const { baseRoutes = [] } = options;

  const router = createRouter({
    history: createWebHashHistory(),
    routes: [...baseRoutes, ...moduleRoutes],
  });

  router.beforeEach((to, _from, next) => {
    const title = to.meta.title as string;
    if (title) {
      document.title = `${title} - ${import.meta.env.VITE_APP_TITLE ?? '主数据管理平台'}`;
    }
    next();
  });

  return router;
}

/**
 * 从当前路由树中提取模块的"侧边栏菜单"列表。
 *
 * AppLayout 在 setup 中调用：
 *   const route = useRoute();
 *   const items = useModuleMenu(route);
 * 返回的每个 item 含 { name, path, title, icon } 字段。
 */
import type { RouteLocationNormalizedLoaded } from 'vue-router';

export interface MenuItem {
  name?: RouteLocationNormalizedLoaded['matched'][number]['name'];
  path?: string;
  title?: string;
  icon?: string;
  order: number;
}

export function useModuleMenu(route: RouteLocationNormalizedLoaded): MenuItem[] {
  // 找到第一个 meta.menu === true 的父级路由（模块根）
  const parent = route.matched.find((r) => (r.meta as Record<string, unknown> | undefined)?.menu === true);
  const children = parent?.children ?? [];
  return children
    .filter((c) => {
      const meta = c.meta as Record<string, unknown> | undefined;
      return meta?.menu !== false && meta?.hidden !== true;
    })
    .map((c) => {
      const meta = c.meta as Record<string, unknown> | undefined;
      return {
        name: c.name,
        path: typeof c.path === 'string' ? c.path : undefined,
        title: meta?.title as string | undefined,
        icon: meta?.icon as string | undefined,
        order: (meta?.order as number | undefined) ?? 0,
      };
    })
    .sort((a, b) => a.order - b.order);
}

export default createAppRouter;
