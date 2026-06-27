description: Tp-* 11 类封装组件 API 详表、Element Plus 必填属性、按钮图标规则、TpTreeLazy 阈值与 API。
---

# 组件使用指南（Tp-*）

> Element Plus 必填属性、按钮类型规则、图标引入规则等通用部分继承 `profiles/frontend/vue/vue3/ui-element-plus.md`，本文件仅规定 Tp-* 封装组件的具体 API。

## 1 Tp-* 封装组件一览（11 类）

| 场景 | 封装组件 | 替代的 Element Plus 底层 |
|------|---------|----------------------|
| 列表/表格 | `TpTable` | `el-table` |
| 分页器 | `TpPagination` | `el-pagination` |
| 操作确认 | `TpConfirm` | `el-popconfirm` / `ElMessageBox` |
| 全局消息 | `TpMessage` | `ElMessage` / `ElNotification` |
| 空状态 | `TpEmpty` | `el-empty` |
| 详情分组 | `TpSectionTitle` | `el-divider` + `el-text` |
| 主页面外壳 | `TpPageFrame` | 手写 `bg-white p-4 rounded-lg` |
| 页头 | `TpPageHeader` | `el-page-header` / 手写标题 div |
| 树形控件 | `TpTree` / `TpTreeLazy` | `el-tree` |
| 左右分栏布局 | `TpLeftTreeLayout` | 手写 flex 分栏 |
| 卡片视图容器 | `TpCardList` | 手写 `<div class="grid">` |

## 2 TpTable

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-001** | 节点 MUST 带 `class="flex-1 min-h-0"`（禁止 `h-full`）。 |
| **MDF-FE-CMP-002** | 通过 `:data` 传入数组；内置 `TpPagination`，禁止再次使用 `el-pagination`。 |
| **MDF-FE-CMP-003** | MUST 提供 `#empty` 槽位（使用 `TpEmpty`）。 |
| **MDF-FE-CMP-004** | 列表渲染 MUST `:key`，禁止使用 index。 |
| **MDF-FE-CMP-005** | 长字段 MUST `show-overflow-tooltip`。 |
| **MDF-FE-CMP-006** | 操作列 MUST `link` 类型 + `fixed="right"`。 |
| **MDF-FE-CMP-007** | 状态列用 `el-tag` + `:type="statusTagType(row.status)"` 映射。 |
| **MDF-FE-CMP-008** | MUST 显式 import：`import TpTable from '@mdm/common/components/data/TpTable.vue'`。 |

```vue
<TpTable
  class="flex-1 min-h-0"
  :data="tableData"
  :loading="loading"
  :total="total"
  v-model:pageNum="query.pageNum"
  v-model:pageSize="query.pageSize"
  :columns="columns"
  @page-change="handlePageChange"
  @size-change="handlePageChange"
>
  <template #empty>
    <TpEmpty description="暂无数据" />
  </template>
</TpTable>
```

## 3 TpPagination

完整视觉规格见 `references/design-tokens.md §1`。关键 props：

| prop | 类型 | 默认 | 说明 |
|------|------|-----|------|
| `pageNum` | `number` | `1` | 当前页（v-model） |
| `pageSize` | `number` | `20` | 每页条数（v-model） |
| `total` | `number` | `0` | 总数 |
| `pageSizes` | `number[]` | `[10,20,50,100]` | 每页条数选项 |
| `layout` | `string` | `'total, sizes, prev, pager, next, jumper'` | Element Plus 分页布局 |

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-009** | MUST 显式 import：`import TpPagination from '@mdm/common/components/data/TpPagination.vue'`。 |
| **MDF-FE-CMP-010** | 禁止修改圆角为 4px、禁止修改上一/下一页背景色（破坏视觉规范）。 |

## 4 TpConfirm

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-011** | 重要操作（删除/批量/发布） MUST 使用。 |
| **MDF-FE-CMP-012** | MUST NOT 表格行内轻量确认用 `el-popconfirm`。 |
| **MDF-FE-CMP-013** | MUST NOT 直接使用 `ElMessageBox`。 |

