| 字段 | 值 |
|------|-----|
| 版本 | 2.0 |
| 层级 | L1 |
| 包类型 | frontend-module |
| 引入条件 | `fingerprint.profiles contains 'frontend-vue'` |
| 所属前端包 | `frontend-vue/vue3/` |
| 适用版本 | Vue 3.5+ |
| 依赖规范 | `vue3/script-setup.md`、`common/architecture.md` |

# 状态管理规范（Vue 3）

> 本文件定义 Vue 3 项目的 4 类状态（本地/共享/服务端/URL）、跨组件通信优先级、Pinia 规范。
> 本文件规则仅适用于 Vue 3。

---

## 1 状态分类

前端状态 MUST 按以下四类进行管理：

| 分类 | 说明 | 管理方式 |
|------|------|---------|
| 本地状态 | 组件内部 UI 状态 | `ref()` / `reactive()` |
| 共享状态 | 跨组件/页面的全局数据 | Pinia |
| 服务端状态 | 来自 API 的异步数据 | 业务模块 API + 缓存策略 |
| URL 状态 | 路由参数、查询字符串 | Vue Router |

**PROF-FE-861** 开发前 MUST 明确每个状态的分类，选择对应管理方式，MUST NOT 将服务端状态存入全局 Store。 [MUST]

```typescript
// 本地状态：表单输入、展开/折叠、当前选中项
const isExpanded = ref(false);
const selectedId = ref<number | null>(null);

// 共享状态：用户登录信息、主题、权限
const useUserStore = defineStore('user', () => { /* ... */ });

// 服务端状态：用户列表、配置详情（来自 API）
const tableData = ref<User[]>([]);
const loadData = async () => {
  const res = await listUser(query.value);
  tableData.value = res.data?.rows ?? [];
};

// URL 状态：分页参数、搜索关键字、筛选条件
const route = useRoute();
const router = useRouter();
```

---

## 2 本地状态

**PROF-FE-862** 组件内部 UI 状态 MUST 使用本地状态管理，MUST NOT 提升到全局 Store。 [MUST]

```vue
<!-- ✅ 正确：本地状态 -->
<script setup lang="ts">
import { ref } from 'vue';
import type { User } from './types';

const props = defineProps<{ user: User }>();

const isEditing = ref(false);
const isExpanded = ref(false);
</script>

<template>
  <div>
    <UserDetail v-if="isExpanded" :user="user" />
    <UserSummary v-else :user="user" />
    <button @click="isExpanded = !isExpanded">
      {{ isExpanded ? '收起' : '展开' }}
    </button>
  </div>
</template>
```

```typescript
// ❌ 错误：将 UI 状态放入全局 Store
export const useAppStore = defineStore('app', {
  state: () => ({
    isUserCardExpanded: false,    // 不应放在全局
    isUserCardEditing: false,     // 不应放在全局
  }),
});
```

**PROF-FE-863** 本地状态逻辑复杂时（3 个以上关联状态），SHOULD 抽取为 composable 函数，使用 `ref` + `computed` 集中管理。 [SHOULD]

```typescript
// composables/useFormState.ts
import { ref, computed } from 'vue';

interface FormState {
  values: UserFormData;
  errors: Record<string, string>;
}

export function useFormState(initialValues: UserFormData) {
  const values = ref<UserFormData>({ ...initialValues });
  const errors = ref<Record<string, string>>({});
  const isSubmitting = ref(false);

  const isDirty = computed(() =>
    JSON.stringify(values.value) !== JSON.stringify(initialValues)
  );

  const setField = (field: string, value: unknown) => {
    (values.value as Record<string, unknown>)[field] = value;
    errors.value[field] = '';
  };

  const setErrors = (newErrors: Record<string, string>) => {
    errors.value = newErrors;
    isSubmitting.value = false;
  };

  const submitStart = () => { isSubmitting.value = true; };
  const submitEnd = () => { isSubmitting.value = false; };

  const reset = () => {
    values.value = { ...initialValues };
    errors.value = {};
    isSubmitting.value = false;
  };

  return { values, errors, isSubmitting, isDirty, setField, setErrors, submitStart, submitEnd, reset };
}
```

---

## 3 共享状态（Pinia Store）

**PROF-FE-864** 全局 Store MUST 按业务域拆分为独立的切片（slice/module），MUST NOT 使用单一巨型 Store。 [MUST]

```typescript
// stores/user.ts
import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@/modules/user/types/user';
import type { ID } from '@/common/types/base';

export const useUserStore = defineStore('user', () => {
  const currentUser = ref<User | null>(null);
  const permissions = ref<string[]>([]);
  const token = ref<string>('');

  const isAdmin = computed(() => permissions.value.includes('admin'));
  const isAuthenticated = computed(() => !!token.value);

  const setUserInfo = (user: User, perms: string[]) => {
    currentUser.value = user;
    permissions.value = perms;
  };

  const setToken = (newToken: string) => {
    token.value = newToken;
  };

  const clearUserInfo = () => {
    currentUser.value = null;
    permissions.value = [];
    token.value = '';
  };

  return { currentUser, permissions, token, isAdmin, isAuthenticated, setUserInfo, setToken, clearUserInfo };
});
```

**PROF-FE-865** 全局 Store SHOULD 包含以下类型的状态： [SHOULD]

- 用户认证信息（登录状态、Token、权限）
- 应用级配置（主题、语言、布局偏好）
- 跨页面共享的业务数据（购物车、全局通知）

