<script lang="ts" setup>
/**
 * AppLayout - 总入口布局
 *
 * 三栏结构：TopBar（顶栏）+ Sidebar（侧边栏）+ Main（主内容区）。
 * - 侧边栏折叠状态由 useUiStore().sidebarCollapsed 控制
 * - 顶栏显示应用标题 + 语言切换入口 + 用户信息入口（按需扩展）
 * - 主内容区使用 <router-view /> 承载业务页面
 *
 * 业务菜单来源：当前激活模块路由的 children（meta.menu === true）。
 * 各模块 routes 须在父级 meta 声明 `menu: true, title, icon`，
 * 在子路由 meta 声明 `menu: true, order, icon`，AppLayout 自动渲染。
 *
 * 注意：所有模块业务菜单放在各模块的 routes meta 中，
 * AppLayout 不包含业务菜单——这里只提供最简骨架。
 */
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ArrowDown, Expand, Fold } from '@element-plus/icons-vue';
import { useUiStore } from '../stores/useUiStore';
import { useModuleMenu } from '../router';

defineOptions({ name: 'AppLayout' });

const uiStore = useUiStore();
const route = useRoute();
const router = useRouter();

const collapsed = computed({
  get: () => uiStore.sidebarCollapsed,
  set: (v: boolean) => { uiStore.setSidebarCollapsed(v); },
});

const menuItems = computed(() => useModuleMenu(route));

function toggleSidebar() {
  uiStore.toggleSidebar();
}

function handleMenuClick(path: string) {
  // 拼接模块根路径 + 子 path（path 可能是相对的）
  const fullPath = path.startsWith('/') ? path : `/${path}`;
  router.push(fullPath).catch(() => {
    // 重复点击同一路由时 push 会 reject，忽略
  });
}
</script>

<template>
  <el-container class="app-layout h-screen">
    <!-- 顶栏 -->
    <el-header class="app-header" :height="collapsed ? '60px' : '60px'">
      <div class="app-header__left">
        <el-button
          link
          :icon="collapsed ? Expand : Fold"
          @click="toggleSidebar"
        />
        <span class="app-header__title">主数据管理平台</span>
      </div>
      <div class="app-header__right">
        <el-dropdown>
          <span class="app-header__user">
            用户 <el-icon class="el-icon--right"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item disabled>账号：未登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="app-body">
      <!-- 侧边栏 -->
      <el-aside
        class="app-aside"
        :width="collapsed ? '64px' : '220px'"
      >
        <el-menu
          v-if="!collapsed"
          class="app-aside__menu"
          :default-active="route.path"
          @select="handleMenuClick"
        >
          <el-menu-item
            v-for="item in menuItems"
            :key="item.path ?? String(item.name ?? '')"
            :index="item.path ?? String(item.name ?? '')"
          >
            <el-icon v-if="item.icon">
              <component :is="item.icon" />
            </el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
        <div v-else class="app-aside__placeholder-collapsed">
          <span>·</span>
        </div>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="app-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.app-layout {
  width: 100%;
}
.app-header {
  background: #fff;
  border-bottom: 1px solid #ccc;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
}
.app-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.app-header__title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
}
.app-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.app-header__user {
  cursor: pointer;
  color: #606266;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.app-aside {
  background: #fff;
  border-right: 1px solid #ccc;
  transition: width 0.2s;
  overflow: hidden;
}
.app-aside__menu {
  height: 100%;
  border-right: none;
}
.app-aside__placeholder-collapsed {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}
.app-main {
  padding: 0;
  background-color: #f0f2f5;
  overflow: hidden;
}
</style>
