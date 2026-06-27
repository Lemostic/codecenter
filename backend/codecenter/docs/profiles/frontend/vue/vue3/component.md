| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `vue3/script-setup.md`、`common/architecture.md` |

# 组件设计规范（Vue 3）

> 本文件定义 Vue 3 组件的 Props 设计、组件体量、组合模式、样式隔离。
> 本文件规则仅适用于 Vue 3。

---

## 1 Props 接口设计

### 1.1 类型化 Props

**PROF-FE-831** Props MUST 使用 TypeScript interface 定义，MUST NOT 使用内联类型或 `any`。 [MUST]

```vue
<script setup lang="ts">
// ✅ 正确
interface Props {
  user: User;
  showActions?: boolean;
  maxCount?: number;
}

const props = defineProps<Props>();
const emit = defineEmits<{
  (e: 'edit', user: User): void;
  (e: 'delete', id: number): void;
}>();

// ❌ 错误：内联类型 / any
// defineProps<{ user: any; showActions?: boolean }>();
</script>
```

### 1.2 可选 Props 默认值

**PROF-FE-832** 可选 Props SHOULD 提供合理默认值，MUST 在 interface 中使用 `?` 标记可选。 [SHOULD/MUST]

```vue
<script setup lang="ts">
interface Props {
  type?: 'primary' | 'secondary' | 'danger';
  size?: 'small' | 'medium' | 'large';
  disabled?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
  size: 'medium',
  disabled: false,
});
</script>
```

### 1.3 事件通信

**PROF-FE-833** 组件通信 SHOULD 使用 `defineEmits` 定义事件。 [SHOULD]

详见 `vue3/script-setup.md` §4。

---

## 2 组件体量限制

**PROF-FE-834** 单个组件文件 MUST NOT 超过 600 行（含样式），超出 MUST 拆分为子组件或提取 composable。 [MUST]

```vue
<!-- 拆分前：250 行 -->
<script setup lang="ts">
// 50 行状态逻辑
// 30 行搜索表单逻辑
// 80 行表格逻辑
// 40 行分页逻辑
</script>
<template>
  <!-- 模板 -->
</template>
<style scoped>
/* 50 行样式 */
</style>

<!-- 拆分后 -->
<script setup lang="ts">
import UserSearchForm from './UserSearchForm.vue';
import UserTable from './UserTable.vue';
import { EncapsulatedPagination } from '@/common/components/data';
import { useUserList } from './composables/useUserList';

const { users, loading, query, setQuery, page, setPage } = useUserList();
</script>

<template>
  <div class="container">
    <UserSearchForm :query="query" @change="setQuery" />
    <UserTable :data="users" :loading="loading" />
    <EncapsulatedPagination :current="page" @change="setPage" />
  </div>
</template>
```

**PROF-FE-835** 组件逻辑 SHOULD 提取到 composable，保持 template 简洁性。 [SHOULD]

```typescript
// composables/useUserList.ts
import { ref, computed } from 'vue';
import { listUser } from '@/modules/user/api/user';
import type { User, UserQuery, PageResult } from '@/modules/user/types/user';

export const useUserList = () => {
  const query = ref<UserQuery>({ page: 1, pageSize: 20 });
  const page = ref(1);
  const tableData = ref<User[]>([]);
  const total = ref(0);
  const loading = ref(false);

  const loadData = async () => {
    loading.value = true;
    try {
      const res = await listUser({ ...query.value, page: page.value, pageSize: query.value.pageSize });
      tableData.value = res.data?.rows ?? [];
      total.value = res.data?.total ?? 0;
    } catch (error) {
      console.error('[useUserList.loadData]', error);
    } finally {
      loading.value = false;
    }
  };

  const setQuery = (val: Partial<UserQuery>) => {
    query.value = { ...query.value, ...val };
  };
  const setPage = (val: number) => { page.value = val; };

  return {
    users: computed(() => tableData.value),
    total: computed(() => total.value),
    loading,
    query,
    setQuery,
    page,
    setPage,
    loadData,
  };
};
```

---

## 3 命名规范

| 规则 | 说明 |
|------|------|
| **PROF-FE-836** | 组件文件名 MUST 使用 PascalCase，与组件名一致。 [MUST] |
| **PROF-FE-837** | 组件名 MUST 具有描述性，MUST NOT 使用缩写或单字母命名。 [MUST] |
| **PROF-FE-838** | Vue composable MUST 以 `use` 开头命名。 [MUST] |

