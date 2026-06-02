/**
 * 消息发送方类型
 */
export type MessageRole = 'user' | 'ai'

/**
 * 消息状态
 * - pending: 正在接收流式数据
 * - done: 接收完成
 * - error: 接收出错
 */
export type MessageStatus = 'pending' | 'done' | 'error'

/**
 * 单条聊天消息
 */
export interface ChatMessage {
  /** 唯一 id（用于列表 key 与局部更新） */
  id: string
  /** 发送方 */
  role: MessageRole
  /** 消息内容（AI 消息会持续拼接文本片段） */
  content: string
  /** 消息状态 */
  status: MessageStatus
  /** 消息创建时间戳 */
  timestamp: number
}

/**
 * SSE 增量回调参数
 */
export interface SSECallbacks {
  /** 每收到一个文本片段时触发 */
  onChunk: (chunk: string) => void
  /** 流结束时触发 */
  onComplete: () => void
  /** 出错时触发 */
  onError: (err: Error) => void
}
