<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import TpPageHeader from '@mdm/common/components/structure/TpPageHeader.vue';
import TpPageFrame from '@mdm/common/components/layout/TpPageFrame.vue';
import { getModel, createModel, updateModel } from '../api/model';
import type { ModelCreateDTO } from '../types/model';

defineOptions({ name: 'ModelEditor' });

// ========== 路由 ==========
const route = useRoute();
const router = useRouter();

// ========== 状态 ==========
const isEdit = computed(() => !!route.params.id);
const id = computed(() => route.params.id as string);

const loading = ref(false);
const saving = ref(false);
const formRef = ref<FormInstance>();
const formData = ref<ModelCreateDTO>({
  name: '',
  code: '',
  tableName: '',
  modelType: 'normal',
  topicId: '',
  datasourceId: '',
  description: '',
});

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入名称', trigger: 'blur' },
    { max: 30, message: '名称不超过30字符', trigger: 'blur' },
  ],
  code: [
    { required: true, message: '请输入编码', trigger: 'blur' },
    { pattern: /^[A-Z][A-Za-z0-9_]*$/, message: '以大写字母开头，仅允许字母数字下划线', trigger: 'blur' },
  ],
};

// ========== 数据加载（编辑模式） ==========
const fetchDetail = async () => {
  if (!isEdit.value) return;
  loading.value = true;
  try {
    const res = await getModel(id.value);
    const d = res.data?.data;
    if (d) {
      formData.value = {
        name: d.name,
        code: d.code,
        tableName: d.tableName,
        modelType: d.modelType,
        topicId: d.topicId,
        datasourceId: d.datasourceId,
        description: d.description ?? '',
      };
    }
  } catch (error) {
    ElMessage.error('加载失败');
    console.error('[fetchDetail]', error);
  } finally {
    loading.value = false;
  }
};

// ========== 操作方法 ==========
const handleSubmit = async () => {
  if (!formRef.value) return;
  try {
    await formRef.value.validate();
    saving.value = true;
    if (isEdit.value) {
      await updateModel({ id: id.value, ...formData.value });
    } else {
      await createModel(formData.value);
    }
    ElMessage.success('保存成功');
    router.push({ name: 'model-design-list' });
  } catch (error) {
    if (error instanceof Error) ElMessage.error(error.message);
  } finally {
    saving.value = false;
  }
};

const handleCancel = () => {
  router.back();
};

// ========== 生命周期 ==========
onMounted(fetchDetail);
</script>

<template>
  <TpPageFrame>
    <!-- 页头 -->
    <TpPageHeader
      :title="isEdit ? '编辑模型' : '新增模型'"
      :back-to="{ name: 'model-design-list' }"
    />

    <!-- 内容区（自适应撑开，内容多时可滚动） -->
    <div v-loading="loading" class="flex-1 min-h-0 overflow-auto">
      <el-form
        ref="formRef"
        size="default"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-card shadow="never" class="mb-4">
          <div class="mb-4 text-base font-semibold" style="color: #1d1d1f;">基本信息</div>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="名称" prop="name" required>
                <el-input
                  size="default"
                  v-model="formData.name"
                  placeholder="请输入名称"
                  clearable
                  maxlength="30"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="编码" prop="code" required>
                <el-input
                  size="default"
                  v-model="formData.code"
                  placeholder="请输入编码（大写字母开头）"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="描述" prop="description">
            <el-input
              size="default"
              v-model="formData.description"
              type="textarea"
              :rows="3"
              placeholder="请输入描述"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-card>
      </el-form>
    </div>

    <!-- 底部按钮（始终贴底） -->
    <div class="footer-actions flex-shrink-0">
      <el-button size="default" @click="handleCancel">取消</el-button>
      <el-button
        size="default"
        type="primary"
        :loading="saving"
        @click="handleSubmit"
      >
        确定
      </el-button>
    </div>
  </TpPageFrame>
</template>

<style scoped>
.footer-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 16px;
  border-top: 1px solid var(--el-border-color-lighter);
}
</style>
