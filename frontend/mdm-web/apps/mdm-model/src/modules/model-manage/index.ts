/**
 * modules/model-design/index.ts
 *
 * 主数据建模模块公开面
 */

// 公开页面
export { default as ModelList } from './views/ModelList.vue';
export { default as ModelDetail } from './views/ModelDetail.vue';
export { default as TopicList } from './views/TopicList.vue';

// 子模块类型（供行业资产库等外部模块引用）
export type { ModelVO, ModelCreateDTO, ModelUpdateDTO, ModelQuery } from './types/model';
export type { TopicVO, TopicCreateDTO, TopicUpdateDTO, TopicQuery, TopicTreeNode } from './types/topic';
export type { AttributeVO, AttributeCreateDTO, AttributeQuery } from './types/attribute';
export type { FormDesignVO, FormDesignQuery } from './types/form-design';
export type { SimilarityRuleVO, SimilarityRuleCreateDTO, SimilarityRuleQuery } from './types/similarity-rule';
export type { CodingRuleVO, CodingRuleCreateDTO, CodingRuleQuery } from './types/coding-rule';
export type { QualityRuleVO, QualityRuleCreateDTO, QualityRuleQuery } from './types/quality-rule';
