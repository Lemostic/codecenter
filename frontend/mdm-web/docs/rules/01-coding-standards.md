---
description: Vue 3 + TypeScript + Element Plus 通用编码规范。涵盖组件 script setup 顺序、API 函数命名铁律、类型系统（基座类型 + 5件套）、国际化 Key 规范。
paths:
  - "**/*.vue"
  - "**/*.ts"
---

## 1 设计原则

| # | 原则 | 含义 |
|---|------|------|
| 1 | **显式优于隐式** | 所有规则必须显式写出，AI 按字面执行，禁止"猜测意图" |
| 2 | **唯一选择优于多选** | 同类问题只有一种标准做法（如命名、API 函数、目录结构） |
| 3 | **可预测优于聪明** | 命名/位置/模式必须可预测，禁止"灵感式"命名 |
| 4 | **类型即文档** | 用 TypeScript 类型表达意图，禁止用注释代替类型 |
| 5 | **模板可复制** | 每个场景必须提供可复制模板，AI 填空而非创作 |
| 6 | **决策可追溯** | 每个文件位置必须有决策依据 |
| 7 | **边界要硬** | 跨模块边界硬约束，AI 不得"顺手"越界 |

---

## 2 技术栈基线

> 具体版本号由项目配置定义。以下为推荐的最低版本基线。

| 技术 | 推荐最低版本 | 说明 |
|------|------------|------|
| Vue | 3.5+ | Composition API + `<script setup>` |
| TypeScript | 5.6+ | `strict: true`，禁止 `any` |
| Vite | 5.4+ | 构建工具 |
| Element Plus | 2.13+ | UI 组件库 |
| Tailwind CSS | 3.4+ | 布局/间距/颜色原子类 |
| Pinia | 2.2+ | 状态管理 |
| Vue Router | 4.4+ | 路由 |
| vue-i18n | 9.13+ | 国际化 |
| Axios | 1.7+ | HTTP 客户端 |
| Node.js | 20 LTS+ | 开发环境 |

- ❌ 禁止引入项目配置未声明的依赖
- ❌ 禁止使用 CommonJS 语法，必须 ES Module

---

## 3 组件 script setup 规范

### 3.1 块内顺序（10 步骤，必须按此顺序）

```vue
<script setup lang="ts">
// ========== 1. 外部 import（按 vue → 第三方 → 业务模块 顺序）==========
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import type { FormInstance, FormRules } from 'element-plus';
// ========== 2. defineOptions（组件名）==========
defineOptions({ name: 'ComponentName' });
// ========== 3. Props 定义（TypeScript 泛型）==========
const props = defineProps<{
  id: string;
  title?: string;
  data?: SomeType;
}>();
// ========== 4. Emits 定义 ==========
const emit = defineEmits<{
  (e: 'update:data', value: SomeType): void;
  (e: 'success'): void;
}>();
// ========== 5. 响应式数据（state）==========
const loading = ref(false);
const formData = ref({ name: '', description: '' });
// ========== 6. Computed 计算属性 ==========
const isValid = computed(() => formData.value.name.length > 0);
// ========== 7. 方法（camelCase）==========
const handleSubmit = async () => { /* ... */ };
// ========== 8. Watch 监听 ==========
watch(() => props.data, (newVal) => { /* ... */ }, { immediate: true });
// ========== 9. 生命周期 ==========
onMounted(() => { loadData(); });
// ========== 10. defineExpose（可选）==========
defineExpose({ loadData, validate });
</script>

<template>
  <!-- 模板内容 -->
</template>

<style scoped>
/* 样式内容 */
</style>
```

### 3.2 块顺序

**强制要求**：`<script setup>` → `<template>` → `<style scoped>`

- ❌ 禁止 `<template>` 放在 `<script>` 前面
- ❌ 禁止 `<style>` 放在 `<template>` 前面

### 3.3 defineProps

