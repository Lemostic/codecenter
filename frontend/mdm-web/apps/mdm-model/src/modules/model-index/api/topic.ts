/**
 * modules/model-index/api/topic.ts
 *
 * 主题域 API（严格按 5 函数 + 树操作）
 */
import { http } from '@mdm/core/http';
import type { PaginatedResponse, ApiResponse } from '@mdm/common/types/api';
import type {
  TopicVO, TopicCreateDTO, TopicUpdateDTO, TopicQuery, TopicTreeNode,
} from '@/modules/model-index/types/topic';
import type { ID } from '@mdm/common/types/base';

const PREFIX = '/api/v1/model-design/topic';

/** 列表查询 */
export const listTopic = (query: TopicQuery) =>
  http.get<PaginatedResponse<TopicVO>>(PREFIX, { params: query });

/** 详情查询 */
export const getTopic = (id: ID) =>
  http.get<ApiResponse<TopicVO>>(`${PREFIX}/${id}`);

/** 新增 */
export const createTopic = (data: TopicCreateDTO) =>
  http.post<ApiResponse<TopicVO>>(PREFIX, data);

/** 更新 */
export const updateTopic = (data: TopicUpdateDTO) =>
  http.put<ApiResponse<TopicVO>>(`${PREFIX}/${data.id}`, data);

/** 删除（支持批量） */
export const deleteTopic = (ids: ID[]) =>
  http.delete<ApiResponse<void>>(PREFIX, { data: { ids } });

// ========== 树操作 ==========

/** 获取根节点树（懒加载第一层） */
export const getTopicRootTree = () =>
  http.get<ApiResponse<TopicTreeNode[]>>(`${PREFIX}/tree/root`);

/** 获取子节点（懒加载） */
export const getTopicChildren = (parentId: ID) =>
  http.get<ApiResponse<TopicTreeNode[]>>(`${PREFIX}/tree/${parentId}/children`);

/** 获取完整树（主题域管理弹窗用） */
export const getTopicFullTree = () =>
  http.get<ApiResponse<TopicVO[]>>(`${PREFIX}/tree/full`);

/** 导出主题 */
export const exportTopic = (topicId?: ID): Promise<Blob> =>
  http.get(`${PREFIX}/export`, { params: { topicId }, responseType: 'blob' }) as unknown as Promise<Blob>;

/** 导入主题 */
export const importTopic = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<ApiResponse<{ total: number; success: number; failed: number }>>(
    `${PREFIX}/import`,
    formData,
  );
};

/** 校验主题名称唯一性（同父级下） */
export const checkTopicNameUnique = (
  name: string,
  parentId: ID | null,
  excludeId?: ID,
) =>
  http.get<ApiResponse<boolean>>(`${PREFIX}/check-name`, {
    params: { name, parentId, excludeId },
  });
