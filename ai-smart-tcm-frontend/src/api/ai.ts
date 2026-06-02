import { SSEParser } from './sse'
import type { SSECallbacks } from '@/types/chat'

/**
 * API 前缀
 */
const API_PREFIX = import.meta.env.VITE_API_PREFIX || '/api'

/**
 * AI 云中医问诊 - SSE 流式接口
 *
 * 后端实现：
 *   GET /api/ai/tcm_app/chat/sse?message=xxx&chatId=xxx
 *   返回 Flux<ServerSentEvent<String>>，每个 data 即一个文本碎片
 *
 * 前端约定：
 *   一次完整调用 = 一个 AI 消息气泡
 *   文本碎片通过 onChunk 持续追加到同一条消息中
 *   流结束后通过 onComplete 通知
 */
export function chatWithTCMStream(
  message: string,
  chatId: string,
  callbacks: SSECallbacks
): SSEParser {
  const params = new URLSearchParams({ message, chatId }).toString()
  const url = `${API_PREFIX}/ai/tcm_app/chat/sse?${params}`

  const parser = new SSEParser()
  parser.connect(url, callbacks)
  return parser
}

/**
 * AI 超级智能体 - SSE 流式接口
 *
 * 后端实现：
 *   GET /api/ai/manus/chat?message=xxx
 *   返回 SseEmitter
 */
export function chatWithManusStream(
  message: string,
  callbacks: SSECallbacks
): SSEParser {
  const params = new URLSearchParams({ message }).toString()
  const url = `${API_PREFIX}/ai/manus/chat?${params}`

  const parser = new SSEParser()
  parser.connect(url, callbacks)
  return parser
}
