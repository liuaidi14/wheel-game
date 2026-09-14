import axios from 'axios'

axios.defaults.baseURL = 'http://localhost:8080'

export const wheelApi = {
  // ==================== 剧本组（① 加 type 隔离） ====================

  /**
   * 获取剧本组列表
   * @param {number} userId 用户ID
   * @param {string} [type] 可选：'life'（人生）| 'battle'（对战），不传返回全部
   */
  getGroups(userId, type) {
    const params = { userId }
    if (type) params.type = type
    return axios.get('/api/custom-wheel/groups', { params })
  },

  /**
   * 创建剧本组
   * @param {number} userId 用户ID
   * @param {string} name 剧本组名称
   * @param {string} [type='life'] 类型：'life' | 'battle'
   */
  createGroup(userId, name, type = 'life') {
    return axios.post('/api/custom-wheel/group', name, {
      params: { userId, type },
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  renameGroup(userId, groupId, newName) {
    return axios.put('/api/custom-wheel/group', newName, {
      params: { userId, groupId },
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  deleteGroup(userId, groupId) {
    return axios.delete('/api/custom-wheel/group', {
      params: { userId, groupId }
    })
  },

  // ==================== ② copyGroup 一键复制 ====================

  /**
   * 复制剧本组（含阶段、选项、属性），用于从人生剧本导入到对战剧本
   * 一次请求完成，后端带事务
   *
   * @param {number} userId 用户ID
   * @param {number} sourceGroupId 源剧本组ID（如人生剧本组）
   * @param {string} targetName 新剧本组名称
   * @param {string} [type='battle'] 新剧本组类型
   * @returns {Promise<{data: {id:number, name:string, type:string, stageCount:number, attributeCount:number}}>}
   */
  copyGroup(userId, sourceGroupId, targetName, type = 'battle') {
    return axios.post('/api/custom-wheel/copy-group', null, {
      params: { userId, sourceGroupId, targetName, type }
    })
  },

  // ==================== 阶段 & 选项 ====================

  getStages(userId, groupId) {
    return axios.get('/api/custom-wheel/stages', { params: { userId, groupId } })
  },

  saveStages(userId, groupId, data) {
    return axios.post('/api/custom-wheel/stages', data, {
      params: { userId, groupId }
    })
  },

  getGraphData(userId, groupId) {
    return axios.get('/api/custom-wheel/graph-data', { params: { userId, groupId } })
  },

  // ==================== 属性定义 ====================

  getAttributes(userId, groupId) {
    return axios.get('/api/custom-wheel/attributes', { params: { userId, groupId } })
  },

  createAttribute(userId, groupId, name) {
    return axios.post('/api/custom-wheel/attribute', name, {
      params: { userId, groupId },
      headers: { 'Content-Type': 'text/plain' }
    })
  },

  deleteAttribute(userId, id) {
    return axios.delete(`/api/custom-wheel/attribute/${id}`, { params: { userId } })
  },
}
