<template>
  <div class="chat-room" :class="`theme-${themeColor}`">
    <!-- ============ 顶部导航栏 ============ -->
    <header class="chat-header">
      <div class="header-left">
        <el-button
          v-if="showSidebarToggle"
          text
          class="menu-btn"
          @click="$emit('toggleSidebar')"
        >
          <el-icon :size="20"><Menu /></el-icon>
        </el-button>
        <el-button text class="back-btn" @click="goBack">
          <el-icon :size="20"><ArrowLeft /></el-icon>
        </el-button>
        <div class="header-icon">
          <el-icon :size="22" color="#fff">
            <component :is="themeIconComp" />
          </el-icon>
        </div>
        <div class="header-text">
          <div class="title">{{ title }}</div>
          <div class="subtitle">{{ subtitle }}</div>
        </div>
      </div>
      <div class="header-right">
        <el-tag
          :type="isStreaming ? 'warning' : 'success'"
          size="small"
          effect="dark"
          round
        >
          {{ isStreaming ? 'AI 思考中…' : '在线' }}
        </el-tag>
        <el-button text class="new-session-btn" @click="onNewSession">
          <el-icon><Refresh /></el-icon>
          <span>新会话</span>
        </el-button>
      </div>
    </header>

    <!-- ============ 消息列表 ============ -->
    <main class="chat-body" ref="bodyRef">
      <div class="chat-body-inner">
        <!-- 欢迎语卡片 -->
        <div v-if="messages.length === 0" class="welcome-card">
          <div class="welcome-icon">
            <el-icon :size="48" :color="iconColor">
              <component :is="themeIconComp" />
            </el-icon>
          </div>
          <h3>{{ welcomeTitle }}</h3>
          <p>{{ welcomeMessage }}</p>
          <div class="welcome-tips">
            <div class="tip" v-for="tip in welcomeTips" :key="tip" @click="fillInput(tip)">
              {{ tip }}
            </div>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="msg in messages"
          :key="msg.id"
          class="message-row"
          :class="msg.role === 'user' ? 'row-user' : 'row-ai'"
        >
          <!-- AI 消息：头像在左 -->
          <template v-if="msg.role === 'ai'">
            <div class="avatar avatar-ai">
              <el-icon :size="20" color="#fff">
                <component :is="themeIconComp" />
              </el-icon>
            </div>
            <div class="bubble bubble-ai">
              <div class="bubble-content message-content">
                <span v-if="msg.content">{{ msg.content }}</span>
                <span v-else class="loading-dots">
                  <span></span><span></span><span></span>
                </span>
                <span v-if="msg.status === 'pending'" class="typing-cursor">▍</span>
              </div>
              <div v-if="msg.status === 'error'" class="error-text">
                <el-icon><CircleClose /></el-icon>
                <span>消息接收失败</span>
              </div>
            </div>
          </template>

          <!-- 用户消息：头像在右 -->
          <template v-else>
            <div class="bubble bubble-user">
              <div class="bubble-content message-content">{{ msg.content }}</div>
            </div>
            <div class="avatar avatar-user">
              <el-icon :size="20" color="#fff"><User /></el-icon>
            </div>
          </template>
        </div>
      </div>
    </main>

    <!-- ============ 底部输入区 ============ -->
    <footer class="chat-footer">
      <div class="input-wrapper">
        <el-input
          v-model="inputText"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 4 }"
          :placeholder="placeholder"
          :disabled="false"
          resize="none"
          @keydown="handleKeydown"
          class="chat-input"
        />
        <div class="action-buttons">
          <el-button
            v-if="isStreaming"
            type="warning"
            plain
            round
            @click="stopGeneration"
          >
            <el-icon><VideoPause /></el-icon>
            <span>停止</span>
          </el-button>
          <el-button
            v-else
            type="primary"
            :disabled="!canSend"
            round
            @click="sendMessage"
          >
            <el-icon><Promotion /></el-icon>
            <span>发送</span>
          </el-button>
        </div>
      </div>
      <div class="footer-tip">
        <span>会话 ID：{{ chatId }}</span>
        <span>· 内容由 AI 生成，仅供参考</span>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { generateUUID } from '@/utils/uuid'
import { SSEParser } from '@/api/sse'
import { getChatMessages } from '@/api/ai'
import type { ChatMessage, SSECallbacks } from '@/types/chat'

/**
 * Props 类型定义
 */
