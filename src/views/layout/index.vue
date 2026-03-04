<template>
  <div class="common-layout">
    <el-container style="height: 100vh;">
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
      
      <el-container>
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
            
            <!-- 地图与地块管理 -->
            <el-sub-menu index="/land">
              <template #title>
                <el-icon><MapLocation /></el-icon>
                <span>地图与地块管理</span>
              </template>
              <el-menu-item index="/land/map">
                <el-icon><Picture /></el-icon>
                <template #title>电子地图加载</template>
              </el-menu-item>
              <el-menu-item index="/land/plot">
                <el-icon><Crop /></el-icon>
                <template #title>地块信息管理</template>
              </el-menu-item>
              <el-menu-item index="/land/boundary">
                <el-icon><Fence /></el-icon>
                <template #title>地块边界编辑</template>
              </el-menu-item>
            </el-sub-menu>
            
            <!-- 路径规划算法核心模块 -->
            <el-sub-menu index="/algorithm">
              <template #title>
                <el-icon><Algorithm /></el-icon>
                <span>路径规划算法</span>
              </template>
              <el-menu-item index="/algorithm/basic">
                <el-icon><LineChart /></el-icon>
                <template #title>基础路径算法</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/optimize">
                <el-icon><TrendCharts /></el-icon>
                <template #title>优化路径算法</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/compare">
                <el-icon><ScaleToOriginal /></el-icon>
                <template #title>算法效果对比</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 无人机参数配置 -->
            <el-sub-menu index="/drone">
              <template #title>
                <el-icon><Airplane /></el-icon>
                <span>无人机参数配置</span>
              </template>
              <el-menu-item index="/drone/info">
                <el-icon><Setting /></el-icon>
                <template #title>无人机型号管理</template>
              </el-menu-item>
              <el-menu-item index="/drone/parameter">
                <el-icon><Sliders /></el-icon>
                <template #title>飞行参数设置</template>
              </el-menu-item>
              <el-menu-item index="/drone/constraint">
                <el-icon><Lock /></el-icon>
                <template #title>飞行约束配置</template>
              </el-menu-item>
            </el-sub-menu>

            <!-- 路径仿真与结果展示 -->
            <el-sub-menu index="/simulation">
              <template #title>
                <el-icon><VideoPlay /></el-icon>
                <span>路径仿真与结果</span>
              </template>
              <el-menu-item index="/simulation/preview">
                <el-icon><Eye /></el-icon>
                <template #title>路径仿真预览</template>
              </el-menu-item>
              <el-menu-item index="/simulation/result">
                <el-icon><DataBoard /></el-icon>
                <template #title>规划结果统计</template>
              </el-menu-item>
              <el-menu-item index="/simulation/log">
                <el-icon><Document /></el-icon>
                <template #title>仿真日志查看</template>
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
// import { ElMessage } from 'element-plus'
// import { log } from 'node:console'
// import { request } from '@/utils/request'
// import { loginornotApi } from '@/api/userloginornot'
const router = useRouter()
const logout = () => {
  //弹出确认框, 如果确认, 则退出登录, 跳转到登录页面
  ElMessageBox.confirm('确认退出登录吗?', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {//确认, 则清空登录信息
    ElMessage.success('退出登录成功')
    localStorage.removeItem('loginUser')
    router.push('/login')//跳转到登录页面
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
      // ElMessage.info('退出登录功能待实现')
      // 这里可以添加退出登录的逻辑
      logout() // 调用退出登录函数
      break
  }
}

let loading = ref(true) // 加载状态
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
/* 全局布局样式 */
.common-layout {
  height: 100vh;
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

/* 主内容区 */
.main-content {
  background-color: #f5f7fa;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 60px);
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
</style>