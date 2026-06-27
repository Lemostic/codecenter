---
description: 项目架构（下）— 页面命名铁律（List/Detail/Editor/Index）、路由规范、状态管理分层、业务模块文件定位决策树、Composable 签名参考。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1 页面命名铁律

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

| 维度 | 规则 | 示例 |
|------|------|------|
| 文件名 | PascalCase + 后缀 | `ModelList.vue` |
| 组件名（defineOptions） | 与文件名一致 | `ModelList` |
| 路由名 | kebab-case | `model-list` |
| 路由 path | kebab-case | `/model/list` |
| API 函数中的实体名 | PascalCase | `listModel` |

**AI 编码校验**：
- 收到"加一个 X 的列表页"任务 → 直接生成 `XList.vue` + `XDetail.vue` + `XEditor.vue` 三件套
- 收到"加一个 X 的管理主页"任务 → 生成 `XIndex.vue`
- 收到"加一个 X 的设计器/画布"任务 → 生成 `X{业务名}.vue`

---

## 2 路由规范补充

- ✅ 路由通过 `@mdm/core/router` 的 `createAppRouter()` 工厂创建，各模块声明 `routes.ts`，核心只做合并
- ✅ 每个模块必须导出 `routes.ts`（即使是空数组）
- ✅ 路由 name 格式：`{moduleName}-{type}`（如 `model-design-list`）
- ✅ 路由 path 用相对路径（不要重复 `MODULE_NAME`）
- ✅ 权限码格式：`{moduleName}:{action}`（如 `model-design:read`）

---

## 3 状态管理补充

**Store 分层**：

| 层级 | 位置 | 命名 | 示例 |
|------|------|------|------|
| 全局应用 | `@mdm/common/stores/` | `useAppStore` | 主题、布局、国际化、sidebarCollapsed |
| 业务全局 | `@mdm/common/stores/business/` | `useDictStore` | 数据字典 |
| 模块私有 | `modules/{m}/stores/` | `use{Entity}Store` | 业务实体状态 |

- ❌ 禁止 store 内直接操作 DOM
- ❌ 禁止在 state 中存组件实例
- ❌ 禁止跨模块直接调用对方的 store（应通过公开面 + 事件总线）
- ✅ Store ID 格式：`{module-name}-{entity}`（保证全局唯一）
- ✅ 异步方法必须用 `async/await`

---

## 4 业务模块文件定位决策树

### 新增页面
```
是独立业务页（带路由）？
├─ 是 → 是数据列表/详情/编辑/主页？
│       ├─ 是 → views/{Entity}{List|Detail|Editor|Index}.vue
│       └─ 否（设计器/画布/特殊业务）→ views/{Entity}{业务名}.vue
└─ 否（嵌入子组件）→ components/{ComponentName}.vue
```

### 新增组件
```
被几个模块使用？
├─ ≥2 个模块 → @mdm/common/components/{category}/{ComponentName}.vue
├─ 仅本模块 ≥2 页面 → modules/{m}/components/{ComponentName}.vue
└─ 只用 1 次 → 内联到使用它的 .vue 文件
```

### 新增 API
```
→ modules/{m}/api/{entity}.ts
  路径前缀：/api/v1/{module}/{entity}
  必须导出：list{Entity} / get{Entity} / create{Entity} / update{Entity} / delete{Entity}
```

### 新增 Store
```
状态范围？
├─ 全局共享 → @mdm/common/stores/use{Name}Store.ts
├─ 跨模块共享 → @mdm/common/stores/business/{name}Store.ts
└─ 模块私有 → modules/{m}/stores/use{Name}Store.ts
```

### 新增类型
```
类型使用范围？
├─ 跨模块基座 → @mdm/core/types/{name}.ts（@mdm/common/types 重新导出）
├─ 模块业务实体 → modules/{m}/types/{entity}.ts（5 件套全导出）
└─ 单文件内部使用 → 内联，不单独导出
```

---

## 5 Composable 签名参考

AI 生成业务代码时须按以下签名调用 composable，禁止自行编造参数或返回值。

### useCrudList

```ts
import { useCrudList } from '@mdm/common/composables/useCrudList';

const { data, total, loading, pageNum, pageSize, filters, fetchData, handleDelete, handlePageChange, handleSearch, handleReset } = useCrudList<Entity>({
  listApi: (params) => listEntity(params),
  deleteApi: (id) => deleteEntity(id),   // 可选
  defaultPageSize: 20,                    // 可选
});
```

### useImportExport

```ts
import { useImportExport } from '@mdm/common/composables/useImportExport';

const { importLoading, exportLoading, handleImport, handleExport, queryTaskStatus } = useImportExport({
  importApi: (file) => importEntity(file),
  exportApi: (params) => exportEntity(params),
  taskQueryApi: (id) => getImportTask(id), // 可选
});
```

### useFormValidation

```ts
import { useFormValidation } from '@mdm/common/composables/useFormValidation';

const { validate, resetFields, rules } = useFormValidation();
// rules.required('请输入名称')  → 返回 FormRule
// rules.maxLength(50)           → 返回 FormRule
// rules.pattern(/^[a-z]+$/, '仅允许小写字母') → 返回 FormRule
```
