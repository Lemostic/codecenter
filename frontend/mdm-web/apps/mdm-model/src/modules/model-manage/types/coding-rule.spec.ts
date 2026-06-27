/**
 * coding-rule 单元测试
 *
 * 测试编码规则类型定义、枚举常量、状态转换等
 */
import { describe, it, expect } from 'vitest';
import {
  CODING_RULE_STATUS_OPTIONS,
  CODING_RULE_STATUS_LABEL,
  CODING_RULE_STATUS_TAG,
  SEGMENT_TYPE_OPTIONS,
  SEGMENT_TYPE_LABEL,
  CODING_GENERATION_TIMING_OPTIONS,
  CODING_GENERATION_TIMING_LABEL,
  RULE_DEFINITION_TYPE_OPTIONS,
  DATE_FORMAT_OPTIONS,
  type CodingRuleStatus,
  type SegmentType,
  type CodingGenerationTiming,
  type RuleDefinitionType,
  type DateFormat,
} from './coding-rule';

describe('coding-rule 类型定义', () => {
  describe('CODING_RULE_STATUS_OPTIONS', () => {
    it('应包含 4 种状态', () => {
      expect(CODING_RULE_STATUS_OPTIONS).toHaveLength(4);
    });

    it('应包含编辑中状态', () => {
      const status = CODING_RULE_STATUS_OPTIONS.find((s) => s.value === 'draft');
      expect(status).toBeDefined();
      expect(status?.label).toBe('编辑中');
    });

    it('应包含生效状态', () => {
      const status = CODING_RULE_STATUS_OPTIONS.find((s) => s.value === 'active');
      expect(status).toBeDefined();
      expect(status?.label).toBe('生效');
    });

    it('应包含停用状态', () => {
      const status = CODING_RULE_STATUS_OPTIONS.find((s) => s.value === 'disabled');
      expect(status).toBeDefined();
      expect(status?.label).toBe('停用');
    });

    it('应包含历史状态', () => {
      const status = CODING_RULE_STATUS_OPTIONS.find((s) => s.value === 'history');
      expect(status).toBeDefined();
      expect(status?.label).toBe('历史');
    });
  });

  describe('CODING_RULE_STATUS_LABEL', () => {
    it('应正确映射所有状态到中文标签', () => {
      expect(CODING_RULE_STATUS_LABEL['draft']).toBe('编辑中');
      expect(CODING_RULE_STATUS_LABEL['active']).toBe('生效');
      expect(CODING_RULE_STATUS_LABEL['disabled']).toBe('停用');
      expect(CODING_RULE_STATUS_LABEL['history']).toBe('历史');
    });
  });

  describe('CODING_RULE_STATUS_TAG', () => {
    it('应返回正确的 Element Plus Tag 类型', () => {
      expect(CODING_RULE_STATUS_TAG['draft']).toBe('warning');
      expect(CODING_RULE_STATUS_TAG['active']).toBe('success');
      expect(CODING_RULE_STATUS_TAG['disabled']).toBe('info');
      expect(CODING_RULE_STATUS_TAG['history']).toBe('info');
    });
  });

  describe('SEGMENT_TYPE_OPTIONS', () => {
    it('应包含 9 种码段类型', () => {
      expect(SEGMENT_TYPE_OPTIONS).toHaveLength(9);
    });

    it('应包含固定码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'fixed');
      expect(segment?.label).toBe('固定码');
    });

    it('应包含流水码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'serial');
      expect(segment?.label).toBe('流水码');
    });

    it('应包含日期码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'date');
      expect(segment?.label).toBe('日期码');
    });

    it('应包含特征码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'feature');
      expect(segment?.label).toBe('特征码');
    });

    it('应包含区间流水码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'rangeSerial');
      expect(segment?.label).toBe('区间流水码');
    });

    it('应包含引用码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'ref');
      expect(segment?.label).toBe('引用码');
    });

    it('应包含动态流水码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'dynamicSerial');
      expect(segment?.label).toBe('动态映射流水码');
    });

    it('应包含日期流水码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'dateSerial');
      expect(segment?.label).toBe('日期流水码');
    });

    it('应包含引用流水码', () => {
      const segment = SEGMENT_TYPE_OPTIONS.find((s) => s.value === 'refSerial');
      expect(segment?.label).toBe('引用流水码');
    });
  });

  describe('SEGMENT_TYPE_LABEL', () => {
    it('应正确映射所有码段类型到中文标签', () => {
      expect(SEGMENT_TYPE_LABEL['fixed']).toBe('固定码');
      expect(SEGMENT_TYPE_LABEL['serial']).toBe('流水码');
      expect(SEGMENT_TYPE_LABEL['date']).toBe('日期码');
      expect(SEGMENT_TYPE_LABEL['feature']).toBe('特征码');
      expect(SEGMENT_TYPE_LABEL['rangeSerial']).toBe('区间流水码');
      expect(SEGMENT_TYPE_LABEL['ref']).toBe('引用码');
      expect(SEGMENT_TYPE_LABEL['dynamicSerial']).toBe('动态映射流水码');
      expect(SEGMENT_TYPE_LABEL['dateSerial']).toBe('日期流水码');
      expect(SEGMENT_TYPE_LABEL['refSerial']).toBe('引用流水码');
    });
  });

  describe('CODING_GENERATION_TIMING_OPTIONS', () => {
    it('应包含 3 种生成时机', () => {
      expect(CODING_GENERATION_TIMING_OPTIONS).toHaveLength(3);
    });

    it('应包含按钮生成', () => {
      const timing = CODING_GENERATION_TIMING_OPTIONS.find((t) => t.value === 'button');
      expect(timing?.label).toBe('按钮生成');
    });

    it('应包含保存时生成', () => {
      const timing = CODING_GENERATION_TIMING_OPTIONS.find((t) => t.value === 'onSave');
      expect(timing?.label).toBe('保存时生成');
    });

    it('应包含生效时生成', () => {
      const timing = CODING_GENERATION_TIMING_OPTIONS.find((t) => t.value === 'onActive');
      expect(timing?.label).toBe('生效时生成');
    });
  });

  describe('CODING_GENERATION_TIMING_LABEL', () => {
    it('应正确映射所有生成时机到中文标签', () => {
      expect(CODING_GENERATION_TIMING_LABEL['button']).toBe('按钮生成');
      expect(CODING_GENERATION_TIMING_LABEL['onSave']).toBe('保存时生成');
      expect(CODING_GENERATION_TIMING_LABEL['onActive']).toBe('生效时生成');
    });
  });

  describe('RULE_DEFINITION_TYPE_OPTIONS', () => {
    it('应包含 2 种定义方式', () => {
      expect(RULE_DEFINITION_TYPE_OPTIONS).toHaveLength(2);
    });

    it('应包含码段组合', () => {
      const type = RULE_DEFINITION_TYPE_OPTIONS.find((t) => t.value === 'segment');
      expect(type?.label).toBe('码段组合');
    });

    it('应包含脚本自定义', () => {
      const type = RULE_DEFINITION_TYPE_OPTIONS.find((t) => t.value === 'script');
      expect(type?.label).toBe('脚本自定义');
    });
  });

  describe('DATE_FORMAT_OPTIONS', () => {
    it('应包含 9 种日期格式', () => {
      expect(DATE_FORMAT_OPTIONS).toHaveLength(9);
    });

    it('应包含常用格式', () => {
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'yyyy')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'yyyyMMdd')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'yyyyMM')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'yy')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'yyMMdd')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'mm')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'mmdd')).toBe(true);
      expect(DATE_FORMAT_OPTIONS.some((f) => f.value === 'dd')).toBe(true);
    });
  });
});

describe('类型守卫', () => {
  it('CodingRuleStatus 应接受有效状态值', () => {
    const status: CodingRuleStatus = 'draft';
    expect(status).toBe('draft');
  });

  it('SegmentType 应接受有效码段类型值', () => {
    const type: SegmentType = 'fixed';
    expect(type).toBe('fixed');
  });

  it('CodingGenerationTiming 应接受有效生成时机值', () => {
    const timing: CodingGenerationTiming = 'button';
    expect(timing).toBe('button');
  });

  it('RuleDefinitionType 应接受有效定义类型值', () => {
    const type: RuleDefinitionType = 'segment';
    expect(type).toBe('segment');
  });

  it('DateFormat 应接受有效日期格式值', () => {
    const format: DateFormat = 'yyyyMMdd';
    expect(format).toBe('yyyyMMdd');
  });
});
