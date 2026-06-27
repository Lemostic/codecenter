/**
 * @mdm/config-vite 统一导出面
 */
import type { Plugin } from 'vite';
import type { UserConfig } from 'vite';
import path from 'node:path';
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers';

export * from './mock.js';

export interface CreatePluginOptions {
  root: string;
  extraComponentDirs?: string[];
}

export function createMdmPlugins(options: CreatePluginOptions) {
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

export interface MdmViteOptions {
  root: string;
  port?: number;
  useMock?: boolean;
  mockPlugin?: Plugin;
  extraComponentDirs?: string[];
}

export function createMdmViteConfig(options: MdmViteOptions): UserConfig {
  const {
    root,
    port = 3000,
    useMock = false,
    mockPlugin,
    extraComponentDirs = [],
  } = options;

  const plugins: Plugin[] = [
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