interface ChatRoomProps {
  /** 页面标题 */
  title: string
  /** 副标题 */
  subtitle?: string
  /** 主题色: green=中医, blue=智能体 */
  themeColor?: 'green' | 'blue'
  /** 主题图标组件 */
  themeIconComp: any
  /** 发送消息回调：返回 SSEParser 实例（用于终止） */
  onSend: (message: string, chatId: string, callbacks: SSECallbacks) => SSEParser
  /** 占位提示 */
  placeholder?: string
  /** 欢迎语标题 */
  welcomeTitle?: string
  /** 欢迎语描述 */
  welcomeMessage?: string
  /** 提示问题列表 */
  welcomeTips?: string[]
  /** 外部传入的 chatId（受控模式）。为空则首次挂载时自动生成 */
  initialChatId?: string
  /** 是否显示"菜单"按钮（移动端用） */
  showSidebarToggle?: boolean
}

const props = withDefaults(defineProps<ChatRoomProps>(), {
  subtitle: '',
  themeColor: 'blue',
  placeholder: '请输入消息，按 Enter 发送，Shift + Enter 换行',
  welcomeTitle: '开始对话吧',
  welcomeMessage: '向 AI 发送一条消息，开启你的智能对话',
  welcomeTips: () => [],
  initialChatId: '',
  showSidebarToggle: false
})

/**
 * 事件
 * - toggleSidebar: 移动端点击菜单按钮
 * - requestNewSession: 点击 ChatRoom 内部的"新会话"按钮（父组件负责跳路由）
 */
const emit = defineEmits<{
  toggleSidebar: []
  requestNewSession: []
}>()

const router = useRouter()

/** 主题色对应的 icon 颜色 */
const iconColor = computed(() => (props.themeColor === 'green' ? '#67c23a' : '#409eff'))

/** 当前聊天会话 id（受控：外部传值则用外部的，否则自动生成） */
const chatId = ref<string>(props.initialChatId || generateUUID())

/** 消息列表 */
const messages = ref<ChatMessage[]>([])

/** 输入框文本 */
const inputText = ref('')

/** 当前 SSE 连接 */
let currentParser: SSEParser | null = null

/** 是否正在流式接收 */
const isStreaming = ref(false)

/** 是否正在加载历史 */
const isLoadingHistory = ref(false)

/** 消息区域 DOM */
const bodyRef = ref<HTMLElement | null>(null)

/** 是否可发送 */
const canSend = computed(() => inputText.value.trim().length > 0 && !isStreaming.value)

/** 当前正在流式接收的 AI 消息 id（用于 onChunk 局部更新） */
let activeMessageId = ''

/* ============ 历史会话加载 ============ */

/**
 * 拉取指定 chatId 的历史消息并填充到 messages
 */
const loadHistory = async (id: string) => {
  if (!id) return
  isLoadingHistory.value = true
  try {
    const history = await getChatMessages(id)
    messages.value = history.map((m) => ({
      id: generateUUID(),
      role: m.role,
      content: m.content,
      status: 'done',
      timestamp: Date.now()
    }))
    await nextTick()
    scrollToBottom()
  } catch {
    ElMessage.error('加载历史失败')
    messages.value = []
  } finally {
    isLoadingHistory.value = false
  }
}

/**
 * 切换到全新会话（清空消息 + 新生成 chatId）
 */
const startNewSession = () => {
  // 中断流
  if (currentParser && !currentParser.isCompleted()) {
    currentParser.abort()
  }
  currentParser = null
  isStreaming.value = false
  messages.value = []
  inputText.value = ''
  chatId.value = generateUUID()
}

/** 监听外部传入的 chatId 变化 */
watch(
  () => props.initialChatId,
  (newId, oldId) => {
    if (newId === oldId) return
    if (!newId) {
      // 父组件传空字符串 = 全新会话
      startNewSession()
      return
    }
    if (newId === chatId.value) return
    // 切换到指定会话
    chatId.value = newId
    loadHistory(newId)
  },
  { immediate: false }
)

/* ============ 生命周期 ============ */
onMounted(async () => {
  if (props.initialChatId) {
    // 受控模式：外部指定了 chatId，加载其历史
    await loadHistory(props.initialChatId)
  }
  // 否则 chatId 已在 ref 初始化时自动生成，messages 保持空（显示欢迎页）
})

onBeforeUnmount(() => {
  // 离开页面时主动中断未完成的流
  if (currentParser && !currentParser.isCompleted()) {
    currentParser.abort()
  }
  currentParser = null
})

/* ============ 方法 ============ */

