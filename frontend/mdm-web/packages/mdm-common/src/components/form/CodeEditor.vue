<script setup lang="ts">
/**
 * CodeEditor - 代码编辑器
 *
 * 支持 Groovy/JavaScript 语法高亮，提供语法校验和复制功能。
 *
 * 用法：
 *   <CodeEditor v-model="code" language="groovy" :line-numbers="true" />
 */
import { ref, computed, watch, onMounted } from 'vue';
import TpMessage from '@mdm/common/components/feedback/TpMessage';

defineOptions({ name: 'CodeEditor' });

interface Props {
  modelValue: string;
  /** 语言类型（groovy/js） */
  language?: string;
  /** 是否只读 */
  readonly?: boolean;
  /** 编辑器高度 */
  height?: string;
  /** 是否显示行号 */
  lineNumbers?: boolean;
  /** 是否显示功能按钮 */
  showActions?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  language: 'groovy',
  readonly: false,
  height: '200px',
  lineNumbers: true,
  showActions: true,
});

const emit = defineEmits<{
  (e: 'update:modelValue', v: string): void;
  (e: 'validate', fn: (script: string) => Promise<{ valid: boolean; errors: string[] }>): void;
}>();

const textareaRef = ref<HTMLTextAreaElement>();
const highlightedCode = ref('');

// Groovy 关键字
const GROOVY_KEYWORDS = [
  'def', 'class', 'interface', 'enum', 'extends', 'implements',
  'if', 'else', 'while', 'for', 'switch', 'case', 'default', 'break', 'continue', 'return',
  'try', 'catch', 'finally', 'throw', 'throws',
  'import', 'package', 'as', 'in', 'assert', 'trait', 'mixin',
  'public', 'private', 'protected', 'static', 'final', 'abstract', 'synchronized', 'volatile',
  'native', 'strictfp', 'transient', 'annotation',
  'true', 'false', 'null', 'this', 'super',
  'new', 'instanceof', 'typeof', 'void',
];

const GROOVY_TYPES = [
  'String', 'int', 'long', 'double', 'float', 'boolean', 'char', 'byte', 'short',
  'Integer', 'Long', 'Double', 'Float', 'Boolean', 'Character', 'Byte', 'Short',
  'List', 'Map', 'Set', 'Array', 'Object', 'Class', 'Date', 'BigDecimal',
];

