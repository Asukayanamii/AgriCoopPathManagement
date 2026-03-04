import request from '@/utils/request'

//登录
export const loginApi = (data) => request.post('/user/login', data)