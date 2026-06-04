<template>
  <aside class="chat-history-sidebar">
    <div class="sidebar-header">
      <el-button type="primary" class="new-chat-btn" @click="$emit('new')">
        <el-icon><Plus /></el-icon>
        <span>新建对话</span>
      </el-button>
    </div>

    <div class="sidebar-list">
      <el-scrollbar v-if="history.length > 0" height="100%">
        <div
          v-for="item in history"
          :key="item.chatId"
          class="history-item"
          :class="{ active: item.chatId === currentChatId }"
          @click="$emit('select', item.chatId)"
        >
          <el-icon class="item-icon"><ChatLineRound /></el-icon>
          <div class="item-content">
            <div class="item-title" :title="item.title">{{ item.title || '新对话' }}</div>
            <div class="item-meta">
              <span>{{ item.messageCount }} 条</span>
              <span class="dot">·</span>
              <span>{{ formatTime(item.updatedAt) }}</span>
            </div>
          </div>
          <el-button
            text
            class="delete-btn"
            title="删除对话"
            @click.stop="$emit('delete', item.chatId)"
          >
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
      </el-scrollbar>

      <div v-else class="empty-state">
        <el-icon :size="40" color="#c0c4cc"><ChatLineSquare /></el-icon>
        <p>还没有对话</p>
        <p class="hint">点击上方按钮开始一次新对话</p>
      </div>
    </div>
  </aside>
</template>

<script setup lang="ts">
import { ChatLineRound, ChatLineSquare, Delete, Plus } from '@element-plus/icons-vue'
import type { ChatHistoryItem } from '@/types/chat'

/**
 * Props 类型定义
 */
interface ChatHistorySidebarProps {
  /** 历史会话列表 */
  history: ChatHistoryItem[]
  /** 当前激活的会话 ID（高亮） */
  currentChatId: string
  /** 加载状态 */
  loading?: boolean
}

withDefaults(defineProps<ChatHistorySidebarProps>(), {
  loading: false
})

/**
 * 事件
 * - select: 点击某条历史
 * - new:    点击"新建对话"
 * - delete: 点击删除按钮
 */
defineEmits<{
  select: [chatId: string]
  new: []
  delete: [chatId: string]
}>()

/**
 * 把 ISO 时间格式化为"刚刚 / N 分钟前 / 昨天 HH:mm / YYYY-MM-DD"
 */
const formatTime = (iso: string): string => {
  if (!iso) return ''
  const t = new Date(iso)
  if (isNaN(t.getTime())) return iso
  const now = Date.now()
  const diff = Math.floor((now - t.getTime()) / 1000)

  if (diff < 60) return '刚刚'
  if (diff < 3600) return `${Math.floor(diff / 60)} 分钟前`
  if (diff < 86400) return `${Math.floor(diff / 3600)} 小时前`
  if (diff < 86400 * 2) return `昨天 ${pad(t.getHours())}:${pad(t.getMinutes())}`

  const sameYear = new Date(now).getFullYear() === t.getFullYear()
  const m = pad(t.getMonth() + 1)
  const d = pad(t.getDate())
  return sameYear ? `${m}-${d}` : `${t.getFullYear()}-${m}-${d}`
}

const pad = (n: number) => n.toString().padStart(2, '0')
</script>

<style scoped>
.chat-history-sidebar {
  display: flex;
  flex-direction: column;
  width: 280px;
  height: 100%;
  background: #fafafa;
  border-right: 1px solid #ebeef5;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 14px 12px;
  border-bottom: 1px solid #f0f0f0;
}
.new-chat-btn {
  width: 100%;
}

.sidebar-list {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

.history-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  margin: 4px 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  position: relative;
}
.history-item:hover {
  background: #f0f0f0;
}
.history-item.active {
  background: #ecf5ff;
  color: #409eff;
}
.history-item.active .item-meta,
.history-item.active .item-icon {
  color: #409eff;
}

.item-icon {
  color: #909399;
  flex-shrink: 0;
  font-size: 16px;
}
.item-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
}
.item-title {
  font-size: 13px;
  font-weight: 500;
  line-height: 1.4;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.item-meta {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
  display: flex;
  gap: 4px;
}
.item-meta .dot {
  opacity: 0.5;
}
.delete-btn {
  opacity: 0;
  transition: opacity 0.15s;
  color: #909399;
}
.history-item:hover .delete-btn,
.history-item.active .delete-btn {
  opacity: 1;
}
.delete-btn:hover {
  color: #f56c6c !important;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #909399;
  text-align: center;
}
.empty-state p {
  margin: 8px 0 0;
  font-size: 13px;
}
.empty-state .hint {
  font-size: 12px;
  color: #c0c4cc;
  margin-top: 4px;
}
</style>