```typescript
// 基础用法
const props = defineProps<{ id: string; title?: string; data?: SomeType }>();
// 带默认值
const props = withDefaults(
  defineProps<{ visible: boolean; row?: SomeVO | null; size?: 'small' | 'default' | 'large' }>(),
  { visible: false, row: null, size: 'default' }
);
```

- ✅ 必须用 TypeScript 泛型（不是 runtime declaration）
- ❌ 禁止 `const props = defineProps({ id: String })`（运行时声明，丢失类型）
- ❌ 禁止 Props 超过 10 个（超出拆分组件）

### 3.4 defineEmits

```typescript
const emit = defineEmits<{
  (e: 'update:data', value: SomeType): void;
  (e: 'success'): void;
}>();
emit('update:data', newValue);
```

- ✅ 事件名必须用 camelCase（如 `update:data`、`delete-success`）

### 3.5 defineExpose

```typescript
defineExpose({ loadData, validate, reset });
```

- ✅ 暴露的方法必须有 JSDoc 注释
- ❌ 禁止暴露内部状态（`state.value` 等）

### 3.6 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 组件文件 | PascalCase | `UserCard.vue` |
| 组件名（defineOptions） | PascalCase，与文件名一致 | `UserCard` |
| 方法名 | camelCase | `handleSubmit`、`loadData` |
| 事件名 | camelCase | `update:data` |
| Props | camelCase | `userId`、`isLoading` |
| 响应式变量 | camelCase | `formData`、`tableData` |
| CSS 类名 | kebab-case | `project-card` |

---

## 4 API 函数规范

### 4.1 命名铁律

| ✅ 允许 | ❌ 禁止 |
|---------|---------|
| `listEntity` | `queryEntity`、`fetchEntities`、`getList`、`searchEntity` |
| `getEntity` | `findEntity`、`queryEntityById`、`getDetail` |
| `createEntity` | `addEntity`、`insertEntity`、`saveEntity` |
| `updateEntity` | `modifyEntity`、`editEntity`、`saveEntity` |
| `deleteEntity` | `removeEntity`、`delEntity`、`destroyEntity` |

### 4.2 铁律

- ❌ 禁止使用 `axios` 原生，必须用项目封装的 HTTP 实例（`import { http } from '@mdm/core/http'`）
- ❌ 禁止用 `get/post/put/del` 解构导入（统一使用 `http` 实例）
- ❌ 禁止函数名前缀加 `api.`（如 `api.listEntity`）
- ❌ 禁止返回 `any`，必须明确泛型
- ❌ 禁止省略 5 个函数中的任何一个（即使目前不用）
- ✅ 函数顺序固定：list → get → create → update → delete
- ✅ 路径前缀统一：`/api/v1/{module}/{entity}`
- ✅ Token 注入、业务错误码解包、401 自动跳转登录 均由 `@mdm/core/http` 拦截器处理，业务代码无需关心

### 4.3 错误处理标准模式

```typescript
const loadData = async () => {
  loading.value = true;
  try {
    const res = await listEntity(query.value);
    tableData.value = res.data.list;
    total.value = res.data.total;
  } catch (error) {
    TpMessage.error('加载失败');
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};
```

- ❌ 禁止省略 `catch`（异步错误必须处理）
- ❌ 禁止只 `console.log`（必须用统一消息组件提示用户）
- ✅ `finally` 中必须重置 `loading`
- ✅ 错误日志必须带 `console.error('[方法名]', error)` 前缀
- ✅ 业务码错误由拦截器统一处理（无需在业务代码中手动判断 `res.success`）

---

## 5 类型系统规范

### 5.1 基座类型模式

每个项目应定义统一的基座类型，供所有业务实体继承：

```typescript
// @mdm/core/types/base.ts
export type ID = string;
export interface BaseEntity {
  id: ID;
  name: string;
  description?: string;
  createdAt: string;       // ISO 8601
  updatedAt: string;
  createdBy?: string;
  updatedBy?: string;
}
export interface PaginationParams { pageNum: number; pageSize: number; }
export interface PaginatedData<T> { list: T[]; total: number; pageNum: number; pageSize: number; }
```

