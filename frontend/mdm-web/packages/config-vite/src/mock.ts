/**
 * 通用 mock 工厂
 *
 * 各 app 提供本 app 的种子数据 + handler 列表，调用此工厂生成 Vite 插件。
 * 原 `mock/vite-plugin-mock.ts` 的通用工具迁入此处。
 */
import type { Plugin } from 'vite';
import type { IncomingMessage, ServerResponse } from 'node:http';

export interface MockHandler {
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  pattern: RegExp;
  handler: (req: IncomingMessage, res: ServerResponse) => void | Promise<void>;
}

export interface MockPluginOptions {
  handlers: MockHandler[];
  defaultDelay?: number; // ms
}

export function createMockPlugin(options: MockPluginOptions): Plugin {
  const { handlers, defaultDelay = 100 } = options;

  return {
    name: 'mdm-mock',
    configureServer(server) {
      server.middlewares.use(async (req, res, next) => {
        if (!req.url?.startsWith('/api/')) {
          return next();
        }
        // 剥离 query 字符串后再匹配 pattern（handler 仍可读取 req.url 完整 URL）
        const path = req.url.split('?')[0];
        for (const h of handlers) {
          if (h.method !== req.method) continue;
          if (!h.pattern.test(path)) continue;
          await new Promise((r) => setTimeout(r, defaultDelay));
          await h.handler(req, res);
          return;
        }
        next();
      });
    },
  };
}

export function createResponse(
  res: ServerResponse,
  data: unknown,
  status = 200,
) {
  res.statusCode = status;
  res.setHeader('Content-Type', 'application/json');
  res.end(JSON.stringify({ success: true, data }));
}
