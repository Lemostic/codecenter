/**
 * 根 ESLint 配置（扁平配置，eslint v9 风格）。
 *
 * 实际规则在每个 app / 包内通过 ESLint cascading 覆盖。
 * 此处仅声明根级 ignore 与共享基础规则。
 */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  ignorePatterns: [
    '**/node_modules/**',
    '**/dist/**',
    '**/.turbo/**',
    '**/tsconfig.tsbuildinfo',
    '**/coverage/**',
    '**/mock/**', // mock 数据中常含示例代码
  ],
  parser: 'vue-eslint-parser',
  parserOptions: {
    parser: '@typescript-eslint/parser',
    ecmaVersion: 2022,
    sourceType: 'module',
    extraFileExtensions: ['.vue'],
  },
  extends: [
    'eslint:recommended',
    'plugin:@typescript-eslint/recommended',
    'plugin:vue/vue3-recommended',
  ],
  plugins: ['@typescript-eslint', 'vue'],
  rules: {
    'vue/component-tags-order': ['error', { order: ['script', 'template', 'style'] }],
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    'vue/multi-word-component-names': 'off',
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_', varsIgnorePattern: '^_' }],
    'no-debugger': 'error',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    'no-restricted-imports': [
      'error',
      {
        patterns: [
          {
            group: ['@mdm/core/*/index', '@mdm/common/*/index'],
            message: '禁止深路径导入 internal package 入口，请走 package 公开面',
          },
        ],
      },
    ],
  },
};
