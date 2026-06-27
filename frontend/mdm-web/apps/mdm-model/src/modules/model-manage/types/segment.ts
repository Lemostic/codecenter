/**
 * modules/model-design/types/segment.ts
 *
 * 码段管理 - 业务实体类型（5 件套）
 *
 * 码段是构成编码的基本单元，支持 8 类码段：
 * 1. 固定码（fixed）    - 编码过程中保持不变的字符串
 * 2. 流水码（serial）  - 按长度、初始值、步长定义
 * 3. 日期码（date）     - 通过时间属性或系统时间动态生成
 * 4. 特征码（feature）  - 定义编码长度和特征码段关联属性
 * 5. 区间流水码（rangeSerial）- 基于特定属性值划分独立流水号段
 * 6. 引用码（ref）      - 引用某一属性的值作为该码段的值
 * 7. 动态流水码（dynamicSerial）- 基于引用属性的不同值生成不同的流水码
 * 8. 日期流水码（dateSerial）- 日期+流水号的复合结构
 * 9. 引用流水码（refSerial）- 引用值+流水号的复合结构
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';
import type {
  SegmentType, CodeSegment, FixedSegment, SerialSegment, DateSegment,
  RangeSerialSegment, RefSegment, FeatureSegment, DynamicSerialSegment,
  RefSerialSegment, DateSerialSegment, SegmentCreateDTO,
  DateFormat, RangeItem, FeatureItem,
} from './coding-rule';

// ========== 导出复用类型 ==========
export {
  type SegmentType,
  type CodeSegment,
  type FixedSegment,
  type SerialSegment,
  type DateSegment,
  type RangeSerialSegment,
  type RefSegment,
  type FeatureSegment,
  type DynamicSerialSegment,
  type RefSerialSegment,
  type DateSerialSegment,
  type SegmentCreateDTO,
  type DateFormat,
  type RangeItem,
  type FeatureItem,
  SEGMENT_TYPE_OPTIONS,
  SEGMENT_TYPE_LABEL,
  DATE_FORMAT_OPTIONS,
} from './coding-rule';

// ========== 1. 数据库实体 ==========

/** 码段实体（统一存储结构，通过 type 区分具体类型） */
export interface SegmentEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId?: ID;
  /** 码段类型 */
  type: SegmentType;
  /** 码段编码（系统生成，如 GD001、LS001） */
  code: string;
  /** 码段名称 */
  name: string;
  /** 状态 */
  status: 'enabled' | 'disabled';
  /** 引用状态 */
  referenceStatus: 'unused' | 'used';
  /** 描述/备注 */
  description?: string;

  // 固定码字段
  /** 固定值 */
  value?: string;

  // 流水码字段
  /** 码段长度 */
  length?: number;
  /** 起始值 */
  startValue?: number;
  /** 步长 */
  step?: number;

  // 日期码字段
  /** 是否当前时间 */
  isCurrentTime?: boolean;
  /** 引用属性 ID */
  refAttributeId?: ID;
  /** 日期格式 */
  format?: DateFormat;

  // 特征码字段
  /** 是否定长 */
  isFixedLength?: boolean;
  /** 码段最大长度 */
  maxLength?: number;
  /** 填充值 */
  fillValue?: string;
  /** 码段关联属性 ID */
  attributeId?: ID;
  /** 特征对象 */
  featureObject?: string;
  /** 特征值列表（JSON 字符串存储） */
  featureItemsJson?: string;

  // 区间流水码字段
  /** 区间关联属性 ID */
  rangeAttributeId?: ID;
  /** 区间关联属性名称 */
  rangeAttributeName?: string;
  /** 特征对象 */
  rangeFeatureObject?: string;
  /** 区间项列表（JSON 字符串存储） */
  rangeItemsJson?: string;

  // 引用码/引用流水码字段
  /** 是否引用自身属性 */
  isOwnAttribute?: boolean;
  /** 引用来源属性 ID */
  refSourceAttributeId?: ID;
  /** 引用模型 ID */
  refModelId?: ID;
  /** 引用属性 ID */
  refAttributeId?: ID;
  /** 截取起始位置 */
  substringPosition?: 'head' | 'tail';
  /** 截取步长 */
  substringLength?: number;

  // 公共前缀后缀
  /** 前缀 */
  prefix?: string;
  /** 后缀 */
  suffix?: string;
}

