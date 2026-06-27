| 字段 | 值 |
|------|-----|
| 版本 | 1.0 |
| 层级 | L2 |
| 引入条件 | `fingerprint.domains contains 'mdm-frontend'` 且 `fingerprint.profiles contains 'frontend-vue'` |
| 适用架构 | 主数据产品（MDM）前端工程，基于 Vue 3 + Element Plus + Tailwind 的 pnpm monorepo |
| 依赖规范 | `frontend-vue` 包（13 份规范）、`naming-conventions`、`api-design` |
| 互斥规范 | 无 |
| 规则编号前缀 | MDF-FE |

# MDM 前端领域规范

> 版本: 1.0 | 状态: 已激活 | 最后更新: 2026-06-25

---

## 一、定位与边界

本规范是 `frontend-vue` 通用规范的**领域扩展**，不重复 L1 已声明的规则（命名铁律、API 5 件套、类型 5 件套、i18n 4 段式、Element Plus 必填、按钮图标、p-3 间距等）。本规范只规定 **MDM 项目特有的工程约定**：

1. `Tp-*` 封装组件的具体 API 与使用细节
2. `@mdm/core` / `@mdm/common` / `apps/{app}/modules/{m}` 路径别名与目录约定
3. MDM 业务 Composable 签名（`useCrudList` / `useImportExport` / `useFormValidation`）
4. `statusTagType` 状态映射标准实现
5. `ModelCard` 主数据模型卡片的视觉与结构规格
6. `TpPagination` 视觉与结构规格
7. MDM 主数据 `BaseEntity` 字段集约定

如本规范与 `frontend-vue` 包出现冲突，**以 `frontend-vue` 为准**（SPI 仅扩展，不可覆盖）。

---

## 二、核心规则

### 规则 MDF-FE-001：Tp-* 封装组件 API 与使用约定

**优先级：MUST**

MDM 项目统一使用 `Tp-` 前缀的封装组件替代 `frontend-vue` 通用的 `{EncapsulatedXxx}` 占位符。两者场景与约束一一对应，API 是占位符的具体落地。

| 封装组件 | 对应占位符 | 关键 Props / 行为 |
|---------|-----------|----------------|
| `TpTable` | `{EncapsulatedTable}` | 节点 MUST 带 `class="flex-1 min-h-0"`；内置 `TpPagination` |
| `TpPagination` | `{EncapsulatedPagination}` | 圆角 2px、容器 48px 高、上一/下一页 `#F5F7FA` 背景、激活页 `1px solid #337BFF` 描边（详见 `references/design-tokens.md`） |
| `TpConfirm` | `{EncapsulatedConfirm}` | Promise 风格；重要操作 MUST 使用 |
| `TpMessage` | `{EncapsulatedMessage}` | `.success` / `.error` / `.warning` / `.info`；错误日志 MUST 带 `[方法名]` 前缀 |
| `TpEmpty` | `{EncapsulatedEmpty}` | `state="no-data"\|"no-permission"\|"load-failed"`；加载失败可 `@action="loadData"` 重试 |
| `TpSectionTitle` | `{EncapsulatedSectionTitle}` | 详情页分组 MUST 使用 |
| `TpPageFrame` | `{EncapsulatedPageFrame}` | 零 props；强制 import `import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue'` |
| `TpPageHeader` | `PageHeader` | 编辑页/详情页 MUST 使用；`title` 必填、`backTo` 指定返回路由 |
| `TpTree` / `TpTreeLazy` | `{EncapsulatedTree}` / `{EncapsulatedTreeLazy}` | `fieldMap` prop 统一映射；`TpTreeLazy` 节点 > 100 时使用；暴露 `setSearch(keyword)` / `clearSearch()` / `setSelected(id)` / `getSelected()` |
| `TpLeftTreeLayout` | `{EncapsulatedTreeLayout}` | `defaultWidth` 推荐 280、`minWidth=200`、`maxExtraWidth=200`、`storageKey` 持久化（如 `model-tree-width`） |
| `TpCardList` | `{EncapsulatedCardList}` | `grid-min-width` 默认 280px、grid 间距档位 2/3/4/6/8 默认 4；插槽 `#item` |

