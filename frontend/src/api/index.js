import axios from 'axios'

axios.defaults.baseURL = 'http://localhost:8080'

export const wheelApi = {
  getGroups(userId) {
    return axios.get('/api/custom-wheel/groups', { params: { userId } })
  },
  createGroup(userId, name) {
    return axios.post('/api/custom-wheel/group', name, {
      params: { userId },
      headers: { 'Content-Type': 'text/plain' }
    })
  },
  renameGroup(userId, groupId, newName) {
    return axios.put('/api/custom-wheel/group', newName, {
      params: { userId, groupId },
      headers: { 'Content-Type': 'text/plain' }
    })
  },
  // --- 【新增】删除组 ---
  deleteGroup(userId, groupId) {
    return axios.delete('/api/custom-wheel/group', {
      params: { userId, groupId }
    })
  },
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
  getAttributes(userId, groupId) { // ✨ 加上 groupId
    return axios.get('/api/custom-wheel/attributes', { params: { userId, groupId } })
  },
  createAttribute(userId, groupId, name) {
    return axios.post('/api/custom-wheel/attribute', name, { 
      params: { userId, groupId },
      headers: { 'Content-Type': 'text/plain' } // ✨【极重要！必须加上这句，否则名字会被变成 form 表单格式】
    })
  },
  deleteAttribute(userId, id) {
    return axios.delete(`/api/custom-wheel/attribute/${id}`, { params: { userId } })
  },
}