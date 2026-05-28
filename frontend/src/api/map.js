import request from '@/utils/request'

// 获取可用地图列表
export const getMapList = () => request.get('/map/list')

// 获取指定地图的节点数据
export const getMapNodes = (mapId) => request.get(`/map/nodes/${mapId}`)

// 保存节点标记数据
export const saveNodes = (data) => request.post('/map/nodes', data)

// 删除节点
export const deleteNode = (nodeId) => request.delete(`/map/nodes/${nodeId}`)

// 更新节点属性
export const updateNode = (nodeId, data) => request.put(`/map/nodes/${nodeId}`, data)