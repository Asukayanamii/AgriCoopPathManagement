<script setup>
import { ref } from 'vue'
import { loginApi } from '@/api/login'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

// 登录表单数据
let loginForm = ref({ username: '', password: '' })
const router = useRouter()

// 登录处理逻辑（保持原有逻辑不变）
const login = async () => {
  try {
    // 简单的表单验证
    if (!loginForm.value.username || !loginForm.value.password) {
      ElMessage.warning('请输入用户名和密码')
      return
    }
    
    const result = await loginApi(loginForm.value)
    if (result.code) { // 登录成功
      ElMessage.success('登录成功')
      localStorage.setItem('loginUser', JSON.stringify(result.data))
      await router.push('/') // 跳转首页
    } else {
      ElMessage.error(result.msg || '登录失败，请检查账号密码')
    }
  } catch (error) {
    ElMessage.error('网络异常，请稍后重试')
    console.error('登录请求异常:', error)
  }
}

// 重置表单（保持原有逻辑不变）
const cancel = () => {
  loginForm.value = {
    username: '',
    password: ''
  }
}
</script>

<template>
  <div class="login-container">
    <!-- 背景装饰 -->
    <div class="bg-decoration"></div>
    
    <!-- 登录卡片 -->
    <div class="login-card">
      <!-- 系统标题区域 -->
      <div class="login-header">
        <div class="logo-icon">🌾</div>
        <h1 class="system-title">农业无人机路径规划系统</h1>
        <p class="system-desc">智能规划 · 精准作业</p>
      </div>
      
      <!-- 登录表单区域 -->
      <el-form 
        class="login-form" 
        label-width="0"
        @keyup.enter="login"
      >
        <el-form-item prop="username" class="form-item">
          <div class="input-wrapper">
            <span class="input-icon">👤</span>
            <el-input 
              v-model="loginForm.username" 
              placeholder="请输入用户名"
              class="custom-input"
              size="large"
            />
          </div>
        </el-form-item>
        
        <el-form-item prop="password" class="form-item">
          <div class="input-wrapper">
            <span class="input-icon">🔑</span>
            <el-input 
              type="password" 
              v-model="loginForm.password" 
              placeholder="请输入密码"
              class="custom-input"
              size="large"
            />
          </div>
        </el-form-item>
        
        <el-form-item class="btn-group">
          <el-button 
            type="primary" 
            class="login-btn"
            @click="login"
            size="large"
          >
            登 录
          </el-button>
          <el-button 
            type="default" 
            class="reset-btn"
            @click="cancel"
            size="large"
          >
            重 置
          </el-button>
        </el-form-item>
      </el-form>
      
      <!-- 底部版权信息 -->
      <div class="login-footer">
        <span>© 2026 农业无人机系统 版权所有</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 全局样式重置 - 关键修复：消除滚动条和白边 */
:root {
  margin: 0;
  padding: 0;
}

/* 全局容器样式 - 核心修复 */
.login-container {
  width: 100vw;          /* 视口宽度100% */
  height: 100vh;         /* 视口高度100% */
  display: flex;
  justify-content: center;
  align-items: center;
  position: fixed;       /* 固定定位，完全覆盖视口 */
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;      /* 隐藏溢出内容，消除滚动条 */
  margin: 0;             /* 清除默认margin */
  padding: 0;            /* 清除默认padding */
  background: linear-gradient(135deg, #e8f4f8 0%, #f0f8fb 100%);
}

/* 背景装饰效果 - 确保铺满 */
.bg-decoration {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 80%, rgba(120, 119, 198, 0.1) 0%, transparent 50%),
              radial-gradient(circle at 80% 20%, rgba(255, 255, 255, 0.2) 0%, transparent 50%);
  z-index: 0;
}

/* 登录卡片样式 */
.login-card {
  width: 420px;
  padding: 40px 35px;
  background: #ffffff;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.08);
  position: relative;
  z-index: 1;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}

/* 卡片悬浮效果 */
.login-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.12);
}

/* 登录头部样式 */
.login-header {
  text-align: center;
  margin-bottom: 35px;
}

.logo-icon {
  font-size: 48px;
  margin-bottom: 15px;
  display: inline-block;
  animation: float 3s ease-in-out infinite;
}

.system-title {
  font-size: 24px;
  font-weight: 600;
  color: #2d3748;
  margin: 0 0 8px 0;
  letter-spacing: 0.5px;
}

.system-desc {
  font-size: 14px;
  color: #718096;
  margin: 0;
}

/* 表单样式 */
.login-form {
  margin-bottom: 20px;
  width: 100%;
}

/* 表单项 - 确保居中 */
.form-item {
  width: 100%;
  margin: 0 auto 15px;
  display: flex;
  justify-content: center;
}

/* 输入框容器 - 居中关键样式 */
.input-wrapper {
  position: relative;
  width: 100%;
  max-width: 380px; /* 限制最大宽度，保证视觉协调 */
  margin: 0 auto;
}

/* 简约风格图标 */
.input-icon {
  position: absolute;
  left: 18px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 16px;
  color: #8898aa;
  z-index: 1;
  font-weight: 300;
}

/* 自定义输入框样式 - 确保居中 */
.custom-input {
  padding-left: 48px !important;
  height: 50px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background-color: #f8fafc;
  transition: all 0.3s ease;
  width: 100%;
}

.custom-input:focus {
  border-color: #4299e1;
  box-shadow: 0 0 0 3px rgba(66, 153, 225, 0.1);
  background-color: #ffffff;
}

.custom-input:hover {
  border-color: #cbd5e0;
}

/* 按钮组样式 */
.btn-group {
  margin-top: 25px;
  display: flex;
  gap: 15px;
  width: 100%;
  justify-content: center;
  max-width: 380px;
  margin-left: auto;
  margin-right: auto;
}

.login-btn {
  flex: 1;
  height: 50px;
  border-radius: 10px;
  background: linear-gradient(135deg, #4299e1 0%, #38b2ac 100%);
  border: none;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 15px rgba(66, 153, 225, 0.3);
}

.reset-btn {
  flex: 1;
  height: 50px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  color: #4a5568;
  font-size: 16px;
  font-weight: 500;
  transition: all 0.3s ease;
}

.reset-btn:hover {
  border-color: #cbd5e0;
  background-color: #f8fafc;
  transform: translateY(-2px);
}

/* 底部信息 */
.login-footer {
  text-align: center;
  margin-top: 20px;
  font-size: 12px;
  color: #a0aec0;
}

/* 浮动动画 */
@keyframes float {
  0% { transform: translateY(0px); }
  50% { transform: translateY(-10px); }
  100% { transform: translateY(0px); }
}

/* 响应式适配 */
@media (max-width: 480px) {
  .login-card {
    width: 90%;
    padding: 30px 20px;
  }
  
  .system-title {
    font-size: 20px;
  }
  
  .input-wrapper {
    max-width: 100%;
  }
  
  .btn-group {
    flex-direction: column;
    gap: 10px;
    max-width: 100%;
  }
}

/* 全局滚动条隐藏 - 兜底方案 */
::-webkit-scrollbar {
  display: none;
}
</style>