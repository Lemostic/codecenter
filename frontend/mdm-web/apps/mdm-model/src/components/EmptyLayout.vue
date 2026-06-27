<script setup lang="ts">
/**
 * EmptyLayout - 空白布局（用于嵌入式二级应用）
 *
 * 不包含顶部栏和侧边栏，页面内容直接全屏显示。
 * 用于当外层已经有菜单容器的情况。
 */
defineOptions({ name: 'EmptyLayout' });
</script>

<template>
  <div class="empty-layout h-screen w-screen overflow-hidden">
    <router-view />
  </div>
</template>

<style scoped>
.empty-layout {
  background: var(--el-bg-color);
}
</style>
