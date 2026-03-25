<template>
  <div class="chat-panel">
    <div class="chat-header">
      <span class="chat-title">协商聊天</span>
      <span class="chat-status" :class="connected ? 'online' : 'offline'">
        {{ connected ? '已连接' : connecting ? '连接中...' : '未连接' }}
      </span>
    </div>

    <div ref="msgListRef" class="msg-list">
      <div v-if="messages.length === 0" class="empty-tip">暂无消息，发送消息开始协商</div>
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="msg-item"
        :class="msg.senderId === String(userInfo?.id) ? 'self' : 'other'"
      >
        <UserAvatar
          v-if="msg.senderId !== String(userInfo?.id)"
          :src="msg.senderAvatar"
          :name="msg.senderNickname"
          :size="32"
          class="avatar"
        />
        <div class="bubble-wrap">
          <span v-if="msg.senderId !== String(userInfo?.id)" class="sender-name">
            {{ msg.senderNickname }}
          </span>
          <div class="bubble">
            <div v-if="msg.content" class="msg-text">{{ msg.content }}</div>
            <div v-if="msg.images?.length" class="msg-images">
              <el-image
                v-for="(img, idx) in msg.images"
                :key="idx"
                :src="img"
                :preview-src-list="msg.images"
                fit="cover"
                style="width: 120px; height: 120px; border-radius: 6px; margin-right: 6px; margin-bottom: 6px"
              />
            </div>
          </div>
          <span class="time">{{ formatTime(msg.createdAt) }}</span>
        </div>
        <UserAvatar
          v-if="msg.senderId === String(userInfo?.id)"
          :src="msg.senderAvatar"
          :name="msg.senderNickname"
          :size="32"
          class="avatar"
        />
      </div>
    </div>

    <div class="input-area">
      <div class="input-wrapper">
        <el-input
          v-model="inputText"
          placeholder="输入消息，Enter 发送"
          :disabled="!connected"
          @keydown.enter.prevent="send"
          maxlength="1000"
          show-word-limit
        />
        <el-upload
          class="image-upload-btn"
          :show-file-list="false"
          :auto-upload="false"
          :on-change="handleImageChange"
          accept=".jpg,.jpeg,.png,.gif,.webp"
          multiple
        >
          <el-icon title="上传图片"><Picture /></el-icon>
        </el-upload>
      </div>
      <div v-if="uploadingImages.length > 0" class="uploading-images">
        <div v-for="(img, idx) in uploadingImages" :key="idx" class="uploading-item">
          <el-image :src="img" fit="cover" />
          <el-icon class="remove-icon" @click="removeUploadingImage(idx)"><Close /></el-icon>
        </div>
      </div>
      <el-button
        type="primary"
        :disabled="!connected || (!inputText.trim() && uploadingImages.length === 0)"
        :loading="sending"
        @click="send"
      >
        发送
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Close } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { useChat } from '@/composables/useChat'
import { formatTime } from '@/utils/format'
import { fileApi } from '@/api/file'
import UserAvatar from '@/components/UserAvatar.vue'

const props = defineProps<{ grabRecordId: string }>()

const userStore = useUserStore()
const userInfo = userStore.userInfo

const { messages, connected, connecting, loadHistory, connect, sendMessage } = useChat(props.grabRecordId)

const inputText = ref('')
const uploadingImages = ref<string[]>([])
const sending = ref(false)
const msgListRef = ref<HTMLElement | null>(null)

function scrollToBottom() {
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}

function send() {
  const content = inputText.value.trim()
  if (!content && uploadingImages.value.length === 0) return
  
  sending.value = true
  if (sendMessage(content, uploadingImages.value)) {
    inputText.value = ''
    uploadingImages.value = []
  }
  sending.value = false
}

