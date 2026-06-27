---
description: Monorepo 三层架构范式（@mdm/core → @mdm/common → apps/modules）。包含分层依赖方向、核心层/共享层职责、业务模块结构、文件位置决策树。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 0 Monorepo 映射说明

在 Monorepo 架构中，本文档描述的三层结构对应以下包布局：

| 本文档中的层 | Monorepo 包 | 说明 |
|-------------|------------|------|
| **核心层** | `packages/mdm-core/`（@mdm/core） | 框架基础设施，自包含，不依赖其他内部包 |
| **共享层** | `packages/mdm-common/`（@mdm/common） | 跨应用共享的组件/composable/类型，依赖 @mdm/core |
| **业务层** | `apps/{app}/src/modules/` | 各 app 内的业务模块，可依赖核心层和共享层 |

依赖方向（跨包）：
```
apps/{app}/src/modules/{m} ──→ @mdm/common ──→ @mdm/core
       │                            │              ▲
       │                            └──────────────┘
       └──────────────────────────────────────────┘
```

- `@mdm/config-vite` 是构建工具预设，仅作为 app 的 devDependency，不参与运行时依赖
- app 之间禁止互相依赖
- 导入路径使用包名：`@mdm/core`、`@mdm/common`，而非相对路径

---

## 1 分层架构

中大型项目应采用分层架构，将代码分为三个层级：

| 层 | 职责 | 能否依赖业务 |
|----|------|------------|
| **核心层** | 框架基础设施（HTTP/Auth/Router/i18n/Layout/Error） | ❌ 严禁 |
| **共享层** | 跨模块复用的组件/composable/工具/类型 | ❌ 严禁 |
| **业务层** | 各业务模块 | ✅ 可依赖核心层 + 共享层 |

### 依赖方向（硬约束）

```
业务模块 ──→ 共享层 ──→ 核心层
    │           │          ▲
    │           └──────────┘
    └──────────────────────┘
```

- ❌ 核心层禁止 import 共享层或业务层
- ❌ 共享层禁止 import 业务层
- ✅ 业务模块可 import 核心层和共享层
- ✅ 业务模块之间可以互相引用，但**必须**通过公开面（index.ts）

---

## 2 核心层职责

核心层只放与业务完全无关的框架基础设施：

| 子目录 | 职责 | 边界 |
|--------|------|------|
| HTTP | Axios 实例、拦截器、统一错误码处理 | 业务错误码在业务模块内 |
| Auth | Token 管理、权限校验 | 不存业务用户信息 |
| Router | 路由实例、守卫 | 路由声明在各模块，核心只做合并 |
| i18n | i18n 实例、语言切换逻辑 | 语言包内容在各模块 |
| Layout | AppLayout、Sidebar、Header、Footer | 不含业务菜单 |
| Error | 全局错误边界、错误上报 | 业务错误处理在模块 composables |

---

## 3 共享层职责

共享层放跨模块复用的代码：

| 子目录 | 职责 | 命名规范 |
|--------|------|---------|
| components/ | 跨模块通用组件 | 按项目配置命名 |
| composables/ | 跨模块 composable | `use{Name}.ts` |
| api/ | 跨模块通用 API | `{name}Api.ts` |
| services/ | 业务无关服务（导出、WebSocket、tree） | `{name}Service.ts` |
| utils/ | 纯工具函数 | `camelCase.ts` |
| constants/ | 全局常量枚举 | `SCREAMING_SNAKE.ts` |
| directives/ | 自定义指令 | `v-{name}.ts` |
| types/ | 跨模块基座类型 | `{name}.ts` |
| stores/ | 全局 Pinia store | `use{Name}Store.ts` |
| locales/ | 公共语言包 | `zh-CN.ts` / `en-US.ts` |
| styles/ | 全局样式 | `*.scss` |

---

## 4 业务模块结构

每个业务模块应包含以下子目录（按需创建，空目录可不存在）：

| 子目录 | 必须有 | 内容 |
|--------|--------|------|
| `views/` | ✅ | 页面组件（带路由） |
| `components/` | ⚠️ 仅当有 | 模块内复用组件 |
| `stores/` | ⚠️ 仅当有 | Pinia store |
| `api/` | ✅ | 接口函数 |
| `types/` | ✅ | 类型定义 |
| `composables/` | ⚠️ 仅当有 | 组合式函数 |
| `locales/` | ✅ | 语言包 |
| `routes.ts` | ✅ | 路由声明 |
| `index.ts` | ✅ | 公开面（跨模块引用的入口） |

- ❌ 禁止在模块内创建 `common/` `core/` `shared/` 等子目录
- ✅ 空子目录可以不存在

---

## 5 文件位置决策树

> AI 收到"我要加一个 X"任务时，按以下决策树定位文件位置。

### 决策树 1：新增页面

```
是独立业务页（带路由）吗？
├─ 是 → views/{Entity}{后缀}.vue（后缀由项目配置定义）
└─ 否（嵌入到其他页面的子组件）→ components/{ComponentName}.vue
```

### 决策树 2：新增组件

```
该组件被几个模块使用？
├─ 跨模块使用（≥2 个模块）→ 共享层 components/{category}/{ComponentName}.vue
├─ 仅本模块内复用（≥2 个页面）→ modules/{m}/components/{ComponentName}.vue
└─ 只用 1 次 → 内联到使用它的 .vue 文件
```

### 决策树 3：新增 API

```
→ modules/{m}/api/{entity}.ts
  文件内必须导出 5 个函数：list / get / create / update / delete
```

### 决策树 4：新增 Store

```
状态范围？
├─ 全局共享 → 共享层 stores/use{Name}Store.ts
├─ 跨模块共享 → 共享层 stores/business/
└─ 模块私有 → modules/{m}/stores/use{Name}Store.ts
```

### 决策树 5：新增类型

```
类型使用范围？
├─ 跨模块基座 → 共享层 types/{name}.ts
├─ 模块业务实体 → modules/{m}/types/{entity}.ts（5 件套）
└─ 单文件内部使用 → 内联，不单独导出
```

---

## 6 状态管理分层

| 层级 | 位置 | 命名模式 | 示例 |
|------|------|---------|------|
| 全局应用 | 共享层 stores/ | `useAppStore` | 主题、布局、国际化 |
| 业务全局 | 共享层 stores/business/ | `use{Name}Store` | 数据字典 |
| 模块私有 | 模块 stores/ | `use{Entity}Store` | 业务实体状态 |

- ❌ 禁止 store 内直接操作 DOM
- ❌ 禁止在 state 中存组件实例
- ❌ 禁止跨模块直接调用对方 store（应通过公开面）
- ✅ 异步方法必须用 `async/await`

---

## 7 路由规范

- ✅ 每个模块必须导出 `routes.ts`（即使是空数组）
- ✅ 路由声明在模块内（核心层只做合并）
- ✅ 路由 path 用相对路径
- ✅ 权限码格式：`{moduleName}:{action}`

---

## 8 页面命名模式

每个项目应定义统一的页面后缀，用于区分不同类型的页面。推荐的模式：

| 页面类型 | 推荐后缀 | 示例 |
|---------|---------|------|
| 列表页 | `List` | `ModelList.vue` |
| 详情页 | `Detail` | `ModelDetail.vue` |
| 编辑页 | `Editor` | `ModelEditor.vue` |
| 主页面 | `Index` | `ModelVersionIndex.vue` |
| 特殊业务页 | 业务名（需评审） | `ModelDesigner.vue` |

> 具体的后缀规范和命名铁律由项目配置定义。
