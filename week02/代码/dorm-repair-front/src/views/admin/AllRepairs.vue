<template>
  <div class="all-repairs">
    <el-card class="card">
      <template #header>
        <div class="card-header">
          <span>报修管理</span>
          <el-select v-model="statusFilter" placeholder="筛选状态" clearable size="small" style="width: 120px">
            <el-option label="全部" value=""></el-option>
            <el-option label="待处理" value="待处理"></el-option>
            <el-option label="处理中" value="处理中"></el-option>
            <el-option label="已完成" value="已完成"></el-option>
            <el-option label="已取消" value="已取消"></el-option>
          </el-select>
        </div>
      </template>

      <el-table :data="filteredRepairs" border stripe v-loading="loading">
        <el-table-column prop="id" label="编号" width="80"></el-table-column>
        <el-table-column prop="studentAccount" label="学生账号" width="120"></el-table-column>
        <el-table-column prop="deviceType" label="设备类型" width="100"></el-table-column>
        <el-table-column prop="description" label="问题描述" min-width="150"></el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small" @change="handleUpdateStatus(row.id, row.status)" style="width: 100px">
              <el-option label="待处理" value="待处理"></el-option>
              <el-option label="处理中" value="处理中"></el-option>
              <el-option label="已完成" value="已完成"></el-option>
            </el-select>
            <el-button type="danger" size="small" @click="handleDelete(row.id)" style="margin-left: 8px">删除</el-button>
            <el-button size="small" @click="goDetail(row.id)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const router = useRouter()
const repairs = ref([])
const statusFilter = ref('')
const loading = ref(false)

const filteredRepairs = computed(() => {
  return statusFilter.value ? repairs.value.filter(r => r.status === statusFilter.value) : repairs.value
})

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
    const data = await request.get('/repairs')
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

const handleUpdateStatus = async (id, status) => {
  try {
    await request.put(`/repairs/${id}?status=${status}`)
    ElMessage.success('状态更新成功')
    loadRepairs()
  } catch (err) {
    ElMessage.error('更新失败')
  }
}

const handleDelete = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该报修单吗？', '提示', { type: 'warning' })
    await request.delete(`/repairs/${id}`)
    ElMessage.success('删除成功')
    loadRepairs()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

const goDetail = (id) => router.push(`/admin/detail/${id}`)
</script>

<style scoped>
.all-repairs {
  padding: 20px;
}
.card {
  max-width: 1200px;
  margin: 0 auto;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>