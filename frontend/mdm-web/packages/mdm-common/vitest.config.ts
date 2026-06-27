import { defineConfig } from 'vitest/config';
import path from 'node:path';

export default defineConfig({
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['src/**/*.spec.ts', 'src/**/*.test.ts'],
    reporters: ['verbose'],
    outputFile: {
      json: 'reports/test-report.json',
    },
    coverage: {
      provider: 'v8',
      reporter: ['text', 'json', 'html'],
      exclude: [
        'node_modules/**',
        'dist/**',
        '**/*.d.ts',
        'src/**/*.mock.ts',
        'src/**/*.stories.ts',
        'src/types/**',
      ],
    },
  },
  resolve: {
    alias: {
      '@mdm/common': path.resolve(__dirname, 'src'),
    },
  },
});