```typescript
import { TpConfirm } from '@mdm/common/components/feedback/TpConfirm';

const handleDelete = async (id: ID) => {
  await TpConfirm({
    title: '确认删除',
    message: `确定要删除该模型吗？该操作不可恢复。`,
    type: 'warning',
  });
  await deleteModel(id);
  TpMessage.success('删除成功');
  await fetchData();
};
```

## 5 TpMessage

```typescript
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

TpMessage.success('保存成功');
TpMessage.error('加载失败');
TpMessage.warning('该操作不可恢复');
TpMessage.info('请稍候...');
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-014** | 成功/失败/警告提示 MUST 使用 `TpMessage`。 |
| **MDF-FE-CMP-015** | MUST NOT 直接使用 `ElMessage` / `ElNotification`。 |
| **MDF-FE-CMP-016** | 异步操作失败时 MUST 调用 `TpMessage.error`，禁止只 `console.log`。 |

## 6 TpEmpty

| 状态 | state 值 |
|------|---------|
| 正常无数据 | `state="no-data"` |
| 无权限 | `state="no-permission"` |
| 加载失败 | `state="load-failed"` + `@action="loadData"` 重试 |

```vue
<TpEmpty state="load-failed" description="加载失败" @action="loadData" />
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-017** | "无数据/无权限/加载失败"三态 MUST 使用 `TpEmpty`。 |
| **MDF-FE-CMP-018** | MUST 必传 `state` 区分场景。 |
| **MDF-FE-CMP-019** | MUST NOT 直接使用 `el-empty`。 |

## 7 TpSectionTitle

```vue
<TpSectionTitle title="基本信息" />
<TpSectionTitle title="高级配置" :extra="true" />
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-020** | 详情页分组 MUST 使用。 |
| **MDF-FE-CMP-021** | MUST NOT 详情分组用 `el-divider` + `el-text` 手写。 |

## 8 TpPageFrame

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-022** | 所有 List/Detail/Editor 顶层路由页面的根容器 MUST 使用。 |
| **MDF-FE-CMP-023** | 零 props。 |
| **MDF-FE-CMP-024** | MUST 显式 import：`import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue'`。 |
| **MDF-FE-CMP-025** | 内部嵌套 `TpLeftTreeLayout` 时 MUST 给后者加 `class="flex-1 min-h-0"`。 |
| **MDF-FE-CMP-026** | MUST NOT 嵌入 `el-tab-pane` / `el-dialog` / `el-drawer` 内的子内容使用。 |
| **MDF-FE-CMP-027** | MUST NOT 手写 `bg-white p-4 rounded-lg` 等价外壳。 |
| **MDF-FE-CMP-028** | MUST NOT 子内容在外壳同向上再加 padding（避免双层间距）。 |

## 9 TpPageHeader

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-029** | 编辑页和详情页的页头 MUST 使用。 |
| **MDF-FE-CMP-030** | `title` prop 必填。 |
| **MDF-FE-CMP-031** | `backTo` prop 指定返回路由（通常为列表页路由 name），不传则 `router.back()`。 |
| **MDF-FE-CMP-032** | 右侧操作按钮用 `#actions` 插槽。 |
| **MDF-FE-CMP-033** | MUST NOT 手写页头标题 div。 |
| **MDF-FE-CMP-034** | MUST NOT 列表页使用（列表页用全局布局的标题）。 |

```vue
<TpPageHeader title="编辑模型" :back-to="{ name: 'model-design-list' }">
  <template #actions>
    <el-button @click="handleCancel">取消</el-button>
    <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
  </template>
</TpPageHeader>
```

## 10 TpTree / TpTreeLazy

| 节点数 | 组件 | 加载方式 |
|--------|------|---------|
| ≤ 100 | `TpTree` | 同步加载 `:data="treeData"` |
| > 100 | `TpTreeLazy` | 懒加载 `:load="loadTreeNode"` |

