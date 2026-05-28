import request from '@/utils/request'

// 执行算法计算
export const calculateAlgorithm = (data) => request.post('/algorithm/calculate', data)

// 获取算法对比数据
export const getAlgorithmComparison = () => request.get('/algorithm/comparison')

// 执行聚类算法
export const calculateClustering = (data) => request.post('/algorithm/clustering', data)

// 获取算法历史记录
export const getAlgorithmHistory = () => request.get('/algorithm/history')

// 获取算法参数模板
export const getAlgorithmTemplates = () => request.get('/algorithm/templates')