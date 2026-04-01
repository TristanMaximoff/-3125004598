<template>
  <div class="login-container">
    <el-card shadow="hover" class="login-card">
      <h2 class="title">宿舍报修系统 - 登录</h2>
      <el-form :model="loginForm" label-width="80px" class="login-form">
        <el-form-item label="账号">
          <el-input v-model="loginForm.account" placeholder="请输入账号"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleLogin" class="btn">登录</el-button>
          <el-button @click="goRegister">去注册</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import userStore from '../store/user'

const router = useRouter()
const userStoreIns = userStore()

const loginForm = ref({ account: '', password: '' })

const handleLogin = async () => {
  if (!loginForm.value.account || !loginForm.value.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }

  try {
    // ========== 修改：request.post 直接返回 data 字段内容 ==========
    const result = await request.post('/users/login', loginForm.value)
    // result 就是后端返回的 data 字段，即 { token, user }
    userStoreIns.setLogin(result.token, result.user)
    ElMessage.success('登录成功')
    // 根据角色跳转
    if (result.user.role === 'admin') {
      router.push('/admin')
    } else {
      router.push('/student')
    }
  } catch (err) {
    // 错误已在拦截器中处理，这里只做额外提示
    console.error('登录失败', err)
  }
}

const goRegister = () => router.push('/register')
</script>

<style scoped>
.login-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 400px;
  padding: 20px;
  border-radius: 8px;
}
.title {
  text-align: center;
  margin-bottom: 20px;
  color: #409eff;
}
.login-form {
  margin-top: 20px;
}
.btn {
  width: 100%;
}
</style>