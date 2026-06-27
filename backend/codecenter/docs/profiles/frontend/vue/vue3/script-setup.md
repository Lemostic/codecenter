| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `common/architecture.md`、`common/structure.md` |

# script setup 规范（Vue 3）

> 本文件定义 Vue 3 组件的 `<script setup>` 块 10 步顺序、defineProps/Emits/Expose 规则、命名规范。
> 本文件规则仅适用于 Vue 3。

---

## 1 块内顺序（10 步骤）

**PROF-FE-801** Vue 3 组件 MUST 使用 `<script setup lang="ts">` 语法，MUST NOT 使用 Options API。 [MUST]

**PROF-FE-802** `<script setup>` 块内代码 MUST 按以下 10 步骤顺序书写。 [MUST]

```vue
<script setup lang="ts">
// ========== 1. 外部 import（按 vue → 第三方 → 业务模块 顺序）==========
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { Plus } from '@element-plus/icons-vue';
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

---

## 2 块顺序

**PROF-FE-803** 单文件组件 MUST 按 `<script setup>` → `<template>` → `<style scoped>` 顺序书写。 [MUST]

- ❌ 禁止 `<template>` 放在 `<script>` 前面
- ❌ 禁止 `<style>` 放在 `<template>` 前面

---

## 3 defineProps 规范

**PROF-FE-804** Props MUST 使用 TypeScript 泛型（不是 runtime declaration）。 [MUST]

```typescript
// ✅ 正确：TypeScript 泛型
const props = defineProps<{ id: string; title?: string; data?: SomeType }>();

// ❌ 错误：运行时声明，丢失类型
// const props = defineProps({ id: String, title: String });
```

**PROF-FE-805** 带默认值的 Props 使用 `withDefaults`。 [MUST]

```typescript
const props = withDefaults(
  defineProps<{ visible: boolean; row?: SomeVO | null; size?: 'small' | 'default' | 'large' }>(),
  { visible: false, row: null, size: 'default' }
);
```

**PROF-FE-806** Props MUST NOT 超过 10 个，超出 MUST 拆分组件或合并为 object。 [MUST]

---

## 4 defineEmits 规范

**PROF-FE-807** Emits MUST 使用 TypeScript 泛型定义。 [MUST]

```typescript
// ✅ 正确
const emit = defineEmits<{
  (e: 'update:data', value: SomeType): void;
  (e: 'success'): void;
}>();
emit('update:data', newValue);
```

**PROF-FE-808** 事件名 MUST 使用 camelCase（如 `update:data`、`delete-success`）。 [MUST]

---

## 5 defineExpose 规范

**PROF-FE-809** 对外暴露方法使用 `defineExpose`，暴露的方法 MUST 有 JSDoc 注释。 [MUST]

```typescript
/**
 * 加载数据
 * @param id 数据 ID
 */
const loadData = async (id: string) => {
  // ...
};

/**
 * 表单校验
 * @returns 是否通过校验
 */
const validate = (): boolean => {
  return formRef.value?.validate() ?? false;
};

defineExpose({ loadData, validate });
```

**PROF-FE-810** 禁止暴露内部状态（`state.value` 等）。 [MUST]

---

## 6 命名规范

| 类型 | 命名规则 | 示例 |
|------|---------|------|
| 组件文件 | PascalCase | `UserCard.vue` |
| 组件名（defineOptions） | PascalCase，与文件名一致 | `UserCard` |
| 方法名 | camelCase | `handleSubmit`、`loadData` |
| 事件名 | camelCase | `update:data`、`delete-success` |
| Props | camelCase | `userId`、`isLoading` |
| 响应式变量 | camelCase | `formData`、`tableData` |
| CSS 类名 | kebab-case | `project-card` |
| Composables | `use{Name}` | `useAuth`、`usePagination` |

**PROF-FE-811** 组件文件名 MUST 使用 PascalCase，与组件名一致。 [MUST]

**PROF-FE-812** 组件名 MUST 具有描述性，MUST NOT 使用缩写或单字母命名。 [MUST]

```
✅ 正确：
   UserSearchForm.vue / ConfigDetailPanel.vue / OrderStatusBadge.vue

