<template>
  <div class="repair-detail">
    <el-card class="card">
      <template #header>
        <span>报修详情 - #{{ repair.id }}</span>
      </template>

      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="编号">{{ repair.id }}</el-descriptions-item>
        <el-descriptions-item label="学生账号">{{ repair.studentAccount }}</el-descriptions-item>
        <el-descriptions-item label="设备类型">{{ repair.deviceType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(repair.status)">{{ repair.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="问题描述" :span="2">{{ repair.description }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ repair.createTime }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ repair.updateTime || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="现场图片" :span="2" v-if="repair.imageUrl">
          <el-image :src="repair.imageUrl" fit="cover" style="width: 300px; max-height: 300px" :preview-src-list="[repair.imageUrl]"></el-image>
        </el-descriptions-item>
        <el-descriptions-item label="现场图片" :span="2" v-else>无图片</el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button @click="goBack">返回列表</el-button>
        <el-button type="primary" @click="handleUpdateStatus" v-if="repair.status !== '已完成' && repair.status !== '已取消'">
          更新状态
        </el-button>
      </div>

      <!-- 状态更新对话框 -->
      <el-dialog v-model="dialogVisible" title="更新状态" width="300px">
        <el-select v-model="newStatus" placeholder="请选择状态" style="width: 100%">
          <el-option label="待处理" value="待处理"></el-option>
          <el-option label="处理中" value="处理中"></el-option>
          <el-option label="已完成" value="已完成"></el-option>
        </el-select>
        <template #footer>
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmUpdate">确定</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const route = useRoute()
const router = useRouter()
const repair = ref({})
const loading = ref(false)
const dialogVisible = ref(false)
const newStatus = ref('')

const getStatusType = (status) => {
  const map = {
    '待处理': 'warning',
    '处理中': 'primary',
    '已完成': 'success',
    '已取消': 'info'
  }
  return map[status] || 'info'
}

const loadDetail = async () => {
  loading.value = true
  try {
    const data = await request.get(`/repairs/${route.params.id}`)
    repair.value = data
  } catch (err) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDetail()
})

const handleUpdateStatus = () => {
  newStatus.value = repair.value.status
  dialogVisible.value = true
}

const confirmUpdate = async () => {
  try {
    await request.put(`/repairs/${repair.value.id}?status=${newStatus.value}`)
    ElMessage.success('状态更新成功')
    dialogVisible.value = false
    loadDetail()
  } catch (err) {
    ElMessage.error('更新失败')
  }
}

const goBack = () => router.push('/admin')
</script>

<style scoped>
.repair-detail {
  padding: 20px;
}
.card {
  max-width: 800px;
  margin: 0 auto;
}
.actions {
  margin-top: 20px;
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>