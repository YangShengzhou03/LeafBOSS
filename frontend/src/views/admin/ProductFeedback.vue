<template>
  <div class="product-feedback">
    <el-card shadow="never" :body-style="{ padding: '20px' }">
      <el-table :data="feedbacks" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="userName" label="用户" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="content" label="反馈内容" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="反馈时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="danger" @click="deleteFeedback(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
// eslint-disable-next-line no-undef
import { ElMessage, ElMessageBox } from 'element-plus'

import api from '@/services/api.js'

const feedbacks = ref([])

const loadFeedbacks = async () => {
  try {
    const res = await api.admin.getFeedbacks()
    feedbacks.value = res.data || []
  } catch (e) {
    // 接口不存在时静默
  }
}

const deleteFeedback = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该反馈吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.admin.deleteFeedback(id)
    ElMessage.success('反馈已删除')
    loadFeedbacks()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadFeedbacks()
})
</script>

<style scoped>

</style>
