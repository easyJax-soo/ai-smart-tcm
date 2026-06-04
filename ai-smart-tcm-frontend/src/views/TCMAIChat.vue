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
    <!-- 移动端遮罩 -->
    <div v-if="drawerOpen" class="drawer-mask" @click="drawerOpen = false" />

    <div class="main">
      <ChatRoom
        :initial-chat-id="currentChatId"
        :show-sidebar-toggle="isMobile"
        :on-send="handleSend"
        :title="title"
        :subtitle="subtitle"
        theme-color="green"
        :theme-icon-comp="ChatDotRound"
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
import { ChatDotRound } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ChatRoom from '@/components/ChatRoom.vue'
import ChatHistorySidebar from '@/components/ChatHistorySidebar.vue'
import { chatWithTCMStream, listChatHistory, deleteChatHistory } from '@/api/ai'
import type { SSECallbacks, ChatHistoryItem } from '@/types/chat'
import { SSEParser } from '@/api/sse'

/** 该应用的基础路径（用于路由跳转） */
const BASE_PATH = '/tcm-chat'

const route = useRoute()
const router = useRouter()

/** 当前路由上的 chatId（可能为空 = 新会话） */
const currentChatId = computed(() => (route.params.chatId as string) || '')

/** 移动端判断 + 抽屉状态 */
const isMobile = ref(false)
const drawerOpen = ref(false)
const handleResize = () => {
  const mobile = window.innerWidth < 768
  isMobile.value = mobile
  if (!mobile) drawerOpen.value = false
}

/** 历史列表 */
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

/** 路由 chatId 变化时刷新侧边栏（保证新建会话后也能显示） */
watch(
  () => route.params.chatId,
  () => {
    if (!drawerOpen.value) loadHistory()
  }
)

/* ============ 路由跳转 ============ */

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

/* ============ ChatRoom 文案与回调 ============ */

const title = 'AI 云中医问诊'
const subtitle = '智能辨证 · 体质调理'
const placeholder = '请描述您的症状，例如：最近总是失眠、容易疲劳…'
const welcomeTitle = '欢迎来到 AI 云中医问诊'
const welcomeMessage = '融合传统中医智慧与现代 AI，模拟中医师问诊流程，辅助您了解自身体质。'

const welcomeTips: string[] = [
  '我最近总是失眠，该怎么办？',
  '湿气重的表现有哪些？',
  '推荐几款适合气虚体质的茶饮',
  '手脚冰凉是阳虚吗？'
]

const handleSend = (
  message: string,
  chatId: string,
  callbacks: SSECallbacks
): SSEParser => {
  return chatWithTCMStream(message, chatId, callbacks)
}

/* ============ 生命周期 ============ */

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
