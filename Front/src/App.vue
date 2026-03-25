<template>
  <router-view />
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { useTheme } from '@/composables/useTheme'

const userStore = useUserStore()
const messageStore = useMessageStore()

// 只负责初始化主题类名；切换按钮放在具体页面（例如首页顶栏）
useTheme()

onMounted(async () => {
  if (userStore.token) {
    await userStore.getProfile()
    await messageStore.fetchUnreadCount()
  }
})
</script>

<style lang="scss">
#app {
  min-height: 100vh;
  background: var(--app-bg);
  color: var(--app-text-color);
}
</style>
