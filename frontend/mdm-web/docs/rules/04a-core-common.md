---
description: 项目架构（上）— Monorepo 三层架构、技术栈版本锁定（含 Formily）、@mdm/core 子目录边界与 Config Provider 体系、@mdm/common 子目录与 Dm-* 命名约束。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1 技术栈版本锁定

| 技术 | 最低版本 |
|------|---------|
| Vue | 3.5+ |
| TypeScript | 5.6+ |
| Vite | 5.4+ |
| Element Plus | 2.13+ |
| Tailwind CSS | 3.4+ |
| Pinia | 2.2+ |
| Vue Router | 4.4+ |
| vue-i18n | 9.13+ |
| Axios | 1.7+ |
| @formily/element-plus | 1.2+ |
| Node.js | 20 LTS+ |

- ❌ 禁止引入上表之外的第三方依赖（如 lodash、moment）
- 时间处理用 `dayjs`，深拷贝用原生 `structuredClone`，深合并用手写 `deepMerge`（不引入 lodash）
- ❌ 禁止升级到低于表中版本号的版本
- ❌ 禁止使用 CommonJS 语法，必须 ES Module

---

## 2 Monorepo 三层架构

本项目采用 Turborepo + pnpm workspace 的 Monorepo 架构，三层对应以下包布局：

| 层 | 包路径 | 包名 | 职责 |
|----|-------|------|------|
| **核心层** | `packages/mdm-core/` | `@mdm/core` | 框架基础设施（config/auth/http/router/i18n/layout） |
| **共享层** | `packages/mdm-common/` | `@mdm/common` | 跨模块复用组件/composable/工具/类型，依赖 @mdm/core |
| **业务层** | `apps/{app}/src/modules/` | — | 各 app 内的业务模块 |

当前包含一个 app：`mdm-model`（主数据模型管理），共 6 个业务模块：`model-index`、`model-design`、`model-field-config`、`model-manage`、`model-quality-rule`、`model-similar-rules`。

另有构建预设包 `packages/config-vite/`（`@mdm.config-vite`），仅作为 app 的 devDependency，不参与运行时依赖。

### 依赖方向

```
apps/{app}/src/modules/{m} ──→ @mdm/common ──→ @mdm/core
       │                            │              ▲
       │                            └──────────────┘
       └──────────────────────────────────────────┘
```

- ❌ `@mdm/core` 禁止 import `@mdm/common` 或任何 app/业务模块
- ❌ `@mdm/common` 禁止 import app/业务模块
- ✅ `modules/{m}/` 可 import `@mdm/core` 和 `@mdm/common`
- ✅ `modules/{m1}/` 可 import `modules/{m2}/`，但**必须**通过 `modules/{m2}/index.ts` 公开面
- ✅ 导入路径使用包名：`@mdm/core`、`@mdm/common`，而非相对路径
- ❌ app 之间禁止互相依赖

---

## 3 @mdm/core 子目录边界

| 子目录 | 职责 | AI 规则 |
|--------|------|---------|
| `@mdm/core/config/` | 配置类型、默认值、Provider 策略、加载合并、applyConfig | 当前使用 StaticProvider，配置获取方式待定 |
| `@mdm/core/http/` | Axios 实例、拦截器、错误码 | 自动注入 Token、解包业务错误码、401→logout |
| `@mdm/core/auth/` | Token 管理（getToken/setToken/removeToken/isLoggedIn/logout） | 不存业务用户信息，Token 存储在 localStorage |
| `@mdm/core/router/` | createAppRouter() 工厂函数、路由合并 | 路由声明在各模块 routes.ts，核心只做合并 |
| `@mdm/core/i18n/` | createAppI18n() 工厂函数、语言切换 | 语言包加载逻辑在 core，**语言包内容在各模块 locales/** |
| `@mdm/core/layout/` | AppLayout.vue（三栏布局骨架） | 不含业务菜单 |

### 配置体系（Config Provider）

配置获取方式暂未确定（可能为 API / 文件 / SDK），采用策略模式预留扩展能力：

```ts
// @mdm/core/config/types.ts
export interface ConfigProvider {
  load(): Promise<Partial<PlatformConfig>>;
}

// @mdm/core/config/providers/static.ts — 当前使用
export class StaticProvider implements ConfigProvider {
  async load(): Promise<PlatformConfig> { return { ...DEFAULT_CONFIG }; }
}

// 加载流程
export async function loadAndApplyConfig(provider: ConfigProvider): Promise<PlatformConfig> {
  let config = { ...DEFAULT_CONFIG };
  try { const external = await provider.load(); config = deepMerge(DEFAULT_CONFIG, external); }
  catch { console.warn('[config] 配置获取失败，使用框架固定值'); }
  applyConfig(config);
  return config;
}
```

当前 AI 生成代码时，**不依赖任何外部配置源**，所有配置均从 `DEFAULT_CONFIG` 读取。

---

## 4 @mdm/common 子目录与组件命名

| 子目录 | 职责 | 命名规范 |
|--------|------|---------|
| `@mdm/common/components/{category}/` | 跨模块通用组件 | Dm-* 前缀（见下方）或 PascalCase |
| `@mdm/common/composables/` | 跨模块 composable | `use{Name}.ts` |
| `@mdm/common/api/` | 跨模块通用 API | `{name}Api.ts` |
| `@mdm/common/services/` | 业务无关服务 | `{name}Service.ts` |
| `@mdm/common/utils/` | 纯工具函数 | `camelCase.ts` |
| `@mdm/common/constants/` | 全局常量枚举 | `SCREAMING_SNAKE.ts` |
| `@mdm/common/directives/` | 自定义指令 | `v-{name}.ts` |
| `@mdm/common/types/` | 跨模块基座类型（从 @mdm/core 重新导出 + tree/import-export） | `{name}.ts` |
| `@mdm/common/stores/` | 全局 Pinia store | `use{Name}Store.ts` |
| `@mdm/common/locales/` | 公共语言包 | `zh-CN.ts` / `en-US.ts` |
| `@mdm/common/styles/` | 全局样式 | `*.css` |

### Dm-* 命名强约束

| 规则 | 说明 |
|------|------|
| ✅ 9 大高频场景必须 `Dm-` 前缀 | TpTable / TpPagination / TpCardList / TpConfirm / TpMessage / TpEmpty / TpSectionTitle / TpTree（含 TpTreeLazy）/ TpPageFrame |
| ✅ 其它通用组件用 `PascalCase` | 如 `FormDialog.vue`、`SearchForm.vue`、`LoadingOverlay.vue` |
| ❌ 禁止旧的 `Pro*` 命名 | `ProTable` / `ProForm` 等旧命名已废弃 |
| ❌ 禁止非 `Dm-` 前缀覆盖 9 大场景 | |

### components/{category}/ 二级分类

| 分类 | 用途 | 示例 |
|------|------|------|
| `data/` | 数据展示 | TpTable, TpPagination, TpEmpty, TpTree, TpTreeLazy, TpCardList |
| `form/` | 表单 | FormDialog, SearchForm |
| `feedback/` | 用户反馈 | TpConfirm, TpMessage, LoadingOverlay |
| `structure/` | 页面结构 | TpPageHeader, TpPageFrame, TpLeftTreeLayout, TpSectionTitle |
| `pickers/` | 选择器 | IconPicker, TreePicker |
| `display/` | 展示 | DescriptionList, StatusTag |
