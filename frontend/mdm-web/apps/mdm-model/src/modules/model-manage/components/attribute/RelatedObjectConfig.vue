<script setup lang="ts">
/**
 * RelatedObjectConfig - 关联对象配置 (§22)
 * 关联模型/字典、关联字段、显示字段、多值、带出字段、赋值字段、过滤规则
 */
import { ref, computed, onMounted } from 'vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

defineOptions({ name: 'RelatedObjectConfig' });

const props = defineProps<{
  modelId: string;
  attributeId: string;
  attributeName?: string;
}>();

// ========== Mock 数据 ==========
const availableModels = ref([
  { id: 'm1', name: '供应商', code: 'SUPPLIER' },
  { id: 'm2', name: '客户', code: 'CUSTOMER' },
  { id: 'm3', name: '物料', code: 'MATERIAL' },
]);

const availableDicts = ref([
  { id: 'd1', name: '国家编码', code: 'COUNTRY_CODE', displayField: '国家名称' },
  { id: 'd2', name: '行业分类', code: 'INDUSTRY_TYPE', displayField: '行业名称' },
]);

const modelAttributes = ref<{ id: string; name: string; englishName: string; dataType: string }[]>([
  { id: 'ma1', name: '供应商名称', englishName: 'SUP_NAME', dataType: 'VARCHAR' },
  { id: 'ma2', name: '供应商编码', englishName: 'SUP_CODE', dataType: 'VARCHAR' },
  { id: 'ma3', name: '联系人', englishName: 'SUP_CONTACT', dataType: 'VARCHAR' },
  { id: 'ma4', name: '联系电话', englishName: 'SUP_PHONE', dataType: 'VARCHAR' },
  { id: 'ma5', name: '地址', englishName: 'SUP_ADDRESS', dataType: 'VARCHAR' },
]);

// ========== 配置状态 ==========
const relationType = ref<'model' | 'dict'>('model');
const selectedModelId = ref('');
const selectedDictId = ref('');
const relatedFieldIds = ref<string[]>([]);
const displayFieldId = ref('');
const isMultiValue = ref(false);

// 带出字段
const carryOutFields = ref<{ fieldId: string; displayName: string }[]>([]);

// 赋值字段
const assignFields = ref<{ sourceFieldId: string; targetFieldId: string }[]>([]);

// 帮助说明
const helpDrawerVisible = ref(false);

// 当前关联模型的属性(过滤大文本/文件)
const filteredModelAttributes = computed(() =>
  modelAttributes.value.filter(a => a.dataType !== 'TEXT' && a.dataType !== 'FILE'),
);

// §22.4 是否多值(仅字符型可配置)
const canMultiValue = computed(() => true); // Mock: 假设当前属性是字符型

// 保存配置
const handleSave = () => {
  if (relationType.value === 'model' && !selectedModelId.value) {
    TpMessage.warning('请选择关联模型');
    return;
  }
  if (relationType.value === 'dict' && !selectedDictId.value) {
    TpMessage.warning('请选择关联字典');
    return;
  }
  TpMessage.success('关联对象配置已保存');
};
</script>

