<template>
  <div class="rag-page">
    <!-- ============ 顶部导航 ============ -->
    <header class="rag-header">
      <div class="header-inner">
        <div class="header-left">
          <el-button text class="back-btn" @click="goBack">
            <el-icon :size="20"><ArrowLeft /></el-icon>
          </el-button>
          <div class="header-icon">
            <el-icon :size="22" color="#fff"><Files /></el-icon>
          </div>
          <div class="header-text">
            <div class="title">RAG 知识库管理</div>
            <div class="subtitle">文档上传 · 向量化 · 检索增强</div>
          </div>
        </div>
        <div class="header-right">
          <el-tag type="info" effect="dark" size="small" round>
            共 {{ filteredFiles.length }} / {{ files.length }} 份文档
          </el-tag>
        </div>
      </div>
    </header>

    <!-- ============ 主体 ============ -->
    <main class="rag-body">
      <!-- 统计概览 -->
      <section class="stat-row">
        <div class="stat-card stat-blue">
          <div class="stat-icon"><el-icon :size="24"><Document /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ files.length }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </div>
        <div class="stat-card stat-green">
          <div class="stat-icon"><el-icon :size="24"><Collection /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ totalChunks }}</div>
            <div class="stat-label">向量片段</div>
          </div>
        </div>
        <div class="stat-card stat-orange">
          <div class="stat-icon"><el-icon :size="24"><Coin /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ formatSize(totalSize) }}</div>
            <div class="stat-label">占用空间</div>
          </div>
        </div>
        <div class="stat-card stat-purple">
          <div class="stat-icon"><el-icon :size="24"><PriceTag /></el-icon></div>
          <div class="stat-info">
            <div class="stat-value">{{ categoryInfo.set.size }}</div>
            <div class="stat-label">分类数量</div>
          </div>
        </div>
      </section>

      <!-- 工具栏 -->
      <section class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="keyword"
            placeholder="搜索文件名 / 分类 / 标签"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="categoryFilter"
            placeholder="全部分类"
            clearable
            class="category-select"
          >
            <el-option
              v-for="c in categoryInfo.list"
              :key="c"
              :label="c || '未分类'"
              :value="c"
            />
          </el-select>
        </div>
        <div class="toolbar-right">
          <el-button @click="refresh" :loading="loading">
            <el-icon><Refresh /></el-icon>
            <span>刷新</span>
          </el-button>
          <el-button type="primary" @click="openUploadDialog">
            <el-icon><Upload /></el-icon>
            <span>上传文档</span>
          </el-button>
        </div>
      </section>

      <!-- 文档列表 -->
      <section class="list-section">
        <!-- 桌面端：表格 -->
        <div v-if="!isMobile" class="table-wrapper">
          <el-table
            v-loading="loading"
            :data="filteredFiles"
            stripe
            style="width: 100%"
            empty-text="暂无文档，请先上传"
          >
            <el-table-column prop="fileName" label="文件名" min-width="220">
              <template #default="{ row }">
                <div class="file-name-cell">
                  <el-icon class="file-icon"><Document /></el-icon>
                  <span class="file-name-text" :title="r(row).fileName">{{ r(row).fileName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="分类" width="140">
              <template #default="{ row }">
                <el-tag v-if="r(row).category" size="small" type="success">{{ r(row).category }}</el-tag>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="标签" min-width="180">
              <template #default="{ row }">
                <template v-if="(r(row).tags ?? []).length">
                  <el-tag
                    v-for="t in r(row).tags"
                    :key="t"
                    size="small"
                    type="info"
                    effect="plain"
                    class="tag-item"
                  >
                    {{ t }}
                  </el-tag>
                </template>
                <span v-else class="muted">—</span>
              </template>
            </el-table-column>
            <el-table-column label="片段" width="90" align="center">
              <template #default="{ row }">
                <span class="num-cell">{{ r(row).documentCount ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column label="大小" width="110" align="right">
              <template #default="{ row }">
                <span class="muted">{{ formatSize(r(row).fileSize) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="上传时间" width="170">
              <template #default="{ row }">
                <span class="muted">{{ formatTime(r(row).createdAt) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" fixed="right" align="center">
              <template #default="{ row }">
                <el-button
                  type="danger"
                  size="small"
                  link
                  @click="onDelete(r(row))"
                >
                  <el-icon><Delete /></el-icon>
                  <span>删除</span>
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <!-- 移动端：卡片列表 -->
        <div v-else class="mobile-list">
          <div v-if="loading" class="mobile-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中…</span>
          </div>
          <el-empty v-else-if="filteredFiles.length === 0" description="暂无文档" />
          <div
            v-for="row in filteredFiles"
            :key="row.fileHash"
            class="mobile-card"
          >
            <div class="mobile-card-header">
              <el-icon class="file-icon"><Document /></el-icon>
              <span class="file-name-text" :title="row.fileName">{{ row.fileName }}</span>
            </div>
            <div class="mobile-card-body">
              <div class="kv"><span>分类</span><span>{{ row.category || '—' }}</span></div>
              <div class="kv"><span>片段</span><span>{{ row.documentCount ?? 0 }}</span></div>
              <div class="kv"><span>大小</span><span>{{ formatSize(row.fileSize) }}</span></div>
              <div class="kv"><span>时间</span><span>{{ formatTime(row.createdAt) }}</span></div>
              <div v-if="row.tags && row.tags.length" class="kv tags-kv">
                <span>标签</span>
                <div class="mobile-tags">
                  <el-tag
                    v-for="t in row.tags"
                    :key="t"
                    size="small"
                    type="info"
                    effect="plain"
                  >{{ t }}</el-tag>
                </div>
              </div>
            </div>
            <div class="mobile-card-footer">
              <el-button
                type="danger"
                size="small"
                plain
                @click="onDelete(row)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </el-button>
            </div>
          </div>
        </div>
      </section>
    </main>

    <!-- ============ 上传对话框 ============ -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传知识库文档"
      width="520px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form
        ref="uploadFormRef"
        :model="uploadForm"
        :rules="uploadRules"
        label-width="80px"
        label-position="right"
      >
        <el-form-item label="选择文件" prop="file">
          <el-upload
            ref="uploadRef"
            class="rag-uploader"
            :auto-upload="false"
            :limit="1"
            :on-change="onFileChange"
            :on-exceed="onExceed"
            :on-remove="onFileRemove"
            drag
            accept=".md,.markdown,.txt,text/markdown,text/plain"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">
              将文件拖到此处，或<em>点击选择</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">
                仅支持 {{ supportedTypesText }}，单个文件不超过 20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>

        <el-form-item label="分类">
          <el-input
            v-model="uploadForm.category"
            placeholder="例如：伤寒论 / 中药 / 体质辨识"
            maxlength="32"
            clearable
          />
        </el-form-item>

        <el-form-item label="标签">
          <el-input
            v-model="tagsInput"
            placeholder="多个标签用逗号分隔，例如：经典,方剂,内服"
            maxlength="100"
            clearable
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="uploading"
          :disabled="!uploadForm.file"
          @click="submitUpload"
        >
          {{ uploading ? '上传中…' : '开始上传' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules, type UploadFile, type UploadInstance, type UploadRawFile } from 'element-plus'
import {
  listRagFiles,
  deleteRagFile,
  uploadRagFile,
  getRagSupportedTypes
} from '@/api/rag'
import type { RagSourceFile, RagSupportedTypesResponse } from '@/types/rag'

/* ============ 路由 ============ */
const router = useRouter()
const goBack = () => router.push('/')

/* ============ 列表数据 ============ */
const files = ref<RagSourceFile[]>([])
const loading = ref(false)

const loadFiles = async () => {
  loading.value = true
  try {
    const list = await listRagFiles()
    files.value = list || []
  } catch {
    // 拦截器已弹错误
    files.value = []
  } finally {
    loading.value = false
  }
}
const refresh = loadFiles

/* ============ 过滤与统计 ============ */
const keyword = ref('')
const categoryFilter = ref<string | undefined>(undefined)

/** 所有出现过的分类（数组 + 数量） */
const categoryInfo = computed(() => {
  const set = new Set<string>()
  files.value.forEach((f) => f.category && set.add(f.category))
  return { set, list: Array.from(set).sort() }
})

const filteredFiles = computed(() => {
  const kw = keyword.value.trim().toLowerCase()
  return files.value.filter((f) => {
    if (categoryFilter.value && f.category !== categoryFilter.value) return false
    if (!kw) return true
    if (f.fileName?.toLowerCase().includes(kw)) return true
    if (f.category?.toLowerCase().includes(kw)) return true
    if (f.tags?.some((t) => t.toLowerCase().includes(kw))) return true
    return false
  })
})

const totalChunks = computed(() =>
  files.value.reduce((sum, f) => sum + (f.documentCount ?? 0), 0)
)
const totalSize = computed(() =>
  files.value.reduce((sum, f) => sum + (f.fileSize ?? 0), 0)
)

/* ============ 删除 ============ */
const onDelete = async (row: RagSourceFile) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除「${row.fileName}」及其全部向量数据吗？此操作不可恢复。`,
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
    const resp = await deleteRagFile(row.fileHash)
    if (resp?.error) {
      ElMessage.error(resp.error)
      return
    }
    ElMessage.success(resp?.message || '删除成功')
    // 本地移除，避免重拉全表
    files.value = files.value.filter((f) => f.fileHash !== row.fileHash)
  } catch {
    // 错误已由拦截器弹窗
  }
}

/* ============ 上传对话框 ============ */
const uploadDialogVisible = ref(false)
const uploading = ref(false)
const uploadFormRef = ref<FormInstance | null>(null)
const uploadRef = ref<UploadInstance | null>(null)
const tagsInput = ref('')

const uploadForm = reactive<{
  file: UploadRawFile | null
  category: string
}>({
  file: null,
  category: ''
})

const uploadRules: FormRules = {
  file: [
    {
      required: true,
      validator: (_rule, _value, callback) => {
        if (!uploadForm.file) callback(new Error('请选择要上传的文件'))
        else callback()
      },
      trigger: 'change'
    }
  ]
}

/* ============ 支持的格式（懒加载：打开上传弹窗时再拉） ============ */
const supportedTypes = ref<string[]>([])
const supportedTypesText = computed(() => {
  if (!supportedTypes.value.length) return 'Markdown'
  return supportedTypes.value.map((t) => (t.startsWith('.') ? t : `.${t}`)).join(' / ')
})

let supportedTypesLoaded = false
const loadSupportedTypes = async () => {
  if (supportedTypesLoaded) return
  supportedTypesLoaded = true
  try {
    const resp: RagSupportedTypesResponse = await getRagSupportedTypes()
    supportedTypes.value = resp.supportedTypes || []
  } catch {
    supportedTypes.value = []
  }
}

const openUploadDialog = () => {
  uploadDialogVisible.value = true
  uploadForm.file = null
  uploadForm.category = ''
  tagsInput.value = ''
  loadSupportedTypes()
}

const onFileChange = (uploadFile: UploadFile) => {
  if (uploadFile.raw) uploadForm.file = uploadFile.raw
}
const onExceed = () => ElMessage.warning('只能上传一个文件，请先移除已选文件')
const onFileRemove = () => {
  uploadForm.file = null
}

const submitUpload = async () => {
  if (!uploadForm.file) {
    ElMessage.warning('请先选择文件')
    return
  }
  if (uploadFormRef.value) {
    try {
      await uploadFormRef.value.validate()
    } catch {
      return
    }
  }
  uploading.value = true
  try {
    const tags = tagsInput.value
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter(Boolean)
    const resp = await uploadRagFile(
      uploadForm.file,
      uploadForm.category || undefined,
      tags.length ? tags : undefined
    )
    if (resp.error) {
      ElMessage.error(resp.error)
      return
    }
    if (resp.chunks === 0) {
      ElMessage.warning(resp.message || '文件已存在')
    } else {
      ElMessage.success(
        `${resp.message || '上传成功'} · 切分 ${resp.chunks ?? 0} 个片段`
      )
    }
    uploadDialogVisible.value = false
    loadFiles()
  } catch (e: any) {
    ElMessage.error(`上传失败：${e?.message || '未知错误'}`)
  } finally {
    uploading.value = false
  }
}

/* ============ 表格行类型辅助 ============ */
/**
 * Element Plus 的 el-table-column 作用域插槽在 vue-tsc 下 row 推断为 DefaultRow，
 * 借助此小工具在模板里安全访问 RagSourceFile 字段。
 */
const r = (row: unknown): RagSourceFile => row as RagSourceFile

/* ============ 工具函数 ============ */
const formatSize = (bytes?: number): string => {
  if (bytes == null || bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let val = bytes
  while (val >= 1024 && i < units.length - 1) {
    val /= 1024
    i++
  }
  return `${val.toFixed(val >= 10 || i === 0 ? 0 : 1)} ${units[i]}`
}

const formatTime = (iso?: string): string => {
  if (!iso) return '—'
  const d = new Date(iso)
  if (isNaN(d.getTime())) return iso
  const pad = (n: number) => n.toString().padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/* ============ 移动端适配（rAF 节流避免拖动窗口时频繁触发） ============ */
const isMobile = ref(false)
let resizeRaf = 0
const handleResize = () => {
  if (resizeRaf) return
  resizeRaf = requestAnimationFrame(() => {
    isMobile.value = window.innerWidth < 768
    resizeRaf = 0
  })
}

/* ============ 生命周期 ============ */
onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize)
  loadFiles()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeRaf) cancelAnimationFrame(resizeRaf)
})
</script>

<style scoped>
.rag-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg, #f0f5ff 0%, #f5f7fa 60%);
}

/* ============ Header ============ */
.rag-header {
  background: linear-gradient(90deg, #7c3aed 0%, #a855f7 100%);
  color: #fff;
  box-shadow: 0 2px 8px rgba(124, 58, 237, 0.15);
  flex-shrink: 0;
}
.header-inner {
  max-width: 1280px;
  margin: 0 auto;
  height: 56px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.back-btn {
  color: #fff !important;
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
}
.subtitle {
  font-size: 12px;
  opacity: 0.85;
  line-height: 1.2;
  margin-top: 2px;
}
@media (max-width: 480px) {
  .header-inner {
    padding: 0 12px;
  }
  .subtitle {
    display: none;
  }
}

/* ============ Body ============ */
.rag-body {
  flex: 1;
  max-width: 1280px;
  width: 100%;
  margin: 0 auto;
  padding: 20px;
}
@media (max-width: 480px) {
  .rag-body {
    padding: 12px;
  }
}

/* ============ 统计卡片 ============ */
.stat-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}
.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease;
}
.stat-card:hover {
  transform: translateY(-2px);
}
.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}
.stat-blue .stat-icon { background: linear-gradient(135deg, #409eff, #5470d4); }
.stat-green .stat-icon { background: linear-gradient(135deg, #67c23a, #85ce61); }
.stat-orange .stat-icon { background: linear-gradient(135deg, #e6a23c, #f0b86e); }
.stat-purple .stat-icon { background: linear-gradient(135deg, #7c3aed, #a855f7); }
.stat-value {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  line-height: 1.2;
}
.stat-label {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
@media (max-width: 900px) {
  .stat-row {
    grid-template-columns: repeat(2, 1fr);
  }
}

/* ============ 工具栏 ============ */
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.toolbar-left {
  display: flex;
  gap: 10px;
  flex: 1;
  min-width: 280px;
}
.search-input {
  width: 280px;
  max-width: 100%;
}
.category-select {
  width: 160px;
}
.toolbar-right {
  display: flex;
  gap: 8px;
}
@media (max-width: 600px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }
  .toolbar-left {
    flex-direction: column;
    min-width: 0;
  }
  .search-input,
  .category-select {
    width: 100%;
  }
}

/* ============ 列表 ============ */
.list-section {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  overflow: hidden;
}
.table-wrapper {
  padding: 4px;
}
.file-name-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}
.file-icon {
  color: #7c3aed;
  flex-shrink: 0;
}
.file-name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 500;
  color: #303133;
}
.muted {
  color: #909399;
  font-size: 13px;
}
.num-cell {
  font-weight: 600;
  color: #303133;
}
.tag-item {
  margin-right: 4px;
  margin-bottom: 4px;
}

/* ============ 移动端列表 ============ */
.mobile-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.mobile-loading {
  text-align: center;
  padding: 40px 0;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}
.mobile-card {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  overflow: hidden;
}
.mobile-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-weight: 500;
}
.mobile-card-body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.kv {
  display: flex;
  font-size: 13px;
  align-items: center;
}
.kv > span:first-child {
  color: #909399;
  width: 48px;
  flex-shrink: 0;
}
.kv > span:last-child {
  color: #303133;
}
.tags-kv {
  align-items: flex-start;
}
.mobile-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  flex: 1;
}
.mobile-card-footer {
  padding: 8px 12px;
  border-top: 1px solid #f0f0f0;
  text-align: right;
  background: #fafafa;
}

/* ============ 上传对话框 ============ */
.rag-uploader {
  width: 100%;
}
.rag-uploader :deep(.el-upload-dragger) {
  width: 100%;
}
</style>
