import request from './request'
import type {
  RagSourceFile,
  RagUploadResponse,
  RagDeleteResponse,
  RagSupportedTypesResponse
} from '@/types/rag'

/** GET /api/rag/files */
export function listRagFiles(): Promise<RagSourceFile[]> {
  return request.get<RagSourceFile[], RagSourceFile[]>('/rag/files')
}

/** DELETE /api/rag/files/{fileHash} */
export function deleteRagFile(fileHash: string): Promise<RagDeleteResponse> {
  return request.delete<RagDeleteResponse, RagDeleteResponse>(
    `/rag/files/${encodeURIComponent(fileHash)}`
  )
}

/** GET /api/rag/supported-types */
export function getRagSupportedTypes(): Promise<RagSupportedTypesResponse> {
  return request.get<RagSupportedTypesResponse, RagSupportedTypesResponse>('/rag/supported-types')
}

/**
 * POST /api/rag/upload (multipart/form-data)
 *
 * 单独超时 120s：上传 + 切分 + embedding 在大文件场景下可能耗时较长
 */
export function uploadRagFile(
  file: File,
  category?: string,
  tags?: string[]
): Promise<RagUploadResponse> {
  const form = new FormData()
  form.append('file', file)
  if (category) form.append('category', category)
  if (tags && tags.length) {
    tags.forEach((t) => form.append('tags', t))
  }
  return request.post<RagUploadResponse, RagUploadResponse>('/rag/upload', form, {
    timeout: 120000
  })
}
