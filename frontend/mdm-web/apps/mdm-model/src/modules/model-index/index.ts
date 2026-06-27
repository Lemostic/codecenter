/**
 * modules/model-index/index.ts
 *
 * 模型列表模块公开面
 */

// 公开页面
export { default as ModelList } from './views/ModelList.vue';

// 类型
export type { ModelVO, ModelCreateDTO, ModelUpdateDTO, ModelQuery } from './types/model';
