<template>
  <div class="image-upload">
    <div class="upload-list">
      <div v-for="(url, index) in imageList" :key="index" class="upload-item">
        <el-image :src="url" fit="cover" />
        <div class="upload-item-actions">
          <el-icon @click="removeImage(index)"><Delete /></el-icon>
        </div>
      </div>
      <el-upload
        v-if="imageList.length < max"
        class="upload-trigger"
        :show-file-list="false"
        :auto-upload="false"
        :on-change="handleChange"
        :accept="accept"
        multiple
      >
        <el-icon><Plus /></el-icon>
      </el-upload>
    </div>
    <div class="upload-tip">最多上传{{ max }}张图片，支持jpg、png、gif格式，单张不超过5MB</div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { Delete, Plus } from '@element-plus/icons-vue'
import { fileApi } from '@/api/file'

const props = defineProps<{
  modelValue?: string[]
  max?: number
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string[]): void
}>()

const max = props.max || 9
const accept = '.jpg,.jpeg,.png,.gif,.webp'
const imageList = ref<string[]>(props.modelValue || [])

watch(() => props.modelValue, (val) => {
  imageList.value = val || []
})

function removeImage(index: number) {
  imageList.value.splice(index, 1)
  emit('update:modelValue', [...imageList.value])
}

async function handleChange(file: any) {
  const isImage = file.raw?.type.startsWith('image/')
  const isLt5M = file.raw?.size / 1024 / 1024 < 5
  
  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return
  }
  if (!isLt5M) {
    ElMessage.error('单张图片不能超过5MB')
    return
  }
  
  if (imageList.value.length >= max) {
    ElMessage.error(`最多上传${max}张图片`)
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', file.raw)
    const res = await fileApi.upload(formData)
    imageList.value.push((res as any).url ?? '')
    emit('update:modelValue', [...imageList.value])
  } catch (e) {
    ElMessage.error('上传失败')
  }
}
</script>

<style scoped lang="scss">
.image-upload {
  width: 100%;
}

.upload-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.upload-item {
  position: relative;
  width: 100px;
  height: 100px;
  border-radius: 6px;
  overflow: hidden;
  
  .el-image {
    width: 100%;
    height: 100%;
  }
  
  .upload-item-actions {
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: rgba(0, 0, 0, 0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    opacity: 0;
    transition: opacity 0.3s;
    
    .el-icon {
      color: #fff;
      font-size: 20px;
      cursor: pointer;
    }
  }
  
  &:hover .upload-item-actions {
    opacity: 1;
  }
}

.upload-trigger {
  width: 100px;
  height: 100px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: border-color 0.3s;
  
  &:hover {
    border-color: #409eff;
  }
  
  .el-icon {
    font-size: 24px;
    color: #8c939d;
  }
}

.upload-tip {
  margin-top: 8px;
  font-size: 12px;
  color: #909399;
}
</style>
