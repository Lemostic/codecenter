# 填报设计功能详细设计

> 日期：2026-06-22
> 模块：model-design / form-design
> 状态：草稿

## 1. 功能概述

填报设计用于灵活定义数据填报表单，包括：
- 属性分组管理
- 属性样式配置
- 显隐条件配置
- 布局样式配置
- 表单预览

## 2. 整体架构

**三列布局**（参考 Figma node 441:68345）：

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│  模型标题栏：供应商_20583_文雪颖                                    [表单预览]          │
├─────────────────────────────────────────────────────────────────────────────────────┤
│                                                                                     │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  ┌─────────────────────┐│
│  │  复合模型（1+6）  │  │  提示消息                          │  │ 属性样式 显隐配置 布局样式 ││
│  │  ├─ 供应商_xxx  │  │  输入方式根据属性配置显示不同选项    │  │ ┌─────────────────┐││
│  │  ├─ 模型名称1   │  │  ...                              │  │ │                 │││
│  │  ├─ 模型名称2   │  ├──────────────────────────────────┤  │ │ 请先选择字段    │││
│  │  ├─ 模型名称3   │  │  新建字段分组  导入分组及属性样式   │  │ │                 │││
│  │  └─ ...        │  ├──────────────────────────────────┤  │ │                 │││
│  │                 │  │  基本信息                    [▼] │  │ │                 │││
│  │                 │  │  ├─ 机构地址      [编辑][删除] │  │ └─────────────────┘││
│  │                 │  │  ├─ 机构名称      [编辑][删除] │  │                     ││
│  │                 │  ├──────────────────────────────────┤  │ ┌─────────────────┐││
│  │                 │  │  附件信息                    [▼] │  │ │ 保存            │││
│  │                 │  │  ├─ ...                       │  │ └─────────────────┘││
│  │                 │  ├──────────────────────────────────┤  │                     ││
│  │                 │  │  默认分组                    [▼] │  │                     ││
│  │                 │  │  ├─ ...                       │  │                     ││
│  └─────────────────┘  └──────────────────────────────────┘  └─────────────────────┘│
│                                                                                     │
├─────────────────────────────────────────────────────────────────────────────────────┤
│  [一列] [两列] [三列] [四列]                                [保存配置]              │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

**说明**：
- **第一列（字段树）**：树形结构展示复合模型的字段层级，支持展开/折叠
- **第二列（表单预览）**：实时预览表单效果，分组可折叠，支持拖拽排序
- **第三列（属性配置）**：3个Tab（属性样式/显隐配置/布局样式），选中字段后显示配置
- **底部工具栏**：布局列数切换 + 保存按钮

## 3. 组件清单

| 组件 | 类型 | 说明 |
|------|------|------|
| `FormDesignPanel` | 功能面板 | 主容器，三列布局，状态管理 |
| `FieldTreePanel` | 功能组件 | 左侧字段树（复合模型层级结构） |
| `FormPreviewPanel` | 功能组件 | 中间表单预览，拖拽排序，分组折叠 |
| `AttributeConfigPanel` | 功能组件 | 右侧属性配置，3个Tab切换 |
| `LayoutSwitcher` | 工具组件 | 底部布局列数切换（1/2/3/4列） |

**属性配置 Tab 说明**：
| Tab | 说明 |
|-----|------|
| 属性样式 | 控件类型、占位提示、是否只读、是否必填 |
| 显隐配置 | IF 表达式控制字段显隐 |
| 布局样式 | 表单列数（1/2/3/4列） |

## 4. 详细设计

### 4.1 FormDesignPanel

**职责**：状态管理、API 调用、Schema 生成

**Props**：
```typescript
interface Props {
  modelId: string;
  modelStatus: 'draft' | 'published';
  modelVersion: number;
}
```

**状态**：
```typescript
interface FormDesignState {
  // 布局配置
  layoutColumns: 1 | 2 | 3 | 4;
  
  // 分组列表
  groups: FormGroup[];
  
  // 字段样式 Map（key: attributeId）
  fieldStyles: Record<string, FieldStyle>;
  
  // 显隐条件列表
  visibilityConditions: VisibilityCondition[];
  
  // 是否预览模式
  previewMode: boolean;
  
  // 加载状态
  loading: boolean;
  saving: boolean;
}
```

**Schema 生成逻辑**：
1. 遍历所有属性，按分组归类
2. 应用字段样式配置
3. 应用显隐条件
4. 生成 Formily Compatible Schema

### 4.2 FieldTreePanel（第一列）

**职责**：展示复合模型的字段树形结构

