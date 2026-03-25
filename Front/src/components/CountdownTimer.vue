<template>
  <span class="countdown-timer">
    <el-icon><Timer /></el-icon>
    {{ timeText }}
  </span>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  endTime: number
  onEnd?: () => void
}>()

const now = ref(Date.now())
let timer: number | null = null

const diff = computed(() => Math.max(0, props.endTime - now.value))

const timeText = computed(() => {
  const totalSeconds = Math.floor(diff.value / 1000)
  if (totalSeconds <= 0) return '已超时'
  
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})

onMounted(() => {
  timer = window.setInterval(() => {
    now.value = Date.now()
    if (diff.value <= 0) {
      if (timer) clearInterval(timer)
      props.onEnd?.()
    }
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped lang="scss">
.countdown-timer {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}
</style>
