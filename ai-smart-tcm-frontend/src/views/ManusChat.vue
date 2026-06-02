<template>
  <ChatRoom
    title="AI 超级智能体"
    subtitle="多工具协同 · 复杂任务规划"
    theme-color="blue"
    :theme-icon-comp="Cpu"
    placeholder="向超级智能体提出您的需求，例如：帮我制定一份北京三日游攻略"
    welcome-title="欢迎使用 AI 超级智能体"
    welcome-message="通用超级智能体，支持工具调用与多步推理，应对各类业务问题。"
    :welcome-tips="welcomeTips"
    :on-send="handleSend"
  />
</template>

<script setup lang="ts">
import { Cpu } from '@element-plus/icons-vue'
import ChatRoom from '@/components/ChatRoom.vue'
import { chatWithManusStream } from '@/api/ai'
import type { SSECallbacks } from '@/types/chat'
import { SSEParser } from '@/api/sse'

/**
 * 欢迎提示问题
 */
const welcomeTips: string[] = [
  '帮我制定一份北京三日游攻略',
  '请用 Python 写一个快速排序',
  '分析一下当前 A 股市场的主流热点',
  '写一份产品需求文档：在线预约挂号系统'
]

/**
 * 处理用户发送：调用 Manus SSE 接口
 */
const handleSend = (
  message: string,
  _chatId: string,
  callbacks: SSECallbacks
): SSEParser => {
  // Manus 接口不需要 chatId
  return chatWithManusStream(message, callbacks)
}
</script>