通用 props：

| prop | 类型 | 默认 | 说明 |
|------|------|-----|------|
| `fieldMap` | `{ label, children?, isLeaf? }` | `{ label: 'name', children: 'children', isLeaf: 'isLeaf' }` | 字段映射 |
| `nodeKey` | `string` | `'id'` | 节点唯一键字段 |
| `searchable` | `boolean` | `true` | 是否启用搜索框 |
| `highlightCurrent` | `boolean` | `true` | 高亮当前选中节点 |
| `defaultExpandAll` | `boolean` | `false` | 默认展开全部 |

`TpTreeLazy` 额外 props/方法：

| 成员 | 说明 |
|------|------|
| `:load(node, resolve)` | 懒加载函数，返回 `resolve(children)` |
| `setSearch(keyword)` | 设置搜索关键字 |
| `clearSearch()` | 清空搜索 |
| `setSelected(id)` | 编程式选中节点 |
| `getSelected()` | 获取当前选中节点 |

```vue
<TpTreeLazy
  :load="loadTreeNode"
  :field-map="{ label: 'name', isLeaf: 'isLeaf' }"
  node-key="id"
  searchable
  highlight-current
  search-placeholder="搜索分类"
  @node-click="handleNodeClick"
/>
```

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-035** | 左侧分类树 / 选择树 MUST 使用 `TpTree` / `TpTreeLazy`。 |
| **MDF-FE-CMP-036** | MUST NOT 在业务组件中直接 `import { ElTree }` 绕过封装。 |
| **MDF-FE-CMP-037** | `TpTreeLazy` MUST 节点 > 100 时使用。 |

## 11 TpLeftTreeLayout

| prop | 类型 | 默认 | 说明 |
|------|------|-----|------|
| `defaultWidth` | `number` | `300` | 推荐 `280` |
| `minWidth` | `number` | `200` | 最小宽度 |
| `maxExtraWidth` | `number` | `200` | 最大可增加宽度 |
| `storageKey` | `string` | — | localStorage key（如 `model-tree-width`）持久化用户调整 |

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-038** | 所有"左树右表" / "左分类右列表"页面 MUST 使用。 |
| **MDF-FE-CMP-039** | MUST 配合 `TpPageFrame` 使用：`TpPageFrame` 在外层，`TpLeftTreeLayout` 在内层并加 `class="flex-1 min-h-0"`。 |
| **MDF-FE-CMP-040** | 表格工具栏 MUST 放在 `#right` 插槽内（紧贴 `TpTable` 上方）。 |
| **MDF-FE-CMP-041** | MUST NOT 工具栏放在 `TpLeftTreeLayout` 外侧。 |
| **MDF-FE-CMP-042** | MUST NOT 子内容在 `TpLeftTreeLayout` 同向上再加 padding。 |

详见 `references/pages.md §3`。

## 12 TpCardList

| prop | 类型 | 默认 | 说明 |
|------|------|-----|------|
| `data` | `T[]` | `[]` | 数据数组 |
| `gridMinWidth` | `number` | `280` | 网格项最小宽度（与 ModelCard 匹配） |
| `grid` | `number` | `4` | 网格间距档位 2/3/4/6/8 |
| `pageNum` / `pageSize` / `total` | `number` | — | 分页（v-model） |

| 规则 | 说明 |
|------|------|
| **MDF-FE-CMP-043** | MUST 显式 import：`import TpCardList from '@mdm/common/components/data/TpCardList.vue'`。 |
| **MDF-FE-CMP-044** | MUST 节点带 `class="flex-1 min-h-0"`。 |
| **MDF-FE-CMP-045** | MUST 使用作用域插槽 `#item="{ item }"`。 |
| **MDF-FE-CMP-046** | MUST NOT 内部用 `el-pagination` 替换 `TpPagination`。 |

详见 `references/pages.md §5`。