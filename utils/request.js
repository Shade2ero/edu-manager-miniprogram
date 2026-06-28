/**
 * HTTP 请求封装 — 原生微信小程序版
 *
 * 用法：
 *   const api = require('../../utils/request')
 *   api.get('/api/balance', { studentId: 1 }).then(data => {...})
 *   api.post('/api/checkin/scan', { ... }).then(data => {...})
 */

const BASE_URL = 'http://192.168.1.17:8080'

function buildHeader() {
  const token = wx.getStorageSync('token') || ''
  const header = { 'Content-Type': 'application/json' }
  if (token) header['Authorization'] = 'Bearer ' + token
  return header
}

function request(opts = {}) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + opts.url,
      method: opts.method || 'GET',
      data: opts.data || {},
      header: buildHeader(),
      timeout: 15000,
      success(res) {
        if (res.statusCode !== 200) {
          wx.showToast({ title: '请求异常(' + res.statusCode + ')', icon: 'none' })
          return reject(new Error('HTTP ' + res.statusCode))
        }
        const d = res.data
        if (d.code === 0) {
          resolve(d.data)
        } else {
          wx.showToast({ title: d.message || '操作失败', icon: 'none' })
          reject(new Error(d.message))
        }
      },
      fail(err) {
        console.error('网络请求失败:', err)
        wx.showToast({ title: '网络异常，请检查连接', icon: 'none' })
        reject(err)
      }
    })
  })
}

module.exports = {
  request,
  get(url, data) { return request({ url, method: 'GET', data }) },
  post(url, data) { return request({ url, method: 'POST', data }) },
  put(url, data) { return request({ url, method: 'PUT', data }) },
  del(url, data) { return request({ url, method: 'DELETE', data }) },
}
