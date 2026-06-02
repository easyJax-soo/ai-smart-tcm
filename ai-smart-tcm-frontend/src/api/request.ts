import axios, { type AxiosInstance, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

/**
 * API 基础地址（开发环境通过 Vite 代理转发到真实后端）
 */
const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || '') + (import.meta.env.VITE_API_PREFIX || '/api')

/**
 * 通用 Axios 实例：用于非流式接口
 */
const request: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    return config
  },
  (error) => {
    console.error('[Request Error]', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response: AxiosResponse) => {
    return response.data
  },
  (error) => {
    const status = error.response?.status
    const message = (error.response?.data as any)?.message || error.message || '请求失败'
    if (status !== 401) {
      ElMessage.error(`请求错误 (${status || '网络异常'}): ${message}`)
    }
    console.error('[Response Error]', error)
    return Promise.reject(error)
  }
)

export default request
