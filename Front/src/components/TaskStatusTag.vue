<template>
  <span class="status-tag" :class="`status-${status}`">
    <span class="status-dot"></span>
    {{ statusName }}
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TASK_STATUS_MAP } from '@/utils/constants'

const props = defineProps<{
  status: number
}>()

const statusName = computed(() => TASK_STATUS_MAP[props.status] || '未知')
</script>

<style scoped lang="scss">
$accent-red: #ef5350;
$accent-blue: #5b9ef5;
$accent-orange: #ffb74d;
$accent-green: #81c784;
$text-secondary: #a8adb5;

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 12px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  backdrop-filter: blur(10px);
  
  .status-dot {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: currentColor;
    animation: pulse 2s ease-in-out infinite;
  }
  
  // 0: 待审核 - 红色警示
  &.status-0 {
    background: linear-gradient(135deg, rgba($accent-red, 0.2), rgba(#ff7043, 0.2));
    color: $accent-red;
    border: 1px solid rgba($accent-red, 0.3);
  }

  // 1: 待接单 - 蓝色
  &.status-1 {
    background: linear-gradient(135deg, rgba($accent-blue, 0.2), rgba(#42a5f5, 0.2));
    color: $accent-blue;
    border: 1px solid rgba($accent-blue, 0.3);
  }

  // 2: 已抢单待协商 - 蓝色
  &.status-2 {
    background: linear-gradient(135deg, rgba($accent-blue, 0.2), rgba(#42a5f5, 0.2));
    color: $accent-blue;
    border: 1px solid rgba($accent-blue, 0.3);
  }

  // 3: 待支付 - 橙色警示
  &.status-3 {
    background: linear-gradient(135deg, rgba($accent-orange, 0.2), rgba(#ffa726, 0.2));
    color: $accent-orange;
    border: 1px solid rgba($accent-orange, 0.3);
  }

  // 4: 进行中 5: 已完成 6: 已结算 - 绿色
  &.status-4,
  &.status-5,
  &.status-6 {
    background: linear-gradient(135deg, rgba($accent-green, 0.2), rgba(#66bb6a, 0.2));
    color: $accent-green;
    border: 1px solid rgba($accent-green, 0.3);
  }

  // 7: 已互评 8: 已取消 - 灰色
  &.status-7,
  &.status-8 {
    background: rgba($text-secondary, 0.1);
    color: $text-secondary;
    border: 1px solid rgba($text-secondary, 0.2);

    .status-dot {
      animation: none;
    }
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.5;
    transform: scale(0.8);
  }
}
</style>
