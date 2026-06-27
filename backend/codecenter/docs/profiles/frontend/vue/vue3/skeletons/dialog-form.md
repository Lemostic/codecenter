# 弹窗内嵌表单骨架

> 来源：`frontend_ai_coding_rules.md §11.1/§11.2` / `vue3/ui-element-plus §2-3`
>
> 适用：弹窗内嵌表单（新增/编辑/确认）

---

## 标准弹窗骨架

```vue
<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-scrollbar max-height="60vh">
      <el-form ref="formRef" :model="formData" label-width="100px" size="default">
        <el-form-item label="名称" prop="name" required
          :rules="[{ required: true, message: '请输入名称', trigger: 'blur' }]">
          <el-input v-model="formData.name" clearable maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="编码" prop="code" required
          :rules="[{ required: true, message: '请输入编码', trigger: 'blur' }]">
          <el-input v-model="formData.code" clearable maxlength="50" />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="类型 A" value="A" />
            <el-option label="类型 B" value="B" />
            <el-option label="类型 C" value="C" />
          </el-select>
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
    </el-scrollbar>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="saving" @click="handleSave">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue';
import { createEntity, updateEntity } from '@/modules/{m}/api/{entity}';
import { {EncapsulatedMessage} } from '@/common/components/feedback';
import type { FormInstance } from 'element-plus';
import type { EntityCreateDTO, EntityUpdateDTO } from '@/modules/{m}/types/{entity}';

interface Props {
  visible: boolean;
  data?: {EntityVO} | null;
}

const props = withDefaults(defineProps<Props>(), {
  data: null,
});

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void;
  (e: 'success'): void;
}>();

const formRef = ref<FormInstance>();
const saving = ref(false);

const formData = ref<{EntityCreateDTO}>({
  name: '',
  code: '',
  type: 'A',
  status: 'enabled',
  description: '',
});

const visible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val),
});

const title = computed(() => props.data ? '编辑{Entity}' : '新增{Entity}');
const width = computed(() => '800px');
const isEdit = computed(() => !!props.data);

// 监听 data 变化，填充表单
watch(
  () => props.data,
  (newVal) => {
    if (newVal) {
      formData.value = {
        name: newVal.name,
        code: newVal.code,
        type: newVal.type,
        status: newVal.status,
        description: newVal.description,
      };
    } else {
      handleReset();
    }
  },
  { immediate: true }
);

const handleSave = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (!valid) return;
    saving.value = true;
    try {
      if (isEdit.value && props.data) {
        const updateData: {EntityUpdateDTO} = { id: props.data.id, ...formData.value };
        await updateEntity(props.data.id, updateData);
        {EncapsulatedMessage}.success('更新成功');
      } else {
        await createEntity(formData.value);
        {EncapsulatedMessage}.success('创建成功');
      }
      emit('success');
      handleClose();
    } catch (error) {
      console.error('[handleSave]', error);
      {EncapsulatedMessage}.error(isEdit.value ? '更新失败' : '创建失败');
    } finally {
      saving.value = false;
    }
  });
};

const handleReset = () => {
  formData.value = {
    name: '',
    code: '',
    type: 'A',
    status: 'enabled',
    description: '',
  };
  formRef.value?.clearValidate();
};

const handleClose = () => {
  visible.value = false;
  // 关闭后清空（避免下次打开时残留）
  setTimeout(handleReset, 200);
};
</script>
```

---

## 弹窗宽度选择

| 复杂度 | 字段数 | 宽度 |
|--------|--------|------|
| 简单 | 1-3 个字段 | `width="500px"` |
| 中等 | 4-8 个字段 | `width="800px"` |
| 复杂 | > 8 个字段 | `width="1000px"` + el-scrollbar |

---

## 禁忌写法

```vue
<!-- ❌ 禁止：弹窗不写 append-to-body（被父元素 overflow 裁剪） -->
<el-dialog v-model="visible" title="编辑" width="800px">
  <!-- ... -->
</el-dialog>

<!-- ❌ 禁止：弹窗不写 close-on-click-modal="false"（防误关） -->
<el-dialog v-model="visible" title="编辑" width="800px" append-to-body>
  <!-- ... -->
</el-dialog>

<!-- ❌ 禁止：footer 按钮顺序错误（必须 取消 → 确定） -->
<el-dialog v-model="visible" title="编辑" width="800px" append-to-body>
  <template #footer>
    <el-button type="primary">确定</el-button>
    <el-button>取消</el-button>
  </template>
</el-dialog>

<!-- ❌ 禁止：保存按钮不带 :loading -->
<el-button type="primary" @click="handleSave">确定</el-button>
```

---

*本文件为骨架代码参考。占位符 `{Entity}` / `{m}` / `{entity}` 需替换为实际值。*
