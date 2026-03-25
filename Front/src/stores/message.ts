import { defineStore } from 'pinia'
import { ref } from 'vue'
import { messageApi } from '@/api/message'

export const useMessageStore = defineStore('message', () => {
  const unreadCount = ref(0)

  async function fetchUnreadCount() {
    try {
      const res = await messageApi.getUnreadCount()
      unreadCount.value = res.count ?? 0
    } catch (e) {
      // silently fail
    }
  }

  function decrementUnread() {
    if (unreadCount.value > 0) unreadCount.value--
  }

  function clearUnread() {
    unreadCount.value = 0
  }

  return { unreadCount, fetchUnreadCount, decrementUnread, clearUnread }
})
