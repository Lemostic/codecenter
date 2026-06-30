/**
 * modules/model-design/types/coding-rule.ts
 *
 * 编码规则 - 业务实体类型（5 件套 + 版本管理）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 编码规则状态 */
export type CodingRuleStatus = 'draft'     // 编辑中
                             | 'active'   // 生效
                             | 'disabled' // 停用
                             | 'history'; // 历史

export const CODING_RULE_STATUS_OPTIONS: { value: CodingRuleStatus; label: string }[] = [
  { value: 'draft', label: '编辑中' },
  { value: 'active', label: '生效' },
  { value: 'disabled', label: '停用' },
  { value: 'history', label: '历史' },
];

export const CODING_RULE_STATUS_LABEL: Record<CodingRuleStatus, string> = {
  draft: '编辑中',
  active: '生效',
  disabled: '停用',
  history: '历史',
};

export const CODING_RULE_STATUS_TAG: Record<CodingRuleStatus, string> = {
  draft: 'warning',
  active: 'success',
  disabled: 'info',
  history: 'info',
};

/** 码段类型 */
export type SegmentType =
  | 'fixed'             // 固定码
  | 'serial'            // 流水码
  | 'date'              // 日期码
  | 'rangeSerial'       // 区间流水码
  | 'ref'               // 引用码
  | 'feature'           // 特征码
  | 'dynamicSerial'     // 动态映射流水码
  | 'refSerial'         // 引用流水码
  | 'dateSerial';       // 日期流水码

export const SEGMENT_TYPE_OPTIONS: { value: SegmentType; label: string }[] = [
  { value: 'fixed', label: '固定码' },
  { value: 'serial', label: '流水码' },
  { value: 'date', label: '日期码' },
  { value: 'rangeSerial', label: '区间流水码' },
  { value: 'ref', label: '引用码' },
  { value: 'feature', label: '特征码' },
  { value: 'dynamicSerial', label: '动态映射流水码' },
  { value: 'refSerial', label: '引用流水码' },
  { value: 'dateSerial', label: '日期流水码' },
];

export const SEGMENT_TYPE_LABEL: Record<SegmentType, string> = {
  fixed: '固定码',
  serial: '流水码',
  date: '日期码',
  rangeSerial: '区间流水码',
  ref: '引用码',
  feature: '特征码',
  dynamicSerial: '动态映射流水码',
  refSerial: '引用流水码',
  dateSerial: '日期流水码',
};

/**
 * 前端小写驼峰 type → 后端 enum 大写映射
 */
export const FRONT_TYPE_TO_BACK_TYPE: Record<SegmentType, string> = {
  fixed: 'FIXED',
  serial: 'SEQUENCE',
  date: 'DATE',
  rangeSerial: 'SEQUENCE',
  ref: 'REFERENCE',
  feature: 'EIGENVALUE',
  dynamicSerial: 'SEQUENCE',
  refSerial: 'REFERENCE_SEQ',
  dateSerial: 'DATE',
};

export const BACK_TYPE_TO_FRONT_TYPE: Record<string, SegmentType> = {
  FIXED: 'fixed',
  SEQUENCE: 'serial',
  DATE: 'date',
  EIGENVALUE: 'feature',
  REFERENCE: 'ref',
  REFERENCE_SEQ: 'refSerial',
};

/** 编码生成时机 */
export type CodingGenerationTiming = 'button'    // 按钮生成（手动触发）
                                    | 'onSave'   // 保存时生成
                                    | 'onActive'; // 生效时生成

export const CODING_GENERATION_TIMING_OPTIONS: { value: CodingGenerationTiming; label: string }[] = [
  { value: 'button', label: '按钮生成' },
  { value: 'onSave', label: '保存时生成' },
  { value: 'onActive', label: '生效时生成' },
];

export const CODING_GENERATION_TIMING_LABEL: Record<CodingGenerationTiming, string> = {
  button: '按钮生成',
  onSave: '保存时生成',
  onActive: '生效时生成',
};

/** 规则定义方式 */
export type RuleDefinitionType = 'segment' // 码段组合
                               | 'script'; // 脚本自定义

export const RULE_DEFINITION_TYPE_OPTIONS: { value: RuleDefinitionType; label: string }[] = [
  { value: 'segment', label: '码段组合' },
  { value: 'script', label: '脚本自定义' },
];

/** 日期格式 */
export type DateFormat = 'yyyy' | 'yy' | 'yyyyMM' | 'yyMM' | 'yyyyMMdd' | 'yyMMdd' | 'mm' | 'mmdd' | 'dd';

