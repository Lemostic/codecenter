/**
 * modules/model-design/types/similarity-rule.ts
 *
 * 相似规则 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 相似算法类型 */
export type SimilarityAlgorithm =
  | 'editDistance'    // 编辑距离
  | 'jaccard'         // Jaccard 相似度
  | 'cosine'          // 余弦相似度
  | 'soundex'         // Soundex 语音编码
  | 'custom';         // 自定义

export const SIMILARITY_ALGORITHM_OPTIONS: { value: SimilarityAlgorithm; label: string }[] = [
  { value: 'editDistance', label: '编辑距离' },
  { value: 'jaccard', label: 'Jaccard 相似度' },
  { value: 'cosine', label: '余弦相似度' },
  { value: 'soundex', label: 'Soundex' },
  { value: 'custom', label: '自定义' },
];

export const SIMILARITY_ALGORITHM_LABEL: Record<SimilarityAlgorithm, string> = {
  editDistance: '编辑距离',
  jaccard: 'Jaccard 相似度',
  cosine: '余弦相似度',
  soundex: 'Soundex',
  custom: '自定义',
};

/** 组合模式 */
export type CombinationMode = 'weighted' | 'composite';

export const COMBINATION_MODE_OPTIONS: { value: CombinationMode; label: string }[] = [
  { value: 'weighted', label: '多属性加权平均' },
  { value: 'composite', label: '多属性组合相似' },
];

export const COMBINATION_MODE_LABEL: Record<CombinationMode, string> = {
  weighted: '多属性加权平均',
  composite: '多属性组合相似',
};

// ========== 子类型 ==========

/** 属性权重配置 */
export interface AttributeWeight {
  /** 属性 ID */
  attributeId: ID;
  /** 属性名称（冗余显示用） */
  attributeName?: string;
  /** 使用的算法 */
  algorithm: SimilarityAlgorithm;
  /** 权重（0-100，百分比） */
  weight: number;
  /** 算法参数（JSON） */
  algorithmParams?: Record<string, unknown>;
}

// ========== 1. 数据库实体 ==========

export interface SimilarityRuleEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId: ID;
  /** 规则名称 */
  name: string;
  /** 组合模式 */
  combinationMode: CombinationMode;
  /** 整体相似度阈值（百分比 0-100） */
  threshold: number;
  /** 保存时是否检查相似数据 */
  checkOnSave: boolean;
  /** 属性权重配置 */
  attributeWeights: AttributeWeight[];
  /** 排序号 */
  sortOrder: number;
}

// ========== 2. 视图对象 ==========

export interface SimilarityRuleVO extends SimilarityRuleEntity {
  /** 组合模式标签 */
  combinationModeLabel: string;
  /** 参与属性数量 */
  attributeCount: number;
}

// ========== 3. 创建参数 ==========

export interface SimilarityRuleCreateDTO {
  modelId: ID;
  name: string;
  combinationMode: CombinationMode;
  threshold: number;
  checkOnSave?: boolean;
  attributeWeights: Omit<AttributeWeight, 'attributeName'>[];
  sortOrder?: number;
}

// ========== 4. 更新参数 ==========

export interface SimilarityRuleUpdateDTO extends Partial<Omit<SimilarityRuleCreateDTO, 'modelId'>> {
  id: ID;
}

// ========== 5. 查询参数 ==========

export interface SimilarityRuleQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId: ID;
  /** 规则名称模糊搜索 */
  keyword?: string;
}
