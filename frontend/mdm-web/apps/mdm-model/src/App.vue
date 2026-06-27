<script setup lang="ts">
/**
 * mdm-model 根组件
 *
 * 仅承载 <router-view />，由各模块 routes 通过父级 `Layout`
 * 组件（@mdm/core/layout/AppLayout）注入 AppLayout 骨架。
 * 不在这里再包一层 AppLayout，避免双层布局。
 */
defineOptions({ name: 'App' });
</script>

<template>
  <router-view />
</template>
