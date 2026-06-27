import path from 'node:path';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';
export * from './mock';
export function createMdmPlugins(options) {
    const { root, extraComponentDirs = [] } = options;
    return [
        AutoImport({
            resolvers: [ElementPlusResolver()],
            imports: ['vue', 'vue-router', 'pinia', 'vue-i18n'],
            dts: path.resolve(root, 'src/auto-imports.d.ts'),
        }),
        Components({
            resolvers: [ElementPlusResolver()],
            dirs: [
                path.resolve(root, 'src/components'),
                ...extraComponentDirs,
            ],
            dts: path.resolve(root, 'src/components.d.ts'),
        }),
    ];
}
export function createMdmViteConfig(options) {
    const { root, port = 3000, useMock = false, mockPlugin, extraComponentDirs = [], } = options;
    const plugins = [
        ...createMdmPlugins({ root, extraComponentDirs }),
    ];
    if (useMock && mockPlugin) {
        plugins.push(mockPlugin);
    }
    return {
        plugins,
        resolve: {
            alias: {
                '@': path.resolve(root, 'src'),
            },
        },
        server: {
            port,
            fs: {
                allow: [path.resolve(root, '../..')],
            },
        },
        optimizeDeps: {
            include: ['@mdm/core', '@mdm/common'],
        },
    };
}
