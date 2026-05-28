import request from '@/utils/request'

// 开始路径仿真
export const startSimulation = (data) => request.post('/simulation/start', data)

// 获取仿真状态
export const getSimulationStatus = () => request.get('/simulation/status')

// 停止仿真
export const stopSimulation = () => request.post('/simulation/stop')

// 获取仿真结果
export const getSimulationResult = () => request.get('/simulation/result')

// 获取实时监控数据（5秒间隔）
export const getRealtimeMonitorData = () => request.get('/monitor/realtime')