<script setup lang="ts">
/**
 * SegmentFormDialog - 码段新增/编辑/查看弹窗
 *
 * 支持 9 类码段的配置：
 * 1. 固定码 - 固定值、前缀后缀
 * 2. 流水码 - 长度、起始值、步长、前缀后缀
 * 3. 日期码 - 是否当前时间、引用属性、日期格式、前缀后缀
 * 4. 特征码 - 是否定长、长度/填充值、关联属性、特征值维护
 * 5. 区间流水码 - 流水方式、长度、步长、区间属性、区间项维护
 * 6. 引用码 - 引用自身属性、引用来源、截取设置、前缀后缀
 * 7. 动态流水码 - 流水方式、长度、起始值、步长、关联属性、前缀后缀
 * 8. 日期流水码 - 流水方式、日期格式、流水长度、起始值、步长、前缀后缀
 * 9. 引用流水码 - 引用自身属性、引用来源、截取设置、流水长度、前缀后缀
 */
import { ref, reactive, watch, computed } from 'vue';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, Delete } from '@element-plus/icons-vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';
import {
  getSegment, createSegment, updateSegment,
  checkSegmentCodeUnique, checkSegmentNameUnique,
} from '@/modules/model-manage/api/segment';
import type { SegmentVO, SegmentCreateDTO } from '@/modules/model-manage/types/segment';
import {
  SEGMENT_TYPE_OPTIONS, SEGMENT_TYPE_LABEL,
  DATE_FORMAT_OPTIONS,
} from '@/modules/model-manage/types/segment';
import type { SegmentType, DateFormat } from '@/modules/model-manage/types/coding-rule';
import { DATE_FORMAT_OPTIONS as CODING_DATE_FORMAT_OPTIONS } from '@/modules/model-manage/types/coding-rule';
import type { ID } from '@mdm/common/types/base';

defineOptions({ name: 'SegmentFormDialog' });

