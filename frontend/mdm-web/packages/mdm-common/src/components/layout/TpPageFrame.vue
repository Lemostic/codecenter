<script setup lang="ts">
/**
 * TpPageFrame —— 主页面外壳
 *
 * 所有顶层路由页面（List/Detail/Editor）必须用它作为根容器，确保
 * 外层 padding、背景、圆角、flex 撑高在全应用范围内完全一致。
 *
 * 嵌入在 Tab/Dialog/Drawer 内的子内容不要使用本组件（会形成卡片嵌套）。
 *
 * 详见 docs/frontend_ai_coding_rules.md §14.14。
 */
defineOptions({ name: 'TpPageFrame' });
</script>

<template>
  <div class="tp-page-frame flex flex-col h-full bg-white rounded-lg overflow-hidden">
    <slot />
  </div>
</template>
