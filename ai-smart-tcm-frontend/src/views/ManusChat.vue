<template>
  <div class="chat-page">
    <ChatHistorySidebar
      class="sidebar"
      :class="{ 'is-open': drawerOpen }"
      :history="history"
      :current-chat-id="currentChatId"
      :loading="loadingHistory"
      @select="onSelectChat"
      @new="onNewChat"
      @delete="onDeleteChat"
    />
    <div v-if="drawerOpen" class="drawer-mask" @click="drawerOpen = false" />

    <div class="main">
      <ChatRoom
        :initial-chat-id="currentChatId"
        :show-sidebar-toggle="isMobile"
        :on-send="handleSend"
        :title="title"
        :subtitle="subtitle"
        theme-color="blue"
        :theme-icon-comp="Cpu"
        :placeholder="placeholder"
        :welcome-title="welcomeTitle"
        :welcome-message="welcomeMessage"
        :welcome-tips="welcomeTips"
        @toggle-sidebar="drawerOpen = true"
        @request-new-session="onNewChat"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Cpu } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatRoom from '@/components/ChatRoom.vue'
import ChatHistorySidebar from '@/components/ChatHistorySidebar.vue'
import { chatWithManusStream, listChatHistory, deleteChatHistory } from '@/api/ai'
import type { SSECallbacks, ChatHistoryItem } from '@/types/chat'
import { SSEParser } from '@/api/sse'

/** 该应用的基础路径 */
const BASE_PATH = '/manus-chat'

const route = useRoute()
const router = useRouter()

const currentChatId = computed(() => (route.params.chatId as string) || '')

const isMobile = ref(false)
const drawerOpen = ref(false)
const handleResize = () => {
  const mobile = window.innerWidth < 768
  isMobile.value = mobile
  if (!mobile) drawerOpen.value = false
}

const history = ref<ChatHistoryItem[]>([])
const loadingHistory = ref(false)
const loadHistory = async () => {
  loadingHistory.value = true
  try {
    history.value = await listChatHistory()
  } catch {
    // 拦截器已提示
  } finally {
    loadingHistory.value = false
  }
}

watch(
  () => route.params.chatId,
  () => {
    if (!drawerOpen.value) loadHistory()
  }
)

const onSelectChat = (chatId: string) => {
  router.push(`${BASE_PATH}/${chatId}`)
  drawerOpen.value = false
}

const onNewChat = () => {
  router.push(BASE_PATH)
  drawerOpen.value = false
}

const onDeleteChat = async (chatId: string) => {
  try {
    await ElMessageBox.confirm(
      '删除后无法恢复，确定要删除这条对话吗？',
      '删除确认',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }
  try {
    await deleteChatHistory(chatId)
    ElMessage.success('已删除')
    if (currentChatId.value === chatId) {
      router.push(BASE_PATH)
    }
    await loadHistory()
  } catch {
    // 错误已弹
  }
}

const title = 'AI 超级智能体'
const subtitle = '多工具协同 · 复杂任务规划'
const placeholder = '向超级智能体提出您的需求，例如：帮我制定一份北京三日游攻略'
const welcomeTitle = '欢迎使用 AI 超级智能体'
const welcomeMessage = '通用超级智能体，支持工具调用与多步推理，应对各类业务问题。'

const welcomeTips: string[] = [
  '帮我制定一份北京三日游攻略',
  '请用 Python 写一个快速排序',
  '分析一下当前 A 股市场的主流热点',
  '写一份产品需求文档：在线预约挂号系统'
]

const handleSend = (
  _message: string,
  _chatId: string,
  callbacks: SSECallbacks
): SSEParser => {
  // Manus 接口当前不读 chatId，但保留 SSE 协议一致
  return chatWithManusStream(_message, callbacks)
}

onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  loadHistory()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #f5f7fa;
}
.sidebar {
  flex-shrink: 0;
}
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.drawer-mask {
  display: none;
}
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    width: 85%;
    max-width: 320px;
    z-index: 200;
    transform: translateX(-100%);
    transition: transform 0.25s ease;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.1);
  }
  .sidebar.is-open {
    transform: translateX(0);
  }
  .drawer-mask {
    display: block;
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.4);
    z-index: 150;
  }
}
</style>
