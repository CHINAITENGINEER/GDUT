import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { chatApi, type ChatMessageVO } from '@/api/chat'

export function useChat(grabRecordId: string) {
  const userStore = useUserStore()
  const messages = ref<ChatMessageVO[]>([])
  const connected = ref(false)
  const connecting = ref(false)
  let ws: WebSocket | null = null
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null
  let shouldReconnect = true

  async function loadHistory() {
    try {
      const res = await chatApi.history(grabRecordId)
      messages.value = res || []
    } catch (e) {
      console.warn('加载聊天历史失败', e)
    }
  }

  function connect() {
    if (ws && (ws.readyState === WebSocket.OPEN || ws.readyState === WebSocket.CONNECTING)) return
    const token = userStore.token
    if (!token) return

    connecting.value = true
    const protocol = location.protocol === 'https:' ? 'wss' : 'ws'
    const host = (import.meta.env.VITE_WS_HOST || 'localhost:8080') as string
    const url = `${protocol}://${host}/ws/chat/${grabRecordId}?token=${encodeURIComponent(token)}`
    ws = new WebSocket(url)

    ws.onopen = () => {
      connected.value = true
      connecting.value = false
    }

    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        if (data.type === 'message') {
          // 避免重复（重连时历史已加载）
          if (!messages.value.find(m => m.id === data.id)) {
            messages.value.push(data as ChatMessageVO)
          }
        }
      } catch (e) {
        console.warn('解析WS消息失败', e)
      }
    }

    ws.onclose = () => {
      connected.value = false
      connecting.value = false
      if (shouldReconnect) {
        reconnectTimer = setTimeout(connect, 3000)
      }
    }

    ws.onerror = () => {
      connecting.value = false
    }
  }

  function sendMessage(content: string, images: string[] = []) {
    if (!ws || ws.readyState !== WebSocket.OPEN) return false
    ws.send(JSON.stringify({ content, images: images.length > 0 ? images : undefined }))
    return true
  }

  function disconnect() {
    shouldReconnect = false
    if (reconnectTimer) clearTimeout(reconnectTimer)
    ws?.close()
    ws = null
  }

  onUnmounted(disconnect)

  return { messages, connected, connecting, loadHistory, connect, sendMessage, disconnect }
}