**数据结构**：
- 复合模型 = 主模型 + N 个子模型
- 树形结构：主模型（根） + 各子模型（子节点）
- 每个节点显示模型名称和字段数量

**交互**：
- 点击节点：选中该模型，右侧预览区显示该模型字段
- 支持展开/折叠子模型
- 搜索框：过滤节点名称

**与分组的区别**：
- FieldTreePanel 展示的是**模型层级结构**（复合模型特有）
- 分组（Group）是**字段分类**，与模型层级独立

### 4.3 FieldStylePanel

**职责**：字段样式配置

**Props**：
```typescript
interface Props {
  fieldStyles: Record<string, FieldStyle>;
  fields: { id: string; name: string; label: string; dataType: string }[];
}
```

**Emits**：
- `update:fieldStyles`
- `change`

**交互**：
- 左侧：字段列表（按分组显示，选中高亮）
- 右侧：选中字段的样式配置面板

**配置项**：
| 配置项 | 控件类型 | 说明 |
|--------|---------|------|
| 控件类型 | select | text/number/date/select/textarea/file 等 |
| 占位提示 | input | placeholder 文本 |
| 是否只读 | switch | - |
| 是否必填 | switch | 影响表单校验 |
| 占位列数 | select | 1/2/3/4 列 |
| 是否整行 | switch | 占满整行 |

### 4.4 AttributeConfigPanel（第三列）

**职责**：选中字段的属性配置（样式/显隐/布局）

**Tab 1 - 属性样式**：
| 配置项 | 控件类型 | 说明 |
|--------|---------|------|
| 控件类型 | Select | text/number/date/select/textarea/file 等 |
| 占位提示 | Input | placeholder 文本 |
| 是否只读 | Switch | - |
| 是否必填 | Switch | 影响表单校验 |

**Tab 2 - 显隐配置**：

布局：
```
┌─────────────────────────────────────────┐
│  添加显隐规则                            │
├─────┬──────────────────┬────────┬───────┤
│ 序号 │ 条件            │ 目标字段 │ 操作  │
├─────┼──────────────────┼────────┼───────┤
│  1  │ IF(GYS.JGBH=...) │ 机构名称 │ ✎ 🗑 │
└─────┴──────────────────┴────────┴───────┘

┌─────────────────────────────────────────┐
│ 条件配置                                  │
│ ┌──────────┐ ┌───┐ ┌─────┐            │
│ │ 字段选择 ▼│ │ = │ │ 值   │            │
│ └──────────┘ └───┘ └─────┘            │
│ [AND ▼] 目标字段: [选择 ▼]              │
│                      [取消] [确定]       │
└─────────────────────────────────────────┘
                              [保存]
```

交互：
- 点击"添加显隐规则"：显示条件配置表单
- 表格显示已有规则，支持编辑/删除
- 条件 = 字段 + 运算符 + 值
- 支持 AND/OR 逻辑组合
- 目标字段：被控制的字段

**Tab 3 - 布局样式**：

```
┌─────────────────────────────────────────┐
│ 布局样式                                  │
├─────────────────────────────────────────┤
│ 说明：在主数据查看/维护页面，将按以下布局    │
│ 样式进行渲染                              │
│                                          │
│ 表单布局                                  │
│ ○ 单列  ○ 双列  ● 三列  ○ 四列          │
│                                          │
│ 字段布局                                  │
│ ● 左右布局  ○ 上下布局                    │
│                                          │
│                        [保存]            │
└─────────────────────────────────────────┘
```

说明：
- 表单布局：表单整体列数（1/2/3/4列）
- 字段布局：单个字段内 label 和 input 的排布方式

**消息提示框**：
显示输入方式配置说明：
> 在主数据查看/维护页面，将按属性填报设计渲染页面
> 输入方式根据模型属性配置的内容不同，存在不同的可选项和默认值
> 输入方式为单选下拉框时最多显示1000条，建议采用"弹窗页面选择"
> 输入方式为单选按钮时，选项值最多仅显示6个

### 4.5 FormDesignPreview

**职责**：封装 TpDynamicForm，支持显隐条件

**Props**：
```typescript
interface Props {
  schema: ISchema;
  modelValue: Record<string, any>;
  visibilityConditions: VisibilityCondition[];
}
```

**逻辑**：
1. 接收 Formily Schema
2. 根据 visibilityConditions 动态注入 `x-display` 表达式
3. 渲染 TpDynamicForm

### 4.6 LayoutSwitcher

**职责**：布局列数切换

**实现**：四个按钮 `[一列] [两列] [三列] [四列]`，当前选中态高亮

