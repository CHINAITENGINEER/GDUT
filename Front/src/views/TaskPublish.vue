<template>
  <div class="task-publish-page">
    <div class="page-header">
      <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
      <h2>发布任务</h2>
    </div>

    <el-card>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="任务标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入任务标题" maxlength="50" show-word-limit />
        </el-form-item>

        <el-form-item label="任务分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 220px">
            <el-option label="学习辅导" :value="1" />
            <el-option label="跑腿代购" :value="2" />
            <el-option label="技能服务" :value="3" />
            <el-option label="设计创作" :value="4" />
            <el-option label="其他" :value="5" />
          </el-select>
        </el-form-item>

        <el-form-item label="任务描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="5"
            placeholder="请详细描述任务要求"
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="任务金额（元）" prop="amount">
              <el-input-number v-model="form.amount" :min="1" :max="9999" :precision="2" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="截止时间" prop="deadline">
              <el-date-picker
                v-model="form.deadline"
                type="datetime"
                placeholder="请选择截止时间"
                value-format="x"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="交付方式" prop="deliveryType">
          <el-radio-group v-model="form.deliveryType">
            <el-radio :value="0">线上交付</el-radio>
            <el-radio :value="1">线下交付</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="任务图片">
          <ImageUpload v-model="form.taskImages" :max="6" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" @click="handleSubmit">发布任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { taskApi, type TaskPublishDTO } from '@/api/task'
import ImageUpload from '@/components/ImageUpload.vue'

const router = useRouter()
const loading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<TaskPublishDTO>({
  title: '',
  category: 1,
  description: '',
  amount: 1,
  deliveryType: 0,
  deadline: Date.now() + 24 * 60 * 60 * 1000,
  taskImages: []
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入任务标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择任务分类', trigger: 'change' }],
  description: [{ required: true, message: '请输入任务描述', trigger: 'blur' }],
  amount: [{ required: true, message: '请输入任务金额', trigger: 'change' }],
  deadline: [{ required: true, message: '请选择截止时间', trigger: 'change' }],
  deliveryType: [{ required: true, message: '请选择交付方式', trigger: 'change' }]
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (Number(form.deadline) <= Date.now()) {
    ElMessage.warning('截止时间必须晚于当前时间')
    return
  }

  loading.value = true
  try {
    await taskApi.publish({
      ...form,
      deadline: Number(form.deadline)
    })
    ElMessage.success('任务发布成功')
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  }
  loading.value = false
}
</script>

<style scoped lang="scss">
.task-publish-page {
  max-width: 860px;
  margin: 0 auto;
  padding: 20px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;

  h2 {
    flex: 1;
  }
}
</style>
