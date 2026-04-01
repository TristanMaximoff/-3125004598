<template>
  <el-container>
    <el-header>
      <div style="display: flex; align-items: center; gap: 30px;">
        <h2 style="margin: 0;">学生端 - 宿舍报修系统</h2>
        <div style="display: flex; gap: 20px;">
          <span style="cursor: pointer; color: white;" @click="goHome">首页</span>
          <span style="cursor: pointer; color: white;" @click="goRepairs">我的报修</span>
        </div>
      </div>
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

const goHome = () => {
  router.push('/student')
}

const goRepairs = () => {
  router.push('/student/repairs')
}

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
</style>