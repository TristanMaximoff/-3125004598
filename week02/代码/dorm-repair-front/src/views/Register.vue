<template>
  <div class="register-container">
    <el-card shadow="hover" class="register-card">
      <h2 class="title">宿舍报修系统 - 注册</h2>
      <el-form :model="registerForm" label-width="80px" class="register-form">
        <el-form-item label="账号">
          <el-input v-model="registerForm.account" placeholder="学生：3125/3225开头；管理员：0025开头"></el-input>
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="registerForm.password" type="password" placeholder="请输入密码"></el-input>
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="registerForm.role">
            <el-radio label="student">学生</el-radio>
            <el-radio label="admin">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleRegister" class="btn">注册</el-button>
          <el-button @click="goLogin">返回登录</el-button>
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

const router = useRouter()

const registerForm = ref({ account: '', password: '', role: 'student' })

const handleRegister = async () => {
  if (!registerForm.value.account || !registerForm.value.password) {
    ElMessage.warning('请填写完整信息')
    return
  }

  try {
    // ========== 修改：注册成功时 data 是字符串消息 ==========
    const result = await request.post('/users/register', registerForm.value)
    ElMessage.success(result || '注册成功')
    router.push('/login')
  } catch (err) {
    // 错误已在拦截器中处理
    console.error('注册失败', err)
  }
}

const goLogin = () => router.push('/login')
</script>

<style scoped>
.register-container {
  width: 100vw;
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.register-card {
  width: 400px;
  padding: 20px;
  border-radius: 8px;
}
.title {
  text-align: center;
  margin-bottom: 20px;
  color: #409eff;
}
.register-form {
  margin-top: 20px;
}
.btn {
  width: 100%;
}
</style>