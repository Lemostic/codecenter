/**
 * apps/mdm-model/src/router.ts
 *
 * 在 app 层用 import.meta.glob 扫描本 app 的模块路由，
 * 然后传给 @mdm/core/router 的 createAppRouter 工厂。
 */
import type { RouteRecordRaw } from 'vue-router';
import { createAppRouter } from '@mdm/core/router';

const moduleFiles = import.meta.glob<{ default: RouteRecordRaw[] }>(
  ['./modules/*/routes.ts', '!./modules/_*/routes.ts'],
  { eager: true },
);

const moduleRoutes: RouteRecordRaw[] = Object.values(moduleFiles)
  .flatMap((m) => m.default ?? []);

const router = createAppRouter(moduleRoutes, {
  baseRoutes: [{ path: '/', redirect: '/model-design' }],
});

export default router;
