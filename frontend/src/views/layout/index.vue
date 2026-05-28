<template>
  <div class="common-layout">
    <el-container class="el-container-main">
      <!-- Header 区域 - 农业无人机主题 -->
      <el-header class="header">
        <!-- 侧边栏收起/展开触发器 -->
        <div class="toggle-btn" @click="toggleCollapse">
          <el-icon :size="20"><Menu /></el-icon>
        </div>
        
        <span class="title">农业无人机路径规划算法展示系统</span>
        
        <!-- 右上角头像下拉菜单（固定到右侧） -->
        <div class="user-avatar">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="avatar-wrapper">
              <el-avatar :size="40" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png">
                <User />
              </el-avatar>
              <el-icon :size="16" class="dropdown-icon"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">
                  <el-icon><EditPen /></el-icon> 修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      
      <el-container class="el-container-sub">
        <!-- 左侧可折叠菜单 -->
        <el-aside 
          :width="isCollapse ? '64px' : '220px'" 
          class="aside"
          :class="{ 'aside-collapse': isCollapse }"
        >
          <el-menu 
            router
            :collapse="isCollapse"
            :collapse-transition="true"
            default-active="/index"
            unique-opened
          >
            <!-- 首页菜单 -->
            <el-menu-item index="/index">
              <el-icon><Promotion /></el-icon>
              <template #title>系统首页</template>
            </el-menu-item>

            <!-- 地图与节点管理 -->
            <el-sub-menu index="/map">
              <template #title>
                <el-icon><MapLocation /></el-icon>
                <span>地图与节点管理</span>
              </template>
              <el-menu-item index="/map/display">
                <el-icon><Picture /></el-icon>
                <template #title>地图展示与节点标记</template>
              </el-menu-item>
              <el-menu-item index="/map/node-edit">
                <el-icon><EditPen /></el-icon>
                <template #title>节点属性编辑</template>
              </el-menu-item>
              <el-menu-item index="/map/clustering">
                <el-icon><PieChart /></el-icon>
                <template #title>聚类算法展示</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 路径规划算法 -->
            <el-sub-menu index="/algorithm">
              <template #title>
                <el-icon><Algorithm /></el-icon>
                <span>路径规划算法</span>
              </template>
              <el-menu-item index="/algorithm/display">
                <el-icon><LineChart /></el-icon>
                <template #title>算法展示</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/compare">
                <el-icon><ScaleToOriginal /></el-icon>
                <template #title>算法对比分析</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 无人机参数与仿真 -->
            <el-sub-menu index="/simulation">
              <template #title>
                <el-icon><VideoPlay /></el-icon>
                <span>无人机参数与仿真</span>
              </template>
              <el-menu-item index="/drone/config">
                <el-icon><Setting /></el-icon>
                <template #title>无人机参数配置</template>
              </el-menu-item>
              <el-menu-item index="/simulation/preview">
                <el-icon><Eye /></el-icon>
                <template #title>路径仿真预览</template>
              </el-menu-item>
              <el-menu-item index="/simulation/realtime">
                <el-icon><DataLine /></el-icon>
                <template #title>实时系统监控</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 系统管理 -->
            <el-sub-menu index="/system">
              <template #title>
                <el-icon><Tools /></el-icon>
                <span>系统管理</span>
              </template>
              <el-menu-item index="/system/user">
                <el-icon><User /></el-icon>
                <template #title>用户管理</template>
              </el-menu-item>
              <el-menu-item index="/system/config">
                <el-icon><Gear /></el-icon>
                <template #title>系统配置</template>
              </el-menu-item>
            </el-sub-menu>
          </el-menu>
        </el-aside>
        
        <!-- 主内容区 -->
        <el-main class="main-content">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ElMessage, ElMessageBox } from 'element-plus';
import { ref } from 'vue'
import { useRouter } from 'vue-router'
// 全局引入Element Plus图标（避免选择性引入导致白屏）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

