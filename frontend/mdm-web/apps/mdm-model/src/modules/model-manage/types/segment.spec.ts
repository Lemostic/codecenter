/**
 * segment 单元测试
 *
 * 测试码段管理类型定义、码段类型配置等
 */
import { describe, it, expect } from 'vitest';
import {
  SEGMENT_TYPE_CONFIGS,
  type SegmentTypeConfig,
} from './segment';

describe('segment 类型定义', () => {
  describe('SEGMENT_TYPE_CONFIGS', () => {
    it('应包含 9 种码段类型配置', () => {
      expect(SEGMENT_TYPE_CONFIGS).toHaveLength(9);
    });

    it('应包含固定码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'fixed');
      expect(config).toBeDefined();
      expect(config?.label).toBe('固定码');
      expect(config?.description).toContain('保持不变的字符串');
    });

    it('应包含流水码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'serial');
      expect(config).toBeDefined();
      expect(config?.label).toBe('流水码');
      expect(config?.description).toContain('流水码');
    });

    it('应包含日期码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'date');
      expect(config).toBeDefined();
      expect(config?.label).toBe('日期码');
      expect(config?.description).toContain('时间');
    });

    it('应包含特征码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'feature');
      expect(config).toBeDefined();
      expect(config?.label).toBe('特征码');
      expect(config?.description).toContain('特征码段关联属性');
    });

    it('应包含区间流水码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'rangeSerial');
      expect(config).toBeDefined();
      expect(config?.label).toBe('区间流水码');
      expect(config?.description).toContain('属性值');
      expect(config?.description).toContain('流水号段');
    });

    it('应包含引用码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'ref');
      expect(config).toBeDefined();
      expect(config?.label).toBe('引用码');
      expect(config?.description).toContain('引用');
    });

    it('应包含动态流水码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'dynamicSerial');
      expect(config).toBeDefined();
      expect(config?.label).toBe('动态流水码');
      expect(config?.description).toContain('不同的流水码');
    });

    it('应包含日期流水码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'dateSerial');
      expect(config).toBeDefined();
      expect(config?.label).toBe('日期流水码');
      expect(config?.description).toContain('日期');
      expect(config?.description).toContain('流水号');
    });

    it('应包含引用流水码配置', () => {
      const config = SEGMENT_TYPE_CONFIGS.find((c) => c.value === 'refSerial');
      expect(config).toBeDefined();
      expect(config?.label).toBe('引用流水码');
      expect(config?.description).toContain('引用值');
      expect(config?.description).toContain('流水号');
    });

    it('每个配置都应有 value、label 和 description', () => {
      SEGMENT_TYPE_CONFIGS.forEach((config: SegmentTypeConfig) => {
        expect(config.value).toBeDefined();
        expect(config.label).toBeDefined();
        expect(config.description).toBeDefined();
        expect(typeof config.value).toBe('string');
        expect(typeof config.label).toBe('string');
        expect(typeof config.description).toBe('string');
      });
    });

    it('所有配置的 value 都应唯一', () => {
      const values = SEGMENT_TYPE_CONFIGS.map((c) => c.value);
      const uniqueValues = new Set(values);
      expect(uniqueValues.size).toBe(values.length);
    });
  });
});
