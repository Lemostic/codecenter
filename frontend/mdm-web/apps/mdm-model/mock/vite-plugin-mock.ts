/**
 * apps/mdm-model/mock 入口
 *
 * 调用 @mdm/config-vite 的 createMockPlugin，合并本 app 所有模块的 mock handlers。
 *
 * 各模块的 seed 数据 + handler 数组从 ./model-design 等文件 import 后合并。
 * @see docs/前端概要设计.md §4.15
 */
import { createMockPlugin, type MockHandler } from '@mdm/config-vite/mock';
import { mockModelHandlers } from './model-design';
import { mockTopicHandlers } from './topic';
import { mockAttributeHandlers } from './attribute';
import { mockFormDesignHandlers } from './form-design';
import { mockSimilarityRuleHandlers } from './similarity-rule';
import { mockCodingRuleHandlers } from './coding-rule';
import { mockQualityRuleHandlers } from './quality-rule';

const handlers: MockHandler[] = [
  ...mockModelHandlers,
  ...mockTopicHandlers,
  ...mockAttributeHandlers,
  ...mockFormDesignHandlers,
  ...mockSimilarityRuleHandlers,
  ...mockCodingRuleHandlers,
  ...mockQualityRuleHandlers,
];

export const mockPlugin = createMockPlugin({ handlers });

export default mockPlugin;
