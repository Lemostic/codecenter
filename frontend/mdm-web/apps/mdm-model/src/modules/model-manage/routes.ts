import type { RouteRecordRaw } from 'vue-router';

// 使用空白布局（无顶部栏和侧边栏），因为这是嵌入式二级应用
const EmptyLayout = () => import('@/components/EmptyLayout.vue');

const MODULE_NAME = 'model-design';

const routes: RouteRecordRaw[] = [
  {
    path: `/${MODULE_NAME}`,
    component: EmptyLayout,
    children: [
      {
        path: '',
        name: `${MODULE_NAME}-list`,
        component: () => import('./views/ModelList.vue'),
        meta: { title: '模型列表' },
      },
      {
        path: 'topic',
        name: `${MODULE_NAME}-topic`,
        component: () => import('./views/TopicList.vue'),
        meta: { title: '主题域管理' },
      },
      {
        path: ':id',
        name: `${MODULE_NAME}-detail`,
        component: () => import('./views/ModelDetail.vue'),
        meta: { title: '模型管理', hidden: true },
      },
      {
        path: ':id/attribute-editor',
        name: `${MODULE_NAME}-attribute-editor`,
        component: () => import('./views/ModelAttributeEditor.vue'),
        meta: { title: '模型属性管理', hidden: true },
      },
      {
        path: 'segment',
        name: `${MODULE_NAME}-segment`,
        component: () => import('./views/SegmentList.vue'),
        meta: { title: '码段管理' },
      },
    ],
  },
];

export default routes;