```
components/
├── Button/Button.vue
├── UserTable/UserTable.vue
├── SearchForm/SearchForm.vue
└── ConfigPanel/ConfigPanel.vue
```

```
// Vue Composables
export const useAuth = () => { };
export const useTableData = () => { };
export const usePagination = () => { };
export const usePermission = () => { };
```

---

## 4 组件组合模式

**PROF-FE-839** 复杂 UI SHOULD 使用组合模式（Composition），MUST NOT 在单一组件中堆叠所有功能。 [SHOULD/MUST]

```vue
<template>
  <Form @submit="handleSubmit">
    <FormField label="用户名" name="username" :rules="[{ required: true }]">
      <Input placeholder="请输入用户名" />
    </FormField>
    <FormField label="邮箱" name="email" :rules="[{ type: 'email' }]">
      <Input placeholder="请输入邮箱" />
    </FormField>
    <FormActions>
      <Button type="primary">提交</Button>
      <Button type="secondary" @click="handleCancel">取消</Button>
    </FormActions>
  </Form>
</template>
```

---

## 5 样式隔离

### 5.1 隔离方案选择

**PROF-FE-840** 组件样式 MUST 使用隔离方案，MUST NOT 使用全局 CSS（全局样式除外）。 [MUST]

| 方案 | 优先级 | 备注 |
|------|--------|------|
| **Tailwind 原子类** | ⭐⭐⭐ 推荐 | 默认首选，配合 `<style scoped>` |
| **CSS Modules** | ⭐ 不推荐 | 仅在 tailwind 不适用时使用 |
| **Scoped Styles `<style scoped>`** | ⭐⭐ 推荐 | 配合 Tailwind 使用 |
| **CSS-in-JS** | ⭐ 不推荐 | 性能与构建复杂度较高 |

```vue
<template>
  <!-- ✅ 正确：Tailwind 原子类 -->
  <div class="flex items-center gap-2 p-3 bg-white rounded-lg">
    <span class="text-sm font-medium text-[#303133]">标题</span>
  </div>
</template>

<style scoped>
/* 仅在 Tailwind 表达困难时使用 scoped CSS */
</style>
```

```css
/* ❌ 不推荐：CSS Modules 写法（与 Tailwind 体系不兼容） */
.container {
  padding: 16px;
}
```

### 5.2 设计变量

**PROF-FE-841** 颜色、间距、字体等设计变量 SHOULD 使用 CSS 自定义属性（CSS Variables）或 Tailwind 配置统一管理。 [SHOULD]

```css
/* styles/variables.css */
:root {
  --color-primary: #1677ff;
  --color-success: #52c41a;
  --color-danger: #ff4d4f;
  --color-text-primary: #1f1f1f;
  --color-text-secondary: #666;
}
```

**PROF-FE-842** 颜色/间距/圆角 MUST 引用设计 Token，详见 `vue3/design-tokens.md`。 [MUST]

### 5.3 内联样式

**PROF-FE-843** 组件内 MUST NOT 使用内联 style 对象（动态样式除外），MAY 通过 CSS 变量传递动态值。 [MUST/MAY]

```vue
<template>
  <!-- ✅ 正确：CSS 类名 -->
  <div :class="$style.card" />

  <!-- ✅ 允许：动态样式（百分比、计算值） -->
  <div :style="{ width: `${progress}%` }" />

  <!-- ❌ 错误：硬编码内联样式 -->
  <div :style="{ padding: '16px', backgroundColor: '#f5f5f5', borderRadius: '8px' }" />
</template>
```

---

## 6 可访问性

详见 `vue3/script-setup.md` §8。

---

## 7 列表渲染

详见 `vue3/script-setup.md` §7。

---

## 8 禁止行为清单

- ❌ 禁止单个组件文件超过 600 行
- ❌ 禁止 Props 超过 10 个
- ❌ 禁止组件名缩写 / 单字母
- ❌ 禁止硬编码内联样式（动态样式除外）
- ❌ 禁止使用全局 CSS（全局样式除外）
- ❌ 禁止在 `<style scoped>` 中硬编码颜色（应搬到 template 用 Tailwind）

---

*本文件规则仅适用于 Vue 3。*
