import request from '@/utils/request'

export const cluster = (data) => request.post('/algorithm/cluster', data)

export const resourceSearch = (data) => request.post('/algorithm/resource-search', data)

export const pathPlanning = (data) => request.post('/algorithm/path-planning', data)

export const pipeline = (data) => request.post('/algorithm/pipeline', data)
