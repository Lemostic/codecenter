/**
 * modules/model-index/api/topic.ts
 *
 * 主题域 API —— 已对齐后端契约 /api/mdm/encode/themes/*
 *
 * 后端契约：
 *   - 列表（树）  GET    /api/mdm/encode/themes/tree?tenantId=
 *   - 子节点      GET    /api/mdm/encode/themes/children?tenantId=&parentId=
 *   - 创建        POST   /api/mdm/encode/themes
 *   - 更新        PUT    /api/mdm/encode/themes/{id}
 *   - 删除        DELETE /api/mdm/encode/themes/{id}
 *
 * 后端字段映射：
 *   id, parentId, domainCode, domainName, sortOrder, remark, tenantId, createBy, createTime
 */
import { http } from '@mdm/core/http';
import type { ApiResponse } from '@mdm/common/types/api';
import type { ID } from '@mdm/common/types/base';
import type {
  TopicVO, TopicCreateDTO, TopicUpdateDTO, TopicTreeNode,
} from '@/modules/model-index/types/topic';

const PREFIX = '/api/mdm/encode/themes';

// ============== 后端真实字段类型 ==============

export interface BackendTheme {
  id: string;
  parentId: string | null;
  domainCode: string;
  domainName: string;
  sortOrder: number;
  remark: string | null;
  tenantId: string | null;
  createBy: string | null;
  createTime: string | null;
  updateBy: string | null;
  updateTime: string | null;
}

/** 后端主题域 → 前端 VO */
export function toTopicVO(t: BackendTheme): TopicVO {
  return {
    id: t.id,
    name: t.domainName,
    code: t.domainCode,
    parentId: (t.parentId ?? null) as ID | null,
    sortNo: t.sortOrder,
    remark: t.remark ?? '',
    createdBy: t.createBy ?? 'system',
    createdAt: t.createTime ?? '',
    updatedBy: t.updateBy ?? 'system',
    updatedAt: t.updateTime ?? '',
    isLeaf: true, // 由调用方根据 children 修正
  };
}

/** 后端主题域 → 前端树节点 */
export function toTopicTreeNode(t: BackendTheme): TopicTreeNode {
  return {
    id: t.id as ID,
    name: t.domainName,
    parentId: (t.parentId ?? null) as ID | null,
    code: t.domainCode,
    sortNo: t.sortOrder,
    isLeaf: true,
  };
}

/** 前端 DTO → 后端请求体 */
export function toBackendPayload(dto: TopicCreateDTO | TopicUpdateDTO): Partial<BackendTheme> {
  const out: Partial<BackendTheme> = {};
  if ('id' in dto && dto.id) out.id = dto.id as string;
  if ('name' in dto) out.domainName = dto.name;
  if ('code' in dto) out.domainCode = dto.code;
  if ('parentId' in dto) out.parentId = (dto.parentId as string) || null;
  if ('sortNo' in dto) out.sortOrder = dto.sortNo ?? 0;
  if ('remark' in dto) out.remark = dto.remark ?? '';
  return out;
}

// ============== 业务 API ==============

/** 获取根节点树（一次返回所有根节点） */
export async function getTopicRootTree() {
  // 不传 tenantId 让后端以 null 查询，匹配 H2 默认 tenantId=null 的行
  const resp = await http.get<ApiResponse<BackendTheme[]>>(`${PREFIX}/tree`);
  const data = resp.data.data ?? [];
  return {
    ...resp,
    data: {
      ...resp.data,
      data: data.map(toTopicTreeNode),
    },
  };
}

/** 获取子节点（懒加载） */
export async function getTopicChildren(parentId: ID) {
  const resp = await http.get<ApiResponse<BackendTheme[]>>(`${PREFIX}/children`, {
    params: { parentId: parentId === '' ? null : parentId },
  });
  const data = resp.data.data ?? [];
  return {
    ...resp,
    data: {
      ...resp.data,
      data: data.map(toTopicTreeNode),
    },
  };
}

/** 获取完整树（主题域管理弹窗用） */
export async function getTopicFullTree() {
  const resp = await http.get<ApiResponse<BackendTheme[]>>(`${PREFIX}/tree`);
  const data = resp.data.data ?? [];
  return {
    ...resp,
    data: {
      ...resp.data,
      data: data.map(toTopicVO),
    },
  };
}

/** 列表查询 */
export async function listTopic(_query: Record<string, unknown>) {
  return getTopicFullTree();
}

/** 详情查询 */
export async function getTopic(id: ID) {
  const tree = await getTopicFullTree();
  const found = (tree.data.data ?? []).find(t => t.id === id);
  return {
    ...tree,
    data: {
      ...tree.data,
      data: found ?? null,
    },
  };
}

/** 新增 */
export async function createTopic(data: TopicCreateDTO) {
  const payload = toBackendPayload(data);
  const resp = await http.post<ApiResponse<BackendTheme>>(PREFIX, payload);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: resp.data.data ? toTopicVO(resp.data.data) : null,
    },
  };
}

/** 更新 */
export async function updateTopic(data: TopicUpdateDTO) {
  const payload = toBackendPayload(data);
  const resp = await http.put<ApiResponse<BackendTheme>>(`${PREFIX}/${data.id}`, payload);
  return {
    ...resp,
    data: {
      ...resp.data,
      data: resp.data.data ? toTopicVO(resp.data.data) : null,
    },
  };
}

/** 删除（后端单条 DELETE，前端逐个删除并聚合结果） */
export async function deleteTopic(ids: ID[]) {
  const results = await Promise.allSettled(ids.map(id => http.delete<ApiResponse<void>>(`${PREFIX}/${id}`)));
  const failed = results.filter(r => r.status === 'rejected');
  return {
    data: {
      success: failed.length === 0,
      message: failed.length === 0
        ? `成功删除 ${ids.length} 个主题`
        : `${ids.length - failed.length}/${ids.length} 删除成功`,
      data: null,
      timestamp: Date.now(),
    } as ApiResponse<void>,
  };
}

/** 校验主题名称唯一性 — 后端通过 create 校验，先返回 true */
export async function checkTopicNameUnique(
  _name: string,
  _parentId: ID | null,
  _excludeId?: ID,
) {
  return { data: { success: true, data: { data: true } } } as { data: ApiResponse<boolean> };
}

/** 导出主题 — 后端暂未提供 */
export async function exportTopic(_topicId?: ID): Promise<Blob> {
  throw new Error('导出主题接口尚未在后端实现');
}

/** 导入主题 — 后端暂未提供 */
export async function importTopic(_file: File) {
  throw new Error('导入主题接口尚未在后端实现');
}
