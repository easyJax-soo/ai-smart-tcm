import type { SSECallbacks } from '@/types/chat'

/**
 * SSE 解析器
 *
 * 设计要点（关键）：
 * - 一次 SSE 调用 === 一个 AI 消息气泡
 * - 后端持续推送文本碎片（chunk），前端需要在同一个回调中不断追加
 * - 当流结束时，回调 onComplete
 * - 解析遵循 SSE 规范：以 \n\n 分隔事件，每行形如 `data: <内容>`
 * - 某些实现会以 `data:[DONE]` 表示结束，遇到则忽略内容并停止
 */
export class SSEParser {
  private controller: AbortController | null = null
  private completed = false

  /**
   * 发起 SSE 请求
   * @param url 完整请求地址（包含 query）
   * @param callbacks 三个回调
   */
  async connect(url: string, callbacks: SSECallbacks): Promise<void> {
    this.completed = false
    this.controller = new AbortController()
    const { signal } = this.controller

    try {
      const response = await fetch(url, {
        method: 'GET',
        headers: {
          Accept: 'text/event-stream',
          'Cache-Control': 'no-cache'
        },
        signal
      })

      if (!response.ok || !response.body) {
        throw new Error(`SSE 连接失败: HTTP ${response.status}`)
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''

      // 持续读取流
      while (!this.completed) {
        const { value, done } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })

        // SSE 事件以空行 \n\n 分隔
        let splitIndex: number
        // eslint-disable-next-line no-cond-assign
        while ((splitIndex = buffer.indexOf('\n\n')) !== -1) {
          const rawEvent = buffer.slice(0, splitIndex)
          buffer = buffer.slice(splitIndex + 2)
          this.parseEvent(rawEvent, callbacks)
        }
      }

      // 处理结尾残留
      if (buffer.trim()) {
        this.parseEvent(buffer, callbacks)
      }

      if (!this.completed) {
        this.completed = true
        callbacks.onComplete()
      }
    } catch (err: any) {
      if (err?.name === 'AbortError') {
        // 用户主动中断
        return
      }
      this.completed = true
      callbacks.onError(err instanceof Error ? err : new Error(String(err)))
    }
  }

  /**
   * 解析单个 SSE 事件块
   */
  private parseEvent(rawEvent: string, callbacks: SSECallbacks) {
    const lines = rawEvent.split('\n')
    const dataLines: string[] = []

    for (const line of lines) {
      if (line.startsWith('data:')) {
        // 去掉 `data:` 前缀，可选地吃掉前导空格
        const data = line.slice(5).replace(/^ /, '')
        dataLines.push(data)
      }
      // 忽略 event: / id: / retry: 等字段，本项目只用 data
    }

    if (dataLines.length === 0) return

    const data = dataLines.join('\n')

    // 某些后端实现以 [DONE] 标记结束
    if (data === '[DONE]') {
      this.completed = true
      callbacks.onComplete()
      return
    }

    callbacks.onChunk(data)
  }

  /**
   * 中断当前流
   */
  abort() {
    this.completed = true
    this.controller?.abort()
  }

  /**
   * 是否已经完成
   */
  isCompleted() {
    return this.completed
  }
}
