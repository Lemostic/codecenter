# Vitest 测试功能设计方案

## 概述

为 master-data-app 项目添加 Vitest 单元测试和组件测试能力。

## 技术选型

| 工具 | 版本 | 用途 |
|------|------|------|
| `vitest` | ^3.x | 测试运行器，与 Vite 无缝集成 |
| `@vue/test-utils` | ^2.x | Vue 3 组件测试工具 |
| `happy-dom` | ^15.x | 轻量级 DOM 模拟，比 jsdom 更快 |

## 测试范围

- **单元测试**：composables、utils、API 函数
- **组件测试**：Vue 组件的渲染和交互

## 项目结构

### 配置文件位置

```
master-data-app/
├── vitest.config.ts           # 根级别统一配置
├── packages/
│   └── mdm-common/
│       └── src/
│           └── composables/
│               └── useXXX.spec.ts
└── apps/
    └── mdm-model/
        └── src/
            └── modules/
                └── model-design/
                    └── composables/
                        └── useXXX.spec.ts
```

- 根配置定义全局一致的测试环境
- 各包/app 可覆盖 globals、coverage 等选项
- 测试文件直接放在源码旁边（与 .spec.ts 同目录）

## 测试命令

| 命令 | 作用 |
|------|------|
| `pnpm test` | 对所有包运行 vitest |
| `pnpm test:unit` | 只跑单元测试 |
| `pnpm test:model` | 只测试 mdm-model |
| `pnpm test:common` | 只测试 mdm-common |
| `pnpm test:watch` | watch 模式 |
| `pnpm test:coverage` | 生成覆盖率报告 |

## 实现步骤

1. 在根目录安装 vitest、@vue/test-utils、happy-dom
2. 创建根级 `vitest.config.ts`
3. 更新根 `package.json` 添加 test 脚本
4. 为 mdm-common、mdm-model 添加 test 脚本
5. 创建示例测试文件验证配置
6. 配置 CI/CD（可选）

## 根配置要点

- 环境：`happy-dom`
- 全局 API：`vue`, `describe`, `it`, `expect` 等
- 别名：与 vite.config.ts 保持一致（`@`、`#` 等）
- 覆盖配置：各子项目可覆盖 globals、coverage 等选项
