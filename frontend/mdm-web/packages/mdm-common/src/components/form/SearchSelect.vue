<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

/**
 * SearchSelect - 支持滚动加载 + 后端搜索的下拉选择器
 *
 * 符合需求文档全局规范：
 * - 滚动加载：优先加载 30 个选项，滚动到底部自动加载下一页
 * - 后端搜索：用户输入关键词，从后端获取匹配数据
 *
 * 用法：
 *   <SearchSelect
 *     v-model="value"
 *     :api="searchApi"
 *     label-key="name"
 *     value-key="id"
 *     placeholder="请选择"
 *     @change="onChange"
 *   />
 *
 *   // searchApi 签名：
 *   const searchApi = (params: { keyword?: string; pageNum: number; pageSize: number }) =>
 *     Promise<{ rows: T[]; total: number }>
 */
defineOptions({ name: 'SearchSelect' });

interface SearchResult<T> {
  rows: T[];
  total: number;
}

interface Props {
  /** 选中值 */
  modelValue?: string | number | string[] | number[] | null;
  /** 搜索 API */
  api: (params: { keyword?: string; pageNum: number; pageSize: number }) => Promise<SearchResult<any>>;
  /** 选项 label 字段名 */
  labelKey?: string;
  /** 选项 value 字段名 */
  valueKey?: string;
  /** 每页条数 */
  pageSize?: number;
  /** 占位文本 */
  placeholder?: string;
  /** 是否禁用 */
  disabled?: boolean;
  /** 是否多选 */
  multiple?: boolean;
  /** 搜索防抖延迟 ms */
  debounce?: number;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  labelKey: 'name',
  valueKey: 'id',
  pageSize: 30,
  placeholder: '请选择',
  disabled: false,
  multiple: false,
  debounce: 300,
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: any): void;
  (e: 'change', v: any): void;
  (e: 'search', v: string): void;
}>();

const options = ref<Record<string, any>[]>([]);
const loading = ref(false);
const keyword = ref('');
const pageNum = ref(1);
const total = ref(0);
const hasMore = computed(() => options.value.length < total.value);
const remoteMethodRef = ref<() => void>();

let debounceTimer: ReturnType<typeof setTimeout> | null = null;

/** 搜索 */
const handleSearch = (query: string) => {
  keyword.value = query;
  pageNum.value = 1;
  options.value = [];
  total.value = 0;
  emit('search', query);
  loadData(true);
};

/** 加载数据 */
const loadData = async (reset = false) => {
  if (loading.value) return;
  if (!reset && !hasMore.value) return;

  loading.value = true;
  try {
    const res = await props.api({
      keyword: keyword.value || undefined,
      pageNum: reset ? 1 : pageNum.value,
      pageSize: props.pageSize,
    });
    const rows = res.rows ?? [];
    if (reset) {
      options.value = rows;
    } else {
      options.value.push(...rows);
    }
    total.value = res.total ?? 0;
    pageNum.value++;
  } catch (err) {
    console.error('[SearchSelect] load error', err);
  } finally {
    loading.value = false;
  }
};

/** 滚动到底部加载更多 */
const handleScroll = (event: Event) => {
  const el = event.target as HTMLElement;
  const { scrollTop, scrollHeight, clientHeight } = el;
  if (scrollHeight - scrollTop - clientHeight < 50 && hasMore.value && !loading.value) {
    loadData();
  }
};

/** 清空 */
const handleClear = () => {
  keyword.value = '';
  options.value = [];
  pageNum.value = 1;
  total.value = 0;
  emit('update:modelValue', props.multiple ? [] : null);
  emit('change', props.multiple ? [] : null);
};

/** 选中变化 */
const handleChange = (val: any) => {
  emit('update:modelValue', val);
  emit('change', val);
};

/** 回显选项 */
const currentLabel = computed(() => {
  if (props.multiple) {
    if (!Array.isArray(props.modelValue) || !props.modelValue.length) return '';
    const selected = options.value.filter((o) =>
      (props.modelValue as (string | number)[]).includes(o[props.valueKey]),
    );
    return selected.map((o) => o[props.labelKey]).join(', ');
  }
  if (!props.modelValue) return '';
  const item = options.value.find((o) => o[props.valueKey] === props.modelValue);
  return item ? item[props.labelKey] : String(props.modelValue ?? '');
});

// 初始加载
onMounted(() => {
  loadData(true);
});
</script>

<template>
  <el-select
    :model-value="modelValue"
    :filterable="true"
    :remote="true"
    :remote-method="handleSearch"
    :loading="loading"
    :multiple="multiple"
    :disabled="disabled"
    :placeholder="placeholder"
    :clearable="true"
    remote-show-suffix
    class="w-full"
    :persistent="false"
    :reserve-keyword="false"
    @update:model-value="handleChange"
    @clear="handleClear"
    @visible-change="(visible: boolean) => { if (visible && !options.length) loadData(true); }"
  >
    <template v-if="multiple" #header>
      <div class="text-xs px-2 py-1 text-gray-4">
        共 {{ total }} 条{{ keyword ? `（筛选后）` : '' }}，滚动加载更多
      </div>
    </template>

    <el-option
      v-for="item in options"
      :key="item[valueKey]"
      :label="item[labelKey]"
      :value="item[valueKey]"
    />

    <template v-if="hasMore" #append>
      <div class="p-2 text-center text-xs text-gray-4" @click.stop>
        <span v-if="loading">加载中...</span>
        <span v-else class="cursor-pointer text-primary" @click="() => loadData()">加载更多</span>
      </div>
    </template>

    <template #empty>
      <div class="py-4 text-center text-xs text-gray-4">
        <span v-if="loading">加载中...</span>
        <span v-else-if="!options.length && !keyword">请输入关键字搜索</span>
        <span v-else>无匹配结果</span>
      </div>
    </template>
  </el-select>
</template>
