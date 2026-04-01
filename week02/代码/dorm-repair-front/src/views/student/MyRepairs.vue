<template>
  <div class="my-repairs">
    <el-card class="card">
      <template #header>
        <span>我的报修记录</span>
      </template>

      <el-table :data="repairs" border stripe v-loading="loading">
        <el-table-column prop="id" label="编号" width="80"></el-table-column>
        <el-table-column prop="deviceType" label="设备类型" width="100"></el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="150"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="提交时间" width="160"></el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              @click="handleCancel(row.id)"
              :disabled="row.status !== '待处理'"
            >
              取消
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'
import userStore from '../../store/user'

const userStoreIns = userStore()
const repairs = ref([])
const loading = ref(false)

const getStatusType = (status) => {
  const map = {
    '待处理': 'warning',
    '处理中': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}

const loadRepairs = async () => {
  loading.value = true
  try {
    const data = await request.get(`/repairs/my/${userStoreIns.userInfo.account}`)
    repairs.value = data
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRepairs()
})

const handleCancel = async (id) => {
  try {
    await ElMessageBox.confirm('确定取消该报修单吗？', '提示', { type: 'warning' })
    await request.put(`/repairs/cancel/${id}`)
    ElMessage.success('取消成功')
    loadRepairs()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('取消失败')
    }
  }
}
</script>

<style scoped>
.my-repairs {
  padding: 20px;
}
.card {
  max-width: 1000px;
  margin: 0 auto;
}
</style>