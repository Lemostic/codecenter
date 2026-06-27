<script setup lang="ts">
import { ref, watch } from 'vue';
import { TpMessage } from '../feedback/TpMessage';

/**
 * ExpressionBuilder - 可视化表达式构建器
 *
 * 功能：
 * - 左侧：函数分类（字符/数值/转换/逻辑）+ 字段列表
 * - 中间：表达式输入框（支持双击添加）
 * - 常用符号：+ - * / = != > >= < <= and or ( )
 * - 语法检查
 * - 帮助说明
 *
 * 用法：
 *   <ExpressionBuilder v-model="expression" @confirm="onConfirm" />
 */
defineOptions({ name: 'ExpressionBuilder' });

interface Field {
  name: string;
  label: string;
  type?: string;
}

interface Props {
  modelValue: string;
  /** 可用字段列表 */
  fields?: Field[];
  /** 是否只读 */
  readonly?: boolean;
  /** 帮助说明内容 */
  helpText?: string;
}

const props = withDefaults(defineProps<Props>(), {
  fields: () => [],
  readonly: false,
  helpText: '',
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
  (e: 'confirm', v: string): void;
  (e: 'cancel'): void;
}>();

// 表达式内容
const localValue = ref(props.modelValue);
watch(
  () => props.modelValue,
  (v) => {
    localValue.value = v;
  },
);

// 函数分类
const funcCategories = [
  {
    label: '字符函数',
    functions: [
      { name: 'CONCAT', desc: '字符串拼接' },
      { name: 'LENGTH', desc: '字符串长度' },
      { name: 'TRIM', desc: '去空格' },
      { name: 'SUBSTRING', desc: '截取子串' },
      { name: 'UPPER', desc: '转大写' },
      { name: 'LOWER', desc: '转小写' },
      { name: 'REPLACE', desc: '字符串替换' },
      { name: 'IF', desc: '条件判断' },
    ],
  },
  {
    label: '数值函数',
    functions: [
      { name: 'ABS', desc: '绝对值' },
      { name: 'ROUND', desc: '四舍五入' },
      { name: 'FLOOR', desc: '向下取整' },
      { name: 'CEIL', desc: '向上取整' },
      { name: 'MOD', desc: '取模' },
    ],
  },
  {
    label: '转换函数',
    functions: [
      { name: 'TO_CHAR', desc: '转字符串' },
      { name: 'TO_NUMBER', desc: '转数值' },
      { name: 'TO_DATE', desc: '转日期' },
      { name: 'NVL', desc: '空值替换' },
      { name: 'DECODE', desc: '条件值转换' },
    ],
  },
  {
    label: '逻辑运算',
    functions: [
      { name: 'AND', desc: '逻辑与' },
      { name: 'OR', desc: '逻辑或' },
      { name: 'NOT', desc: '逻辑非' },
    ],
  },
];

// 常用符号
const symbols = ['+', '-', '*', '/', '=', '!=', '>', '>=', '<', '<=', '(', ')', "'", ','];

// 当前激活的函数分类 tab
const activeTab = ref(funcCategories[0].label);

// 双击字段/函数/符号添加到表达式
const handleInsert = (text: string) => {
  if (props.readonly) return;
  localValue.value += text;
  emit('update:modelValue', localValue.value);
};

// 清空表达式
const handleClear = () => {
  if (props.readonly) return;
  localValue.value = '';
  emit('update:modelValue', '');
};

// 语法检查
const syntaxError = ref('');
const handleSyntaxCheck = () => {
  if (!localValue.value.trim()) {
    syntaxError.value = '表达式不能为空';
    return;
  }
  // 括号匹配检查
  let depth = 0;
  for (const ch of localValue.value) {
    if (ch === '(') depth++;
    else if (ch === ')') depth--;
    if (depth < 0) {
      syntaxError.value = '括号不匹配';
      return;
    }
  }
  if (depth !== 0) {
    syntaxError.value = '括号不匹配';
    return;
  }
  syntaxError.value = '';
  TpMessage.success('语法检查通过');
};

// 帮助弹窗
const helpVisible = ref(false);

const handleConfirm = () => {
  if (syntaxError.value) return;
  emit('confirm', localValue.value);
};

const handleCancel = () => {
  emit('cancel');
};

// 暴露方法
defineExpose({
  getValue: () => localValue.value,
  setValue: (v: string) => {
    localValue.value = v;
  },
});

/** 默认帮助文本 */
const defaultHelpText = `
<h3 class="font-bold mb-2">运算符</h3>
<table class="w-full text-xs mb-4">
<tr><td class="pr-4">+ - * /</td><td>四则运算</td></tr>
<tr><td>= != &gt; &gt;= &lt; &lt;=</td><td>比较运算</td></tr>
<tr><td>AND OR NOT</td><td>逻辑运算</td></tr>
</table>
<h3 class="font-bold mb-2">示例</h3>
<pre class="text-xs bg-[var(--el-fill-color-light)] p-2 rounded">IF(GYS.GJ='中国')
CONCAT(name, '_suffix')
ROUND(price * 0.9, 2)</pre>
`;
</script>

<template>
  <div class="expression-builder flex gap-3 h-full">
    <!-- 左侧：函数 + 字段 -->
    <div class="w-48 flex-shrink-0 border border-[var(--el-border-color)] rounded flex flex-col">
      <!-- 函数分类 tabs -->
      <el-tabs v-model="activeTab" tab-position="left" class="flex-1 flex expression-builder__funcs">
        <el-tab-pane
          v-for="cat in funcCategories"
          :key="cat.label"
          :label="cat.label"
          :name="cat.label"
          class="h-full overflow-auto"
        >
          <div class="px-2 py-1">
            <div
              v-for="fn in cat.functions"
              :key="fn.name"
              class="expression-builder__func cursor-pointer text-sm px-2 py-1 rounded hover:bg-[var(--el-fill-color-light)]"
              :title="fn.desc"
              @dblclick="handleInsert(`${fn.name}()`)"
            >
              <div class="font-mono text-[var(--el-color-primary)]">{{ fn.name }}()</div>
              <div class="text-xs text-[var(--el-text-color-placeholder)] truncate">{{ fn.desc }}</div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <el-divider class="my-1" />
      <div class="px-3 py-2 border-t border-[var(--el-border-color-lighter)]">
        <div class="text-xs text-[var(--el-text-color-secondary)] mb-1">可用字段</div>
        <div class="max-h-24 overflow-auto">
          <div
            v-for="field in fields"
            :key="field.name"
            class="text-xs px-2 py-1 rounded cursor-pointer hover:bg-[var(--el-fill-color-light)] truncate"
            :title="field.label"
            @dblclick="handleInsert(field.name)"
          >
            <span class="text-[var(--el-color-primary)] font-mono">{{ field.name }}</span>
            <span class="text-[var(--el-text-color-secondary)] ml-1">{{ field.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 右侧：表达式编辑区 -->
    <div class="flex-1 flex flex-col gap-2 min-w-0">
      <!-- 常用符号 + 操作按钮 -->
      <div class="flex items-center justify-between flex-shrink-0">
        <div class="flex items-center gap-1 flex-wrap">
          <el-tag
            v-for="sym in symbols"
            :key="sym"
            class="cursor-pointer font-mono"
            @click="handleInsert(sym)"
          >
            {{ sym }}
          </el-tag>
        </div>
        <div class="flex gap-1">
          <el-button size="small" @click="handleSyntaxCheck">语法检查</el-button>
          <el-button size="small" @click="helpVisible = true">帮助</el-button>
        </div>
      </div>

      <!-- 表达式输入框 -->
      <el-input
        v-model="localValue"
        type="textarea"
        :rows="6"
        :readonly="readonly"
        placeholder="双击左侧函数名/字段/符号插入表达式"
        class="flex-1 font-mono"
        @update:model-value="emit('update:modelValue', $event)"
      />

      <!-- 错误提示 -->
      <div v-if="syntaxError" class="text-sm text-[var(--el-color-danger)]">
        {{ syntaxError }}
      </div>

      <!-- 按钮 -->
      <div class="flex justify-end gap-2 flex-shrink-0">
        <el-button @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确定</el-button>
      </div>
    </div>

    <!-- 帮助弹窗 -->
    <el-dialog v-model="helpVisible" title="表达式帮助" width="600px">
      <div class="text-sm leading-relaxed" v-html="helpText || defaultHelpText" />
    </el-dialog>
  </div>
</template>

<style scoped>
.expression-builder__func:hover {
  background-color: var(--el-fill-color-light);
}
.expression-builder :deep(.el-tabs__header) {
  min-width: 80px;
}
.expression-builder :deep(.el-tabs__nav-wrap) {
  background: var(--el-fill-color-lighter);
}
</style>
