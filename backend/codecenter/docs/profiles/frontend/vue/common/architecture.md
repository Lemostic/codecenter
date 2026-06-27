| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/common/` |
| 适用版本 | Vue 3 |
| 依赖规范 | `universal/api-design.md`（API 路径前缀、错误码） |

# 三层架构与文件定位

> 本文件定义前端项目的分层架构、依赖方向、状态管理分层、路由规范以及 5 个文件定位决策树。
> 本文件规则适用于 Vue 3 前端项目。

---

## 1 三层架构

中大型项目应采用三层架构，将代码分为三个层级：

| 层 | 路径前缀 | 职责 | 能否依赖业务 |
|----|---------|------|------------|
| **核心层** | `src/core/` | 框架基础设施（HTTP/Auth/Router/i18n/Layout/Error） | ❌ 严禁 |
| **共享层** | `src/common/` | 跨模块复用的组件/composable/工具/类型 | ❌ 严禁 |
| **业务层** | `src/modules/` | 业务模块 | ✅ 可依赖核心层 + 共享层 |

### 依赖方向（硬约束）

```
业务模块 ──→ 共享层 ──→ 核心层
   │           │          ▲
   │           └──────────┘
   └──────────────────────┘
```

| 规则 | 说明 |
|------|------|
| **PROF-FE-301** | 核心层禁止 import 共享层或业务层任何文件。[MUST] |
| **PROF-FE-302** | 共享层禁止 import 业务层任何文件。[MUST] |
| **PROF-FE-303** | 业务模块可 import 核心层和共享层。[MUST] |
| **PROF-FE-304** | 业务模块之间可以互相引用，但**必须**通过公开面（`index.ts`）。[MUST] |

---

## 2 核心层（core）职责边界

核心层只放与业务完全无关的框架基础设施：

| 子目录 | 职责 | 边界 |
|--------|------|------|
| `core/http/` | Axios 实例、拦截器、统一错误码处理 | 业务错误码在 `modules/{m}/constants/` |
| `core/auth/` | Token 管理、权限校验 | 不存业务用户信息 |
| `core/router/` | 路由实例、守卫 | 路由声明在各模块，核心只做合并 |
| `core/i18n/` | i18n 实例、语言切换逻辑 | 语言包内容在各模块 `locales/` |
| `core/layout/` | AppLayout、Sidebar、Header、Footer | 不含业务菜单 |
| `core/error/` | 全局错误边界、错误上报 | 业务错误处理在模块 `composables/` |

---

## 3 共享层（common）职责与命名

共享层放跨模块复用的代码：

| 子目录 | 职责 | 命名规范 |
|--------|------|---------|
| `common/components/` | 跨模块通用组件 | PascalCase / 由项目配置定义前缀 |
| `common/composables/` | 跨模块 composable | `use{Name}.ts` |
| `common/api/` | 跨模块通用 API | `{name}Api.ts` |
| `common/services/` | 业务无关服务（导出、WebSocket、tree） | `{name}Service.ts` |
| `common/utils/` | 纯工具函数 | `camelCase.ts` |
| `common/constants/` | 全局常量枚举 | `SCREAMING_SNAKE.ts` |
| `common/directives/` | 自定义指令 | `v-{name}.ts` |
| `common/types/` | 跨模块基座类型 | `{name}.ts` |
| `common/stores/` | 全局 Store（Pinia） | `use{Name}Store.ts` |
| `common/locales/` | 公共语言包 | `zh-CN.ts` / `en-US.ts` |
| `common/styles/` | 全局样式 | `*.scss` |

---

## 4 业务模块（modules）结构

每个业务模块应包含以下子目录（按需创建，空目录可不存在）：

| 子目录 | 必须有 | 内容 |
|--------|--------|------|
| `views/` | ✅ | 页面组件（带路由） |
| `components/` | ⚠️ 仅当有 | 模块内复用组件 |
| `stores/` | ⚠️ 仅当有 | Store（Pinia） |
| `api/` | ✅ | 接口函数 |
| `types/` | ✅ | 类型定义 |
| `composables/` | ⚠️ 仅当有 | 组合式函数 |
| `locales/` | ✅ | 语言包 |
| `routes.ts` | ✅ | 路由声明 |
| `index.ts` | ✅ | 公开面（跨模块引用的入口） |

| 规则 | 说明 |
|------|------|
| **PROF-FE-305** | 禁止在模块内创建 `common/` `core/` `shared/` 等子目录。[MUST] |
| **PROF-FE-306** | 空子目录可以不存在（按需创建）。[MUST] |

---

## 5 文件位置决策树

> AI 收到"我要加一个 X"任务时，按以下决策树定位文件位置。

### 决策树 1：新增页面

```
是独立业务页（带路由）吗？
├─ 是 → 是数据列表/详情/编辑/主页？
│       ├─ 是 → views/{Entity}{List|Detail|Editor|Index}.vue
│       └─ 否（设计器/画布/特殊业务）→ views/{Entity}{业务名}.vue
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
  命名铁律：listEntity / getEntity / createEntity / updateEntity / deleteEntity
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
├─ 模块业务实体 → modules/{m}/types/{entity}.ts（5 件套全导出）
└─ 单文件内部使用 → 内联，不单独导出
```

---

## 6 状态管理分层

| 层级 | 位置 | 命名模式 | 示例 |
|------|------|---------|------|
| 全局应用 | 共享层 `stores/` | `useAppStore` | 主题、布局、国际化 |
| 业务全局 | 共享层 `stores/business/` | `use{Name}Store` | 数据字典 |
| 模块私有 | 模块 `stores/` | `use{Entity}Store` | 业务实体状态 |

| 规则 | 说明 |
|------|------|
| **PROF-FE-307** | 禁止 Store 内直接操作 DOM。[MUST] |
| **PROF-FE-308** | 禁止在 state 中存组件实例。[MUST] |
| **PROF-FE-309** | 禁止跨模块直接调用对方 Store（应通过公开面 + 事件总线）。[MUST] |
| **PROF-FE-310** | 异步方法必须用 `async/await`。[MUST] |
| **PROF-FE-311** | Store ID 格式：`{module-name}-{entity}`，保证全局唯一。[MUST] |

---

## 7 路由规范

| 规则 | 说明 |
|------|------|
| **PROF-FE-312** | 每个模块必须导出 `routes.ts`（即使是空数组）。[MUST] |
| **PROF-FE-313** | 路由声明在模块内（核心层只做合并）。[MUST] |
| **PROF-FE-314** | 路由 path 用相对路径（不要重复 `MODULE_NAME`）。[MUST] |
| **PROF-FE-315** | 权限码格式：`{moduleName}:{action}`（如 `model-design:read`）。[MUST] |
| **PROF-FE-316** | 路由 name 格式：`{moduleName}-{type}`（如 `model-design-list`）。[MUST] |
| **PROF-FE-317** | 禁止在 `core/router/modules/` 写路由。[MUST] |
| **PROF-FE-318** | 路由权限码在 `meta.permission` 中声明。[MUST] |

---

## 8 页面命名铁律

每个项目应定义统一的页面后缀，用于区分不同类型的页面。

| 页面类型 | 强制后缀 | 文件名示例 | 组件名 |
|---------|---------|-----------|--------|
| 列表页 | `List` | `ModelList.vue` | `ModelList` |
| 详情页 | `Detail` | `ModelDetail.vue` | `ModelDetail` |
| 编辑页 | `Editor` | `ModelEditor.vue` | `ModelEditor` |
| 主页面 | `Index` | `ModelVersionIndex.vue` | `ModelVersionIndex` |
| 特殊业务页 | 业务名（需评审） | `ModelDesigner.vue` | `ModelDesigner` |

```
✅ 正确                          ❌ 错误
─────────────────────────────────────────────────────
ModelList.vue                    ModelTable.vue / ModelGrid.vue
ModelDetail.vue                  ModelView.vue / ModelInfo.vue
ModelEditor.vue                  ModelForm.vue / ModelAdd.vue / ModelEdit.vue
ModelVersionIndex.vue            ModelHome.vue / ModelMain.vue
```

**AI 编码校验**：
- 收到"加一个 X 的列表页"任务 → 直接生成 `XList.vue` + `XDetail.vue` + `XEditor.vue` 三件套
- 收到"加一个 X 的管理主页"任务 → 生成 `XIndex.vue`
- 收到"加一个 X 的设计器/画布"任务 → 生成 `X{业务名}.vue`

---

## 9 跨模块引用（公开面）

| 规则 | 说明 |
|------|------|
| **PROF-FE-319** | 跨模块引用必须走 `modules/{m}/index.ts` 公开面，禁止深路径引用（如 `@/modules/*/components/*`）。[MUST] |
| **PROF-FE-320** | 禁止循环依赖。[MUST] |
| **PROF-FE-321** | Barrel exports MUST NOT 导致循环依赖。[MUST] |
| **PROF-FE-322** | 公开面（`index.ts`）中仅导出本模块对外提供的 API/类型/组件。[SHOULD] |

---

## 10 与 L0 通用规范的关系

| L0 规范 | 与本文件的关系 |
|---------|---------------|
| `universal/api-design.md` | API 路径前缀 `/api/v1/{module}/{entity}` 由本文件统一 |
| `universal/naming-conventions.md` | 文件命名（PascalCase/camelCase）由 L0 提供，本文件细化业务模块的页面后缀 |
| `universal/security-baseline.md` | 权限码格式由 L0 提供安全基线，本文件落地面到路由 `meta.permission` |

---

*本文件规则适用于 Vue 3 前端项目。Vue 3 特有的 script setup、状态管理细节见 `vue3/` 子目录。*
