/**
 * RAG 知识库相关类型定义
 *
 * 后端对应实体：com.bobo.aismartcloud.rag.entity.TCMRagSourceFile
 */

/**
 * 已上传的源文件元数据
 */
export interface RagSourceFile {
  /** 主键 id */
  id?: number
  /** 原始文件名 */
  fileName: string
  /** 文件 MD5 哈希（用于删除） */
  fileHash: string
  /** 文件大小（字节） */
  fileSize?: number
  /** 切分后的文档（向量）条数 */
  documentCount?: number
  /** 分类 */
  category?: string
  /** 标签列表 */
  tags?: string[]
  /** 创建时间，ISO 字符串 */
  createdAt?: string
  /** 更新时间，ISO 字符串 */
  updatedAt?: string
}

/**
 * 上传响应
 */
export interface RagUploadResponse {
  message?: string
  fileName?: string
  category?: string
  tags?: string[]
  chunks?: number
  error?: string
}

/**
 * 删除响应
 */
export interface RagDeleteResponse {
  message?: string
  error?: string
}

/**
 * 支持的文档格式响应
 */
export interface RagSupportedTypesResponse {
  supportedTypes: string[]
  extensible: boolean
  note: string
}
