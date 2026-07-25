import request from '@/utils/request'

// === Map / Road Network ===
export const saveNodes = (mapId, data) => request.post(`/map/nodes?mapId=${mapId}`, data)
export const saveEdges = (mapId, data) => request.post(`/map/edges?mapId=${mapId}`, data)
export const buildGraph = (mapId) => request.post(`/map/build?mapId=${mapId}`)
export const getNodes = (mapId) => request.get(`/map/nodes?mapId=${mapId}`)
export const getEdges = (mapId) => request.get(`/map/edges?mapId=${mapId}`)
export const getMapStats = (mapId) => request.get(`/map/stats?mapId=${mapId}`)

// === Task Points & Clustering ===
export const createTasks = (mapId, data) => request.post(`/tasks?mapId=${mapId}`, data)
export const runCluster = (mapId, data) => request.post(`/tasks/cluster?mapId=${mapId}`, data)
export const getTasks = (mapId) => request.get(`/tasks?mapId=${mapId}`)
export const getClusters = (mapId) => request.get(`/tasks/clusters?mapId=${mapId}`)
export const savePriority = (mapId, data) => request.put(`/tasks/priority?mapId=${mapId}`, data)
export const getPriority = (mapId) => request.get(`/tasks/priority?mapId=${mapId}`)

// === Resources ===
export const saveResources = (mapId, data) => request.post(`/resources?mapId=${mapId}`, data)
export const getResources = (mapId) => request.get(`/resources?mapId=${mapId}`)
export const updateResourceState = (id, state) => request.put(`/resources/${id}/state`, { state })

// === Execute ===
export const executeNext = (mapId) => request.post(`/execute/next?mapId=${mapId}`)
export const carArrived = (mapId, clusterId) => request.post(`/execute/car-arrived?mapId=${mapId}`, { clusterId })
export const droneDone = (mapId, clusterId) => request.post(`/execute/drone-done?mapId=${mapId}`, { clusterId })
export const getProgress = (mapId) => request.get(`/execute/progress?mapId=${mapId}`)
