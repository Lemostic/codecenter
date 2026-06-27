| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/common/` |
| 适用版本 | Vue 3 |
| 依赖规范 | `common/architecture.md`（先读架构再读结构） |

# 目录结构与导入规范

> 本文件定义前端项目的标准目录布局、就近原则、Barrel exports、路径别名与导入顺序。
> 本文件规则适用于 Vue 3 前端项目。

---

## 1 标准目录布局

```
src/
├── core/                  # 核心层（框架基础设施）
│   ├── http/              # Axios 实例、拦截器
│   ├── auth/              # Token 管理、权限
│   ├── router/            # 路由实例
│   ├── i18n/              # i18n 实例
│   ├── layout/            # 布局组件
│   └── error/             # 错误边界
│
├── common/                # 共享层（跨模块复用）
│   ├── components/        # 通用组件
│   │   └── {category}/    # 二级分类：data/form/feedback/structure/pickers/display
│   ├── composables/       # composable
│   ├── api/               # 跨模块通用 API
│   ├── services/          # 业务无关服务
│   ├── utils/             # 工具函数
│   ├── constants/         # 全局常量
│   ├── directives/        # 自定义指令
│   ├── types/             # 跨模块基座类型
│   ├── stores/            # 全局 Store
│   ├── locales/           # 公共语言包
│   └── styles/            # 全局样式
│
├── modules/               # 业务层（按业务域拆分）
│   └── {moduleName}/
│       ├── views/         # 页面组件（带路由）
│       ├── components/    # 模块私有组件
│       ├── stores/        # 模块私有 Store
│       ├── api/           # 接口函数
│       ├── types/         # 类型定义
│       ├── composables/   # 组合式函数
│       ├── locales/       # 模块语言包
│       ├── routes.ts      # 路由声明
│       └── index.ts       # 公开面
│
├── App.vue                # 根组件
└── main.ts                # 入口文件
```

---

## 2 功能分组原则

**PROF-FE-201** 项目 MUST 按功能模块组织页面，每个模块拥有独立目录，包含其私有组件与 composable。 [MUST]

```
modules/
├── modelDesign/
│   ├── views/
│   │   ├── ModelList.vue
│   │   ├── ModelDetail.vue
│   │   └── ModelEditor.vue
│   ├── components/
│   │   └── ModelCard.vue
│   ├── api/
│   │   └── model.ts
│   ├── types/
│   │   └── model.ts
│   ├── locales/
│   │   ├── zh-CN.ts
│   │   └── en-US.ts
│   ├── routes.ts
│   └── index.ts
└── dataItem/
    └── ...
```

**PROF-FE-202** 模块私有组件 MUST 放在模块目录下的 `components/` 中，MUST NOT 放入 `common/components/`。 [MUST]

**PROF-FE-203** 公共 `common/components/` MUST 仅存放跨模块复用的通用组件（至少被 2 个模块引用）。 [MUST]

```
# 判断标准
组件被 ≥ 2 个模块引用 → 放入 common/components/{category}/（公共）
组件仅被 1 个模块使用 → 放入 modules/{m}/components/（私有）
```

---

## 3 就近原则（Co-location）

**PROF-FE-204** 文件 SHOULD 放在离使用它最近的位置，MUST NOT 过度抽象到顶层目录。 [SHOULD/MUST]

```
# 正确：就近放置
modules/modelDesign/components/ModelCard.vue          # 仅 modelDesign 使用的组件
modules/modelDesign/composables/useModelList.ts       # 仅 modelDesign 使用的 composable

# 错误：过度抽象
common/components/ModelCard.vue                       # 仅一个模块使用，不应放公共
common/composables/useModelList.ts                    # 仅一个模块使用，不应放公共
```

**PROF-FE-205** 当私有组件/composable 被多个模块使用时，SHOULD 提升到 `common/` 目录，而非复制。 [SHOULD]

---

## 4 Barrel Exports

**PROF-FE-206** 每个组件目录 MUST 通过 `index.vue` 或 `index.ts` 统一导出。 [MUST]

```typescript
// common/components/Button/index.ts
export { default as Button } from './Button.vue';
export type { ButtonProps } from './types';
```

```typescript
// 使用方引入
import { Button } from '@/common/components/Button';
```

**PROF-FE-207** 模块 SHOULD 通过 `index.ts` 公开面对外暴露。 [SHOULD]

```typescript
// modules/modelDesign/index.ts
export { default as ModelList } from './views/ModelList.vue';
export { default as ModelDetail } from './views/ModelDetail.vue';
export type { Model, ModelCreateDTO, ModelQuery } from './types/model';
export { modelApi } from './api/model';
```

**PROF-FE-208** Barrel exports MUST NOT 导致循环依赖，MAY 使用路径别名 `@/` 简化导入。 [MUST/MAY]

---

## 5 路径别名

**PROF-FE-209** 项目 SHOULD 配置路径别名 `@/` 指向 `src/`，MUST NOT 使用多层相对路径（如 `../../../`）。 [SHOULD/MUST]

```typescript
// 正确
import { Button } from '@/common/components/Button';
import { useAuth } from '@/common/composables/useAuth';
import { userApi } from '@/modules/user/api/user';

