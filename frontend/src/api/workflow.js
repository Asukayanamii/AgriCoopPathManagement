import request from '@/utils/request'

// 获取工作流状态
export const getWorkflowStatus = () => request.get('/workflow/status')

// 验证步骤数据完整性
export const validateWorkflowStep = (step) => request.get(`/workflow/validate/${step}`)

// 获取数据依赖关系
export const getDataDependencies = () => request.get('/data/dependencies')

// 更新工作流步骤
export const updateWorkflowStep = (step) => request.post('/workflow/step', { step })

// 获取工作流历史
export const getWorkflowHistory = () => request.get('/workflow/history')

// 重置工作流
export const resetWorkflow = () => request.post('/workflow/reset')