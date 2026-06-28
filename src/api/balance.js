/**
 * 课时账户相关 API
 */
import { get, post } from './request'

export const balanceApi = {
  /**
   * 查询学员某课程的课时余额
   * @param {number} studentId
   * @param {number} courseId
   * @returns {Promise<{id, studentId, courseId, remaining, totalPurchased, totalConsumed, expiresAt}>}
   */
  getBalance(studentId, courseId) {
    return get('/api/balance', { studentId, courseId })
  },

  /**
   * 查询学员所有课程的课时余额列表
   * @param {number} studentId
   * @returns {Promise<Array<{courseId, courseName, coverUrl, category, remaining, totalPurchased, totalConsumed, expiresAt, daysUntilExpiry}>>}
   */
  listBalances(studentId) {
    return get('/api/balance/list', { studentId })
  },

  /**
   * 查询课时流水列表
   * @param {object} params
   * @param {number} params.studentId
   * @param {number} [params.courseId]
   * @param {string} [params.changeType] - PURCHASE/CONSUME/REFUND
   * @param {number} [params.page=1]
   * @param {number} [params.pageSize=20]
   */
  listTransactions(params) {
    return get('/api/balance/transactions', params)
  },

  /**
   * 增加课时（购课后由支付回调调用，前端一般不需要直接调）
   */
  addBalance(data) {
    return post('/api/balance/add', data)
  },

  /**
   * 扣减课时（签到课消，前端一般不需要直接调）
   */
  consumeHours(data) {
    return post('/api/balance/consume', data)
  },
}