// ========== 2. 视图对象 ==========

export interface SegmentVO extends SegmentEntity {
  /** 码段类型名称 */
  typeName?: string;
  /** 核心配置描述 */
  configDescription?: string;
  /** 特征值列表（解析后的对象） */
  featureItems?: FeatureItem[];
  /** 区间项列表（解析后的对象） */
  rangeItems?: RangeItem[];
}

// ========== 3. 创建参数 ==========

export interface SegmentCreateDTO {
  modelId?: ID;
  type: SegmentType;
  name: string;
  description?: string;
  prefix?: string;
  suffix?: string;

  // 固定码
  value?: string;

  // 流水码
  length?: number;
  startValue?: number;
  step?: number;

  // 日期码
  isCurrentTime?: boolean;
  refAttributeId?: ID;
  format?: DateFormat;

  // 特征码
  isFixedLength?: boolean;
  maxLength?: number;
  fillValue?: string;
  attributeId?: ID;
  featureItems?: FeatureItem[];

  // 区间流水码
  rangeAttributeId?: ID;
  rangeItems?: RangeItem[];

  // 引用码/引用流水码
  isOwnAttribute?: boolean;
  refSourceAttributeId?: ID;
  refModelId?: ID;
  refAttributeId?: ID;
  substringPosition?: 'head' | 'tail';
  substringLength?: number;
}

// ========== 4. 更新参数 ==========

export interface SegmentUpdateDTO extends Partial<Omit<SegmentCreateDTO, 'modelId' | 'type'>> {
  id: ID;
  /** 状态（启用/停用） */
  status?: 'enabled' | 'disabled';
}

// ========== 5. 查询参数 ==========

export interface SegmentQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId?: ID;
  /** 码段类型 */
  type?: SegmentType;
  /** 码段名称模糊搜索 */
  keyword?: string;
  /** 码段编码精确搜索 */
  code?: string;
  /** 引用状态 */
  referenceStatus?: 'unused' | 'used';
  /** 状态 */
  status?: 'enabled' | 'disabled';
}

// ========== 辅助类型 ==========

/** 码段类型配置项（用于新增码段时选择） */
export interface SegmentTypeConfig {
  /** 类型值 */
  value: SegmentType;
  /** 类型名称 */
  label: string;
  /** 图标 */
  icon?: string;
  /** 说明 */
  description?: string;
}

export const SEGMENT_TYPE_CONFIGS: SegmentTypeConfig[] = [
  { value: 'fixed', label: '固定码', description: '在编码过程中保持不变的字符串' },
  { value: 'serial', label: '流水码', description: '按长度、初始值、步长定义流水码段' },
  { value: 'date', label: '日期码', description: '通过时间属性或系统当前时间动态生成' },
  { value: 'feature', label: '特征码', description: '定义编码长度和特征码段关联属性' },
  { value: 'rangeSerial', label: '区间流水码', description: '基于特定属性值划分独立流水号段' },
  { value: 'ref', label: '引用码', description: '引用某一属性的值作为该码段的值' },
  { value: 'dynamicSerial', label: '动态流水码', description: '基于引用属性的不同值生成不同的流水码' },
  { value: 'dateSerial', label: '日期流水码', description: '日期+流水号的复合结构' },
  { value: 'refSerial', label: '引用流水码', description: '引用值+流水号的复合结构' },
];

/** 码段下拉选项（用于编码规则配置时选择已有码段） */
export interface SegmentSelectOption {
  id: ID;
  code: string;
  name: string;
  type: SegmentType;
  typeName: string;
  referenceStatus: 'unused' | 'used';
  prefix?: string;
  suffix?: string;
}
