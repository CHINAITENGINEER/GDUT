<template>
  <span class="category-tag" :class="`category-${category}`">
    <span class="category-icon">{{ categoryIcon }}</span>
    {{ categoryName }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { CATEGORY_MAP } from '@/utils/constants'

const props = defineProps<{
  category: number
}>()

const categoryName = computed(() => CATEGORY_MAP[props.category] || '其他')

const iconMap: Record<number, string> = {
  1: '📦',
  2: '📝',
  3: '💻',
  4: '🎓',
  5: '🔧'
}

const categoryIcon = computed(() => iconMap[props.category] || '🔧')
</script>

<style scoped lang="scss">
$accent-blue: #5b9ef5;
$accent-green: #81c784;
$accent-orange: #ffb74d;
$accent-purple: #b39ddb;
$accent-cyan: #4dd0e1;

.category-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  transition: all 0.2s ease;
  backdrop-filter: blur(10px);
  
  .category-icon {
    font-size: 14px;
    line-height: 1;
  }
  
  &.category-1 {
    background: linear-gradient(135deg, rgba($accent-blue, 0.2), rgba($accent-cyan, 0.2));
    color: $accent-blue;
    border: 1px solid rgba($accent-blue, 0.3);
  }
  
  &.category-2 {
    background: linear-gradient(135deg, rgba($accent-green, 0.2), rgba(#a5d6a7, 0.2));
    color: $accent-green;
    border: 1px solid rgba($accent-green, 0.3);
  }
  
  &.category-3 {
    background: linear-gradient(135deg, rgba($accent-orange, 0.2), rgba(#ffcc80, 0.2));
    color: $accent-orange;
    border: 1px solid rgba($accent-orange, 0.3);
  }
  
  &.category-4 {
    background: linear-gradient(135deg, rgba($accent-purple, 0.2), rgba(#ce93d8, 0.2));
    color: $accent-purple;
    border: 1px solid rgba($accent-purple, 0.3);
  }
  
  &.category-5 {
    background: linear-gradient(135deg, rgba(#90a4ae, 0.2), rgba(#b0bec5, 0.2));
    color: #90a4ae;
    border: 1px solid rgba(#90a4ae, 0.3);
  }
}
</style>
