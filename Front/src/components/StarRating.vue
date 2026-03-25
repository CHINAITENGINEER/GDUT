<template>
  <div class="star-rating">
    <el-rate
      v-model="value"
      :disabled="disabled"
      :show-score="showScore"
      :colors="['#99A9BF', '#F7BA2A', '#FF9900']"
      @change="onChange"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{
  modelValue?: number
  disabled?: boolean
  showScore?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: number): void
  (e: 'change', value: number): void
}>()

const value = ref(props.modelValue || 0)

watch(() => props.modelValue, (val) => {
  value.value = val || 0
})

function onChange(val: number) {
  emit('update:modelValue', val)
  emit('change', val)
}
</script>

<style scoped lang="scss">
.star-rating {
  display: inline-block;
}
</style>
