<template>
  <el-avatar :size="size" :src="src || defaultAvatar">
    {{ initials }}
  </el-avatar>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  src?: string
  size?: number | 'large' | 'default' | 'small'
  name?: string
}>()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const initials = computed(() => {
  if (!props.name) return '?'
  return props.name.charAt(0).toUpperCase()
})
</script>
