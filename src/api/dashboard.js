/**
 * 数据看板 API
 */
import { get } from './request'

export const dashboardApi = {
  /**
   * 获取机构月度看板数据
   * @param {number} institutionId
   */
  getDashboard(institutionId = 1) {
    return get('/api/dashboard', { institutionId })
  },
}
