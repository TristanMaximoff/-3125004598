<template>
  <el-container>
    <el-header>
      <h2>管理员端 - 宿舍报修系统</h2>
      <el-button @click="logout">退出登录</el-button>
    </el-header>
    <el-main>
      <router-view></router-view>
    </el-main>
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import userStore from '../../store/user'

const router = useRouter()
const userStoreIns = userStore()

const logout = () => {
  userStoreIns.logout()
  router.push('/login')
}
</script>

<style scoped>
.el-header {
  background-color: #1989fa;
  color: white;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.el-main {
  padding: 20px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 60px);
}
</style>