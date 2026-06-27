---
description: Vue 3 + TypeScript 编码规范 — 设计原则、技术栈基线、组件 script setup 顺序（10步）、命名规范、defineProps/Emits/Expose。
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