<template>
  <div class="related-object-config p-4">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between mb-4">
      <div>
        <h3 class="text-base font-medium">关联对象配置</h3>
        <span class="text-xs text-[var(--el-text-color-secondary)]">
          属性：{{ attributeName || '未命名' }}
        </span>
      </div>
      <el-button size="small" @click="helpDrawerVisible = true">帮助说明</el-button>
    </div>

    <el-form label-position="top" size="small">
      <!-- §22.2/22.3 关联类型 -->
      <el-form-item label="关联类型">
        <el-radio-group v-model="relationType">
          <el-radio value="model">关联模型</el-radio>
          <el-radio value="dict">关联字典</el-radio>
        </el-radio-group>
      </el-form-item>

      <!-- §22.2 关联模型 -->
      <el-form-item v-if="relationType === 'model'" label="关联模型">
        <el-select v-model="selectedModelId" class="w-full" filterable placeholder="请选择生效状态的主数据模型">
          <el-option v-for="m in availableModels" :key="m.id" :label="`${m.name} (${m.code})`" :value="m.id" />
        </el-select>
      </el-form-item>

      <!-- §22.3 关联字典 -->
      <el-form-item v-if="relationType === 'dict'" label="关联字典">
        <el-select v-model="selectedDictId" class="w-full" filterable placeholder="请选择启用状态的数据字典">
          <el-option v-for="d in availableDicts" :key="d.id" :label="`${d.name} (${d.code})`" :value="d.id" />
        </el-select>
      </el-form-item>

      <!-- §22.2 关联字段(多选) -->
      <el-form-item v-if="selectedModelId || selectedDictId" label="关联字段">
        <el-select v-model="relatedFieldIds" class="w-full" multiple filterable placeholder="请选择关联字段">
          <el-option v-for="a in filteredModelAttributes" :key="a.id" :label="`${a.name} (${a.englishName})`" :value="a.id" />
        </el-select>
      </el-form-item>

      <!-- §22.2 显示字段(单选) -->
      <el-form-item v-if="selectedModelId || selectedDictId" label="显示字段">
        <el-select v-model="displayFieldId" class="w-full" filterable placeholder="请选择显示字段">
          <el-option v-for="a in filteredModelAttributes" :key="a.id" :label="`${a.name} (${a.englishName})`" :value="a.id" />
        </el-select>
      </el-form-item>

      <!-- §22.4 是否多值 -->
      <el-form-item label="是否多值">
        <el-switch v-model="isMultiValue" :disabled="!canMultiValue" />
        <span v-if="isMultiValue" class="ml-2 text-xs text-[var(--el-text-color-warning)]">
          多值模式下不能配置带出字段/赋值字段/过滤规则
        </span>
      </el-form-item>

      <!-- §22.5 带出字段配置 -->
      <el-form-item v-if="!isMultiValue && (selectedModelId || selectedDictId)" label="带出字段">
        <div class="w-full">
          <el-button size="small" class="mb-2" @click="carryOutFields.push({ fieldId: '', displayName: '' })">
            + 添加带出字段
          </el-button>
          <div v-for="(field, idx) in carryOutFields" :key="idx" class="flex items-center gap-2 mb-2">
            <el-select v-model="field.fieldId" class="flex-1" filterable placeholder="选择字段">
              <el-option v-for="a in filteredModelAttributes" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
            <el-input v-model="field.displayName" placeholder="显示名称" class="flex-1" />
            <el-button size="small" text type="danger" @click="carryOutFields.splice(idx, 1)">删除</el-button>
          </div>
        </div>
      </el-form-item>

      <!-- §22.6 赋值字段配置 -->
      <el-form-item v-if="!isMultiValue && (selectedModelId || selectedDictId)" label="赋值字段">
        <div class="w-full">
          <el-button size="small" class="mb-2" @click="assignFields.push({ sourceFieldId: '', targetFieldId: '' })">
            + 添加赋值映射
          </el-button>
          <div v-for="(field, idx) in assignFields" :key="idx" class="flex items-center gap-2 mb-2">
            <el-select v-model="field.sourceFieldId" class="flex-1" filterable placeholder="显示属性">
              <el-option v-for="a in filteredModelAttributes" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
            <span class="text-xs">→</span>
            <el-select v-model="field.targetFieldId" class="flex-1" filterable placeholder="赋值属性">
              <el-option v-for="a in filteredModelAttributes" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
            <el-button size="small" text type="danger" @click="assignFields.splice(idx, 1)">删除</el-button>
          </div>
        </div>
      </el-form-item>
    </el-form>

    <!-- 保存按钮 -->
    <div class="mt-4 flex justify-end gap-2">
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" @click="handleSave">保存配置</el-button>
    </div>

    <!-- §22 帮助说明抽屉 -->
    <el-drawer v-model="helpDrawerVisible" title="关联对象配置帮助" size="400px">
      <div class="text-sm space-y-3">
        <div>
          <h4 class="font-medium mb-1">关联对象配置说明</h4>
          <p class="text-[var(--el-text-color-secondary)]">
            关联对象用于配置当前字段与其他模型或字典的关联关系，支持数据带出和赋值。
          </p>
        </div>
        <div>
          <h4 class="font-medium mb-1">关联模型</h4>
          <p class="text-[var(--el-text-color-secondary)]">选择生效状态的主数据模型，配置关联字段和显示字段。</p>
        </div>
        <div>
          <h4 class="font-medium mb-1">关联字典</h4>
          <p class="text-[var(--el-text-color-secondary)]">选择启用状态的数据字典，关联字段默认为字典编码，显示字段默认为字典显示字段。</p>
        </div>
        <div>
          <h4 class="font-medium mb-1">多值模式</h4>
          <p class="text-[var(--el-text-color-secondary)]">仅字符型字段可配置为多值，多值模式下不能配置带出字段/赋值字段/过滤规则。</p>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.related-object-config {
  max-width: 800px;
  margin: 0 auto;
}
</style>
