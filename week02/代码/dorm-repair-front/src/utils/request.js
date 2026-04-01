import axios from 'axios'
import UserStore from '../store/user'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: 'http://localhost:8080',  // 端口改为 8080
  timeout: 10000
})

// 请求拦截器
request.interceptors.request.use(config => {
  const userStore = UserStore()
  // 后端要求 token 放在 header 的 token 字段
  if (userStore.token) {
    config.headers.token = userStore.token
  }
  return config
}, err => Promise.reject(err))

// 响应拦截器
request.interceptors.response.use(res => {
  const { code, msg, data } = res.data
  if (code !== 200) {
    ElMessage.error(msg || '请求失败')
    return Promise.reject(msg)
  }
  // ========== 修改：直接返回 data，而不是包装成 { data, msg, code } ==========
  // 这样 Login.vue 中的 const { data } = await request.post(...) 就能正确获取到 data
  return data
}, err => {
  if (err.response?.status === 401) {
    ElMessage.error('登录已过期，请重新登录')
    const userStore = UserStore()
    userStore.logout()
    window.location.href = '/login'
  } else if (err.response?.status === 500) {
    ElMessage.error('服务器错误，请稍后重试')
  } else if (err.code === 'ERR_NETWORK') {
    ElMessage.error('网络异常，请检查后端服务是否启动')
  } else {
    ElMessage.error('请求失败')
  }
  return Promise.reject(err)
})

export default request