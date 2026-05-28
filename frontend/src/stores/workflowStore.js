import { ref, computed, watch } from 'vue'
import { defineStore } from 'pinia'
import { useMapStore } from './mapStore'
import { useAlgorithmStore } from './algorithmStore'
import { useDroneStore } from './droneStore'

export const useWorkflowStore = defineStore('workflow', () => {
  // 工作流步骤定义
  const STEPS = {
    MAP_SELECTION: 'map-selection',
    NODE_MARKING: 'node-marking',
    DRONE_CONFIG: 'drone-config',
    ALGORITHM_SELECTION: 'algorithm-selection',
    PARAMETER_CONFIG: 'parameter-config',
    CALCULATION: 'calculation',
    SIMULATION_PREVIEW: 'simulation-preview',
    REALTIME_MONITOR: 'realtime-monitor'
  }

  const STEP_LABELS = {
    [STEPS.MAP_SELECTION]: '地图选择',
    [STEPS.NODE_MARKING]: '节点标记',
    [STEPS.DRONE_CONFIG]: '无人机配置',
    [STEPS.ALGORITHM_SELECTION]: '算法选择',
    [STEPS.PARAMETER_CONFIG]: '参数配置',
    [STEPS.CALCULATION]: '算法计算',
    [STEPS.SIMULATION_PREVIEW]: '仿真预览',
    [STEPS.REALTIME_MONITOR]: '实时监控'
  }

  const STEP_ORDER = [
    STEPS.MAP_SELECTION,
    STEPS.NODE_MARKING,
    STEPS.DRONE_CONFIG,
    STEPS.ALGORITHM_SELECTION,
    STEPS.PARAMETER_CONFIG,
    STEPS.CALCULATION,
    STEPS.SIMULATION_PREVIEW,
    STEPS.REALTIME_MONITOR
  ]

  // 状态
  const currentStep = ref(STEPS.MAP_SELECTION)
  const completedSteps = ref([])
  const skippedSteps = ref([])

  // 计算属性
  const currentStepIndex = computed(() => STEP_ORDER.indexOf(currentStep.value))
  const currentStepLabel = computed(() => STEP_LABELS[currentStep.value] || currentStep.value)
  const nextStep = computed(() => {
    const nextIndex = currentStepIndex.value + 1
    return nextIndex < STEP_ORDER.length ? STEP_ORDER[nextIndex] : null
  })
  const prevStep = computed(() => {
    const prevIndex = currentStepIndex.value - 1
    return prevIndex >= 0 ? STEP_ORDER[prevIndex] : null
  })
  const progressPercentage = computed(() => {
    const completedCount = completedSteps.value.length
    const totalSteps = STEP_ORDER.length
    return Math.round((completedCount / totalSteps) * 100)
  })

  const canProceed = computed(() => {
    return validateStep(currentStep.value).valid
  })

  // 数据依赖验证
  function validateStep(step) {
    const mapStore = useMapStore()
    const algorithmStore = useAlgorithmStore()
    const droneStore = useDroneStore()

    switch (step) {
      case STEPS.MAP_SELECTION:
        return {
          valid: !!mapStore.currentMap,
          missing: mapStore.currentMap ? [] : ['地图'],
          message: mapStore.currentMap ? '已选择地图' : '请先选择地图'
        }

      case STEPS.NODE_MARKING:
        return {
          valid: mapStore.hasValidPathData,
          missing: mapStore.hasValidPathData ? [] : ['起点和终点节点'],
          message: mapStore.hasValidPathData ? '节点标记完成' : '需要至少一个起点和一个终点'
        }

      case STEPS.DRONE_CONFIG:
        return {
          valid: true, // 无人机配置是可选的
          missing: [],
          message: '可以配置无人机参数'
        }

      case STEPS.ALGORITHM_SELECTION:
        return {
          valid: mapStore.hasValidPathData,
          missing: mapStore.hasValidPathData ? [] : ['有效节点数据'],
          message: mapStore.hasValidPathData ? '可以选择算法' : '需要有效的节点数据'
        }

      case STEPS.PARAMETER_CONFIG:
        return {
          valid: algorithmStore.algorithmType !== '',
          missing: algorithmStore.algorithmType ? [] : ['算法类型'],
          message: algorithmStore.algorithmType ? '可以配置参数' : '请先选择算法类型'
        }

      case STEPS.CALCULATION:
        return {
          valid: algorithmStore.canCalculate,
          missing: algorithmStore.calculationStatus.missing,
          message: algorithmStore.calculationStatus.message
        }

      case STEPS.SIMULATION_PREVIEW:
        return {
          valid: algorithmStore.hasResult,
          missing: algorithmStore.hasResult ? [] : ['算法计算结果'],
          message: algorithmStore.hasResult ? '可以预览仿真' : '需要先计算算法结果'
        }

      case STEPS.REALTIME_MONITOR:
        return {
          valid: algorithmStore.hasResult,
          missing: algorithmStore.hasResult ? [] : ['算法计算结果'],
          message: algorithmStore.hasResult ? '可以启动实时监控' : '需要先计算算法结果'
        }

      default:
        return {
          valid: true,
          missing: [],
          message: '步骤验证通过'
        }
    }
  }

  // 动作
  function goToStep(step) {
    if (STEP_ORDER.includes(step)) {
      currentStep.value = step
      // 如果跳转到已完成步骤之前的步骤，移除后面的完成标记
      const stepIndex = STEP_ORDER.indexOf(step)
      completedSteps.value = completedSteps.value.filter(
        completedStep => STEP_ORDER.indexOf(completedStep) <= stepIndex
      )
    }
  }

  function next() {
    if (nextStep.value) {
      // 标记当前步骤为完成
      if (!completedSteps.value.includes(currentStep.value)) {
        completedSteps.value.push(currentStep.value)
      }

      // 移动到下一步
      currentStep.value = nextStep.value
      return true
    }
    return false
  }

  function previous() {
    if (prevStep.value) {
      // 移除当前步骤的完成标记
      const index = completedSteps.value.indexOf(currentStep.value)
      if (index !== -1) {
        completedSteps.value.splice(index, 1)
      }

      // 移动到上一步
      currentStep.value = prevStep.value
      return true
    }
    return false
  }

  function skipStep(step) {
    if (!skippedSteps.value.includes(step)) {
      skippedSteps.value.push(step)
      // 如果跳过当前步骤，自动进入下一步
      if (step === currentStep.value) {
        next()
      }
    }
  }

  function unskipStep(step) {
    const index = skippedSteps.value.indexOf(step)
    if (index !== -1) {
      skippedSteps.value.splice(index, 1)
    }
  }

  function resetWorkflow() {
    currentStep.value = STEPS.MAP_SELECTION
    completedSteps.value = []
    skippedSteps.value = []
  }

  function getStepStatus(step) {
    if (skippedSteps.value.includes(step)) {
      return 'skipped'
    }
    if (completedSteps.value.includes(step)) {
      return 'completed'
    }
    if (step === currentStep.value) {
      return 'current'
    }
    const stepIndex = STEP_ORDER.indexOf(step)
    const currentIndex = STEP_ORDER.indexOf(currentStep.value)
    if (stepIndex < currentIndex) {
      return 'pending' // 之前未完成的步骤
    }
    return 'upcoming'
  }

  function getStepValidation(step) {
    return validateStep(step)
  }

  function getCurrentValidation() {
    return validateStep(currentStep.value)
  }

  // 监听步骤变化，自动验证
  watch(currentStep, (newStep) => {
    const validation = validateStep(newStep)
    if (!validation.valid && !skippedSteps.value.includes(newStep)) {
      console.warn(`步骤 ${newStep} 验证失败:`, validation.message)
    }
  })

  // 导出状态和动作
  return {
    // 常量
    STEPS,
    STEP_LABELS,
    STEP_ORDER,

    // 状态
    currentStep,
    completedSteps,
    skippedSteps,

    // 计算属性
    currentStepIndex,
    currentStepLabel,
    nextStep,
    prevStep,
    progressPercentage,
    canProceed,

    // 动作
    goToStep,
    next,
    previous,
    skipStep,
    unskipStep,
    resetWorkflow,
    getStepStatus,
    getStepValidation,
    getCurrentValidation,
    validateStep
  }
})