export const DATE_FORMAT_OPTIONS: { value: DateFormat; label: string }[] = [
  { value: 'yyyy', label: 'yyyy' },
  { value: 'yy', label: 'yy' },
  { value: 'yyyyMM', label: 'yyyyMM' },
  { value: 'yyMM', label: 'yyMM' },
  { value: 'yyyyMMdd', label: 'yyyyMMdd' },
  { value: 'yyMMdd', label: 'yyMMdd' },
  { value: 'mm', label: 'mm' },
  { value: 'mmdd', label: 'mmdd' },
  { value: 'dd', label: 'dd' },
];

// ========== 码段配置 ==========

/** 码段基础接口 */
export interface BaseSegment {
  /** 码段 ID */
  id?: ID;
  /** 码段类型 */
  type: SegmentType;
  /** 排序号 */
  sortOrder: number;
  /** 码段编码（系统生成，如 GD001） */
  code?: string;
  /** 码段名称 */
  name?: string;
  /** 码段前缀 */
  prefix?: string;
  /** 码段后缀 */
  suffix?: string;
  /** 状态 */
  status?: 'enabled' | 'disabled';
  /** 引用状态（是否被编码规则使用） */
  referenceStatus?: 'unused' | 'used';
  /** 描述 */
  description?: string;
}

/** 固定码 */
export interface FixedSegment extends BaseSegment {
  type: 'fixed';
  /** 固定值 */
  value: string;
}

/** 流水码 */
export interface SerialSegment extends BaseSegment {
  type: 'serial';
  /** 码段长度 */
  length: number;
  /** 起始值 */
  startValue: number;
  /** 步长 */
  step: number;
  /** 流水方式（默认数字流水） */
  sequenceType?: 'number'; // 目前仅支持数字流水
}

/** 日期码 */
export interface DateSegment extends BaseSegment {
  type: 'date';
  /** 是否当前时间 */
  isCurrentTime: boolean;
  /** 引用属性 ID（当 isCurrentTime=false 时必填） */
  refAttributeId?: ID;
  /** 引用属性名称 */
  refAttributeName?: string;
  /** 日期格式 */
  format: DateFormat;
}

/** 区间流水码 */
export interface RangeSerialSegment extends BaseSegment {
  type: 'rangeSerial';
  /** 流水方式 */
  sequenceType?: 'number';
  /** 码段长度 */
  length: number;
  /** 步长 */
  step: number;
  /** 起始值 */
  startValue: number;
  /** 区间关联属性 ID */
  rangeAttributeId?: ID;
  /** 区间关联属性名称 */
  rangeAttributeName?: string;
  /** 特征对象（系统自动生成） */
  featureObject?: string;
  /** 区间属性列表 */
  rangeItems?: RangeItem[];
}

/** 区间项 */
export interface RangeItem {
  /** 区间属性值 */
  attributeValue: string;
  /** 流水起始值 */
  startValue: number;
  /** 流水结束值 */
  endValue: number;
}

/** 引用码 */
export interface RefSegment extends BaseSegment {
  type: 'ref';
  /** 是否引用自身属性 */
  isOwnAttribute: boolean;
  /** 引用来源属性 ID */
  refSourceAttributeId?: ID;
  /** 引用来源属性名称 */
  refSourceAttributeName?: string;
  /** 引用模型 ID */
  refModelId?: ID;
  /** 引用模型名称 */
  refModelName?: string;
  /** 引用属性 ID */
  refAttributeId?: ID;
  /** 引用属性名称 */
  refAttributeName?: string;
  /** 截取起始位置（head/tail） */
  substringPosition?: 'head' | 'tail';
  /** 截取步长 */
  substringLength?: number;
}

/** 特征码 */
export interface FeatureSegment extends BaseSegment {
  type: 'feature';
  /** 是否定长 */
  isFixedLength: boolean;
  /** 码段最大长度（不定长时） */
  maxLength?: number;
  /** 码段长度（定长时） */
  length?: number;
  /** 填充值 */
  fillValue?: string;
  /** 码段关联属性 ID */
  attributeId?: ID;
  /** 码段关联属性名称 */
  attributeName?: string;
  /** 特征对象（系统自动生成） */
  featureObject?: string;
  /** 特征属性列表 */
  featureItems?: FeatureItem[];
}

/** 特征项 */
export interface FeatureItem {
  /** 特征属性值 */
  attributeValue: string;
  /** 特征码值 */
  codeValue: string;
}

/** 动态映射流水码 */
export interface DynamicSerialSegment extends BaseSegment {
  type: 'dynamicSerial';
  /** 流水方式 */
  sequenceType?: 'number';
  /** 码段关联属性 ID */
  attributeId?: ID;
  /** 码段关联属性名称 */
  attributeName?: string;
  /** 码段长度 */
  length: number;
  /** 起始值 */
  startValue: number;
  /** 步长 */
  step: number;
}

