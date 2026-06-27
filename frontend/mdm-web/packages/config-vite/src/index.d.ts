/**
 * @mdm/config-vite 统一导出面
 */
import type { Plugin } from 'vite';
import type { UserConfig } from 'vite';
export * from './mock';
export interface CreatePluginOptions {
    root: string;
    extraComponentDirs?: string[];
}
export declare function createMdmPlugins(options: CreatePluginOptions): any[];
export interface MdmViteOptions {
    root: string;
    port?: number;
    useMock?: boolean;
    mockPlugin?: Plugin;
    extraComponentDirs?: string[];
}
export declare function createMdmViteConfig(options: MdmViteOptions): UserConfig;
//# sourceMappingURL=index.d.ts.map