详见 `references/components.md` 与 `references/pages.md`。

---

### 规则 MDF-FE-002：MDM monorepo 路径别名

**优先级：MUST**

MDM 项目是 pnpm monorepo 工程，必须通过以下路径别名引用代码，禁止相对路径穿透：

| 别名 | 指向 | 用途 |
|------|------|------|
| `@mdm/core` | `packages/core/src` | 框架基础设施：HTTP、Auth、Router、i18n、Error |
| `@mdm/common` | `packages/common/src` | 跨应用复用：组件、Composables、工具、类型 |
| `@mdm/types` | `packages/types/src` | 跨应用类型 |
| `~{app}` 或 `@{app}` | `apps/{app}/src` | 单应用内部 |
| `@/` | `apps/{app}/src` | 单应用内相对根目录（兼容 vue 习惯） |

| 规则 | 说明 |
|------|------|
| **MDF-FE-003** | 应用内业务代码 MUST 放 `apps/{app}/modules/{m}` 下，禁止平铺到 `src/`。 |
| **MDF-FE-004** | `apps/{app}/modules/{m}/api/{entity}.ts` MUST 导出 5 个函数，使用 `import { http } from '@mdm/core/http'`。 |
| **MDF-FE-005** | 跨应用复用组件 MUST 放 `packages/common/src/components/{category}/`，导入用 `@mdm/common`。 |
| **MDF-FE-006** | 跨应用类型 MUST 放 `packages/types/src/`，导入用 `@mdm/types`。 |
| **MDF-FE-007** | 禁止使用 `../../../` 多层相对路径。 |

```typescript
// ✅ 正确
import { http } from '@mdm/core/http';
import TpTable from '@mdm/common/components/data/TpTable.vue';
import { useCrudList } from '@mdm/common/composables/useCrudList';
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';
import type { ModelEntity, ModelQuery } from '~model-design/modules/modelDesign/types/model';

// ❌ 错误
import { http } from '../../../../../../core/http';
import TpTable from '../../../components/data/TpTable.vue';
```

---

### 规则 MDF-FE-008：MDM 业务 Composable 签名

**优先级：MUST**

MDM 在 `@mdm/common/composables` 下提供三类业务级 Composable，业务模块 MUST 按以下签名调用，禁止重新实现。

#### useCrudList

```typescript
import { useCrudList } from '@mdm/common/composables/useCrudList';

const {
  data,            // ref<T[]>          列表数据
  total,           // ref<number>       总数
  loading,         // ref<boolean>      加载态
  pageNum,         // ref<number>       当前页
  pageSize,        // ref<number>       每页条数
  filters,         // ref<Record<string, unknown>>  筛选条件
  fetchData,       // () => Promise<void>          触发加载
  handleDelete,    // (id: ID) => Promise<void>    删除（含确认）
  handlePageChange,// (page: number) => void
  handleSearch,    // (filters: Record<string, unknown>) => void
  handleReset,     // () => void
} = useCrudList<Entity>({
  listApi: (params) => listEntity(params),
  deleteApi: (id) => deleteEntity(id),     // 可选
  defaultPageSize: 20,                       // 可选
});
```

#### useImportExport

```typescript
import { useImportExport } from '@mdm/common/composables/useImportExport';

const {
  importLoading,   // ref<boolean>
  exportLoading,   // ref<boolean>
  handleImport,    // (file: File) => Promise<void>
  handleExport,    // () => Promise<void>
  queryTaskStatus, // (taskId: string) => Promise<TaskStatus>
} = useImportExport({
  importApi: (file) => importEntity(file),
  exportApi: (params) => exportEntity(params),
  taskQueryApi: (id) => getImportTask(id),   // 可选
});
```

#### useFormValidation

