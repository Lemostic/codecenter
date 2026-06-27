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
    defaultDelay?: number;
}
export declare function createMockPlugin(options: MockPluginOptions): Plugin;
export declare function createResponse(res: ServerResponse, data: unknown, status?: number): void;
//# sourceMappingURL=mock.d.ts.map