const props = withDefaults(
  defineProps<{
    /** v-model 绑定 */
    modelValue?: boolean;
    /** 弹窗标题（外部传入） */
    title?: string;
    /** 模式 */
    mode?: 'create' | 'edit' | 'view';
    /** 码段 ID（编辑/查看时） */
    segmentId?: ID | null;
    /** 码段类型（新增时） */
    segmentType?: SegmentType;
    /** 所属模型 ID */
    modelId?: ID;
  }>(),
  {
    modelValue: false,
    title: '新增码段',
    mode: 'create',
    segmentId: null,
    segmentType: 'fixed',
    modelId: undefined,
  },
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

// ========== 表单 ==========
const formRef = ref<FormInstance>();
const saving = ref(false);
const loadingDetail = ref(false);

// 基础字段
interface BaseFormData {
  name: string;
  prefix: string;
  suffix: string;
  description: string;
}

// 固定码
interface FixedFormData extends BaseFormData {
  value: string;
}

// 流水码
interface SerialFormData extends BaseFormData {
  length: number;
  startValue: number;
  step: number;
}

// 日期码
interface DateFormData extends BaseFormData {
  isCurrentTime: boolean;
  refAttributeId: ID | null;
  format: DateFormat;
}

// 特征码
interface FeatureFormData extends BaseFormData {
  isFixedLength: boolean;
  maxLength: number;
  length: number;
  fillValue: string;
  attributeId: ID | null;
  featureItems: { attributeValue: string; codeValue: string }[];
}

// 区间流水码
interface RangeSerialFormData extends BaseFormData {
  length: number;
  step: number;
  startValue: number;
  rangeAttributeId: ID | null;
  rangeItems: { attributeValue: string; startValue: number; endValue: number }[];
}

// 引用码
interface RefFormData extends BaseFormData {
  isOwnAttribute: boolean;
  refSourceAttributeId: ID | null;
  refModelId: ID | null;
  refAttributeId: ID | null;
  substringPosition: 'head' | 'tail';
  substringLength: number;
}

// 动态流水码
interface DynamicSerialFormData extends BaseFormData {
  length: number;
  startValue: number;
  step: number;
  attributeId: ID | null;
}

// 日期流水码
interface DateSerialFormData extends BaseFormData {
  format: DateFormat;
  length: number;
  startValue: number;
  step: number;
}

// 引用流水码
interface RefSerialFormData extends BaseFormData {
  isOwnAttribute: boolean;
  refSourceAttributeId: ID | null;
  refModelId: ID | null;
  refAttributeId: ID | null;
  substringPosition: 'head' | 'tail';
  substringLength: number;
  length: number;
  startValue: number;
  step: number;
}

// 联合表单类型
type SegmentFormData =
  | FixedFormData
  | SerialFormData
  | DateFormData
  | FeatureFormData
  | RangeSerialFormData
  | RefFormData
  | DynamicSerialFormData
  | DateSerialFormData
  | RefSerialFormData;

// 默认值工厂
const createDefaultForm = (type: SegmentType): SegmentFormData => {
  const base: BaseFormData = { name: '', prefix: '', suffix: '', description: '' };
  switch (type) {
    case 'fixed':
      return { ...base, value: '' };
    case 'serial':
      return { ...base, length: 3, startValue: 1, step: 1 };
    case 'date':
      return { ...base, isCurrentTime: false, refAttributeId: null, format: 'yyyyMMdd' };
    case 'feature':
      return { ...base, isFixedLength: true, maxLength: 10, length: 3, fillValue: '0', attributeId: null, featureItems: [] };
    case 'rangeSerial':
      return { ...base, length: 3, step: 1, startValue: 1, rangeAttributeId: null, rangeItems: [] };
    case 'ref':
      return { ...base, isOwnAttribute: false, refSourceAttributeId: null, refModelId: null, refAttributeId: null, substringPosition: 'head', substringLength: 0 };
    case 'dynamicSerial':
      return { ...base, length: 3, startValue: 1, step: 1, attributeId: null };
    case 'dateSerial':
      return { ...base, format: 'yyyyMMdd', length: 3, startValue: 1, step: 1 };
    case 'refSerial':
      return { ...base, isOwnAttribute: false, refSourceAttributeId: null, refModelId: null, refAttributeId: null, substringPosition: 'head', substringLength: 0, length: 3, startValue: 1, step: 1 };
    default:
      return { ...base, value: '' };
  }
};

// 表单数据
const formData = ref<SegmentFormData>(createDefaultForm('fixed'));

// 码段类型名称
const typeName = computed(() => SEGMENT_TYPE_LABEL[props.segmentType || 'fixed']);

// 码段配置说明
const typeDescription = computed(() => {
  const desc: Record<SegmentType, string> = {
    fixed: '固定码指在编码过程保持不变的字符串。例如：编码都必须以"ELEC"开头。',
    serial: '流水码指一种按顺序递增的数字，需给定范围和步长。',
    date: '日期码通过关联编码模型中的时间属性，或直接采用系统当前时间动态生成。',
    feature: '特征码定义编码长度和特征码段关联属性，按照关联属性值维护特征码值。',
    rangeSerial: '区间流水码基于特定属性值（区间属性）来划分独立流水号段。',
    ref: '引用码通过读取当前模型或其关联模型中的某一属性字段，将该属性实际值作为码值组成编码。',
    dynamicSerial: '动态流水码基于引用属性的不同值生成不同的流水码值，相同的属性值流水码相同。',
    refSerial: '引用流水码采用"引用值+流水号"的复合结构，拼接引用属性值与顺序号生成标识。',
    dateSerial: '日期流水码采用"日期+流水号"的复合结构，拼接当前日期与当日顺序号生成唯一标识。',
  };
  return desc[props.segmentType || 'fixed'] || '';
});

// 唯一性校验
const validateNameUnique = async (_rule: any, value: string, callback: any) => {
  if (!value || props.mode === 'view') {
    callback();
    return;
  }
  try {
    const res = await checkSegmentNameUnique(value, props.segmentId || undefined);
    if (res.data?.data === false) {
      callback(new Error('码段名称已存在'));
    } else {
      callback();
    }
  } catch {
    callback();
  }
};

// 表单校验规则
const rules = computed<FormRules>(() => {
  const baseRules: FormRules = {
    name: [
      { required: true, message: '请输入码段名称', trigger: 'blur' },
      { max: 50, message: '码段名称最多50个字符', trigger: 'blur' },
      { validator: validateNameUnique, trigger: 'blur' },
    ],
    prefix: [
      { max: 5, message: '前缀最多5个字符', trigger: 'blur' },
    ],
    suffix: [
      { max: 5, message: '后缀最多5个字符', trigger: 'blur' },
    ],
  };

  const typeRules: Record<SegmentType, FormRules> = {
    fixed: {
      value: [{ required: true, message: '请输入固定码值', trigger: 'blur' }],
    },
    serial: {
      length: [{ required: true, message: '请输入码段长度', trigger: 'blur' }],
      startValue: [{ required: true, message: '请输入起始值', trigger: 'blur' }],
    },
    date: {
      format: [{ required: true, message: '请选择日期格式', trigger: 'change' }],
    },
    feature: {
      attributeId: [{ required: true, message: '请选择关联属性', trigger: 'change' }],
    },
    rangeSerial: {
      length: [{ required: true, message: '请输入码段长度', trigger: 'blur' }],
      rangeAttributeId: [{ required: true, message: '请选择区间关联属性', trigger: 'change' }],
    },
    ref: {},
    dynamicSerial: {
      length: [{ required: true, message: '请输入码段长度', trigger: 'blur' }],
      attributeId: [{ required: true, message: '请选择关联属性', trigger: 'change' }],
    },
    dateSerial: {
      format: [{ required: true, message: '请选择日期格式', trigger: 'change' }],
      length: [{ required: true, message: '请输入流水长度', trigger: 'blur' }],
    },
    refSerial: {
      length: [{ required: true, message: '请输入流水长度', trigger: 'blur' }],
    },
  };

  return { ...baseRules, ...typeRules[props.segmentType || 'fixed'] };
});

// ========== 弹窗打开时 ==========
watch(
  () => props.modelValue,
  async (visible) => {
    if (!visible) return;

    if (props.mode === 'view' && props.segmentId) {
      // 查看模式加载详情
      loadingDetail.value = true;
      try {
        const res = await getSegment(props.segmentId);
        const data = res.data?.data as SegmentVO;
        if (data) {
          formData.value = transformToFormData(data);
        }
      } catch (error) {
        TpMessage.error('加载码段详情失败');
        console.error('[SegmentFormDialog] load error', error);
      } finally {
        loadingDetail.value = false;
      }
    } else if (props.mode === 'create') {
      // 新增模式
      formData.value = createDefaultForm(props.segmentType || 'fixed');
    }
  },
);

// 将 API 数据转换为表单数据
const transformToFormData = (data: SegmentVO): SegmentFormData => {
  const base: BaseFormData = {
    name: data.name,
    prefix: data.prefix || '',
    suffix: data.suffix || '',
    description: data.description || '',
  };

  switch (data.type) {
    case 'fixed':
      return { ...base, value: data.value || '' };
    case 'serial':
      return { ...base, length: data.length || 3, startValue: data.startValue ?? 1, step: data.step ?? 1 };
    case 'date':
      return { ...base, isCurrentTime: data.isCurrentTime ?? false, refAttributeId: data.refAttributeId || null, format: data.format || 'yyyyMMdd' };
    case 'feature':
      return {
        ...base,
        isFixedLength: data.isFixedLength ?? true,
        maxLength: data.maxLength || 10,
        length: data.length || 3,
        fillValue: data.fillValue || '0',
        attributeId: data.attributeId || null,
        featureItems: JSON.parse(data.featureItemsJson || '[]'),
      };
    case 'rangeSerial':
      return {
        ...base,
        length: data.length || 3,
        step: data.step || 1,
        startValue: data.startValue || 1,
        rangeAttributeId: data.rangeAttributeId || null,
        rangeItems: JSON.parse(data.rangeItemsJson || '[]'),
      };
    case 'ref':
      return {
        ...base,
        isOwnAttribute: data.isOwnAttribute ?? false,
        refSourceAttributeId: data.refSourceAttributeId || null,
        refModelId: data.refModelId || null,
        refAttributeId: data.refAttributeId || null,
        substringPosition: data.substringPosition || 'head',
        substringLength: data.substringLength || 0,
      };
    case 'dynamicSerial':
      return {
        ...base,
        length: data.length || 3,
        startValue: data.startValue ?? 1,
        step: data.step ?? 1,
        attributeId: data.attributeId || null,
      };
    case 'dateSerial':
      return {
        ...base,
        format: data.format || 'yyyyMMdd',
        length: data.length || 3,
        startValue: data.startValue ?? 1,
        step: data.step ?? 1,
      };
    case 'refSerial':
      return {
        ...base,
        isOwnAttribute: data.isOwnAttribute ?? false,
        refSourceAttributeId: data.refSourceAttributeId || null,
        refModelId: data.refModelId || null,
        refAttributeId: data.refAttributeId || null,
        substringPosition: data.substringPosition || 'head',
        substringLength: data.substringLength || 0,
        length: data.length || 3,
        startValue: data.startValue ?? 1,
        step: data.step ?? 1,
      };
    default:
      return { ...base, value: '' };
  }
};

// 特征码/区间项操作
const addFeatureItem = () => {
  const fd = formData.value as FeatureFormData;
  fd.featureItems.push({ attributeValue: '', codeValue: '' });
};

const removeFeatureItem = (index: number) => {
  const fd = formData.value as FeatureFormData;
  fd.featureItems.splice(index, 1);
};

const addRangeItem = () => {
  const fd = formData.value as RangeSerialFormData;
  fd.rangeItems.push({ attributeValue: '', startValue: 1, endValue: 1 });
};

const removeRangeItem = (index: number) => {
  const fd = formData.value as RangeSerialFormData;
  fd.rangeItems.splice(index, 1);
};

// ========== 保存 ==========
const handleSave = async () => {
  if (props.mode === 'view') {
    emit('update:modelValue', false);
    return;
  }

  try {
    await formRef.value?.validate();
  } catch {
    return;
  }

  saving.value = true;
  try {
    const dto = buildDTO();
    if (props.mode === 'create') {
      await createSegment(dto);
      TpMessage.success('创建成功');
    } else if (props.mode === 'edit' && props.segmentId) {
      await updateSegment({ id: props.segmentId, ...dto });
      TpMessage.success('更新成功');
    }
    emit('success');
    emit('update:modelValue', false);
  } catch (error) {
    console.error('[SegmentFormDialog] save error', error);
  } finally {
    saving.value = false;
  }
};

// 构建 DTO
const buildDTO = (): SegmentCreateDTO => {
  const fd = formData.value;
  const base = {
    modelId: props.modelId,
    type: props.segmentType || 'fixed',
    name: fd.name,
    prefix: fd.prefix || undefined,
    suffix: fd.suffix || undefined,
    description: fd.description || undefined,
  };

  switch (props.segmentType) {
    case 'fixed':
      return { ...base, value: (fd as FixedFormData).value };
    case 'serial':
      return { ...base, length: (fd as SerialFormData).length, startValue: (fd as SerialFormData).startValue, step: (fd as SerialFormData).step };
    case 'date':
      return {
        ...base,
        isCurrentTime: (fd as DateFormData).isCurrentTime,
        refAttributeId: (fd as DateFormData).refAttributeId || undefined,
        format: (fd as DateFormData).format,
      };
    case 'feature':
      return {
        ...base,
        isFixedLength: (fd as FeatureFormData).isFixedLength,
        maxLength: (fd as FeatureFormData).maxLength,
        length: (fd as FeatureFormData).length,
        fillValue: (fd as FeatureFormData).fillValue,
        attributeId: (fd as FeatureFormData).attributeId || undefined,
        featureItems: (fd as FeatureFormData).featureItems.filter(item => item.attributeValue && item.codeValue),
      };
    case 'rangeSerial':
      return {
        ...base,
        length: (fd as RangeSerialFormData).length,
        step: (fd as RangeSerialFormData).step,
        startValue: (fd as RangeSerialFormData).startValue,
        rangeAttributeId: (fd as RangeSerialFormData).rangeAttributeId || undefined,
        rangeItems: (fd as RangeSerialFormData).rangeItems.filter(item => item.attributeValue),
      };
    case 'ref':
      return {
        ...base,
        isOwnAttribute: (fd as RefFormData).isOwnAttribute,
        refSourceAttributeId: (fd as RefFormData).refSourceAttributeId || undefined,
        refAttributeId: (fd as RefFormData).refAttributeId || undefined,
        substringPosition: (fd as RefFormData).substringPosition,
        substringLength: (fd as RefFormData).substringLength,
      };
    case 'dynamicSerial':
      return {
        ...base,
        length: (fd as DynamicSerialFormData).length,
        startValue: (fd as DynamicSerialFormData).startValue,
        step: (fd as DynamicSerialFormData).step,
        attributeId: (fd as DynamicSerialFormData).attributeId || undefined,
      };
    case 'dateSerial':
      return {
        ...base,
        format: (fd as DateSerialFormData).format,
        length: (fd as DateSerialFormData).length,
        startValue: (fd as DateSerialFormData).startValue,
        step: (fd as DateSerialFormData).step,
      };
    case 'refSerial':
      return {
        ...base,
        isOwnAttribute: (fd as RefSerialFormData).isOwnAttribute,
        refSourceAttributeId: (fd as RefSerialFormData).refSourceAttributeId || undefined,
        refAttributeId: (fd as RefSerialFormData).refAttributeId || undefined,
        substringPosition: (fd as RefSerialFormData).substringPosition,
        substringLength: (fd as RefSerialFormData).substringLength,
        length: (fd as RefSerialFormData).length,
        startValue: (fd as RefSerialFormData).startValue,
        step: (fd as RefSerialFormData).step,
      };
    default:
      return base;
  }
};

// 判断是否为某类型
const isType = (type: SegmentType) => props.segmentType === type;
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    :title="title"
    width="700px"
    destroy-on-close
    :close-on-click-modal="false"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <div v-loading="loadingDetail">
      <!-- 顶部提示 -->
      <div class="mb-4 p-3 bg-[var(--el-fill-color)] rounded text-sm text-[var(--el-text-color-regular)]">
        <p class="font-medium mb-1">{{ typeName }}说明</p>
        <p>{{ typeDescription }}</p>
      </div>

      <el-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        label-width="120px"
        :disabled="mode === 'view'"
      >
        <!-- 基础信息 -->
        <el-form-item label="码段类型">
          <el-input :model-value="typeName" disabled />
        </el-form-item>

        <el-form-item label="码段名称" prop="name">
          <el-input v-model="(formData as any).name" placeholder="系统自动生成，支持修改" maxlength="50" />
        </el-form-item>

        <el-form-item label="码段前缀" prop="prefix">
          <el-input v-model="(formData as any).prefix" placeholder="可选，支持数字、字母、特殊字符" maxlength="5" />
        </el-form-item>

        <el-form-item label="码段后缀" prop="suffix">
          <el-input v-model="(formData as any).suffix" placeholder="可选，支持数字、字母、特殊字符" maxlength="5" />
        </el-form-item>

        <!-- 固定码 -->
        <template v-if="isType('fixed')">
          <el-form-item label="固定码值" prop="value">
            <el-input v-model="(formData as FixedFormData).value" placeholder="请输入固定码值" />
          </el-form-item>
        </template>

        <!-- 流水码 -->
        <template v-if="isType('serial')">
          <el-form-item label="码段长度" prop="length">
            <el-input-number v-model="(formData as SerialFormData).length" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="起始值" prop="startValue">
            <el-input-number v-model="(formData as SerialFormData).startValue" :min="0" />
          </el-form-item>
          <el-form-item label="步长" prop="step">
            <el-input-number v-model="(formData as SerialFormData).step" :min="1" />
          </el-form-item>
        </template>

        <!-- 日期码 -->
        <template v-if="isType('date')">
          <el-form-item label="是否当前时间">
            <el-switch v-model="(formData as DateFormData).isCurrentTime" />
          </el-form-item>
          <el-form-item label="日期格式" prop="format">
            <el-select v-model="(formData as DateFormData).format" placeholder="请选择日期格式" style="width: 100%">
              <el-option v-for="opt in DATE_FORMAT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
        </template>

        <!-- 特征码 -->
        <template v-if="isType('feature')">
          <el-form-item label="是否定长">
            <el-switch v-model="(formData as FeatureFormData).isFixedLength" />
          </el-form-item>
          <template v-if="(formData as FeatureFormData).isFixedLength">
            <el-form-item label="码段长度" prop="length">
              <el-input-number v-model="(formData as FeatureFormData).length" :min="1" :max="10" />
            </el-form-item>
            <el-form-item label="填充值">
              <el-input v-model="(formData as FeatureFormData).fillValue" placeholder="填充字符，默认0" style="width: 100px" />
            </el-form-item>
          </template>
          <template v-else>
            <el-form-item label="最大长度" prop="maxLength">
              <el-input-number v-model="(formData as FeatureFormData).maxLength" :min="1" :max="10" />
            </el-form-item>
          </template>
          <el-form-item label="特征值维护">
            <div class="w-full">
              <div v-for="(item, index) in (formData as FeatureFormData).featureItems" :key="index" class="flex items-center gap-2 mb-2">
                <el-input v-model="item.attributeValue" placeholder="属性值" style="width: 150px" />
                <el-input v-model="item.codeValue" placeholder="特征码值" style="width: 150px" />
                <el-button :icon="Delete" circle size="small" @click="removeFeatureItem(index)" />
              </div>
              <el-button type="primary" link :icon="Plus" @click="addFeatureItem">添加特征值</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 区间流水码 -->
        <template v-if="isType('rangeSerial')">
          <el-form-item label="码段长度" prop="length">
            <el-input-number v-model="(formData as RangeSerialFormData).length" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="步长" prop="step">
            <el-input-number v-model="(formData as RangeSerialFormData).step" :min="1" />
          </el-form-item>
          <el-form-item label="起始值" prop="startValue">
            <el-input-number v-model="(formData as RangeSerialFormData).startValue" :min="0" />
          </el-form-item>
          <el-form-item label="流水区间设置">
            <div class="w-full">
              <div class="text-xs text-[var(--el-text-color-secondary)] mb-2">区间起始值 &lt; 结束值，互斥校验</div>
              <div v-for="(item, index) in (formData as RangeSerialFormData).rangeItems" :key="index" class="flex items-center gap-2 mb-2">
                <el-input v-model="item.attributeValue" placeholder="区间属性值" style="width: 150px" />
                <el-input-number v-model="item.startValue" :min="0" placeholder="起始值" style="width: 100px" />
                <el-input-number v-model="item.endValue" :min="0" placeholder="结束值" style="width: 100px" />
                <el-button :icon="Delete" circle size="small" @click="removeRangeItem(index)" />
              </div>
              <el-button type="primary" link :icon="Plus" @click="addRangeItem">添加区间</el-button>
            </div>
          </el-form-item>
        </template>

        <!-- 引用码 -->
        <template v-if="isType('ref')">
          <el-form-item label="引用自身属性">
            <el-switch v-model="(formData as RefFormData).isOwnAttribute" />
          </el-form-item>
          <el-form-item label="截取起始位置">
            <el-radio-group v-model="(formData as RefFormData).substringPosition">
              <el-radio label="head">从头部截取</el-radio>
              <el-radio label="tail">从尾部截取</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="截取步长">
            <el-input-number v-model="(formData as RefFormData).substringLength" :min="0" placeholder="截取长度" />
          </el-form-item>
        </template>

        <!-- 动态流水码 -->
        <template v-if="isType('dynamicSerial')">
          <el-form-item label="码段长度" prop="length">
            <el-input-number v-model="(formData as DynamicSerialFormData).length" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="起始值">
            <el-input-number v-model="(formData as DynamicSerialFormData).startValue" :min="0" />
          </el-form-item>
          <el-form-item label="步长">
            <el-input-number v-model="(formData as DynamicSerialFormData).step" :min="1" />
          </el-form-item>
        </template>

        <!-- 日期流水码 -->
        <template v-if="isType('dateSerial')">
          <el-form-item label="日期格式" prop="format">
            <el-select v-model="(formData as DateSerialFormData).format" placeholder="请选择日期格式" style="width: 100%">
              <el-option v-for="opt in DATE_FORMAT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="流水长度" prop="length">
            <el-input-number v-model="(formData as DateSerialFormData).length" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="起始值">
            <el-input-number v-model="(formData as DateSerialFormData).startValue" :min="0" />
          </el-form-item>
          <el-form-item label="步长">
            <el-input-number v-model="(formData as DateSerialFormData).step" :min="1" />
          </el-form-item>
        </template>

        <!-- 引用流水码 -->
        <template v-if="isType('refSerial')">
          <el-form-item label="引用自身属性">
            <el-switch v-model="(formData as RefSerialFormData).isOwnAttribute" />
          </el-form-item>
          <el-form-item label="截取起始位置">
            <el-radio-group v-model="(formData as RefSerialFormData).substringPosition">
              <el-radio label="head">从头部截取</el-radio>
              <el-radio label="tail">从尾部截取</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="截取步长">
            <el-input-number v-model="(formData as RefSerialFormData).substringLength" :min="0" />
          </el-form-item>
          <el-form-item label="流水长度" prop="length">
            <el-input-number v-model="(formData as RefSerialFormData).length" :min="1" :max="10" />
          </el-form-item>
          <el-form-item label="起始值">
            <el-input-number v-model="(formData as RefSerialFormData).startValue" :min="0" />
          </el-form-item>
          <el-form-item label="步长">
            <el-input-number v-model="(formData as RefSerialFormData).step" :min="1" />
          </el-form-item>
        </template>

        <el-form-item label="描述">
          <el-input v-model="(formData as any).description" type="textarea" :rows="2" placeholder="可选，最多50个字符" maxlength="50" />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">
        {{ mode === 'view' ? '关闭' : '取消' }}
      </el-button>
      <el-button v-if="mode !== 'view'" type="primary" :loading="saving" @click="handleSave">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>
