<script setup lang="ts">
/**
 * ExpressionEditor - 计算表达式编辑器 (§23)
 * 左侧属性列表 + 中间函数区 + 右侧表达式编辑区 + 常用符号栏
 */
import { ref, computed } from 'vue';
import { TpMessage } from '@mdm/common/components/feedback/TpMessage';

defineOptions({ name: 'ExpressionEditor' });

const props = defineProps<{
  modelId: string;
  attributeName?: string;
  /** 初始表达式 */
  initialExpression?: string;
}>();

const emit = defineEmits<{
  confirm: [expression: string];
  close: [];
}>();

// ========== Mock 数据 ==========
// §23.3 属性列表
const attributes = ref([
  { id: 'a1', name: '供应商名称', englishName: 'M_NAME', dataType: 'VARCHAR' },
  { id: 'a2', name: '供应商编码', englishName: 'M_CODE', dataType: 'VARCHAR' },
  { id: 'a3', name: '联系电话', englishName: 'M_PHONE', dataType: 'VARCHAR' },
  { id: 'a4', name: '注册资本', englishName: 'M_CAPITAL', dataType: 'DECIMAL' },
  { id: 'a5', name: '成立日期', englishName: 'M_DATE', dataType: 'DATE' },
  { id: 'a6', name: '数量', englishName: 'M_QTY', dataType: 'INT' },
  { id: 'a7', name: '单价', englishName: 'M_PRICE', dataType: 'DECIMAL' },
]);

// §23.2 函数列表(4类)
interface FuncDef { name: string; category: string; syntax: string; description: string; example: string; }
const functions = ref<FuncDef[]>([
  // 字符函数
  { name: 'CONCAT', category: '字符函数', syntax: 'CONCAT(str1, str2, ...)', description: '连接多个字符串', example: "CONCAT(M_NAME, '-', M_CODE)" },
  { name: 'SUBSTRING', category: '字符函数', syntax: 'SUBSTRING(str, start, length)', description: '截取子字符串', example: 'SUBSTRING(M_CODE, 1, 4)' },
  { name: 'UPPER', category: '字符函数', syntax: 'UPPER(str)', description: '转换为大写', example: 'UPPER(M_NAME)' },
  { name: 'LOWER', category: '字符函数', syntax: 'LOWER(str)', description: '转换为小写', example: 'LOWER(M_NAME)' },
  { name: 'TRIM', category: '字符函数', syntax: 'TRIM(str)', description: '去除首尾空格', example: 'TRIM(M_NAME)' },
  { name: 'LENGTH', category: '字符函数', syntax: 'LENGTH(str)', description: '获取字符串长度', example: 'LENGTH(M_CODE)' },
  // 数值函数
  { name: 'ABS', category: '数值函数', syntax: 'ABS(number)', description: '取绝对值', example: 'ABS(M_CAPITAL)' },
  { name: 'ROUND', category: '数值函数', syntax: 'ROUND(number, decimals)', description: '四舍五入', example: 'ROUND(M_PRICE, 2)' },
  { name: 'CEIL', category: '数值函数', syntax: 'CEIL(number)', description: '向上取整', example: 'CEIL(M_QTY)' },
  { name: 'FLOOR', category: '数值函数', syntax: 'FLOOR(number)', description: '向下取整', example: 'FLOOR(M_PRICE)' },
  { name: 'SUM', category: '数值函数', syntax: 'SUM(a, b, ...)', description: '求和', example: 'SUM(M_QTY * M_PRICE)' },
  // 转换函数
  { name: 'TO_CHAR', category: '转换函数', syntax: 'TO_CHAR(value, format)', description: '转换为字符', example: "TO_CHAR(M_DATE, 'yyyy-MM-dd')" },
  { name: 'TO_NUMBER', category: '转换函数', syntax: 'TO_NUMBER(str)', description: '转换为数字', example: 'TO_NUMBER(M_CODE)' },
  { name: 'TO_DATE', category: '转换函数', syntax: 'TO_DATE(str, format)', description: '转换为日期', example: "TO_DATE('2024-01-01', 'yyyy-MM-dd')" },
  // 逻辑函数
  { name: 'IF', category: '逻辑函数', syntax: 'IF(condition, trueVal, falseVal)', description: '条件判断', example: "IF(M_QTY > 100, '大量', '少量')" },
  { name: 'COALESCE', category: '逻辑函数', syntax: 'COALESCE(val1, val2, ...)', description: '返回第一个非空值', example: "COALESCE(M_PHONE, '无')" },
  { name: 'NULLIF', category: '逻辑函数', syntax: 'NULLIF(val1, val2)', description: '相等返回NULL', example: 'NULLIF(M_CODE, \'\')' },
]);

// 常用符号
const commonSymbols = ['+', '-', '*', '/', '(', ')', '=', '>', '<', '>=', '<=', '<>', 'AND', 'OR', 'NOT', ',', "'"];

