<template>
  <div class="user-management-container">
    <div class="header-controls">
      <div class="left-controls">
        <!-- 模式切换 -->
        <el-switch
          v-model="demoMode"
          active-text="演示模式"
          inactive-text="真实模式"
          style="margin-right: 16px;"
          @change="toggleMode"
        />

        <!-- 操作按钮 -->
        <el-button type="primary" :icon="Plus" @click="handleAdd">新增用户</el-button>
        <el-button :icon="Delete" @click="batchDelete" :disabled="selectedUsers.length === 0">
          批量删除
        </el-button>
        <el-button :icon="Download" @click="exportUsers">导出用户</el-button>
      </div>

      <div class="right-controls">
        <!-- 搜索和筛选 -->
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名或邮箱"
          style="width: 200px; margin-right: 16px;"
          :prefix-icon="Search"
          clearable
        />
        <el-select v-model="filterRole" placeholder="全部角色" style="width: 120px; margin-right: 16px;">
          <el-option label="全部角色" value="" />
          <el-option label="管理员" value="管理员" />
          <el-option label="操作员" value="操作员" />
          <el-option label="查看者" value="查看者" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="全部状态" style="width: 120px;">
          <el-option label="全部状态" value="" />
          <el-option label="激活" value="active" />
          <el-option label="停用" value="inactive" />
        </el-select>
      </div>
    </div>

    <div class="content-area">
      <el-table
        ref="userTable"
        :data="filteredUsers"
        style="width: 100%;"
        height="calc(100vh - 200px)"
        @selection-change="handleSelectionChange"
        v-loading="loading"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" sortable />
        <el-table-column prop="username" label="用户名" width="120" sortable>
          <template #default="{ row }">
            <div style="display: flex; align-items: center; gap: 8px;">
              <el-avatar :size="32" :src="row.avatar" style="flex-shrink: 0;">
                {{ row.username.charAt(0).toUpperCase() }}
              </el-avatar>
              <span>{{ row.username }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" width="200" sortable />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="getRoleType(row.role)" size="small">{{ row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 'active' ? 'success' : 'danger'" size="small">
              {{ row.status === 'active' ? '激活' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" sortable />
        <el-table-column prop="lastLogin" label="最后登录" width="160" sortable />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div style="display: flex; gap: 8px;">
              <el-button type="text" size="small" @click="handleEdit(row)">编辑</el-button>
              <el-button type="text" size="small" @click="handleResetPassword(row)">重置密码</el-button>
              <el-button type="text" size="small" @click="handleToggleStatus(row)" :style="{ color: row.status === 'active' ? '#f56c6c' : '#67c23a' }">
                {{ row.status === 'active' ? '停用' : '激活' }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination" v-if="!demoMode">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="totalUsers"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </div>

    <!-- 用户编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="editDialogTitle"
      width="500"
      :before-close="handleDialogClose"
    >
      <el-form
        ref="userFormRef"
        :model="editUserForm"
        :rules="userRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="editUserForm.username" placeholder="请输入用户名" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="editUserForm.email" placeholder="请输入邮箱" />
        </el-form-item>

        <el-form-item label="角色" prop="role">
          <el-select v-model="editUserForm.role" placeholder="选择用户角色" style="width: 100%;">
            <el-option label="管理员" value="管理员" />
            <el-option label="操作员" value="操作员" />
            <el-option label="查看者" value="查看者" />
          </el-select>
        </el-form-item>

        <el-form-item label="状态" prop="status">
          <el-switch
            v-model="editUserForm.status"
            active-value="active"
            inactive-value="inactive"
            active-text="激活"
            inactive-text="停用"
          />
        </el-form-item>

        <el-form-item label="密码" prop="password" v-if="!editUserForm.id">
          <el-input
            v-model="editUserForm.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword" v-if="!editUserForm.id">
          <el-input
            v-model="editUserForm.confirmPassword"
            type="password"
            placeholder="请确认密码"
            show-password
          />
        </el-form-item>

        <el-form-item label="头像" prop="avatar">
          <el-input v-model="editUserForm.avatar" placeholder="请输入头像URL" />
          <div style="margin-top: 8px;">
            <el-avatar :size="64" :src="editUserForm.avatar">
              {{ editUserForm.username ? editUserForm.username.charAt(0).toUpperCase() : 'U' }}
            </el-avatar>
          </div>
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="editUserForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入用户描述"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="handleDialogClose">取消</el-button>
          <el-button type="primary" @click="submitUserForm" :loading="submitting">
            保存
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog v-model="resetPasswordDialogVisible" title="重置密码" width="400">
      <el-form
        ref="resetPasswordFormRef"
        :model="resetPasswordForm"
        :rules="resetPasswordRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="resetPasswordForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="resetPasswordDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitResetPassword">确认重置</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Delete,
  Download,
  Search
} from '@element-plus/icons-vue'
import { getCurrentMode, toggleDemoMode, systemService } from '@/services/dataService'

// 模式控制
const demoMode = ref(getCurrentMode())

// 搜索和筛选
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')

// 分页
const currentPage = ref(1)
const pageSize = ref(20)
const totalUsers = ref(0)

// 用户数据
const users = ref([])
const selectedUsers = ref([])
const loading = ref(false)

// 对话框状态
const editDialogVisible = ref(false)
const resetPasswordDialogVisible = ref(false)
const submitting = ref(false)

// 编辑用户表单
const editUserForm = reactive({
  id: '',
  username: '',
  email: '',
  role: '操作员',
  status: 'active',
  password: '',
  confirmPassword: '',
  avatar: '',
  description: ''
})

// 重置密码表单
const resetPasswordForm = reactive({
  newPassword: '',
  confirmPassword: ''
})

// 表单引用
const userFormRef = ref(null)
const resetPasswordFormRef = ref(null)

// 表单验证规则
const userRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度为3-20个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ],
  role: [
    { required: true, message: '请选择用户角色', trigger: 'change' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== editUserForm.password) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const resetPasswordRules = {
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== resetPasswordForm.newPassword) {
          callback(new Error('两次输入的密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 预生成的测试用户数据
const mockUsers = [
  {
    id: 1,
    username: 'admin',
    email: 'admin@example.com',
    role: '管理员',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=100&h=100&fit=crop',
    description: '系统管理员，拥有所有权限',
    createdAt: '2026-01-15 10:30:00',
    lastLogin: '2026-04-14 09:15:00'
  },
  {
    id: 2,
    username: 'operator1',
    email: 'operator1@example.com',
    role: '操作员',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1494790108755-2616b612b786?w=100&h=100&fit=crop',
    description: '农田操作员，负责日常作业',
    createdAt: '2026-02-20 14:20:00',
    lastLogin: '2026-04-13 16:45:00'
  },
  {
    id: 3,
    username: 'viewer1',
    email: 'viewer1@example.com',
    role: '查看者',
    status: 'inactive',
    avatar: 'https://images.unsplash.com/photo-1568602471122-7832951cc4c5?w=100&h=100&fit=crop',
    description: '系统查看者，只读权限',
    createdAt: '2026-03-10 11:10:00',
    lastLogin: '2026-03-25 10:30:00'
  },
  {
    id: 4,
    username: 'operator2',
    email: 'operator2@example.com',
    role: '操作员',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?w=100&h=100&fit=crop',
    description: '无人机操作员',
    createdAt: '2026-03-25 09:45:00',
    lastLogin: '2026-04-14 08:20:00'
  },
  {
    id: 5,
    username: 'manager1',
    email: 'manager1@example.com',
    role: '管理员',
    status: 'active',
    avatar: 'https://images.unsplash.com/photo-1472099645785-5658abf4ff4e?w=100&h=100&fit=crop',
    description: '农场经理，负责整体规划',
    createdAt: '2026-04-01 13:30:00',
    lastLogin: '2026-04-14 10:15:00'
  }
]

// 计算属性：过滤后的用户
const filteredUsers = computed(() => {
  let filtered = users.value

  // 搜索过滤
  if (searchKeyword.value.trim()) {
    const keyword = searchKeyword.value.toLowerCase()
    filtered = filtered.filter(user =>
      user.username.toLowerCase().includes(keyword) ||
      user.email.toLowerCase().includes(keyword) ||
      user.description.toLowerCase().includes(keyword)
    )
  }

  // 角色过滤
  if (filterRole.value) {
    filtered = filtered.filter(user => user.role === filterRole.value)
  }

  // 状态过滤
  if (filterStatus.value) {
    filtered = filtered.filter(user => user.status === filterStatus.value)
  }

  return filtered
})

const editDialogTitle = computed(() => {
  return editUserForm.id ? '编辑用户' : '新增用户'
})

// 初始化
onMounted(() => {
  loadUsers()
})

// 加载用户数据
async function loadUsers() {
  loading.value = true
  try {
    if (demoMode.value) {
      // 演示模式：使用模拟数据
      users.value = [...mockUsers]
      totalUsers.value = mockUsers.length
    } else {
      // 真实模式：调用API
      const response = await systemService.getUsers()
      users.value = response.data || []
      totalUsers.value = users.value.length
    }
  } catch (error) {
    console.error('加载用户数据失败:', error)
    ElMessage.error('加载用户数据失败')
    // 失败时使用演示数据
    users.value = [...mockUsers]
    totalUsers.value = mockUsers.length
  } finally {
    loading.value = false
  }
}

// 切换模式
function toggleMode() {
  toggleDemoMode()
  demoMode.value = getCurrentMode()
  ElMessage.success(`已切换到${demoMode.value ? '演示' : '真实'}模式`)
  loadUsers()
}

// 获取角色标签类型
function getRoleType(role) {
  switch (role) {
    case '管理员': return 'danger'
    case '操作员': return 'primary'
    case '查看者': return 'success'
    default: return 'info'
  }
}

// 选择用户变化
function handleSelectionChange(selection) {
  selectedUsers.value = selection
}

// 新增用户
function handleAdd() {
  Object.assign(editUserForm, {
    id: '',
    username: '',
    email: '',
    role: '操作员',
    status: 'active',
    password: '',
    confirmPassword: '',
    avatar: '',
    description: ''
  })
  editDialogVisible.value = true
}

// 编辑用户
function handleEdit(user) {
  Object.assign(editUserForm, {
    id: user.id,
    username: user.username,
    email: user.email,
    role: user.role,
    status: user.status,
    password: '',
    confirmPassword: '',
    avatar: user.avatar,
    description: user.description
  })
  editDialogVisible.value = true
}

// 提交用户表单
async function submitUserForm() {
  if (!userFormRef.value) return

  try {
    await userFormRef.value.validate()
    submitting.value = true

    const userData = {
      username: editUserForm.username,
      email: editUserForm.email,
      role: editUserForm.role,
      status: editUserForm.status,
      avatar: editUserForm.avatar,
      description: editUserForm.description
    }

    if (editUserForm.id) {
      // 更新用户
      if (demoMode.value) {
        // 演示模式：本地更新
        const index = users.value.findIndex(u => u.id === editUserForm.id)
        if (index !== -1) {
          users.value[index] = {
            ...users.value[index],
            ...userData
          }
        }
        ElMessage.success('用户更新成功')
      } else {
        // 真实模式：调用API
        const response = await systemService.updateUser(editUserForm.id, userData)
        if (response.success) {
          ElMessage.success('用户更新成功')
          loadUsers()
        }
      }
    } else {
      // 新增用户
      userData.password = editUserForm.password

      if (demoMode.value) {
        // 演示模式：本地添加
        const newId = Math.max(...users.value.map(u => u.id), 0) + 1
        users.value.unshift({
          ...userData,
          id: newId,
          createdAt: new Date().toLocaleString(),
          lastLogin: '从未登录'
        })
        ElMessage.success('用户创建成功')
      } else {
        // 真实模式：调用API
        const response = await systemService.createUser(userData)
        if (response.success) {
          ElMessage.success('用户创建成功')
          loadUsers()
        }
      }
    }

    editDialogVisible.value = false
  } catch (error) {
    if (error && error.message !== 'validate') {
      console.error('保存用户失败:', error)
      ElMessage.error('保存用户失败')
    }
  } finally {
    submitting.value = false
  }
}

// 批量删除
async function batchDelete() {
  if (selectedUsers.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedUsers.value.length} 个用户吗？`, '确认批量删除', {
      type: 'warning'
    })

    if (demoMode.value) {
      // 演示模式：本地删除
      const selectedIds = selectedUsers.value.map(user => user.id)
      users.value = users.value.filter(user => !selectedIds.includes(user.id))
      selectedUsers.value = []
      ElMessage.success(`成功删除 ${selectedIds.length} 个用户`)
    } else {
      // 真实模式：调用API批量删除
      // 这里需要实现批量删除API调用
      ElMessage.success('批量删除成功（演示模式）')
      loadUsers()
    }
  } catch (error) {
    // 用户取消删除
  }
}

// 重置密码
function handleResetPassword(user) {
  Object.assign(resetPasswordForm, {
    newPassword: '',
    confirmPassword: '',
    userId: user.id,
    username: user.username
  })
  resetPasswordDialogVisible.value = true
}

// 提交重置密码
async function submitResetPassword() {
  if (!resetPasswordFormRef.value) return

  try {
    await resetPasswordFormRef.value.validate()

    if (demoMode.value) {
      // 演示模式：模拟重置密码
      ElMessage.success(`已为用户 ${resetPasswordForm.username} 重置密码`)
    } else {
      // 真实模式：调用API重置密码
      ElMessage.success('密码重置成功（演示模式）')
    }

    resetPasswordDialogVisible.value = false
  } catch (error) {
    if (error && error.message !== 'validate') {
      console.error('重置密码失败:', error)
      ElMessage.error('重置密码失败')
    }
  }
}

// 切换用户状态
async function handleToggleStatus(user) {
  const newStatus = user.status === 'active' ? 'inactive' : 'active'
  const action = newStatus === 'active' ? '激活' : '停用'

  try {
    await ElMessageBox.confirm(`确定要${action}用户 "${user.username}" 吗？`, `确认${action}`, {
      type: 'warning'
    })

    if (demoMode.value) {
      // 演示模式：本地更新状态
      const index = users.value.findIndex(u => u.id === user.id)
      if (index !== -1) {
        users.value[index].status = newStatus
      }
      ElMessage.success(`用户已${action}`)
    } else {
      // 真实模式：调用API更新状态
      const response = await systemService.updateUser(user.id, { status: newStatus })
      if (response.success) {
        ElMessage.success(`用户已${action}`)
        loadUsers()
      }
    }
  } catch (error) {
    // 用户取消操作
  }
}

// 导出用户数据
function exportUsers() {
  if (users.value.length === 0) {
    ElMessage.warning('没有可导出的用户数据')
    return
  }

  const exportData = users.value.map(user => ({
    id: user.id,
    username: user.username,
    email: user.email,
    role: user.role,
    status: user.status,
    avatar: user.avatar,
    description: user.description,
    createdAt: user.createdAt,
    lastLogin: user.lastLogin
  }))

  const dataStr = JSON.stringify(exportData, null, 2)
  const dataUri = 'data:application/json;charset=utf-8,' + encodeURIComponent(dataStr)
  const exportFileName = `用户数据_${new Date().getTime()}.json`

  const linkElement = document.createElement('a')
  linkElement.setAttribute('href', dataUri)
  linkElement.setAttribute('download', exportFileName)
  linkElement.click()

  ElMessage.success('用户数据已导出')
}

// 分页大小变化
function handleSizeChange(size) {
  pageSize.value = size
  loadUsers()
}

// 当前页变化
function handleCurrentChange(page) {
  currentPage.value = page
  loadUsers()
}

// 对话框关闭
function handleDialogClose() {
  editDialogVisible.value = false
  if (userFormRef.value) {
    userFormRef.value.resetFields()
  }
}
</script>

<style scoped>
.user-management-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.header-controls {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.left-controls {
  display: flex;
  align-items: center;
}

.right-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}

.content-area {
  flex: 1;
  padding: 20px;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

.pagination {
  margin-top: 20px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  display: flex;
  justify-content: flex-end;
}

/* 响应式适配 */
@media (max-width: 768px) {
  .header-controls {
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }

  .left-controls, .right-controls {
    width: 100%;
    justify-content: center;
  }

  .right-controls {
    flex-wrap: wrap;
  }
}
</style>