const router = useRouter()
const logout = () => {
  // 自定义退出登录确认弹窗，优化样式和动画
  ElMessageBox.confirm(
    '确定要退出当前账号吗？', // 更友好的提示文案
    '退出登录', // 弹窗标题
    {
      // 弹窗样式与动画配置
      confirmButtonText: '确认退出',
      cancelButtonText: '取消',
      type: 'warning',
      // 自定义类名，用于样式美化
      customClass: 'custom-logout-box',
      // 动画相关配置
      closeOnClickModal: false, // 点击遮罩不关闭
      closeOnPressEscape: true, // 按ESC可关闭
      showClose: true, // 显示关闭按钮
      // 按钮样式配置
      confirmButtonClass: 'logout-confirm-btn',
      cancelButtonClass: 'logout-cancel-btn',
      // 弹窗大小
      center: true, // 内容居中
      draggable: false // 禁止拖拽
    }
  ).then(() => {
    // 确认退出逻辑
    ElMessage({
      message: '退出登录成功',
      type: 'success',
      duration: 1500, // 提示时长更短，更流畅
      customClass: 'custom-logout-message'
    })
    // 清空登录信息
    localStorage.removeItem('loginUser')
    // 延迟跳转，让提示动画更完整
    setTimeout(() => {
      router.push('/login')
    }, 800)
  }).catch(() => {
    // 取消退出的提示（可选，可根据需求删除）
    ElMessage({
      message: '已取消退出',
      type: 'info',
      duration: 1000,
      customClass: 'custom-logout-message'
    })
  })
}
// 侧边栏折叠状态
const isCollapse = ref(false)

// 切换侧边栏折叠/展开
const toggleCollapse = () => {
  isCollapse.value = !isCollapse.value
}

// 处理下拉菜单命令
const handleCommand = (command) => {
  switch(command) {
    case 'password':
      ElMessage.info('修改密码功能待实现')
      // 这里可以添加打开修改密码弹窗的逻辑
      break
    case 'logout':
      logout() // 调用退出登录函数
      break
  }
}

let loading = ref(true) // 加载状态
let data = ref({}) // 补充缺失的data定义，避免报错
// 定义请求函数
const fetchData = async () => {
  try {
    // 页面加载前发送请求
    const { loginornotApi } = await import('@/api/userloginornot')
    const response = await loginornotApi()
    data.value = response.data // 赋值数据
  } catch (error) {
    console.error('请求失败:', error)
    ElMessage.error('数据加载失败，请重试')
  } finally {
    loading.value = false // 无论成功失败，都结束加载状态
  }
}

// 立即执行请求函数（页面加载时就触发）
fetchData()
</script>

<style scoped>
/* 全局样式重置 - 消除默认边距和滚动条 */
:root {
  margin: 0;
  padding: 0;
}

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

/* 全局布局样式 - 核心修复 */
.common-layout {
  width: 100vw;          /* 视口宽度100% */
  height: 100vh;         /* 视口高度100% */
  position: fixed;       /* 固定定位，完全覆盖视口 */
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;      /* 隐藏溢出，消除滚动条 */
  margin: 0;
  padding: 0;
}