/** 引用流水码 */
export interface RefSerialSegment extends BaseSegment {
  type: 'refSerial';
  /** 是否引用自身属性 */
  isOwnAttribute: boolean;
  /** 引用来源属性 ID */
  refSourceAttributeId?: ID;
  /** 引用来源属性名称 */
  refSourceAttributeName?: string;
  /** 引用模型 ID */
  refModelId?: ID;
  /** 引用模型名称 */
  refModelName?: string;
  /** 引用属性 ID */
  refAttributeId?: ID;
  /** 引用属性名称 */
  refAttributeName?: string;
  /** 截取起始位置 */
  substringPosition?: 'head' | 'tail';
  /** 截取步长 */
  substringLength?: number;
  /** 流水长度 */
  length: number;
  /** 起始值 */
  startValue: number;
  /** 步长 */
  step: number;
}

/** 日期流水码 */
export interface DateSerialSegment extends BaseSegment {
  type: 'dateSerial';
  /** 流水方式 */
  sequenceType?: 'number';
  /** 日期格式 */
  format: DateFormat;
  /** 流水长度 */
  length: number;
  /** 起始值 */
  startValue: number;
  /** 步长 */
  step: number;
}

/** 码段联合类型 */
export type CodeSegment =
  | FixedSegment
  | SerialSegment
  | DateSegment
  | RangeSerialSegment
  | RefSegment
  | FeatureSegment
  | DynamicSerialSegment
  | RefSerialSegment
  | DateSerialSegment;

// ========== 1. 数据库实体 ==========

export interface CodingRuleEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId: ID;
  /** 编码属性 ID */
  attributeId: ID;
  /** 规则名称 */
  name: string;
  /** 版本号 */
  version: number;
  /** 状态 */
  status: CodingRuleStatus;
  /** 规则定义方式 */
  ruleDefinitionType: RuleDefinitionType;
  /** 编码生成时机 */
  generationTiming: CodingGenerationTiming;
  /** Groovy 脚本（当 ruleDefinitionType='script' 时） */
  script?: string;
  /** 码段配置列表 */
  segments: CodeSegment[];
  /** 编码前缀 */
  prefix?: string;
  /** 示例编码 */
  sampleCode?: string;
  /** 修改人 */
  updater?: string;
  /** 修改时间 */
  updateTime?: string;
  /** 版本组 ID（同一声源的多个版本共享） */
  versionGroupId?: ID;
}

// ========== 2. 视图对象 ==========

export interface CodingRuleVO extends CodingRuleEntity {
  /** 编码属性名称 */
  attributeName?: string;
  /** 规则描述（自动组合） */
  ruleDescription?: string;
  /** 码段类型列表（用于描述显示） */
  segmentTypeList?: SegmentType[];
}

/** 分组后的编码规则（用于列表展示） */
export interface CodingRuleGroupVO {
  /** 分组键（attributeId） */
  attributeId: ID;
  /** 编码属性名称 */
  attributeName?: string;
  /** 最高版本规则 */
  latestRule: CodingRuleVO;
  /** 所有版本规则 */
  allVersions: CodingRuleVO[];
  /** 是否展开 */
  expanded?: boolean;
}

// ========== 3. 创建参数 ==========

export interface CodingRuleCreateDTO {
  modelId: ID;
  attributeId: ID;
  name: string;
  ruleDefinitionType: RuleDefinitionType;
  generationTiming: CodingGenerationTiming;
  script?: string;
  segments?: Omit<CodeSegment, 'id' | 'code' | 'name' | 'status' | 'referenceStatus'>[];
  prefix?: string;
}

/** 码段创建参数（不含业务字段） */
export type SegmentCreateDTO = Omit<CodeSegment, 'id' | 'code' | 'name' | 'status' | 'referenceStatus'>;

// ========== 4. 更新参数 ==========

export interface CodingRuleUpdateDTO extends Partial<CodingRuleCreateDTO> {
  id: ID;
}

// ========== 5. 查询参数 ==========

export interface CodingRuleQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId: ID;
  /** 编码属性 ID（可选，查询单个属性的规则） */
  attributeId?: ID;
  /** 规则名称模糊搜索 */
  keyword?: string;
  /** 状态筛选 */
  status?: CodingRuleStatus;
  /** 版本组 ID */
  versionGroupId?: ID;
  /** 是否只查询最高版本 */
  latestOnly?: boolean;
}

// ========== 码段查询参数 ==========

export interface SegmentQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId?: ID;
  /** 码段类型 */
  type?: SegmentType;
  /** 码段名称模糊搜索 */
  keyword?: string;
  /** 引用状态 */
  referenceStatus?: 'unused' | 'used';
}
