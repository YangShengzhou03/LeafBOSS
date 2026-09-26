<template>
  <div class="notice-management">
    <el-card shadow="never" :body-style="{ padding: '20px' }">
      <div class="header">
        <el-button type="primary" @click="dialogVisible = true">
          新建通知
        </el-button>
      </div>

      <el-table :data="notices" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="productName" label="商品" width="120" :show-overflow-tooltip="true" />
        <el-table-column prop="content" label="内容" show-overflow-tooltip />
        <el-table-column prop="level" label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="noticeLevelType(row.level)">{{ noticeLevelText(row.level) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">{{ (row.createdAt || '').replace('T', ' ') }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" link @click="editNotice(row)">编辑</el-button>
            <el-button type="danger" link @click="deleteNotice(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑通知' : '新建通知'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="商品">
          <el-select v-model="form.productName" placeholder="请选择商品或受众">
            <el-option-group label="按受众">
              <el-option label="管理员" value="管理员" />
              <el-option label="普通用户" value="普通用户" />
              <el-option label="代理商" value="代理商" />
            </el-option-group>
            <el-option-group label="按商品">
              <el-option v-for="p in productList" :key="p.id" :label="p.name" :value="p.name" />
            </el-option-group>
          </el-select>
        </el-form-item>
        <el-form-item label="级别">
          <el-select v-model="form.level" placeholder="请选择通知级别">
            <el-option label="Primary" value="primary" />
            <el-option label="Success" value="success" />
            <el-option label="Info" value="info" />
            <el-option label="Warning" value="warning" />
            <el-option label="Danger" value="danger" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="4" placeholder="请输入通知内容" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="createNotice">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/services/api.js'

const notices = ref([])
const productList = ref([])
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({ productName: '', level: 'info', content: '' })

const levelTypeMap = { primary: 'primary', success: 'success', info: 'info', warning: 'warning', danger: 'danger', error: 'danger' }
const levelTextMap = { primary: '主要', success: '成功', info: '提示', warning: '警告', danger: '危险', error: '危险' }

const noticeLevelType = (level) => levelTypeMap[level] || 'info'
const noticeLevelText = (level) => levelTextMap[level] || level

const loadNotices = async () => {
  try {
    const res = await api.admin.getNotices()
    notices.value = res.data || []
  } catch (e) {
    // 接口不存在时静默
  }
}

const loadProducts = async () => {
  try {
    const response = await api.admin.getProductList({
      page: 1,
      size: 1000,
      status: 'active'
    })
    if (response && response.data) {
      productList.value = response.data.records || response.data.content || []
    } else {
      productList.value = []
    }
  } catch (e) {
    productList.value = []
  }
}

const resetForm = () => {
  editingId.value = null
  form.value = { productName: '', level: 'info', content: '' }
}

const editNotice = (row) => {
  editingId.value = row.id
  form.value = {
    productName: row.productName,
    level: row.level,
    content: row.content
  }
  dialogVisible.value = true
}

const createNotice = async () => {
  if (!form.value.productName || !form.value.level || !form.value.content) {
    ElMessage.warning('请选择商品、级别并填写内容')
    return
  }
  try {
    if (editingId.value) {
      await api.admin.updateNotice(editingId.value, form.value)
      ElMessage.success('通知已更新')
    } else {
      await api.admin.createNotice(form.value)
      ElMessage.success('通知已创建')
    }
    dialogVisible.value = false
    resetForm()
    loadNotices()
  } catch (e) {
    ElMessage.error(e.message || '保存失败')
  }
}

const deleteNotice = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该通知吗？', '提示', { type: 'warning' })
  } catch {
    return
  }
  try {
    await api.admin.deleteNotice(id)
    ElMessage.success('通知已删除')
    loadNotices()
  } catch (e) {
    ElMessage.error(e.message || '删除失败')
  }
}

onMounted(() => {
  loadNotices()
  loadProducts()
})
</script>

<style scoped>
.notice-management .header {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  margin-bottom: 16px;
}
</style>