// ========== 状态 ==========
const expression = ref(props.initialExpression || '');
const expressionRef = ref<HTMLTextAreaElement | null>(null);
const selectedFuncCategory = ref('字符函数');
const selectedFunc = ref<FuncDef | null>(null);
const attrSearchKeyword = ref('');
const helpDrawerVisible = ref(false);

// 函数分类
const funcCategories = computed(() => {
  const cats = new Set(functions.value.map(f => f.category));
  return Array.from(cats);
});

// 过滤后的函数
const filteredFunctions = computed(() =>
  functions.value.filter(f => f.category === selectedFuncCategory.value),
);

// 过滤后的属性
const filteredAttributes = computed(() => {
  const kw = attrSearchKeyword.value.trim().toLowerCase();
  let list = attributes.value.filter(a => a.dataType !== 'TEXT' && a.dataType !== 'FILE');
  if (kw) {
    list = list.filter(a => a.name.toLowerCase().includes(kw) || a.englishName.toLowerCase().includes(kw));
  }
  return list;
});

// ========== 操作 ==========
// §23.4 双击函数添加到表达式
const handleFuncDblClick = (func: FuncDef) => {
  expression.value += func.name + '()';
  TpMessage.success(`已添加函数: ${func.name}`);
};

// §23.4 双击属性添加到表达式
const handleAttrDblClick = (attr: typeof attributes.value[0]) => {
  expression.value += attr.englishName;
  TpMessage.success(`已添加字段: ${attr.name}`);
};

// 添加符号
const handleSymbolClick = (symbol: string) => {
  expression.value += symbol;
};

// 单击函数显示详情
const handleFuncClick = (func: FuncDef) => {
  selectedFunc.value = func;
};

// §23.5 语法校验
const syntaxResult = ref<'pass' | 'fail' | null>(null);
const handleSyntaxCheck = () => {
  if (!expression.value.trim()) {
    syntaxResult.value = 'fail';
    TpMessage.warning('表达式不能为空');
    return;
  }
  // Mock: 简单检查括号匹配
  let count = 0;
  for (const ch of expression.value) {
    if (ch === '(') count++;
    if (ch === ')') count--;
    if (count < 0) {
      syntaxResult.value = 'fail';
      TpMessage.error('语法错误：括号不匹配');
      return;
    }
  }
  if (count !== 0) {
    syntaxResult.value = 'fail';
    TpMessage.error('语法错误：括号不匹配');
    return;
  }
  syntaxResult.value = 'pass';
  TpMessage.success('语法校验通过');
};

// 确认
const handleConfirm = () => {
  if (syntaxResult.value !== 'pass' && expression.value.trim()) {
    handleSyntaxCheck();
    if (syntaxResult.value !== 'pass') return;
  }
  emit('confirm', expression.value);
};
</script>