```typescript
import { useFormValidation } from '@mdm/common/composables/useFormValidation';

const { validate, resetFields, rules } = useFormValidation();
// rules.required('请输入名称')           → FormRule
// rules.maxLength(50)                    → FormRule
// rules.pattern(/^[a-z]+$/, '仅允许小写') → FormRule
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-009** | 列表页 MUST 使用 `useCrudList`，禁止手写 `loading + pageNum + pageSize + fetchData` 四件套。 |
| **MDF-FE-010** | 导入/导出场景 MUST 使用 `useImportExport`，禁止裸调 API。 |
| **MDF-FE-011** | 表单校验 MUST 使用 `useFormValidation` 的 `rules`，禁止内联写 rules 数组。 |

---

### 规则 MDF-FE-012：statusTagType 状态映射标准实现

**优先级：MUST**

MDM 统一以下 6 种状态到 `el-tag` 的 `type` 映射，状态字段渲染 MUST 使用：

```typescript
// packages/common/src/constants/statusTagType.ts
export const statusTagType = (status: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
    enabled: 'success',
    disabled: 'info',
    error: 'danger',
  };
  return map[status] ?? 'info';
};
```

```vue
<template>
  <el-tag :type="statusTagType(row.status)" size="default">{{ row.status }}</el-tag>
</template>
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-013** | 状态字段 MUST 从 `@mdm/common/constants/statusTagType` 导入，禁止各模块本地复刻。 |
| **MDF-FE-014** | 新增业务状态（如 `pending`、`expired`）MUST 同步扩展该 map，并提交评审。 |

---

### 规则 MDF-FE-015：MDM BaseEntity 字段集

**优先级：MUST**

MDM 主数据业务实体 MUST 继承 `@mdm/common/types/base` 中的 `BaseEntity`，字段集固定如下：

```typescript
// packages/common/src/types/base.ts
export type ID = string;

export interface BaseEntity {
  id: ID;
  name: string;            // 主数据名称
  description?: string;    // 描述（可选）
  createdAt: string;       // ISO 8601
  updatedAt: string;
  createdBy?: ID;
  updatedBy?: ID;
}

export interface PaginationParams {
  pageNum: number;         // MDM 使用 pageNum（与前端 vue 标准 page 不同）
  pageSize: number;
}

export interface PaginatedData<T> {
  list: T[];               // MDM 后端字段为 list，非 rows
  total: number;
  pageNum: number;
  pageSize: number;
}
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-016** | 业务实体 MUST 继承 `BaseEntity`；`name` 是主数据必备字段，`description` 可选。 |
| **MDF-FE-017** | 分页参数 `PaginationParams` 使用 `pageNum`（非 `page`），分页响应字段为 `list`（非 `rows`）——MDM 与通用 `frontend-vue` 的差异在此固化。 |
| **MDF-FE-018** | DTO MUST NOT 包含 `id` / `createdAt` / `updatedAt` 等后端生成字段。 |

> 与通用 `frontend-vue/common/type-system.md` 的 `PageResult` / `rows` 字段差异说明：MDM 后端统一返回 `PaginatedData<T>`（`list` + `pageNum`），前端在 API 层直接透传使用。

---

## 三、扩展规则

### 规则 MDF-FE-101：TpTreeLazy 使用阈值

**优先级：SHOULD**

| 节点数 | 推荐组件 |
|--------|---------|
| ≤ 100 | `TpTree`（同步加载） |
| > 100 | `TpTreeLazy`（懒加载 + `:load`） |

```vue
<TpTreeLazy
  :load="loadTreeNode"
  :field-map="{ label: 'name', children: 'children', isLeaf: 'isLeaf' }"
  node-key="id"
  searchable
  highlight-current
  search-placeholder="搜索分类"
  @node-click="handleNodeClick"
/>
```

---

### 规则 MDF-FE-102：左树右表 `storageKey` 命名

**优先级：SHOULD**

`TpLeftTreeLayout` 的 `storageKey` MUST 用 `{module}-{tree-name}-width` 命名，保证全局唯一且与用户调整持久化作用域一致：

```vue
<TpLeftTreeLayout
  class="flex-1 min-h-0"
  :default-width="280"
  storage-key="model-design-category-tree-width"
>
```

---

### 规则 MDF-FE-103：ModelCard 模型卡片规格

**优先级：MUST**

`ModelCard` 是主数据模型列表的卡片视图标准组件，规格固定如下：

#### 容器

| 属性 | 值 |
|------|-----|
| border-radius | `4px`（非 `rounded-lg`） |
| border | `1px solid #dcdfe6` |
| background | `#ffffff` |
| cursor | `pointer` |
| 默认阴影 | `0px 1px 4px 0px rgba(0, 0, 0, 0.08)` |
| hover 阴影 | `0px 2px 8px 0px rgba(0, 0, 0, 0.12)` |
| 选中态 | `ring-2 ring-[var(--el-color-primary)]` |
| hover border-color | `var(--el-color-primary-light-3)` |
| 过渡 | `transition-all duration-200 ease-in-out` |

