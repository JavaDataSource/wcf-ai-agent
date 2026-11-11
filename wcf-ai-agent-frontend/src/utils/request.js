import axios from 'axios'

/**
 * Axios 实例：用于项目中的普通（非流式）HTTP 请求。
 *
 * 注意：
 * 1. 后端接口地址前缀为 http://localhost:8123/api；
 * 2. 开发环境通过 vite.config.js 中的 proxy 将 /api 代理到后端，天然避免跨域；
 * 3. 如果部署到其他环境，可通过环境变量 VITE_API_BASE_URL 覆盖。
 */
const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60000,
})

// 响应拦截器：直接返回响应体数据，出错时统一打印日志
request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('[Axios 请求出错]', error?.message || error)
    return Promise.reject(error)
  },
)

export default request