```typescript
// @mdm/core/types/api.ts
export interface ApiResponse<T = unknown> {
  success: boolean;
  data: T;
  message?: string;
  code?: string;
}
```

### 5.2 业务实体 5 件套（强约束）

每个业务实体必须导出以下 5 个类型，不得省略任何一个：

```typescript
// modules/{m}/types/{entity}.ts
// 5 个类型必须全导出，缺一不可
import type { BaseEntity, ID, PaginationParams } from '@mdm/common/types/base';

// 1. 数据库实体（与表结构对齐）
export interface {Entity}Entity extends BaseEntity { name: string; /* ... */ }
// 2. 视图对象（前端展示用）
export interface {Entity}VO extends {Entity}Entity { displayName: string; /* ... */ }
// 3. 创建参数（不包含 id/审计字段）
export interface {Entity}CreateDTO { name: string; /* 必填业务字段 */ }
// 4. 更新参数（继承创建参数 + id）
export interface {Entity}UpdateDTO extends Partial<{Entity}CreateDTO> { id: ID; }
// 5. 查询参数（分页 + 筛选）
export interface {Entity}Query extends PaginationParams { name?: string; /* 筛选条件 */ }
```

| 规则 | 说明 |
|------|------|
| ❌ 5 件套必须全导出 | 缺一个视为不合格 |
| ❌ DTO 不得包含 id/createdAt 等 | 这些字段由后端生成 |
| ❌ VO 不得直接传给 API | 必须用 DTO 转换 |
| ✅ 5 件套顺序固定 | Entity → VO → CreateDTO → UpdateDTO → Query |

---

## 6 国际化规范

### 6.1 Key 命名：4 段式 + 命名空间

```
格式：{moduleName}.{pageName}.{elementName}.{actionName}

示例：
✅ moduleA.list.btn.create        （模块A.列表页.按钮.新增）
✅ moduleA.editor.title            （模块A.编辑页.标题）
❌ common.save                     （无命名空间，多模块必冲突）
❌ moduleA.btn.create              （缺 pageName 段）
```

### 6.2 语言包结构

```typescript
// modules/{m}/locales/zh-CN.ts
export default {
  list: {
    title: '{Entity}列表',
    btn: { create: '新增', edit: '编辑', delete: '删除' },
    col: { name: '名称', code: '编码', status: '状态' },
  },
  editor: {
    title: '编辑{Entity}',
    label: { name: '名称', code: '编码' },
  },
  message: {
    createSuccess: '创建成功',
    deleteConfirm: '确定要删除"{name}"吗？',
  },
};
```

### 6.3 铁律

- ❌ 禁止使用通用 key（`common.save`、`app.confirm`）
- ❌ 禁止中英文混在一个 key
- ✅ Key 必须以模块名开头（camelCase）
- ✅ 嵌套层级不超过 4 层
- ✅ zh-CN 与 en-US 语言包同步维护
- ✅ 翻译缺失时 fallback 到 i18n 兜底文案（在 `core/i18n/` 中定义）

---

## 7 组件拆分（软建议）

- 单文件 `.vue` ≤ 600 行（不含 `<style>`）——超过仅作 PR 评审提示，不强制
- 若模板中出现两种独立业务实体或两组互不相关的状态机，建议按"职责"拆为子组件
- 拆出的子组件放模块内 `components/` 目录

---

## 8 ESLint 配置建议

```javascript
// eslint.config.js
{
  rules: {
    'vue/component-tags-order': ['error', { order: ['script', 'template', 'style'] }],
    'vue/component-name-in-template-casing': ['error', 'PascalCase'],
    '@typescript-eslint/no-explicit-any': 'error',
    '@typescript-eslint/no-unused-vars': ['error', { argsIgnorePattern: '^_' }],
    'no-debugger': 'error',
    'no-console': ['warn', { allow: ['warn', 'error'] }],
  }
}
```

> 项目可根据模块结构补充 `no-restricted-imports` 规则，限制跨模块深路径引用。