## 5. 数据结构

### 5.1 FormGroup

```typescript
interface FormGroup {
  id: string;
  name: string;
  sortOrder: number;
  attributeIds: string[];
  collapsible: boolean;
  defaultCollapsed: boolean;
}
```

### 5.2 FieldStyle

```typescript
interface FieldStyle {
  attributeId: string;
  controlType: ControlType;
  colSpan: 1 | 2 | 3 | 4;
  fullRow: boolean;
  placeholder?: string;
  readonly?: boolean;
  required?: boolean;
}
```

### 5.3 VisibilityCondition

```typescript
interface VisibilityCondition {
  id: string;
  targetAttributeId: string; // 被控制的字段
  logic: 'and' | 'or';
  conditions: {
    sourceAttributeId: string; // 源字段
    operator: ConditionOperator;
    value: string;
  }[];
  action: 'show' | 'hide';
}
```

### 5.4 FormDesignSchema（内部使用）

```typescript
interface FormDesignSchema {
  layoutColumns: 1 | 2 | 3 | 4;
  groups: FormGroup[];
  fieldStyles: Record<string, FieldStyle>;
  visibilityConditions: VisibilityCondition[];
}
```

## 6. API 交互

| 操作 | API | 时机 |
|------|-----|------|
| 加载配置 | `getFormDesignByModel(modelId, version)` | 组件 mounted |
| 保存配置 | `createFormDesign` / `updateFormDesign` | 用户点击保存 |
| 表单预览 | 无（纯前端） | 切换预览模式 |

## 7. 状态管理

在 `FormDesignPanel` 内部使用 `reactive/ref` 管理状态，暂不引入独立 store。

```typescript
const state = reactive({
  layoutColumns: 2,
  groups: [],
  fieldStyles: {},
  visibilityConditions: [],
  previewMode: false,
  loading: false,
  saving: false,
});
```

## 8. Schema 生成算法

```
输入：groups, fieldStyles, visibilityConditions, fields
输出：Formily ISchema

1. 遍历 groups（按 sortOrder）
   a. 创建 GroupSection schema（用于折叠面板标题）
   b. 遍历 group.attributeIds
      - 查找 fieldStyles[attributeId]
      - 应用 controlType、colSpan、placeholder 等
      - 注入 x-display 表达式（基于 visibilityConditions）
   c. 将字段 schema 加入 GroupSection
2. 返回 ObjectSchema
```

## 9. 文件结构

```
src/modules/model-design/components/form-design/
├── FormDesignPanel.vue         # 主容器（已有占位，需重写）
├── FieldTreePanel.vue          # 字段树（新建）
├── FormPreviewPanel.vue        # 表单预览（新建）
├── AttributeConfigPanel.vue    # 属性配置面板（新建）
├── StyleConfigTab.vue         # 属性样式Tab（新建）
├── VisibilityConfigTab.vue    # 显隐配置Tab（新建）
└── LayoutConfigTab.vue        # 布局样式Tab（新建）
```

## 10. 实现顺序

1. **FormDesignPanel** - 主容器、状态管理、API 调用
2. **LayoutSwitcher** - 布局切换（简单）
3. **GroupList** - 分组管理（核心功能）
4. **FieldStylePanel** - 样式配置（依赖字段数据）
5. **VisibilityConfigPanel** - 显隐条件（依赖 ExpressionBuilder）
6. **FormDesignPreview** - 表单预览（最终验证）
7. **集成测试** - 端到端测试

## 11. 依赖项

- `TpDynamicForm` - 已存在于 `@mdm/common`
- `ExpressionBuilder` - 已存在于 `@mdm/common`
- `DraggableList` - 已存在于 `@mdm/common`
- `Formily` - 已存在于项目

## 12. Mock 数据

在 `apps/mdm-model/mock/form-design.ts` 中添加：

```typescript
export const mockFormDesign = {
  layoutColumns: 2,
  groups: [
    { id: 'g1', name: '基本信息', sortOrder: 1, attributeIds: ['a1', 'a2'], collapsible: true, defaultCollapsed: false },
    { id: 'g2', name: '扩展信息', sortOrder: 2, attributeIds: ['a3', 'a4'], collapsible: true, defaultCollapsed: true },
  ],
  fieldStyles: {
    a1: { attributeId: 'a1', controlType: 'text', colSpan: 2, fullRow: false, placeholder: '请输入' },
    a2: { attributeId: 'a2', controlType: 'date', colSpan: 1, fullRow: false },
  },
  visibilityConditions: [],
};
```
