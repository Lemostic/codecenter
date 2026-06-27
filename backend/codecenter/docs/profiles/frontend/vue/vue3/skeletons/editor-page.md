# 编辑页骨架

> 来源：`frontend_ai_coding_rules.md §11.13` / `vue3/page-patterns §4`
>
> 适用：编辑页（新建/修改）+ flex 三段式布局 + PageHeader

---

## 编辑页 flex 三段式布局

编辑页/详情页带底部操作栏的 flex 纵向三段式布局，使用 `{EncapsulatedPageFrame}` + `<PageHeader>`。

```vue
<!-- ✅ 正确：flex 三段式布局 + {EncapsulatedPageFrame} + PageHeader -->
<template>
  <{EncapsulatedPageFrame}>
    <!-- 页头（返回 + 标题） -->
    <PageHeader title="编辑用户" :back-to="{ name: 'user-list' }">
      <template #actions>
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      </template>
    </PageHeader>

    <!-- 内容区（flex-1 自适应撑开，内容多时可滚动） -->
    <div class="flex-1 min-h-0 overflow-auto p-3">
      <el-form :model="formData" label-width="100px" size="default">
        <el-form-item label="用户名" prop="username" required
          :rules="[{ required: true, message: '请输入用户名', trigger: 'blur' }]">
          <el-input v-model="formData.username" clearable maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="邮箱" prop="email" required
          :rules="[{ required: true, type: 'email', message: '请输入有效邮箱', trigger: 'blur' }]">
          <el-input v-model="formData.email" clearable />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" clearable maxlength="11" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="enabled">启用</el-radio>
            <el-radio value="disabled">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入描述"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
    </div>
  </{EncapsulatedPageFrame}>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { getUser, createUser, updateUser } from '@/modules/user/api/user';
import { {EncapsulatedMessage} } from '@/common/components/feedback';
import PageHeader from '@/common/components/structure/PageHeader.vue';
import type { UserVO, UserCreateDTO, UserUpdateDTO } from '@/modules/user/types/user';

defineOptions({ name: 'UserEditor' });

const route = useRoute();
const router = useRouter();

const id = route.params.id as string | undefined;
const isEdit = !!id;

const formData = ref<UserCreateDTO>({
  username: '',
  email: '',
  phone: '',
  status: 'enabled',
  description: '',
});

const saving = ref(false);

// 加载详情
const loadDetail = async () => {
  if (!id) return;
  try {
    const res = await getUser(id);
    const user: UserVO = res.data;
    formData.value = {
      username: user.username,
      email: user.email,
      phone: user.phone,
      status: user.status,
      description: user.description,
    };
  } catch (error) {
    console.error('[loadDetail]', error);
    {EncapsulatedMessage}.error('加载详情失败');
  }
};

// 保存
const handleSave = async () => {
  saving.value = true;
  try {
    if (isEdit) {
      const updateData: UserUpdateDTO = { id, ...formData.value };
      await updateUser(id, updateData);
      {EncapsulatedMessage}.success('更新成功');
    } else {
      await createUser(formData.value);
      {EncapsulatedMessage}.success('创建成功');
    }
    router.push({ name: 'user-list' });
  } catch (error) {
    console.error('[handleSave]', error);
    {EncapsulatedMessage}.error(isEdit ? '更新失败' : '创建失败');
  } finally {
    saving.value = false;
  }
};

// 取消
const handleCancel = () => {
  router.back();
};

onMounted(() => {
  if (isEdit) loadDetail();
});
</script>
```

---

## 禁忌写法

```vue
<!-- ❌ 禁止：手写页头标题 div（必须用 PageHeader） -->
<template>
  <div class="page-list p-4 flex flex-col h-full bg-white rounded-lg">
    <div class="flex items-center mb-4">
      <el-button @click="router.back()" :icon="ArrowLeft" link />
      <h1 class="text-lg font-semibold ml-2">编辑用户</h1>
    </div>
    <!-- 表单 -->
  </div>
</template>

<!-- ❌ 禁止：底部按钮靠 margin-top 定位（内容少时悬空） -->
<template>
  <div class="page-list p-4">
    <!-- 表单 -->
    <div style="margin-top: 24px;">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary">保存</el-button>
    </div>
  </div>
</template>
```

---

*本文件为骨架代码参考。具体封装组件名由项目配置决定。*
