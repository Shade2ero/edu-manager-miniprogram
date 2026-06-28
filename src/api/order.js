/**
 * 订单与支付 API
 */
import { get, post } from './request'

export const orderApi = {
  /**
   * 创建购买订单并获取微信支付参数
   *
   * @param {object} params
   * @param {number} params.studentId - 学员ID
   * @param {number} params.packageId - 课时包ID
   * @param {string} params.openid - 微信OpenID
   * @returns {Promise<{orderNo, totalAmount, courseName, packageName, totalHours, payParams}>}
   */
  createOrder(params) {
    return post('/api/orders', params, { showLoading: true, loadingText: '下单中...' })
  },

  /**
   * 查询订单支付状态
   * @param {string} orderNo - 订单号
   * @returns {Promise<string>} PAID / PENDING / CLOSED
   */
  queryOrderStatus(orderNo) {
    return get('/api/orders/status', { orderNo })
  },
}
