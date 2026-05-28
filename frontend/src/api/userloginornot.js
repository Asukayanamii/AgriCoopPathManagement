import request from '@/utils/request'

//登录
export const loginornotApi = () => request.get('/user/loginornot')