<template>
  <div class="expression-editor flex flex-col h-full">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between px-4 py-3 border-b border-[var(--el-border-color-lighter)]">
      <div>
        <span class="text-sm font-medium">计算表达式配置</span>
        <span class="text-xs text-[var(--el-text-color-secondary)] ml-2">属性：{{ attributeName || '未命名' }}</span>
      </div>
      <div class="flex gap-2">
        <el-button size="small" @click="helpDrawerVisible = true">帮助说明</el-button>
      </div>
    </div>

    <!-- §23.1 主体三列布局 -->
    <div class="flex-1 flex min-h-0">
      <!-- §23.3 左侧属性列表 -->
      <div class="w-56 border-r border-[var(--el-border-color-lighter)] flex flex-col">
        <div class="p-2 border-b border-[var(--el-border-color-lighter)]">
          <div class="text-xs font-medium mb-1">属性列表</div>
          <el-input v-model="attrSearchKeyword" size="small" placeholder="搜索属性" clearable />
        </div>
        <div class="flex-1 overflow-auto p-2">
          <div class="text-xs text-[var(--el-text-color-secondary)] mb-1">模型属性</div>
          <div
            v-for="attr in filteredAttributes"
            :key="attr.id"
            class="attr-item px-2 py-1 text-xs rounded cursor-pointer hover:bg-[var(--el-fill-color-light)] mb-1"
            :title="`双击添加到表达式`"
            @dblclick="handleAttrDblClick(attr)"
          >
            <span class="font-medium">{{ attr.name }}</span>
            <span class="text-[var(--el-text-color-placeholder)] ml-1">({{ attr.englishName }})</span>
          </div>
        </div>
      </div>

      <!-- §23.2 中间函数区 -->
      <div class="w-56 border-r border-[var(--el-border-color-lighter)] flex flex-col">
        <div class="p-2 border-b border-[var(--el-border-color-lighter)]">
          <div class="text-xs font-medium mb-1">函数列表</div>
          <el-radio-group v-model="selectedFuncCategory" size="small">
            <el-radio-button v-for="cat in funcCategories" :key="cat" :value="cat">
              {{ cat }}
            </el-radio-button>
          </el-radio-group>
        </div>
        <div class="flex-1 overflow-auto p-2">
          <div
            v-for="func in filteredFunctions"
            :key="func.name"
            class="func-item px-2 py-1.5 text-xs rounded cursor-pointer mb-1"
            :class="selectedFunc?.name === func.name ? 'bg-[var(--el-color-primary-light-8)]' : 'hover:bg-[var(--el-fill-color-light)]'"
            @click="handleFuncClick(func)"
            @dblclick="handleFuncDblClick(func)"
          >
            <div class="font-medium">{{ func.name }}</div>
            <div class="text-[var(--el-text-color-placeholder)] truncate">{{ func.description }}</div>
          </div>
        </div>

        <!-- 函数详情 -->
        <div v-if="selectedFunc" class="p-2 border-t border-[var(--el-border-color-lighter)] bg-[var(--el-fill-color-lighter)]">
          <div class="text-xs space-y-1">
            <div><span class="font-medium">语法：</span>{{ selectedFunc.syntax }}</div>
            <div><span class="font-medium">说明：</span>{{ selectedFunc.description }}</div>
            <div><span class="font-medium">示例：</span><code class="text-[var(--el-color-primary)]">{{ selectedFunc.example }}</code></div>
          </div>
        </div>
      </div>

      <!-- §23.4 右侧表达式编辑区 -->
      <div class="flex-1 flex flex-col">
        <!-- 常用符号栏 -->
        <div class="p-2 border-b border-[var(--el-border-color-lighter)]">
          <div class="text-xs text-[var(--el-text-color-secondary)] mb-1">常用符号（点击添加）</div>
          <div class="flex flex-wrap gap-1">
            <el-button
              v-for="sym in commonSymbols"
              :key="sym"
              size="small"
              class="min-w-0 px-2"
              @click="handleSymbolClick(sym)"
            >
              {{ sym }}
            </el-button>
          </div>
        </div>

        <!-- 表达式输入框 -->
        <div class="flex-1 p-3">
          <div class="text-xs text-[var(--el-text-color-secondary)] mb-1">
            表达式编辑区（双击函数/字段添加，支持手动输入，常量用单引号标记）
          </div>
          <el-input
            ref="expressionRef"
            v-model="expression"
            type="textarea"
            :rows="8"
            placeholder="请输入或双击左侧属性/中间函数添加表达式..."
            class="expression-input"
          />

          <!-- §23.5 校验结果 -->
          <div v-if="syntaxResult" class="mt-2">
            <el-tag v-if="syntaxResult === 'pass'" type="success" size="small">语法校验通过</el-tag>
            <el-tag v-else type="danger" size="small">语法校验失败</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部操作栏 -->
    <div class="px-4 py-3 border-t border-[var(--el-border-color-lighter)] flex items-center justify-between">
      <el-button @click="handleSyntaxCheck">语法检查</el-button>
      <div class="flex gap-2">
        <el-button @click="emit('close')">取消</el-button>
        <el-button type="primary" @click="handleConfirm">确认</el-button>
      </div>
    </div>

    <!-- §23.8 帮助说明抽屉 -->
    <el-drawer v-model="helpDrawerVisible" title="计算表达式帮助" size="450px">
      <div class="text-sm space-y-4">
        <div>
          <h4 class="font-medium mb-1">基本规则</h4>
          <ul class="list-disc list-inside text-[var(--el-text-color-secondary)] text-xs space-y-1">
            <li>双击左侧属性或中间函数可快速添加到表达式</li>
            <li>常量使用单引号标记，如 'hello'</li>
            <li>支持四则运算 + - * / 和比较运算 = > < >= <= <></li>
            <li>逻辑运算使用 AND / OR / NOT</li>
          </ul>
        </div>
        <div>
          <h4 class="font-medium mb-1">函数分类</h4>
          <ul class="list-disc list-inside text-[var(--el-text-color-secondary)] text-xs space-y-1">
            <li>字符函数：CONCAT, SUBSTRING, UPPER, LOWER, TRIM, LENGTH</li>
            <li>数值函数：ABS, ROUND, CEIL, FLOOR, SUM</li>
            <li>转换函数：TO_CHAR, TO_NUMBER, TO_DATE</li>
            <li>逻辑函数：IF, COALESCE, NULLIF</li>
          </ul>
        </div>
        <div>
          <h4 class="font-medium mb-1">示例</h4>
          <div class="bg-[var(--el-fill-color-light)] rounded p-2 text-xs font-mono">
            <div>CONCAT(M_NAME, '-', UPPER(M_CODE))</div>
            <div>IF(M_QTY > 100, M_PRICE * 0.9, M_PRICE)</div>
            <div>ROUND(M_CAPITAL / 10000, 2)</div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.expression-editor {
  background: var(--el-bg-color);
}

.attr-item,
.func-item {
  transition: background-color 0.15s;
  user-select: none;
}

.expression-input :deep(textarea) {
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
}
</style>