/** 返回上一页 */
const goBack = () => {
  if (isStreaming.value) {
    ElMessageBox.confirm('AI 正在回复中，确定要离开吗？', '提示', {
      type: 'warning',
      confirmButtonText: '确定离开',
      cancelButtonText: '继续等待'
    })
      .then(() => router.back())
      .catch(() => {})
  } else {
    router.back()
  }
}

/** 开启新会话（事件冒泡到父组件，父组件负责跳路由） */
const onNewSession = () => {
  if (messages.value.length === 0) {
    emit('requestNewSession')
    return
  }
  const doReset = () => {
    if (currentParser && !currentParser.isCompleted()) {
      currentParser.abort()
    }
    currentParser = null
    isStreaming.value = false
    messages.value = []
    inputText.value = ''
    emit('requestNewSession')
  }
  if (isStreaming.value) {
    ElMessageBox.confirm('AI 正在回复中，开启新会话将中断当前回复。', '提示', {
      type: 'warning',
      confirmButtonText: '开启新会话',
      cancelButtonText: '取消'
    })
      .then(doReset)
      .catch(() => {})
  } else {
    doReset()
  }
}

/** 滚动到底部 */
const scrollToBottom = async () => {
  await nextTick()
  if (bodyRef.value) {
    bodyRef.value.scrollTo({
      top: bodyRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

/** 发送消息 */
const sendMessage = async () => {
  const text = inputText.value.trim()
  if (!text || isStreaming.value) return

  // 1. 追加用户消息
  const userMsg: ChatMessage = {
    id: generateUUID(),
    role: 'user',
    content: text,
    status: 'done',
    timestamp: Date.now()
  }
  messages.value.push(userMsg)

  // 2. 创建空的 AI 消息占位（关键：先占位再增量更新）
  const aiMsg: ChatMessage = {
    id: generateUUID(),
    role: 'ai',
    content: '',
    status: 'pending',
    timestamp: Date.now()
  }
  messages.value.push(aiMsg)
  activeMessageId = aiMsg.id

  // 3. 清空输入框
  inputText.value = ''

  // 4. 滚动到底部
  scrollToBottom()

  // 5. 启动 SSE
  isStreaming.value = true

  const callbacks: SSECallbacks = {
    onChunk: (chunk: string) => {
      // 关键逻辑：同一个 AI 消息气泡，文本碎片持续追加（打字机效果）
      const target = messages.value.find((m) => m.id === activeMessageId)
      if (target) {
        target.content += chunk
        // 增量滚动到底部，确保用户能看到最新内容
        scrollToBottom()
      }
    },
    onComplete: () => {
      isStreaming.value = false
      const target = messages.value.find((m) => m.id === activeMessageId)
      if (target) {
        target.status = 'done'
      }
      currentParser = null
      scrollToBottom()
    },
    onError: (err: Error) => {
      isStreaming.value = false
      const target = messages.value.find((m) => m.id === activeMessageId)
      if (target) {
        target.status = 'error'
        if (!target.content) {
          target.content = `请求失败：${err.message}`
        }
      }
      currentParser = null
      ElMessage.error(`对话出错：${err.message}`)
    }
  }

  try {
    currentParser = props.onSend(text, chatId.value, callbacks)
  } catch (err: any) {
    isStreaming.value = false
    const target = messages.value.find((m) => m.id === activeMessageId)
    if (target) {
      target.status = 'error'
      target.content = `请求失败：${err?.message || String(err)}`
    }
    ElMessage.error(`发起请求失败：${err?.message || String(err)}`)
  }
}

/** 停止生成 */
const stopGeneration = () => {
  if (currentParser && !currentParser.isCompleted()) {
    currentParser.abort()
  }
  isStreaming.value = false
  const target = messages.value.find((m) => m.id === activeMessageId)
  if (target) {
    target.status = 'done'
  }
  currentParser = null
  ElMessage.info('已停止生成')
}

/** 键盘事件：Enter 发送，Shift+Enter 换行 */
const handleKeydown = (e: Event | KeyboardEvent) => {
  const ke = e as KeyboardEvent
  if (ke.key === 'Enter' && !ke.shiftKey) {
    ke.preventDefault()
    sendMessage()
  }
}

/** 点击提示填充到输入框 */
const fillInput = (text: string) => {
  inputText.value = text
}
</script>

<style scoped>
.chat-room {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
  overflow: hidden;
}

/* ============ Header ============ */
.chat-header {
  flex-shrink: 0;
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 10;
}
.theme-green .chat-header {
  background: linear-gradient(90deg, #67c23a 0%, #85ce61 100%);
}
.theme-blue .chat-header {
  background: linear-gradient(90deg, #409eff 0%, #5470d4 100%);
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.back-btn {
  color: #fff !important;
  font-size: 18px;
}
.header-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.header-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.subtitle {
  font-size: 12px;
  opacity: 0.85;
  line-height: 1.2;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
.new-session-btn {
  color: #fff !important;
  font-size: 13px;
}
.new-session-btn span {
  margin-left: 4px;
}
@media (max-width: 480px) {
  .new-session-btn span {
    display: none;
  }
  .subtitle {
    display: none;
  }
}

/* ============ Body ============ */
.chat-body {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 20px 16px;
  -webkit-overflow-scrolling: touch;
}
.chat-body-inner {
  max-width: 900px;
  margin: 0 auto;
}

/* 欢迎卡片 */
.welcome-card {
  text-align: center;
  padding: 40px 20px;
  color: #606266;
}
.welcome-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}
.welcome-card h3 {
  font-size: 18px;
  margin: 0 0 8px;
  color: #303133;
}
.welcome-card p {
  font-size: 13px;
  margin: 0 0 20px;
}
.welcome-tips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  max-width: 700px;
  margin: 0 auto;
}
.tip {
  padding: 8px 14px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-radius: 18px;
  font-size: 13px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}
.theme-green .tip:hover {
  border-color: #67c23a;
  color: #67c23a;
  background: #f0f9eb;
}
.theme-blue .tip:hover {
  border-color: #409eff;
  color: #409eff;
  background: #ecf5ff;
}

/* ============ Message ============ */
.message-row {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
  align-items: flex-start;
}
.row-user {
  justify-content: flex-end;
}
.row-ai {
  justify-content: flex-start;
}
.avatar {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.theme-green .avatar-ai {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}
.theme-blue .avatar-ai {
  background: linear-gradient(135deg, #409eff, #5470d4);
}
.avatar-user {
  background: linear-gradient(135deg, #909399, #b1b3b8);
}
.bubble {
  max-width: calc(100% - 60px);
  min-height: 36px;
  padding: 10px 14px;
  border-radius: 12px;
  position: relative;
  word-break: break-word;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.bubble-ai {
  background: #fff;
  border: 1px solid #ebeef5;
  border-top-left-radius: 4px;
}
.bubble-user {
  background: #409eff;
  color: #fff;
  border-top-right-radius: 4px;
}
.theme-green .bubble-user {
  background: #67c23a;
}
.bubble-content {
  line-height: 1.6;
  font-size: 14px;
}
.bubble-user .bubble-content {
  white-space: pre-wrap;
}
.typing-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s steps(2) infinite;
  color: #909399;
}
@keyframes blink {
  to {
    opacity: 0;
  }
}
.loading-dots {
  display: inline-flex;
  gap: 4px;
  padding: 4px 0;
}
.loading-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #c0c4cc;
  animation: dot-bounce 1.4s ease-in-out infinite;
}
.loading-dots span:nth-child(2) {
  animation-delay: 0.2s;
}
.loading-dots span:nth-child(3) {
  animation-delay: 0.4s;
}
@keyframes dot-bounce {
  0%, 80%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  40% {
    transform: scale(1.2);
    opacity: 1;
  }
}
.error-text {
  margin-top: 6px;
  font-size: 12px;
  color: #f56c6c;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* ============ Footer ============ */
.chat-footer {
  flex-shrink: 0;
  background: #fff;
  border-top: 1px solid #ebeef5;
  padding: 12px 16px;
  padding-bottom: calc(12px + env(safe-area-inset-bottom));
}
.input-wrapper {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  align-items: flex-end;
  gap: 10px;
}
.chat-input {
  flex: 1;
}
.chat-input :deep(.el-textarea__inner) {
  resize: none;
  border-radius: 10px;
  padding: 8px 12px;
  line-height: 1.5;
  max-height: 120px;
}
.action-buttons {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}
.action-buttons .el-button {
  height: 36px;
  padding: 0 16px;
}
.footer-tip {
  max-width: 900px;
  margin: 6px auto 0;
  font-size: 11px;
  color: #909399;
  text-align: center;
  display: flex;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}

/* ============ Mobile 适配 ============ */
@media (max-width: 600px) {
  .chat-header {
    padding: 0 10px;
  }
  .chat-body {
    padding: 12px 10px;
  }
  .chat-footer {
    padding: 8px 10px;
  }
  .bubble {
    max-width: calc(100% - 50px);
  }
  .avatar {
    width: 32px;
    height: 32px;
  }
  .footer-tip {
    display: none;
  }
}
</style>
