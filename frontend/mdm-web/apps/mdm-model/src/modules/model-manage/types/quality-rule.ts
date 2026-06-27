/**
 * modules/model-design/types/quality-rule.ts
 *
 * 质量规则 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 规则类型 */
export type QualityRuleType =
  | 'notNull'          // 非空校验
  | 'unique'           // 唯一性校验
  | 'format'           // 格式校验
  | 'range'            // 范围校验
  | 'regex'            // 正则校验
  | 'custom'           // 自定义校验
  | 'crossTable';      // 跨表校验

export const QUALITY_RULE_TYPE_OPTIONS: { value: QualityRuleType; label: string }[] = [
  { value: 'notNull', label: '非空校验' },
  { value: 'unique', label: '唯一性校验' },
  { value: 'format', label: '格式校验' },
  { value: 'range', label: '范围校验' },
  { value: 'regex', label: '正则校验' },
  { value: 'custom', label: '自定义校验' },
  { value: 'crossTable', label: '跨表校验' },
];

export const QUALITY_RULE_TYPE_LABEL: Record<QualityRuleType, string> = {
  notNull: '非空校验',
  unique: '唯一性校验',
  format: '格式校验',
  range: '范围校验',
  regex: '正则校验',
  custom: '自定义校验',
  crossTable: '跨表校验',
};

/** 规则状态 */
export type QualityRuleStatus = 'enabled' | 'disabled';

export const QUALITY_RULE_STATUS_OPTIONS: { value: QualityRuleStatus; label: string }[] = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '停用' },
];

export const QUALITY_RULE_STATUS_LABEL: Record<QualityRuleStatus, string> = {
  enabled: '启用',
  disabled: '停用',
};

/** 校验时机 */
export type CheckTiming = 'onSave' | 'onSubmit' | 'onImport' | 'scheduled';

export const CHECK_TIMING_OPTIONS: { value: CheckTiming; label: string }[] = [
  { value: 'onSave', label: '保存时' },
  { value: 'onSubmit', label: '提交时' },
  { value: 'onImport', label: '导入时' },
  { value: 'scheduled', label: '定时检查' },
];

// ========== 子类型 ==========

/** 规则条件配置 */
export interface RuleCondition {
  /** 关联属性 ID */
  attributeId: ID;
  /** 属性名称（冗余） */
  attributeName?: string;
  /** 规则类型 */
  ruleType: QualityRuleType;
  /** 规则参数（JSON，根据 ruleType 不同而变化） */
  params?: Record<string, unknown>;
  /** 规则描述文本（自动生成或手动填写） */
  description: string;
}

/** 阈值配置 */
export interface ThresholdConfig {
  /** 是否启用阈值 */
  enabled: boolean;
  /** 阈值百分比（0-100） */
  thresholdPercent?: number;
  /** 阈值动作（warn/block） */
  action?: 'warn' | 'block';
}

/** 核准规则配置 */
export interface ApprovalConfig {
  /** 是否启用核准 */
  enabled: boolean;
  /** 核准人 ID 列表 */
  approverIds?: ID[];
  /** 核准说明 */
  description?: string;
}

// ========== 1. 数据库实体 ==========

export interface QualityRuleEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId: ID;
  /** 规则名称 */
  name: string;
  /** 规则类型 */
  ruleType: QualityRuleType;
  /** 规则描述 */
  description: string;
  /** 规则条件 */
  conditions: RuleCondition[];
  /** 规则状态 */
  status: QualityRuleStatus;
  /** 校验时机 */
  checkTiming: CheckTiming[];
  /** 阈值配置 */
  thresholdConfig?: ThresholdConfig;
  /** 核准规则 */
  approvalConfig?: ApprovalConfig;
  /** 是否数据入库 */
  dataStorage: boolean;
  /** 排序号 */
  sortOrder: number;
}

// ========== 2. 视图对象 ==========

export interface QualityRuleVO extends QualityRuleEntity {
  /** 规则类型标签 */
  ruleTypeLabel: string;
  /** 状态标签 */
  statusLabel: string;
  /** 关联属性名称列表 */
  attributeNames: string[];
}

// ========== 3. 创建参数 ==========

export interface QualityRuleCreateDTO {
  modelId: ID;
  name: string;
  ruleType: QualityRuleType;
  description: string;
  conditions: Omit<RuleCondition, 'attributeName'>[];
  checkTiming: CheckTiming[];
  thresholdConfig?: ThresholdConfig;
  approvalConfig?: ApprovalConfig;
  dataStorage?: boolean;
  sortOrder?: number;
}

// ========== 4. 更新参数 ==========

export interface QualityRuleUpdateDTO extends Partial<Omit<QualityRuleCreateDTO, 'modelId'>> {
  id: ID;
}

// ========== 5. 查询参数 ==========

export interface QualityRuleQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId: ID;
  /** 规则名称模糊搜索 */
  keyword?: string;
  /** 规则类型过滤 */
  ruleType?: QualityRuleType;
  /** 状态过滤 */
  status?: QualityRuleStatus;
}