// 简单的语法高亮函数
const highlightCode = (code: string, lang: string): string => {
  if (!code) return '';

  let escaped = code
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;');

  if (lang === 'groovy' || lang === 'js') {
    // 注释高亮（单行 // 和多行 /* */）
    escaped = escaped.replace(/(\/\/[^\n]*)/g, '<span class="token comment">$1</span>');
    escaped = escaped.replace(/(\/\*[\s\S]*?\*\/)/g, '<span class="token comment">$1</span>');

    // 字符串高亮
    escaped = escaped.replace(/("(?:[^"\\]|\\.)*"|'(?:[^'\\]|\\.)*')/g, '<span class="token string">$1</span>');

    // 数字高亮
    escaped = escaped.replace(/\b(\d+\.?\d*)\b/g, '<span class="token number">$1</span>');

    // 关键字高亮
    const keywords = lang === 'groovy' ? GROOVY_KEYWORDS : ['const', 'let', 'var', 'function', 'async', 'await', 'yield'];
    escaped = escaped.replace(new RegExp(`\\b(${keywords.join('|')})\\b`, 'g'), '<span class="token keyword">$1</span>');

    // 类型高亮
    if (lang === 'groovy') {
      escaped = escaped.replace(new RegExp(`\\b(${GROOVY_TYPES.join('|')})\\b`, 'g'), '<span class="token type">$1</span>');
    }

    // 方法调用高亮
    escaped = escaped.replace(/(\w+)(\s*\()/g, '<span class="token function">$1</span>$2');
  }

  return escaped;
};

// 行数
const lineCount = computed(() => {
  if (!props.modelValue) return 1;
  return props.modelValue.split('\n').length;
});

const lineNumbersArr = computed(() =>
  Array.from({ length: lineCount.value }, (_, i) => i + 1),
);

// 复制
const handleCopy = async () => {
  try {
    await navigator.clipboard.writeText(props.modelValue);
    TpMessage.success('复制成功');
  } catch {
    TpMessage.error('复制失败');
  }
};

// 输入处理
const handleInput = (event: Event) => {
  const target = event.target as HTMLTextAreaElement;
  emit('update:modelValue', target.value);
};

// 同步滚动
const handleScroll = (event: Event) => {
  const target = event.target as HTMLTextAreaElement;
  const highlightEl = textareaRef.value?.previousElementSibling as HTMLElement;
  if (highlightEl) {
    highlightEl.scrollTop = target.scrollTop;
    highlightEl.scrollLeft = target.scrollLeft;
  }
};

// Tab 键处理
const handleKeydown = (event: KeyboardEvent) => {
  if (event.key === 'Tab') {
    event.preventDefault();
    const target = event.target as HTMLTextAreaElement;
    const start = target.selectionStart;
    const end = target.selectionEnd;
    const value = props.modelValue;
    const newValue = value.substring(0, start) + '  ' + value.substring(end);
    emit('update:modelValue', newValue);
    // 设置光标位置
    requestAnimationFrame(() => {
      target.selectionStart = target.selectionEnd = start + 2;
    });
  }
};

// 监听内容变化，更新高亮
watch(() => props.modelValue, (newVal) => {
  highlightedCode.value = highlightCode(newVal, props.language || 'groovy');
}, { immediate: true });

onMounted(() => {
  highlightedCode.value = highlightCode(props.modelValue, props.language || 'groovy');
});
</script>

<template>
  <div
    class="code-editor"
    :class="{ 'is-readonly': readonly }"
  >
    <!-- 工具栏 -->
    <div v-if="showActions && !readonly" class="code-editor__toolbar">
      <span class="text-xs text-[var(--el-text-color-secondary)]">{{ language.toUpperCase() }}</span>
      <div class="flex-1" />
      <el-button size="small" link @click="handleCopy">
        <el-icon><DocumentCopy /></el-icon>
        复制
      </el-button>
    </div>

    <!-- 编辑器主体 -->
    <div class="code-editor__body" :style="{ height }">
      <!-- 行号列 -->
      <div v-if="lineNumbers" class="code-editor__lines" aria-hidden="true">
        <div
          v-for="n in lineNumbersArr"
          :key="n"
          class="code-editor__line-num"
        >
          {{ n }}
        </div>
      </div>

      <!-- 代码显示区（语法高亮） -->
      <div
        class="code-editor__highlight"
        aria-hidden="true"
        v-html="highlightedCode"
      />

      <!-- 代码输入区 -->
      <textarea
        ref="textareaRef"
        :value="modelValue"
        :readonly="readonly"
        spellcheck="false"
        class="code-editor__textarea"
        :style="{ height }"
        @input="handleInput"
        @scroll="handleScroll"
        @keydown="handleKeydown"
      />
    </div>
  </div>
</template>

<script lang="ts">
// Element Plus Icons
import { DocumentCopy } from '@element-plus/icons-vue';
export default { name: 'CodeEditor' };
</script>

<style scoped>
.code-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  background: var(--el-fill-color-light);
  font-family: 'Courier New', Courier, monospace;
  font-size: 13px;
  line-height: 1.6;
  overflow: hidden;
}

.code-editor.is-readonly {
  opacity: 0.7;
}

.code-editor__toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 4px 8px;
  background: var(--el-fill-color);
  border-bottom: 1px solid var(--el-border-color);
}

.code-editor__body {
  display: flex;
  height: 100%;
  overflow: auto;
  position: relative;
}

.code-editor__lines {
  flex-shrink: 0;
  min-width: 40px;
  padding: 8px 0;
  background: var(--el-fill-color);
  border-right: 1px solid var(--el-border-color-lighter);
  user-select: none;
  text-align: right;
  overflow: hidden;
}

.code-editor__line-num {
  padding: 0 8px;
  color: var(--el-text-color-placeholder);
  font-size: 12px;
  line-height: 1.6;
}

.code-editor__highlight {
  position: absolute;
  left: 40px;
  top: 0;
  right: 0;
  bottom: 0;
  padding: 8px 12px;
  pointer-events: none;
  white-space: pre;
  overflow: hidden;
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  color: var(--el-text-color-regular);
}

.code-editor__textarea {
  position: absolute;
  left: 40px;
  top: 0;
  right: 0;
  bottom: 0;
  width: calc(100% - 40px);
  padding: 8px 12px;
  border: none;
  resize: none;
  background: transparent;
  color: transparent;
  caret-color: var(--el-text-color-regular);
  font-family: inherit;
  font-size: inherit;
  line-height: inherit;
  outline: none;
  white-space: pre;
  overflow-wrap: normal;
  tab-size: 2;
  z-index: 1;
}

.code-editor.is-readonly .code-editor__textarea {
  cursor: not-allowed;
  color: transparent;
}

/* 语法高亮样式 */
:deep(.token.keyword) {
  color: #d73a49;
  font-weight: 500;
}

:deep(.token.type) {
  color: #6f42c1;
}

:deep(.token.string) {
  color: #032f62;
}

:deep(.token.number) {
  color: #005cc5;
}

:deep(.token.comment) {
  color: #6a737d;
  font-style: italic;
}

:deep(.token.function) {
  color: #6f42c1;
}
</style>