**PROF-FE-866** Store MUST NOT 存储可从 URL 或 API 直接获取的数据。 [MUST]

```typescript
// ❌ 错误：将 API 数据存入全局 Store
export const useUserStore = defineStore('user', () => {
  const userList = ref([]);           // 应从 API 直接获取
  const configList = ref([]);         // 应从 API 直接获取
  return { userList, configList };
});
```

---

## 4 服务端状态

**PROF-FE-867** 服务端数据 MUST 通过业务模块 API 函数获取，由组件或 composable 管理加载状态。 [MUST]

```vue
<script setup lang="ts">
import { ref } from 'vue';
import { listUser } from '@/modules/user/api/user';
import type { User, UserQuery, PageResult } from '@/modules/user/types/user';

const query = ref<UserQuery>({ page: 1, pageSize: 20 });
const tableData = ref<User[]>([]);
const total = ref(0);
const loading = ref(false);

const loadData = async () => {
  loading.value = true;
  try {
    const res = await listUser(query.value);
    tableData.value = res.data?.rows ?? [];
    total.value = res.data?.total ?? 0;
  } catch (error) {
    console.error('[loadData]', error);
  } finally {
    loading.value = false;
  }
};
</script>
```

| 规则 | 说明 |
|------|------|
| **PROF-FE-868** | 服务端数据 MUST 在组件销毁时清理（或使用 keep-alive 策略）。[MUST] |
| **PROF-FE-869** | 重复请求 SHOULD 防抖，避免短时间内多次触发。[SHOULD] |
| **PROF-FE-870** | 列表数据 SHOULD 配置分页参数与总数。[MUST] |

---

## 5 派生状态

**PROF-FE-871** 可从现有状态计算得出的数据 MUST 使用派生状态，MUST NOT 冗余存储。 [MUST]

```typescript
// ✅ 正确：派生状态
const filteredUsers = computed(() =>
  users.value.filter(u =>
    u.username.includes(searchText.value) && u.status === filterStatus.value
  )
);

const totalCount = computed(() => data.value?.total ?? 0);

const hasPermission = computed(() =>
  permissions.value.includes('user:edit')
);
```

```typescript
// ❌ 错误：冗余存储（手动同步，容易不一致）
const filteredList = ref([]);
watch([users, searchText], () => {
  filteredList.value = users.value.filter(u =>
    u.username.includes(searchText.value)
  );
});
```

**PROF-FE-872** 派生状态 SHOULD 使用 `computed` 处理，确保响应式自动追踪依赖。 [SHOULD]

---

## 6 状态命名规范

| 规则 | 说明 |
|------|------|
| **PROF-FE-873** | 布尔状态 SHOULD 使用 `is` / `has` / `should` / `can` 前缀。 [SHOULD] |
| **PROF-FE-874** | 事件处理函数 SHOULD 使用 `handle` 前缀。 [SHOULD] |
| **PROF-FE-875** | Store 中的 action 命名 SHOULD 使用动词短语，描述意图而非实现。 [SHOULD] |

```typescript
const isLoading = ref(false);
const isExpanded = ref(false);
const hasError = ref(false);
const isVisible = ref(true);
const canEdit = ref(false);
```

```typescript
// Store action
setUserInfo(user, permissions);
clearUserInfo();
toggleSidebar();
updateTheme(theme);
```

---

## 7 跨组件通信优先级

**PROF-FE-876** 跨组件通信 MUST 按以下优先级选择方案： [MUST]

| 优先级 | 方案 | 适用场景 |
|--------|------|----------|
| 1 | Props 传递 | 父子组件直接通信 |
| 2 | 组合模式 / Slot | 父子组件内容分发 |
| 3 | Composable | 兄弟组件共享逻辑 |
| 4 | 全局 Store | 真正的全局状态 |
| 5 | Provide / Inject | 主题、国际化等框架级数据 |

```vue
<script setup lang="ts">
import { provide, ref } from 'vue';
import { useUserStore } from '@/common/stores/user';

// 优先级 1：Props
// 在模板中通过 :data 和 @change 传递

// 优先级 4：全局 Store
const { currentUser } = useUserStore();

// 优先级 5：Provide / Inject（框架级）
const theme = ref('light');
provide('theme', theme);
</script>

<template>
  <!-- 优先级 1：Props -->
  <Child :data="data" @change="handleChange" />

  <!-- 优先级 2：Slot -->
  <Layout>
    <template #sidebar>
      <Sidebar />
    </template>
  </Layout>
</template>
```

---

## 8 全局 Store 选型参考

| 框架 | 推荐方案 | 备选方案 |
|------|---------|---------|
| Vue 3 | **Pinia** | Vuex 4（仅旧项目） |

**PROF-FE-877** Store MUST 保持精简，单个 slice SHOULD 不超过 100 行；全局状态总量 SHOULD 控制在合理范围内。 [MUST/SHOULD]

---

## 9 禁止行为清单

- ❌ 禁止将 UI 状态（isEditing、isExpanded）放入全局 Store
- ❌ 禁止将 API 数据缓存到 Store（应在 composable / 组件内管理）
- ❌ 禁止使用 Options API 风格定义 Store
- ❌ 禁止 Store 内直接操作 DOM
- ❌ 禁止在 state 中存组件实例
- ❌ 禁止跨模块直接调用对方 Store
- ❌ 禁止将派生状态用 `ref` 存储（应使用 `computed`）

---

*本文件规则仅适用于 Vue 3。*