/* 主容器样式 */
.el-container-main {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

/* 子容器样式 */
.el-container-sub {
  height: calc(100% - 60px); /* 减去header高度 */
  width: 100%;
  overflow: hidden;
}

/* 头部样式 */
.header {
  background-image: linear-gradient(to right, #006400, #228B22, #32CD32, #90EE90, #98FB98);
  display: flex;
  align-items: center;
  padding: 0 20px;
  position: relative;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 10;
  height: 60px;          /* 固定header高度，避免高度变化 */
  width: 100%;           /* 宽度100%，消除右侧白边 */
}

/* 侧边栏切换按钮 */
.toggle-btn {
  color: white;
  cursor: pointer;
  margin-right: 20px;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.toggle-btn:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.title {
  color: white;
  font-size: 24px;
  font-family: 微软雅黑, 楷体;
  font-weight: bolder;
  flex: 1;
  text-align: center;
}

/* 用户头像区域（固定到右侧） */
.user-avatar {
  margin-left: auto; /* 关键：将头像推到最右侧 */
  display: flex;
  align-items: center;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  color: white;
  gap: 8px;
  padding: 4px 8px;
  border-radius: 20px;
  transition: background-color 0.3s;
}

.avatar-wrapper:hover {
  background-color: rgba(255, 255, 255, 0.2);
}

.dropdown-icon {
  transition: transform 0.3s;
}

:deep(.el-dropdown:hover .dropdown-icon) {
  transform: rotate(180deg);
}

/* 侧边栏样式 */
.aside {
  border-right: 1px solid #e5e7eb;
  background-color: #f9fafb;
  transition: all 0.3s ease;
  overflow: hidden;
  height: 100%; /* 高度100%，消除底部白边 */
}

.aside-collapse {
  width: 64px !important;
}

/* 菜单样式优化 */
:deep(.el-menu) {
  border-right: none;
  height: 100%;
  background-color: #f9fafb;
}

:deep(.el-sub-menu__title) {
  color: #166534;
  font-weight: 500;
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item) {
  color: #14532d;
  height: 50px;
  line-height: 50px;
}

:deep(.el-menu-item.is-active) {
  color: #ffffff;
  background-color: #228B22;
}

:deep(.el-menu-item:hover),
:deep(.el-sub-menu__title:hover) {
  background-color: rgba(34, 139, 34, 0.1);
}

/* 主内容区 - 核心修复 */
.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
  height: 100%;          /* 改为100%，基于父容器高度 */
  width: 100%;           /* 宽度100%，消除右侧白边 */
  margin: 0;             /* 清除默认margin */
}

/* 响应式适配 */
@media (max-width: 768px) {
  .title {
    font-size: 18px;
  }
  
  .aside {
    position: fixed;
    z-index: 9;
    height: 100vh;
  }
  
  .main-content {
    margin-left: 64px;
  }
  
  .aside:not(.aside-collapse) + .main-content {
    margin-left: 220px;
  }
}

/* 退出登录弹窗整体样式 */
:deep(.custom-logout-box) {
  width: 420px !important;
  border-radius: 12px !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
  border: none !important;
  overflow: hidden;
  animation: fadeInUp 0.3s ease-out !important;
}

/* 弹窗标题样式 */
:deep(.custom-logout-box .el-message-box__title) {
  font-size: 18px !important;
  font-weight: 600 !important;
  color: #2d3748 !important;
  padding: 20px 24px 0 24px !important;
}

/* 弹窗内容样式 */
:deep(.custom-logout-box .el-message-box__content) {
  padding: 24px !important;
  font-size: 15px !important;
  color: #4a5568 !important;
  line-height: 1.6 !important;
}

/* 弹窗按钮区域 */
:deep(.custom-logout-box .el-message-box__btns) {
  padding: 0 24px 20px 24px !important;
  display: flex !important;
  gap: 12px !important;
  justify-content: flex-end !important;
}

/* 确认退出按钮样式 */
:deep(.logout-confirm-btn) {
  padding: 8px 20px !important;
  border-radius: 8px !important;
  background: linear-gradient(135deg, #e53e3e 0%, #c53030 100%) !important;
  border: none !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
}

:deep(.logout-confirm-btn:hover) {
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(229, 62, 62, 0.25) !important;
}

/* 取消按钮样式 */
:deep(.logout-cancel-btn) {
  padding: 8px 20px !important;
  border-radius: 8px !important;
  background: #f8fafc !important;
  color: #4a5568 !important;
  border: 1px solid #e2e8f0 !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  transition: all 0.3s ease !important;
}

:deep(.logout-cancel-btn:hover) {
  background: #f1f5f9 !important;
  border-color: #cbd5e0 !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05) !important;
}

/* 提示消息样式 */
:deep(.custom-logout-message) {
  border-radius: 8px !important;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1) !important;
  animation: fadeIn 0.3s ease-out !important;
}

/* 弹窗淡入上移动画 */
@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 消息提示淡入动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式适配 */
@media (max-width: 480px) {
  :deep(.custom-logout-box) {
    width: 90% !important;
  }
  
  :deep(.custom-logout-box .el-message-box__btns) {
    flex-direction: column !important;
  }
  
  :deep(.logout-confirm-btn), :deep(.logout-cancel-btn) {
    width: 100% !important;
  }
}

/* 全局滚动条隐藏 - 兜底方案 */
::-webkit-scrollbar {
  display: none;
}

:deep(body) {
  overflow: hidden;
}
</style>