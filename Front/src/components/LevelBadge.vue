<template>
  <span class="level-badge" :style="{ backgroundColor: color }">
    Lv.{{ level }} {{ levelName }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { LEVEL_TABLE, LEVEL_COLORS } from '@/utils/constants'

const props = defineProps<{
  level: number
  showName?: boolean
}>()

const levelInfo = computed(() => LEVEL_TABLE.find(l => l.level === props.level))
const levelName = computed(() => props.showName ? (levelInfo.value?.levelName || '') : '')
const color = computed(() => LEVEL_COLORS[props.level] || '#909399')
</script>

<style scoped lang="scss">
.level-badge {
  display: inline-block;
  padding: 2px 8px;
  font-size: 12px;
  border-radius: 4px;
  color: #fff;
}
</style>
