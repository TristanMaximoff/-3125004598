<template>
  <div class="student-home">
    <el-row :gutter="20">
      <!-- 绑定宿舍卡片 -->
      <el-col :span="24" :md="12">
        <el-card class="card">
          <template #header>
            <span>绑定宿舍</span>
          </template>

          <el-form :model="bindForm" label-width="80px">
            <el-form-item label="宿舍号">
              <el-input v-model="bindForm.dormNum" placeholder="格式：东1 101 或 西2 305"></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleBind" :loading="bindLoading">绑定宿舍</el-button>
            </el-form-item>
          </el-form>

          <el-alert
            v-if="isBound"
            title="已绑定宿舍"
            type="success"
            :description="`当前宿舍：${bindForm.dormNum}`"
            show-icon
          />
          <el-alert v-else title="未绑定宿舍" type="warning" description="请先绑定宿舍后再提交报修" show-icon />
        </el-card>
      </el-col>

      <!-- 创建报修单卡片 -->
      <el-col :span="24" :md="12">
        <el-card class="card">
          <template #header>
            <span>创建报修单</span>
          </template>

          <el-form :model="repairForm" label-width="80px">
            <el-form-item label="宿舍号">
              <el-input v-model="repairForm.dormNum" disabled placeholder="请先绑定宿舍"></el-input>
            </el-form-item>
            <el-form-item label="设备类型" required>
              <el-select v-model="repairForm.deviceType" placeholder="请选择" style="width: 100%">
                <el-option label="电灯" value="电灯"></el-option>
                <el-option label="风扇" value="风扇"></el-option>
                <el-option label="水龙头" value="水龙头"></el-option>
                <el-option label="马桶" value="马桶"></el-option>
                <el-option label="网络" value="网络"></el-option>
                <el-option label="其他" value="其他"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="问题描述" required>
              <el-input v-model="repairForm.description" type="textarea" rows="3" placeholder="详细描述问题..."></el-input>
            </el-form-item>
            <el-form-item label="现场图片">
              <el-upload
                :action="uploadUrl"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :before-upload="beforeUpload"
              >
                <el-button type="primary">选择图片</el-button>
              </el-upload>
              <div v-if="repairForm.imageUrl" class="image-preview">
                <el-image :src="repairForm.imageUrl" fit="cover" style="width: 100px; height: 100px; margin-top: 10px"></el-image>
                <el-button size="small" @click="repairForm.imageUrl = ''" style="margin-left: 10px">删除</el-button>
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSubmit" :loading="submitLoading" :disabled="!isBound">提交报修</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import userStore from '../../store/user'

const userStoreIns = userStore()
const bindLoading = ref(false)
const submitLoading = ref(false)

const bindForm = ref({ dormNum: '' })
const repairForm = ref({ dormNum: '', deviceType: '', description: '', imageUrl: '' })
const userDormInfo = ref({})

// 是否已绑定宿舍
const isBound = computed(() => {
  return userDormInfo.value.dormDirection && userDormInfo.value.dormBuilding && userDormInfo.value.dormRoom
})

// 上传地址和请求头
const uploadUrl = 'http://localhost:8080/repairs/upload'
const uploadHeaders = computed(() => ({
  token: userStoreIns.token
}))

// 加载用户信息
const loadUserInfo = async () => {
  try {
    const data = await request.get(`/users/${userStoreIns.userInfo.id}`)
    userDormInfo.value = data

    if (data.dormDirection && data.dormBuilding && data.dormRoom) {
      bindForm.value.dormNum = `${data.dormDirection}${data.dormBuilding} ${data.dormRoom}`
      repairForm.value.dormNum = bindForm.value.dormNum
    }
  } catch (err) {
    console.error('加载用户信息失败', err)
  }
}

onMounted(() => {
  loadUserInfo()
})

// 绑定宿舍
const handleBind = async () => {
  if (!bindForm.value.dormNum) {
    ElMessage.warning('请输入宿舍号，格式如：东1 101')
    return
  }

  const parts = bindForm.value.dormNum.trim().split(' ')
  if (parts.length !== 2) {
    ElMessage.warning('格式错误，请输入：东1 101')
    return
  }

  const buildingPart = parts[0]
  const room = parts[1]

  // 解析宿舍楼号（如：东1 -> 方向：东，楼栋：1）
  const direction = buildingPart.charAt(0)
  const building = buildingPart.slice(1)

  if (!['东', '西', '南', '北'].includes(direction)) {
    ElMessage.warning('宿舍方向必须是东、西、南、北之一')
    return
  }

  if (!/^\d+$/.test(building)) {
    ElMessage.warning('宿舍楼号必须是数字')
    return
  }

  if (!/^\d+$/.test(room)) {
    ElMessage.warning('房间号必须是数字')
    return
  }

  bindLoading.value = true
  try {
    await request.put('/users', {
      id: userStoreIns.userInfo.id,
      dormDirection: direction,
      dormBuilding: parseInt(building),
      dormRoom: parseInt(room),
      dormStatus: 1
    })
    ElMessage.success('宿舍绑定成功')
    // 重新加载用户信息
    await loadUserInfo()
    repairForm.value.dormNum = bindForm.value.dormNum
  } catch (err) {
    ElMessage.error('绑定失败')
  } finally {
    bindLoading.value = false
  }
}

// 上传前校验
const beforeUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt2M = file.size / 1024 / 1024 < 2

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
    return false
  }
  if (!isLt2M) {
    ElMessage.error('图片大小不能超过 2MB')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (res) => {
  if (res.code === 200) {
    repairForm.value.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('图片上传失败')
}

// 提交报修
const handleSubmit = async () => {
  if (!repairForm.value.deviceType) {
    ElMessage.warning('请选择设备类型')
    return
  }
  if (!repairForm.value.description) {
    ElMessage.warning('请填写问题描述')
    return
  }
  if (!isBound.value) {
    ElMessage.warning('请先绑定宿舍')
    return
  }

  submitLoading.value = true
  try {
    await request.post('/repairs', {
      studentAccount: userStoreIns.userInfo.account,
      deviceType: repairForm.value.deviceType,
      description: repairForm.value.description,
      imageUrl: repairForm.value.imageUrl,
      status: '待处理'
    })
    ElMessage.success('报修提交成功')
    // 清空表单
    repairForm.value.deviceType = ''
    repairForm.value.description = ''
    repairForm.value.imageUrl = ''
  } catch (err) {
    ElMessage.error('提交失败')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.student-home {
  padding: 20px;
}
.card {
  height: 100%;
  margin-bottom: 20px;
}
.image-preview {
  display: flex;
  align-items: center;
  margin-top: 10px;
}
</style>