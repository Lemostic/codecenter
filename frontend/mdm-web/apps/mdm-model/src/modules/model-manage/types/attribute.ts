/**
 * modules/model-design/types/attribute.ts
 *
 * 属性配置 - 业务实体类型（5 件套）
 */
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// ========== 枚举/常量 ==========

/** 数据类型分类（归一化后的逻辑类型） */
export type DataCategory = 'string' | 'number' | 'date' | 'file' | 'clob';

/** 物理数据类型（由数据源决定，此处列出常见类型） */
export type PhysicalDataType =
  | 'VARCHAR' | 'CHAR' | 'TEXT' | 'CLOB'
  | 'INT' | 'BIGINT' | 'DECIMAL' | 'FLOAT' | 'DOUBLE'
  | 'DATE' | 'DATETIME' | 'TIMESTAMP'
  | 'BLOB';

export const DATA_CATEGORY_LABEL: Record<DataCategory, string> = {
  string: '字符',
  number: '数值',
  date: '日期',
  file: '文件',
  clob: '大文本',
};

/** 长度单位 */
export type LengthUnit = 'byte' | 'char';

export const LENGTH_UNIT_LABEL: Record<LengthUnit, string> = {
  byte: '字节',
  char: '字符',
};

/** 属性状态 */
export type AttributeStatus = 'enabled' | 'disabled';

export const ATTRIBUTE_STATUS_OPTIONS: { value: AttributeStatus; label: string }[] = [
  { value: 'enabled', label: '启用' },
  { value: 'disabled', label: '停用' },
];

export const ATTRIBUTE_STATUS_LABEL: Record<AttributeStatus, string> = {
  enabled: '启用',
  disabled: '停用',
};

/** 关联对象类型 */
export type RelationType = 'model' | 'dictionary';

export const RELATION_TYPE_LABEL: Record<RelationType, string> = {
  model: '关联模型',
  dictionary: '关联字典',
};

// ========== 关联对象配置 ==========

/** 带出字段配置 */
export interface BringOutField {
  /** 关联模型/字典中的字段 */
  sourceField: string;
  /** 显示名称（可自定义） */
  displayName: string;
}

/** 赋值字段配置 */
export interface AssignField {
  /** 当前模型中的目标属性 ID */
  targetAttributeId: ID;
  /** 关联模型/字典中的源字段 */
  sourceField: string;
}

/** 过滤规则配置 */
export interface FilterRule {
  /** 过滤值（关联对象中的字段） */
  filterField: string;
  /** 过滤条件属性（当前模型中配置了关联对象的字段） */
  conditionAttributeId: ID;
  /** 条件引用对象（自动带出） */
  conditionRefObject: string;
  /** 过滤条件值（条件引用对象中的字段） */
  conditionValueField: string;
  /** 过滤条件属性是否必填 */
  conditionRequired: boolean;
}

/** 关联对象完整配置 */
export interface RelationConfig {
  /** 关联类型 */
  relationType: RelationType;
  /** 关联模型/字典 ID */
  refObjectId: ID;
  /** 关联模型/字典名称 */
  refObjectName?: string;
  /** 关联字段（可多选） */
  refFieldIds: ID[];
  /** 显示字段 */
  displayFieldId: ID;
  /** 是否多值 */
  multiValue: boolean;
  /** 带出字段 */
  bringOutFields?: BringOutField[];
  /** 赋值字段 */
  assignFields?: AssignField[];
  /** 过滤规则 */
  filterRules?: FilterRule[];
}

// ========== 计算表达式 ==========

/** 计算表达式配置 */
export interface ExpressionConfig {
  /** 表达式文本 */
  expression: string;
  /** 表达式描述 */
  description?: string;
}

// ========== 1. 数据库实体 ==========

export interface AttributeEntity extends BaseEntity {
  /** 所属模型 ID */
  modelId: ID;
  /** 属性名称（业务名称，模型内唯一） */
  name: string;
  /** 英文名称 / 字段名称（模型内唯一） */
  englishName: string;
  /** 物理数据类型 */
  dataType: PhysicalDataType;
  /** 数据分类（归一化逻辑类型） */
  dataCategory: DataCategory;
  /** 长度（含精度长度） */
  length?: number;
  /** 长度单位 */
  lengthUnit?: LengthUnit;
  /** 精度 */
  precision?: number;
  /** 仅为正数 */
  positiveOnly: boolean;
  /** 是否必填 */
  required: boolean;
  /** 是否唯一 */
  unique: boolean;
  /** 注释 */
  comment: string;
  /** 排序号 */
  sortOrder: number;
  /** 属性状态 */
  status: AttributeStatus;
  /** 版本号 */
  version: number;
  /** 是否匹配字段 */
  matchField: boolean;
  /** 是否流程关键字段 */
  processField: boolean;
  /** 是否文件字段 */
  fileField: boolean;
  /** 关联对象配置 */
  relationConfig?: RelationConfig;
  /** 计算表达式配置 */
  expressionConfig?: ExpressionConfig;
  /** 默认值 */
  defaultValue?: string;
  /** 是否曾生效过 */
  hasBeenActive: boolean;
}

// ========== 2. 视图对象 ==========

export interface AttributeVO extends AttributeEntity {
  /** 状态标签文本 */
  statusLabel: string;
  /** 数据分类标签 */
  dataCategoryLabel: string;
  /** 关联对象显示文本 */
  relationDisplay?: string;
  /** 计算表达式摘要 */
  expressionSummary?: string;
  /** 是否被引用（删除校验用） */
  isReferenced?: boolean;
  /** 引用详情（删除校验时展示） */
  referenceDetails?: string[];
}

// ========== 3. 创建参数 ==========

export interface AttributeCreateDTO {
  modelId: ID;
  name: string;
  englishName: string;
  dataType: PhysicalDataType;
  length?: number;
  precision?: number;
  positiveOnly?: boolean;
  required?: boolean;
  unique?: boolean;
  comment?: string;
  sortOrder?: number;
}

// ========== 4. 更新参数 ==========

export interface AttributeUpdateDTO {
  id: ID;
  name?: string;
  englishName?: string;
  dataType?: PhysicalDataType;
  length?: number;
  precision?: number;
  positiveOnly?: boolean;
  required?: boolean;
  unique?: boolean;
  comment?: string;
  sortOrder?: number;
}

// ========== 5. 查询参数 ==========

export interface AttributeQuery extends PaginationParams {
  /** 所属模型 ID */
  modelId: ID;
  /** 版本号 */
  version?: number;
  /** 名称或英文名称模糊搜索 */
  keyword?: string;
  /** 状态过滤 */
  status?: AttributeStatus;
}

// ========== 扩展类型 ==========

/** 属性配置项支持矩阵 */
export const DATA_CATEGORY_CONFIG_SUPPORT: Record<DataCategory, {
  processField: boolean;
  matchField: boolean;
  relationConfig: boolean;
  expressionConfig: boolean;
  fileField: boolean;
}> = {
  string: { processField: true, matchField: true, relationConfig: true, expressionConfig: true, fileField: false },
  number: { processField: true, matchField: true, relationConfig: true, expressionConfig: true, fileField: false },
  date:   { processField: true, matchField: true, relationConfig: true, expressionConfig: true, fileField: false },
  file:   { processField: false, matchField: false, relationConfig: false, expressionConfig: false, fileField: true },
  clob:   { processField: false, matchField: false, relationConfig: false, expressionConfig: true, fileField: false },
};
