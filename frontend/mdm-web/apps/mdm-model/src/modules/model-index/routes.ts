import type { RouteRecordRaw } from 'vue-router';

const Layout = () => import('@mdm/core/layout/AppLayout.vue');

const MODULE_NAME = 'model-index';
const MODULE_DISPLAY_NAME = '模型列表';
const MODULE_ICON = 'List';
const MODULE_ORDER = 1;

const routes: RouteRecordRaw[] = [
  {
    path: `/${MODULE_NAME}`,
    component: Layout,
    meta: {
      title: MODULE_DISPLAY_NAME,
      icon: MODULE_ICON,
      menu: true,
      order: MODULE_ORDER,
    },
    children: [
      {
        path: '',
        name: `${MODULE_NAME}-list`,
        component: () => import('./views/ModelList.vue'),
        meta: { title: '模型列表', icon: 'List', menu: true, order: 1, permission: `${MODULE_NAME}:read` },
      },
    ],
  },
];

export default routes;
