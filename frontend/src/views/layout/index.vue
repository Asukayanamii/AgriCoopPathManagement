<template>
  <div class="common-layout">
    <el-container class="el-container-main">
      <el-header class="header">
        <div class="toggle-btn" @click="toggleCollapse">
          <el-icon :size="20"><Menu /></el-icon>
        </div>
        <span class="title">农业无人机与小车协同算法展示系统</span>
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
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon> 退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-container class="el-container-sub">
        <el-aside :width="isCollapse ? '64px' : '220px'" class="aside" :class="{ 'aside-collapse': isCollapse }">
          <el-menu router :collapse="isCollapse" :collapse-transition="true" default-active="/index" unique-opened>
            <el-menu-item index="/index">
              <el-icon><Promotion /></el-icon>
              <template #title>系统首页</template>
            </el-menu-item>
            <el-sub-menu index="/algorithm">
              <template #title>
                <el-icon><Algorithm /></el-icon>
                <span>算法模块</span>
              </template>
              <el-menu-item index="/algorithm/cluster">
                <el-icon><PieChart /></el-icon>
                <template #title>聚类算法</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/path-planning">
                <el-icon><Connection /></el-icon>
                <template #title>路径规划</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/resource-search">
                <el-icon><Search /></el-icon>
                <template #title>资源搜索</template>
              </el-menu-item>
              <el-menu-item index="/algorithm/pipeline">
                <el-icon><VideoPlay /></el-icon>
                <template #title>协同流水线</template>
              </el-menu-item>
            </el-sub-menu>
            <el-menu-item index="/map-edit">
              <el-icon><Picture /></el-icon>
              <template #title>地图预处理</template>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-main class="main-content">
          <router-view></router-view>
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const isCollapse = ref(false)
const toggleCollapse = () => { isCollapse.value = !isCollapse.value }

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出当前账号吗？', '退出登录', {
      confirmButtonText: '确认退出', cancelButtonText: '取消', type: 'warning'
    }).then(() => {
      localStorage.removeItem('loginUser')
      setTimeout(() => router.push('/login'), 500)
    }).catch(() => {})
  }
}
</script>

<style scoped>
* { margin: 0; padding: 0; box-sizing: border-box; }
.common-layout { width: 100vw; height: 100vh; position: fixed; top: 0; left: 0; overflow: hidden; }
.el-container-main { height: 100%; width: 100%; overflow: hidden; }
.el-container-sub { height: calc(100% - 60px); width: 100%; overflow: hidden; }
.header {
  background: linear-gradient(to right, #006400, #228B22, #32CD32);
  display: flex; align-items: center; padding: 0 20px; height: 60px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1); z-index: 10;
}
.toggle-btn { color: white; cursor: pointer; margin-right: 20px; padding: 8px; border-radius: 4px; }
.toggle-btn:hover { background: rgba(255,255,255,0.2); }
.title { color: white; font-size: 22px; font-family: 微软雅黑; font-weight: bold; flex: 1; text-align: center; }
.user-avatar { margin-left: auto; display: flex; align-items: center; }
.avatar-wrapper { display: flex; align-items: center; cursor: pointer; color: white; gap: 8px; padding: 4px 8px; border-radius: 20px; }
.avatar-wrapper:hover { background: rgba(255,255,255,0.2); }
.aside { border-right: 1px solid #e5e7eb; background: #f9fafb; transition: all 0.3s; overflow: hidden; height: 100%; }
.aside-collapse { width: 64px !important; }
:deep(.el-menu) { border-right: none; height: 100%; background: #f9fafb; }
:deep(.el-sub-menu__title) { color: #166534; font-weight: 500; height: 50px; line-height: 50px; }
:deep(.el-menu-item) { color: #14532d; height: 50px; line-height: 50px; }
:deep(.el-menu-item.is-active) { color: #fff; background: #228B22; }
:deep(.el-menu-item:hover), :deep(.el-sub-menu__title:hover) { background: rgba(34,139,34,0.1); }
.main-content { background: #f5f7fa; padding: 20px; overflow-y: auto; height: 100%; }
</style>
