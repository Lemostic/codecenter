# statusTagType 标准映射

> 来源：`frontend_ai_coding_rules.md §11.5/§11.6`
>
> 适用：所有 `el-tag` 状态徽标的 type 映射

---

## 状态 → Tag Type 映射表

| 状态值 | Tag Type | 颜色 | 典型场景 |
|--------|----------|------|----------|
| `draft` | `info` | 灰 | 草稿 |
| `published` | `success` | 绿 | 已发布 |
| `archived` | `warning` | 黄 | 已归档 |
| `enabled` | `success` | 绿 | 启用 |
| `disabled` | `info` | 灰 | 禁用 |
| `error` | `danger` | 红 | 错误 |
| `pending` | `warning` | 黄 | 待审核 |
| `rejected` | `danger` | 红 | 已拒绝 |
| `processing` | `info` | 灰 | 处理中 |

> **未知状态 fallback**：`'info'`（灰色兜底）

---

## 标准实现

```typescript
/**
 * 状态 → Element Plus Tag Type 映射
 * @param status 业务状态值
 * @returns Tag type（success / warning / info / danger）
 */
export const statusTagType = (status: string): 'success' | 'warning' | 'info' | 'danger' => {
  const map: Record<string, 'success' | 'warning' | 'info' | 'danger'> = {
    draft: 'info',
    published: 'success',
    archived: 'warning',
    enabled: 'success',
    disabled: 'info',
    error: 'danger',
    pending: 'warning',
    rejected: 'danger',
    processing: 'info',
  };
  return map[status] ?? 'info';
};

/**
 * 状态显示文本映射
 */
export const statusText = (status: string): string => {
  const map: Record<string, string> = {
    draft: '草稿',
    published: '已发布',
    archived: '已归档',
    enabled: '启用',
    disabled: '禁用',
    error: '异常',
    pending: '待审核',
    rejected: '已拒绝',
    processing: '处理中',
  };
  return map[status] ?? status;
};
```

**位置**：`common/utils/status.ts`（或 `common/utils/format.ts` 中导出）

---

## 在表格列中使用

```vue
<template>
  <el-table :data="tableData" v-loading="loading" border>
    <el-table-column prop="name" label="名称" min-width="120" show-overflow-tooltip />
    <el-table-column prop="status" label="状态" min-width="100">
      <template #default="{ row }">
        <el-tag :type="statusTagType(row.status)" size="default">
          {{ statusText(row.status) }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createdAt" label="创建时间" min-width="160" />
  </el-table>
</template>

<script setup lang="ts">
import { statusTagType, statusText } from '@/common/utils/status';
import { listUser } from '@/modules/user/api/user';
import type { User } from '@/modules/user/types/user';

defineProps<{
  data: User[];
  loading: boolean;
}>();
</script>
```

---

## 项目配置说明

| 项 | 说明 |
|----|------|
| 状态码枚举 | 由业务模块 `constants/status.ts` 定义 |
| 状态文本 | 由 i18n 语言包管理（i18n key：`{module}.status.{statusCode}`） |
| 颜色映射 | 由 `statusTagType` 工具函数统一 |

---

## 禁忌写法

```vue
<!-- ❌ 禁止：硬编码 Tag type -->
<el-tag type="success">{{ row.status }}</el-tag>
<el-tag type="warning">{{ row.status }}</el-tag>

<!-- ❌ 禁止：超过 4 种状态用同一组件（拆为多标签或增加字段） -->
<el-tag :type="getType(row.status1)">{{ row.status1 }}</el-tag>
<el-tag :type="getType(row.status2)">{{ row.status2 }}</el-tag>
<el-tag :type="getType(row.status3)">{{ row.status3 }}</el-tag>
<el-tag :type="getType(row.status4)">{{ row.status4 }}</el-tag>
<el-tag :type="getType(row.status5)">{{ row.status5 }}</el-tag>

<!-- ❌ 禁止：状态文本直接显示英文/拼音（应通过 statusText 映射或 i18n） -->
<el-tag :type="statusTagType(row.status)">{{ row.status }}</el-tag>
<!-- 应改为 -->
<el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
```

---

*本文件为骨架代码参考。*