❌ 错误：
   USF.vue / Panel.vue / C.vue
```

**PROF-FE-813** 布尔状态 SHOULD 使用 `is` / `has` / `should` / `can` 前缀。 [SHOULD]

```typescript
const isLoading = ref(false);
const isExpanded = ref(false);
const hasError = ref(false);
const isVisible = ref(true);
const canEdit = ref(false);
```

**PROF-FE-814** 事件处理函数 SHOULD 使用 `handle` 前缀；`on` 前缀用于 props 回调。 [SHOULD]

```vue
<script setup lang="ts">
// 组件内部处理函数（handle 前缀）
const handleSubmit = () => { };
const handleCancel = () => { };
const handleDelete = (id: number) => { };

// 组件 emit 事件（事件名不加 on 前缀）
const emit = defineEmits<{
  submit: [data: FormData];
  cancel: [];
  delete: [id: number];
}>();

// Props 中的回调（on 前缀，Vue 自动映射为 @submit / @cancel / @delete）
const props = defineProps<{
  onSubmit: (data: FormData) => void;
  onCancel: () => void;
  onDelete: (id: number) => void;
}>();
</script>
```

**PROF-FE-815** Store 中的 action 命名 SHOULD 使用动词短语，描述意图而非实现。 [SHOULD]

```typescript
// ✅ 正确
setUserInfo(user, permissions);
clearUserInfo();
toggleSidebar();
updateTheme(theme);

// ❌ 错误
setData(d);              // 过于模糊
doSomething();           // 无意义
flag = true;             // 非函数命名
```

---

## 7 列表渲染 key

**PROF-FE-816** 列表渲染 MUST 提供稳定唯一的 `key`，MUST NOT 使用数组 index 作为 key。 [MUST]

```vue
<template>
  <!-- ✅ 正确：使用稳定唯一字段 -->
  <UserCard
    v-for="user in users"
    :key="user.id"
    :user="user"
  />

  <!-- ❌ 错误：使用 index -->
  <UserCard
    v-for="(user, index) in users"
    :key="index"
    :user="user"
  />
</template>
```

---

## 8 可访问性基础

**PROF-FE-817** 交互元素 MUST 提供语义化 HTML 标签（`button`、`a`、`input`），MUST NOT 使用 `div` 模拟交互。 [MUST]

```vue
<template>
  <!-- ✅ 正确 -->
  <button class="btn" @click="handleClick">提交</button>
  <a href="/detail" class="link">查看详情</a>

  <!-- ❌ 错误 -->
  <div class="btn" @click="handleClick">提交</div>
</template>
```

**PROF-FE-818** 图标按钮 MUST 提供 `aria-label` 属性描述用途。 [MUST]

```vue
<template>
  <button
    :aria-label="`删除用户 ${user.username}`"
    @click="onDelete(user.id)"
  >
    <DeleteIcon />
  </button>
</template>
```

---

## 9 异步操作必备要素

| 要素 | 是否必须 | 说明 |
|------|---------|------|
| `try/catch/finally` | ✅ 必须 | 错误处理标准模式 |
| `loading` 状态 | ✅ 必须 | 触发与结束均要重置 |
| 消息提示 | ✅ 必须 | 成功/失败都给用户反馈 |
| 错误日志 | ✅ 必须 | 带方法名前缀便于排查 |

详见 `common/api-conventions.md` §4。

---

## 10 禁止行为清单

- ❌ 禁止使用 Options API（`export default { data() {}, methods: {} }`）
- ❌ 禁止 `<template>` 放在 `<script>` 前面
- ❌ 禁止使用运行时声明 `defineProps({ id: String })`
- ❌ 禁止 Props 超过 10 个
- ❌ 禁止列表渲染使用 `index` 作为 key
- ❌ 禁止使用 `div` 模拟交互按钮
- ❌ 禁止暴露内部状态（`state.value`）

---

*本文件规则仅适用于 Vue 3。*
