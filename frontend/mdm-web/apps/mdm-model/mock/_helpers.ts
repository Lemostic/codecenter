/**
 * apps/mdm-model/mock/_helpers.ts
 *
 * Mock 通用工具：响应 / query / body 解析，分页、模糊匹配等。
 * 不引入第三方库（手写实现），与 packages/config-vite/src/mock.ts 的 MockHandler 配合。
 */
import type { IncomingMessage, ServerResponse } from 'node:http';

/** 统一 JSON 成功响应（自动包成 ApiResponse 格式） */
export function sendJson(
  res: ServerResponse,
  data: unknown,
  status = 200,
): void {
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json');
  res.end(JSON.stringify({ success: true, data }));
}

/** 统一 JSON 错误响应 */
export function sendError(
  res: ServerResponse,
  message: string,
  status = 500,
): void {
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json');
  res.end(JSON.stringify({ success: false, message, code: String(status) }));
}

/** 从 URL 中解析 query 参数 */
export function parseQuery(url: string): Record<string, string> {
  const idx = url.indexOf('?');
  if (idx < 0) return {};
  const out: Record<string, string> = {};
  for (const pair of url.slice(idx + 1).split('&')) {
    if (!pair) continue;
    const eq = pair.indexOf('=');
    const k = eq < 0 ? pair : pair.slice(0, eq);
    const v = eq < 0 ? '' : pair.slice(eq + 1);
    try {
      out[decodeURIComponent(k)] = decodeURIComponent(v);
    } catch {
      out[k] = v;
    }
  }
  return out;
}

/** 读取并解析 JSON body（POST/PUT/PATCH 用） */
export async function parseBody<T = Record<string, unknown>>(
  req: IncomingMessage,
): Promise<T> {
  return new Promise((resolve, reject) => {
    const chunks: Buffer[] = [];
    req.on('data', (c: Buffer) => chunks.push(c));
    req.on('end', () => {
      const text = Buffer.concat(chunks).toString('utf-8');
      if (!text) return resolve({} as T);
      try {
        resolve(JSON.parse(text) as T);
      } catch (e) {
        reject(e);
      }
    });
    req.on('error', reject);
  });
}

/** 从 req.url 中剥离 query 字符串 */
export function basePath(url: string): string {
  const idx = url.indexOf('?');
  return idx < 0 ? url : url.slice(0, idx);
}

/** 从路径中提取 :id 占位符对应的值（pattern 必须含捕获组） */
export function matchParam(url: string, pattern: RegExp): string | null {
  const path = basePath(url);
  const m = pattern.exec(path);
  return m?.[1] ?? null;
}

/**
 * 通用分页工具：按 page / pageSize 切片并返回 PaginatedData 格式
 *
 * @param list 完整数据数组
 * @param page 页码（从 1 开始）
 * @param pageSize 每页条数
 */
export function paginate<T>(
  list: T[],
  page: number,
  pageSize: number,
): { rows: T[]; total: number } {
  const start = (Math.max(1, page) - 1) * Math.max(1, pageSize);
  return {
    rows: list.slice(start, start + pageSize),
    total: list.length,
  };
}

/** 通用模糊匹配：检查 item[field] 是否包含 keyword（不区分大小写） */
export function matchesKeyword(
  item: Record<string, unknown>,
  fields: string[],
  keyword: string,
): boolean {
  if (!keyword) return true;
  const kw = keyword.toLowerCase();
  return fields.some((f) => {
    const v = item[f];
    if (v == null) return false;
    return String(v).toLowerCase().includes(kw);
  });
}
