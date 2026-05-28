import request from '@/utils/request'

// 获取无人机配置
export const getDroneConfig = () => request.get('/drone/config')

// 保存无人机配置
export const saveDroneConfig = (data) => request.post('/drone/config', data)

// 获取可用机型列表
export const getDroneModels = () => request.get('/drone/models')

// 获取当前无人机状态
export const getDroneStatus = () => request.get('/drone/status')

// 更新无人机状态
export const updateDroneStatus = (data) => request.post('/drone/status/update', data)