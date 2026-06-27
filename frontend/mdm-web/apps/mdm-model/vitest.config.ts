import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import path from 'node:path';

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['src/**/*.spec.ts', 'tests/**/*.spec.ts'],
    setupFiles: ['tests/setup.ts'],
    reporters: ['verbose'],
    outputFile: {
      json: 'reports/test-report.json',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      include: ['src/**/*.{ts,vue}'],
      exclude: [
        'node_modules/**',
        'dist/**',
        '**/*.d.ts',
        '**/*.mock.ts',
        'src/types/**',
        'src/main.ts',
        'tests/**',
      ],
    },
  },
  resolve: {
    alias: [
      // model-design 别名指向 model-manage 模块（与 vite.config.ts 保持一致）
      {
        find: /^@\/modules\/model-design\//,
        replacement: path.resolve(__dirname, 'src/modules/model-manage/') + '/',
      },
      { find: '@', replacement: path.resolve(__dirname, 'src') },
      {
        find: '@mdm/common',
        replacement: path.resolve(__dirname, '../../packages/mdm-common/src'),
      },
      {
        find: '@mdm/core',
        replacement: path.resolve(__dirname, '../../packages/mdm-core/src'),
      },
    ],
  },
});
