/**
 * mock/form-design.ts
 * 填报设计 Mock 数据
 */

export const mockFormDesign = {
  id: 'fd_1',
  modelId: 'model_1',
  version: 1,
  layoutColumns: 2,
  fieldLayout: 'horizontal',
  groups: [
    {
      id: 'g1',
      name: '基本信息',
      parentId: null,
      sortOrder: 1,
      attributeIds: ['attr_1', 'attr_2'],
      collapsible: true,
      defaultCollapsed: false,
    },
    {
      id: 'g2',
      name: '联系信息',
      parentId: null,
      sortOrder: 2,
      attributeIds: ['attr_3', 'attr_4'],
      collapsible: true,
      defaultCollapsed: false,
    },
  ],
  fieldStyles: [
    { attributeId: 'attr_1', controlType: 'text', colSpan: 1, fullRow: false, readonly: false, inputMethod: 'single-line', tooltipEffect: 'text' },
    { attributeId: 'attr_2', controlType: 'date', colSpan: 1, fullRow: false, readonly: false, inputMethod: 'auto', tooltipEffect: 'text' },
    { attributeId: 'attr_3', controlType: 'text', colSpan: 1, fullRow: false, readonly: false, inputMethod: 'single-line', tooltip: '请输入联系电话', tooltipEffect: 'icon' },
    { attributeId: 'attr_4', controlType: 'textarea', colSpan: 2, fullRow: true, readonly: false, inputMethod: 'multi-line', heightMultiple: 3, tooltipEffect: 'text' },
  ],
  visibilityConditions: [],
  sortFields: [
    { attributeId: 'attr_8', direction: 'asc', priority: 1 },
  ],
  treeStyle: { displayMode: 'list' },
};

export const mockFormDesignList = [mockFormDesign];

// ========== Mock HTTP Handlers ==========
import type { MockHandler } from '@mdm/config-vite/mock';
import {
  basePath,
  parseBody,
  sendError,
  sendJson,
} from './_helpers';

const PREFIX = '/api/v1/model-design/form-design';

export const mockFormDesignHandlers: MockHandler[] = [
  // 获取表单设计配置 (按 modelId)
  {
    method: 'GET',
    pattern: new RegExp(`^${PREFIX}/model/([^/]+)$`),
    handler: async (req: any, res: any) => {
      sendJson(res, mockFormDesign);
    },
  },
  // 保存表单设计配置
  {
    method: 'PUT',
    pattern: new RegExp(`^${PREFIX}/([^/]+)$`),
    handler: async (req: any, res: any) => {
      const body = await parseBody<typeof mockFormDesign>(req);
      Object.assign(mockFormDesign, body);
      sendJson(res, mockFormDesign);
    },
  },
];