// 错误：过深的相对路径
import { Button } from '../../../common/components/Button';
```

**tsconfig.json 配置示例**：
```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  }
}
```

---

## 6 导入顺序

**PROF-FE-210** 导入顺序 SHOULD 遵循：框架 → 第三方库 → 内部模块（别名）→ 相对路径 → 样式文件。 [SHOULD]

```typescript
// 1. 框架
import { ref, computed } from 'vue';
import { defineStore } from 'pinia';

// 2. 第三方库
import { useQuery } from '@tanstack/vue-query';
import dayjs from 'dayjs';
import { ElMessage } from 'element-plus';

// 3. 内部模块（别名导入）
import { Button } from '@/common/components/Button';
import { useAuth } from '@/common/composables/useAuth';
import { userApi } from '@/modules/user/api/user';

// 4. 相对路径导入
import { UserTable } from './components/UserTable';
import { useUserList } from './composables/useUserList';

// 5. 样式
import styles from './style.module.css';
```

---

## 7 环境变量与配置

**PROF-FE-211** 环境变量 MUST 使用 `.env` / `.env.production` 文件管理，MUST NOT 硬编码在源码中。 [MUST]

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080
VITE_APP_TITLE=管理后台(开发)

# .env.production
VITE_API_BASE_URL=https://api.example.com
VITE_APP_TITLE=管理后台
```

```typescript
// 正确：通过 import.meta.env 访问
const baseURL = import.meta.env.VITE_API_BASE_URL;

// 错误：硬编码
const baseURL = 'http://localhost:8080';
```

**PROF-FE-212** 禁止引入项目配置未声明的第三方依赖。[MUST]

---

## 8 TypeScript 严格模式

**PROF-FE-213** TypeScript MUST 启用严格模式（`strict: true`），MUST NOT 关闭核心类型检查。 [MUST]

```json
// tsconfig.json
{
  "compilerOptions": {
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noImplicitReturns": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "forceConsistentCasingInFileNames": true
  }
}
```

**PROF-FE-214** 禁止使用 `any` 类型。必要时使用 `unknown` + 类型守卫。[MUST]

```typescript
// 正确
function process(data: unknown) {
  if (typeof data === 'string') {
    return data.toUpperCase();
  }
  throw new Error('Invalid data type');
}

// 错误
function process(data: any) {
  return data.anything; // 不安全
}
```

---

## 9 ESLint 配置建议

```javascript
// eslint.config.js (flat config)
{
  rules: {
    // 导入顺序
    'import/order': ['error', {
      groups: ['builtin', 'external', 'internal', 'parent', 'sibling', 'index', 'style'],
      'newlines-between': 'always',
      alphabetize: { order: 'asc' }
    }],
    // 禁止未使用的变量
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    // 禁止 any
    '@typescript-eslint/no-explicit-any': 'error',
    // 禁止 console
    'no-console': ['warn', { allow: ['warn', 'error'] }],
    // 禁止 debugger
    'no-debugger': 'error',
  }
}
```

| 规则 | 说明 |
|------|------|
| **PROF-FE-215** | 项目应配置 ESLint + Prettier，CI 中 MUST 通过 lint 检查。[MUST] |
| **PROF-FE-216** | 禁止用 `console.log` 调试（生产代码），调试用断点或测试框架。[SHOULD] |

---

## 10 组件体量限制

| 规则 | 说明 |
|------|------|
| **PROF-FE-217** | 单个组件文件 MUST NOT 超过 600 行（含样式），超出 MUST 拆分为子组件或提取 composable。[MUST] |
| **PROF-FE-218** | 组件 Props ≤ 10 个，超出 MUST 拆分组件或合并 props 为 object。[MUST] |
| **PROF-FE-219** | 函数 ≤ 50 行，超出 MUST 拆分。[MUST] |

---

*本文件规则适用于 Vue 3 前端项目。*
