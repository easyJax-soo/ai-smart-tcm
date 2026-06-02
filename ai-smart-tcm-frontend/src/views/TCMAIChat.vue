<template>
  <ChatRoom
    title="AI 云中医问诊"
    subtitle="智能辨证 · 体质调理"
    theme-color="green"
    :theme-icon-comp="ChatDotRound"
    placeholder="请描述您的症状，例如：最近总是失眠、容易疲劳…"
    welcome-title="欢迎来到 AI 云中医问诊"
    welcome-message="融合传统中医智慧与现代 AI，模拟中医师问诊流程，辅助您了解自身体质。"
    :welcome-tips="welcomeTips"
    :on-send="handleSend"
  />
</template>

<script setup lang="ts">
import { ChatDotRound } from '@element-plus/icons-vue'
import ChatRoom from '@/components/ChatRoom.vue'
import { chatWithTCMStream } from '@/api/ai'
import type { SSECallbacks } from '@/types/chat'
import { SSEParser } from '@/api/sse'

/**
 * 欢迎提示问题
 */
const welcomeTips: string[] = [
  '我最近总是失眠，该怎么办？',
  '湿气重的表现有哪些？',
  '推荐几款适合气虚体质的茶饮',
  '手脚冰凉是阳虚吗？'
]

/**
 * 处理用户发送：调用 TCM SSE 接口
 */
const handleSend = (
  message: string,
  chatId: string,
  callbacks: SSECallbacks
): SSEParser => {
  return chatWithTCMStream(message, chatId, callbacks)
}
</script>
