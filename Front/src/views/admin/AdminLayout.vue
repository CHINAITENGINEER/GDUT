<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <span>🛡️ 管理后台</span>
      </div>
      <nav class="sidebar-nav">
        <router-link
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          active-class="nav-item--active"
        >
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>
      <div class="sidebar-footer">
        <router-link to="/" class="nav-item">
          <el-icon :size="18"><HomeFilled /></el-icon>
          <span>返回前台</span>
        </router-link>
      </div>
    </aside>

    <div class="main-content">
      <header class="topbar">
        <h2 class="page-title">{{ currentTitle }}</h2>
        <div class="topbar-right">
          <span class="admin-badge">管理员</span>
        </div>
      </header>
      <main class="content-area">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import {
  HomeFilled, DataAnalysis, User, Document,
  Setting, TrendCharts, Warning
} from '@element-plus/icons-vue'

const route = useRoute()

const navItems = [
  { path: '/admin/dashboard', label: '后台总览', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/tasks/pending', label: '任务审核', icon: Document },
  { path: '/admin/fee-config', label: '手续费配置', icon: Setting },
  { path: '/admin/trades', label: '交易统计', icon: TrendCharts },
  { path: '/admin/disputes', label: '争议处理', icon: Warning },
]

const currentTitle = computed(() => route.meta.title as string || '管理后台')
</script>

<style scoped lang="scss">
$sidebar-bg: #111827;
$sidebar-active: #4f46e5;
$sidebar-text: #9ca3af;
$sidebar-text-active: #ffffff;
$border: #e5e7eb;
$topbar-bg: #ffffff;

.admin-layout {
  display: flex;
  min-height: 100vh;
  background: #f9fafb;
}

.sidebar {
  width: 220px;
  flex-shrink: 0;
  background: $sidebar-bg;
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
}

.sidebar-brand {
  padding: 24px 20px;
  font-size: 15px;
  font-weight: 700;
  color: #ffffff;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}

.sidebar-nav {
  flex: 1;
  padding: 12px 0;
  overflow-y: auto;
}

.sidebar-footer {
  padding: 12px 0;
  border-top: 1px solid rgba(255,255,255,0.08);
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 20px;
  color: $sidebar-text;
  text-decoration: none;
  font-size: 14px;
  border-radius: 0;
  transition: all 0.15s;
  cursor: pointer;

  &:hover {
    color: $sidebar-text-active;
    background: rgba(255,255,255,0.06);
  }

  &.nav-item--active {
    color: $sidebar-text-active;
    background: $sidebar-active;
  }
}

.main-content {
  margin-left: 220px;
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  height: 60px;
  background: $topbar-bg;
  border-bottom: 1px solid $border;
  position: sticky;
  top: 0;
  z-index: 50;

  .page-title {
    font-size: 16px;
    font-weight: 700;
    color: #111827;
  }

  .admin-badge {
    font-size: 12px;
    padding: 3px 10px;
    background: #ede9fe;
    color: #4f46e5;
    border-radius: 20px;
    font-weight: 600;
  }
}

.content-area {
  flex: 1;
  padding: 24px 28px;
  overflow: auto;
}
</style>
