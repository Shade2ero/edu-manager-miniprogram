/**
 * HTTP 请求封装 — 基于 uni.request 的 Promise 包装
 *
 * <p>功能：</p>
 * <ul>
 *   <li>自动拼接 BASE_URL</li>
 *   <li>自动注入 Token（登录态）</li>
 *   <li>统一响应拦截（code !== 0 时自动 toast + reject）</li>
 *   <li>网络异常捕获与友好提示</li>
 *   <li>请求/响应日志（开发调试）</li>
 * </ul>
 */

// ==================== 配置 ====================

/** 后端 API 基础地址 */
const BASE_URL = 'http://localhost:8080'

/** 请求超时时间（毫秒） */
const TIMEOUT = 15000

// ==================== Token 管理 ====================

/** 从本地存储获取 Token */
function getToken() {
  return uni.getStorageSync('token') || ''
}

// ==================== 请求拦截 ====================

/**
 * 处理请求头
 */
function buildHeader(customHeader = {}) {
  const token = getToken()
  const header = {
    'Content-Type': 'application/json',
    ...customHeader,
  }
  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }
  return header
}

// ==================== 响应拦截 ====================

/**
 * 统一处理后端响应
 * @returns 成功时返回 data 字段，失败时 reject
 */
function handleResponse(res) {
  const { statusCode, data } = res

  // HTTP 状态码异常
  if (statusCode !== 200) {
    const msg = statusCode === 401 ? '登录已过期，请重新登录'
      : statusCode === 403 ? '您没有权限执行此操作'
      : statusCode === 404 ? '请求的资源不存在'
      : statusCode >= 500 ? '服务器繁忙，请稍后重试'
      : `请求异常（${statusCode}）`

    uni.showToast({ title: msg, icon: 'none', duration: 2500 })
    return Promise.reject(new Error(msg))
  }

  // 业务状态码判断
  const { code, message, data: bizData } = data

  if (code === 0) {
    // 成功 — 直接返回业务数据
    return bizData
  }

  // 业务异常
  const errorMsg = message || '操作失败'
  uni.showToast({ title: errorMsg, icon: 'none', duration: 2500 })
  return Promise.reject(new Error(errorMsg))
}

// ==================== 核心请求方法 ====================

/**
 * 发起 HTTP 请求
 *
 * @param {object} options - 请求配置
 * @param {string} options.url - 接口路径（相对于 BASE_URL）
 * @param {string} [options.method='GET'] - 请求方法
 * @param {object} [options.data] - 请求参数
 * @param {object} [options.header] - 自定义请求头
 * @param {boolean} [options.showLoading=true] - 是否显示 loading
 * @param {string} [options.loadingText='加载中...'] - loading 文案
 * @returns {Promise} resolve 后端 data 字段，reject 错误信息
 */
export function request(options = {}) {
  const {
    url,
    method = 'GET',
    data = {},
    header = {},
    showLoading = false,
    loadingText = '加载中...',
  } = options

  // Loading 提示
  if (showLoading) {
    uni.showLoading({ title: loadingText, mask: true })
  }

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      header: buildHeader(header),
      timeout: TIMEOUT,
      success: (res) => {
        handleResponse(res).then(resolve).catch(reject)
      },
      fail: (err) => {
        console.error('网络请求失败:', err)
        let msg = '网络异常，请检查网络连接'
        if (err.errMsg) {
          if (err.errMsg.includes('timeout')) {
            msg = '请求超时，请稍后重试'
          } else if (err.errMsg.includes('fail')) {
            msg = '网络连接失败，请检查网络设置'
          }
        }
        uni.showToast({ title: msg, icon: 'none', duration: 2500 })
        reject(new Error(msg))
      },
      complete: () => {
        if (showLoading) {
          uni.hideLoading()
        }
      },
    })
  })
}

/**
 * GET 请求快捷方法
 */
export function get(url, data = {}, options = {}) {
  return request({ ...options, url, method: 'GET', data })
}

/**
 * POST 请求快捷方法
 */
export function post(url, data = {}, options = {}) {
  return request({ ...options, url, method: 'POST', data })
}