#### 头部

| 属性 | 值 |
|------|-----|
| 图标框尺寸 | 36×36px |
| 图标框 border-radius | `4px` |
| 图标框 background | `#337bff` |
| 图标尺寸 | 20px，白色 |
| 复选框位置 | `absolute top-4 right-4 z-10` |
| 标题 font-size | 14px |
| 标题 font-weight | 500 |
| 标题 color | `#3d4247` |
| 编码 font-size | 12px |
| 编码 color | `#666666` |
| 图标与文字间距 | `gap: 15px` |
| 头部 padding | `px-4 pt-4 pb-2` |

#### 信息行

| 属性 | 值 |
|------|-----|
| font-size | 13px |
| color | `#666666` |
| line-height | 30px |
| 布局 | 标签与值拼接为整行文本 |

#### 底部标签区

- 布局：`justify-between`
- 密级标签（可选）+ 版本/状态组合标签 + 授权按钮
- 版本左半：`border-radius: 3px 0 0 3px`、描边、`color: #333`
- 状态右半：`border-radius: 0 3px 3px 0`、`background: rgba(227,77,89,0.1)`、`color: #e34d59`

---

## 四、与 frontend-vue 的差异说明

| 项 | frontend-vue（L1） | spi-mdm-frontend（L2） |
|----|-------------------|----------------------|
| 封装组件前缀 | `{EncapsulatedXxx}` 占位符 | `Tp-` 前缀（具体实现） |
| 路径别名 | `@/` 指向 `src/` | `@mdm/core`、`@mdm/common`、`@mdm/types`、`~{app}` |
| 目录布局 | `src/{core,common,modules}` | `apps/{app}/src/modules` + `packages/{core,common,types}/src` |
| 分页字段 | `page` / `rows` | `pageNum` / `list`（MDM 后端约定） |
| `BaseEntity` | 6 字段（无 `name`） | 强制 `name` + 可选 `description` |
| 状态映射 | 仅给出模板 | 6 种状态标准 map，强制复用 |
| 业务 Composable | 不涉及 | `useCrudList` / `useImportExport` / `useFormValidation` 强制签名 |
| `ModelCard` 规格 | 不涉及 | 完整视觉与结构规格 |

---

## 五、验收标准

| 验收项 | 标准 |
|--------|------|
| 路径别名 | `@mdm/core`、`@mdm/common`、`@mdm/types` 在 tsconfig paths 中声明 |
| 封装组件引用 | 所有 11 类强制场景使用 `Tp-*`，无 `el-table`/`ElMessage`/`el-empty`/`el-popconfirm`/`el-pagination` 绕过 |
| Composable 使用 | 列表页必含 `useCrudList`，导入导出页必含 `useImportExport`，表单必含 `useFormValidation` |
| BaseEntity | 所有业务实体继承 `@mdm/common/types/base` 的 `BaseEntity` |
| 状态映射 | 所有状态字段渲染来自 `statusTagType` 常量 |
| ModelCard | 容器/头部/信息行/底部标签符合 MDF-FE-103 规格 |

---

## 六、变更记录

| 版本 | 日期 | 说明 |
|------|------|------|
| 1.0 | 2026-06-25 | 初版：从主数据前端 V2 来源文档提炼，剔除与 frontend-vue 重复内容 |

---

## 七、参考文献

- `references/getting-started.md`：技术栈基线、设计原则
- `references/api-development.md`：MDM 特有的 API/类型/i18n 落地说明
- `references/components.md`：Tp-* 组件 API 详表 + Element Plus 必填
- `references/pages.md`：TpPageFrame/TpLeftTreeLayout/TpPageHeader 骨架
- `references/design-tokens.md`：TpPagination + ModelCard 视觉规格
- `references/selfcheck.md`：MDM 特有检查项清单