async function handleImageChange(file: any) {
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
  if (uploadingImages.value.length >= 5) {
    ElMessage.error('最多上传5张图片')
    return
  }
  
  try {
    const formData = new FormData()
    formData.append('file', file.raw)
    const res = await fileApi.upload(formData)
    uploadingImages.value.push((res as any).url ?? '')
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

function removeUploadingImage(idx: number) {
  uploadingImages.value.splice(idx, 1)
}

watch(() => messages.value.length, scrollToBottom)

onMounted(async () => {
  await loadHistory()
  connect()
  scrollToBottom()
})
</script>

<style scoped lang="scss">
$dark-bg:     #1a1f2e;
$card-bg:     #252d3d;
$border-dark: #3a4556;
$text-primary: #e8eaed;
$text-secondary: #a8adb5;
$accent-blue: #5b9ef5;
$accent-green: #81c784;
$self-bubble: #1e3a5f;
$other-bubble: #2a3347;

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 480px;
  background: rgba($card-bg, 0.5);
  border: 1px solid $border-dark;
  border-radius: 12px;
  overflow: hidden;
  margin-top: 24px;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  background: rgba(#000, 0.2);
  border-bottom: 1px solid $border-dark;

  .chat-title {
    font-size: 14px;
    font-weight: 700;
    color: $text-primary;
    letter-spacing: 1px;
    text-transform: uppercase;
  }

  .chat-status {
    font-size: 12px;
    font-weight: 600;
    padding: 3px 10px;
    border-radius: 20px;

    &.online {
      background: rgba($accent-green, 0.15);
      color: $accent-green;
      border: 1px solid rgba($accent-green, 0.3);
    }

    &.offline {
      background: rgba($text-secondary, 0.1);
      color: $text-secondary;
      border: 1px solid rgba($text-secondary, 0.2);
    }
  }
}

.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;

  &::-webkit-scrollbar {
    width: 4px;
  }
  &::-webkit-scrollbar-track { background: transparent; }
  &::-webkit-scrollbar-thumb {
    background: rgba($border-dark, 0.6);
    border-radius: 2px;
  }

  .empty-tip {
    text-align: center;
    color: $text-secondary;
    font-size: 13px;
    margin-top: 40px;
  }
}

.msg-item {
  display: flex;
  align-items: flex-end;
  gap: 10px;

  &.self {
    flex-direction: row-reverse;

    .bubble-wrap { align-items: flex-end; }

    .bubble {
      background: linear-gradient(135deg, $self-bubble, darken($self-bubble, 5%));
      border: 1px solid rgba($accent-blue, 0.3);
      color: $text-primary;
    }
  }

  &.other {
    .bubble {
      background: $other-bubble;
      border: 1px solid $border-dark;
      color: $text-primary;
    }
  }

  .avatar {
    flex-shrink: 0;
  }

  .bubble-wrap {
    display: flex;
    flex-direction: column;
    gap: 4px;
    max-width: 65%;

    .sender-name {
      font-size: 11px;
      color: $text-secondary;
      padding-left: 4px;
    }

    .bubble {
      padding: 10px 14px;
      border-radius: 12px;
      font-size: 14px;
      line-height: 1.6;
      word-break: break-word;

      .msg-text { margin-bottom: 6px; }
      .msg-images { display: flex; flex-wrap: wrap; gap: 6px; }
    }

    .time {
      font-size: 10px;
      color: $text-secondary;
      padding: 0 4px;
    }
  }
}

.input-area {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid $border-dark;
  background: rgba(#000, 0.15);

  .input-wrapper {
    display: flex;
    gap: 8px;
    align-items: center;

    :deep(.el-input) {
      flex: 1;

      .el-input__wrapper {
        background: rgba($dark-bg, 0.6);
        border-color: $border-dark;
        box-shadow: none;

        &:hover, &.is-focus {
          border-color: $accent-blue;
        }
      }

      .el-input__inner {
        color: $text-primary;
        font-size: 14px;
      }
    }

    .image-upload-btn {
      :deep(.el-upload) {
        cursor: pointer;
        .el-icon {
          font-size: 18px;
          color: $text-secondary;
          transition: color 0.2s;

          &:hover { color: $accent-blue; }
        }
      }
    }
  }

  .uploading-images {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;

    .uploading-item {
      position: relative;
      width: 60px;
      height: 60px;
      border-radius: 6px;
      overflow: hidden;

      :deep(.el-image) { width: 100%; height: 100%; }

      .remove-icon {
        position: absolute;
        top: 2px;
        right: 2px;
        background: rgba(#000, 0.6);
        color: #fff;
        border-radius: 50%;
        padding: 2px;
        cursor: pointer;
        font-size: 12px;
      }
    }
  }

  :deep(.el-button) {
    border-radius: 8px;
    font-weight: 600;
    min-width: 72px;
  }
}

body.theme-light {
  .chat-panel {
    background: rgba(#ffffff, 0.78);
    border-color: #e5e7eb;
  }

  .chat-header {
    background: rgba(255, 255, 255, 0.65);
    border-bottom: 1px solid #e5e7eb;
  }

  .msg-list {
    .empty-tip {
      color: #6b7280;
    }
  }

  .msg-item.other .bubble {
    background: #f3f4f6;
    border: 1px solid #e5e7eb;
    color: #111827;
  }

  .msg-item.self .bubble {
    background: #dbeafe;
    border: 1px solid rgba(91, 158, 245, 0.45);
    color: #0f172a;
  }

  .bubble-wrap .time {
    color: #6b7280;
  }

  .input-area {
    border-top: 1px solid #e5e7eb;
    background: rgba(0, 0, 0, 0.03);
  }
}

</style>
