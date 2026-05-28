import request from '@/utils/request'

// 获取用户列表
export const getUsers = () => request.get('/system/users')

// 创建用户
export const createUser = (data) => request.post('/system/user', data)

// 更新用户
export const updateUser = (userId, data) => request.put(`/system/user/${userId}`, data)

// 删除用户
export const deleteUser = (userId) => request.delete(`/system/user/${userId}`)

// 获取系统配置
export const getSystemConfig = () => request.get('/system/config')

// 更新系统配置
export const updateSystemConfig = (data) => request.post('/system/config', data)