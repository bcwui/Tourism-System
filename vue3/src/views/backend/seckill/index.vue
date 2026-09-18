<template>
  <div class="seckill-management">
    <div class="page-header">
      <h1 class="page-title">秒杀管理</h1>
      <p class="page-subtitle">Seckill Management</p>
    </div>

    <div class="action-bar">
      <div class="action-right">
        <el-button type="primary" @click="showAddDialog" class="add-btn">
          <el-icon><Plus /></el-icon> 添加秒杀活动
        </el-button>
      </div>
    </div>

    <!-- 秒杀活动列表表格 -->
    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="activityList"
        border
        style="width: 100%"
        class="seckill-table"
      >
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="ticketName" label="关联门票" min-width="140">
          <template #default="scope">
            <span class="ticket-name">{{ scope.row.ticketName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="originalPrice" label="原价" width="90">
          <template #default="scope">
            <span class="price">¥{{ scope.row.originalPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="seckillPrice" label="秒杀价" width="100">
          <template #default="scope">
            <span class="seckill-price">¥{{ scope.row.seckillPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="seckillStock" label="DB库存" width="80" />
        <el-table-column label="Redis库存" width="90">
          <template #default="scope">
            <span :class="scope.row.redisStock > 0 ? 'redis-stock' : 'redis-stock-zero'">
              {{ scope.row.redisStock != null ? scope.row.redisStock : '-' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="limitPerUser" label="限购" width="70" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="scope">
            <el-tag v-if="computeStatus(scope.row) === 'upcoming'" type="info">未开始</el-tag>
            <el-tag v-else-if="computeStatus(scope.row) === 'active'" type="success">进行中</el-tag>
            <el-tag v-else type="danger">已结束</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button type="warning" size="small" plain :loading="preloadingId === scope.row.id" @click="handlePreload(scope.row)" class="action-btn">
              <el-icon><Lightning /></el-icon> 预热
            </el-button>
            <el-button type="primary" size="small" plain @click="handleEdit(scope.row)" class="action-btn">
              <el-icon><Edit /></el-icon> 编辑
            </el-button>
            <el-button type="danger" size="small" plain @click="handleDelete(scope.row)" class="action-btn">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          :current-page="currentPage"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 添加/编辑秒杀活动对话框 -->
    <el-dialog
      :title="isEdit ? '编辑秒杀活动' : '添加秒杀活动'"
      v-model="dialogVisible"
      width="55%"
      class="seckill-dialog"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
        :disabled="formLoading"
      >
        <el-form-item label="关联门票" prop="ticketId">
          <el-select
            v-model="formData.ticketId"
            placeholder="请选择门票"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in ticketOptions"
              :key="item.id"
              :label="`${item.ticketName} (¥${item.price} / 库存${item.stock})`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="秒杀价格" prop="seckillPrice">
              <el-input-number v-model="formData.seckillPrice" :precision="2" :min="0.01" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="秒杀库存" prop="seckillStock">
              <el-input-number v-model="formData.seckillStock" :min="1" :step="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="每人限购" prop="limitPerUser">
              <el-input-number v-model="formData.limitPerUser" :min="1" :max="10" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-date-picker
                v-model="formData.startTime"
                type="datetime"
                placeholder="选择开始时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-date-picker
                v-model="formData.endTime"
                type="datetime"
                placeholder="选择结束时间"
                format="YYYY-MM-DD HH:mm:ss"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="formLoading">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Lightning } from '@element-plus/icons-vue'
import { adminGetSeckillList, adminCreateSeckill, adminUpdateSeckill, adminDeleteSeckill, adminPreloadSeckillStock } from '@/api/seckill'
import request from '@/utils/request'

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const activityList = ref([])
const loading = ref(false)
const preloadingId = ref(null)
const now = ref(Date.now())

let statusTimer = null

const parseTime = (str) => {
  if (!str) return 0
  return new Date(str.replace(' ', 'T')).getTime()
}

const computeStatus = (row) => {
  const t = now.value
  const start = parseTime(row.startTime)
  const end = parseTime(row.endTime)
  if (t < start) return 'upcoming'
  if (t <= end) return 'active'
  return 'ended'
}

const dialogVisible = ref(false)
const isEdit = ref(false)
const formLoading = ref(false)
const formRef = ref(null)
const ticketOptions = ref([])

const formData = reactive({
  id: null,
  ticketId: null,
  seckillPrice: 0,
  seckillStock: 0,
  limitPerUser: 1,
  startTime: '',
  endTime: ''
})

const formRules = {
  ticketId: [{ required: true, message: '请选择关联门票', trigger: 'change' }],
  seckillPrice: [{ required: true, message: '请输入秒杀价格', trigger: 'blur' }],
  seckillStock: [{ required: true, message: '请输入秒杀库存', trigger: 'blur' }],
  limitPerUser: [{ required: true, message: '请设置每人限购数量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }]
}

const fetchList = async () => {
  loading.value = true
  try {
    const res = await adminGetSeckillList({ currentPage: currentPage.value, size: pageSize.value })
    activityList.value = res.records || []
    total.value = res.total || 0
  } catch (e) {
    console.error('获取秒杀活动列表失败:', e)
  } finally {
    loading.value = false
  }
}

const fetchTicketOptions = async () => {
  try {
    await request.get('/ticket/page', { currentPage: 1, size: 200 }, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        ticketOptions.value = (res.records || []).filter(t => t.status === 1)
      }
    })
  } catch (e) {
    console.error('获取门票列表失败:', e)
  }
}

const handleSizeChange = (size) => { pageSize.value = size; fetchList() }
const handleCurrentChange = (page) => { currentPage.value = page; fetchList() }

const showAddDialog = () => {
  isEdit.value = false
  resetForm()
  if (ticketOptions.value.length === 0) fetchTicketOptions()
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  resetForm()
  Object.assign(formData, {
    id: row.id,
    ticketId: row.ticketId,
    seckillPrice: row.seckillPrice,
    seckillStock: row.seckillStock,
    limitPerUser: row.limitPerUser,
    startTime: row.startTime,
    endTime: row.endTime
  })
  if (ticketOptions.value.length === 0) fetchTicketOptions()
  dialogVisible.value = true
}

const handleDelete = (row) => {
  ElMessageBox.confirm(
    `确定要删除关联"${row.ticketName}"的秒杀活动吗？删除后将同时清理Redis缓存。`,
    '提示',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await adminDeleteSeckill(row.id)
      fetchList()
    } catch (e) {
      console.error('删除失败:', e)
    }
  }).catch(() => {})
}

const handlePreload = async (row) => {
  preloadingId.value = row.id
  try {
    await adminPreloadSeckillStock(row.id)
    fetchList()
  } catch (e) {
    console.error('预热失败:', e)
  } finally {
    preloadingId.value = null
  }
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.assign(formData, {
    id: null, ticketId: null, seckillPrice: 0, seckillStock: 0,
    limitPerUser: 1, startTime: '', endTime: ''
  })
}

const submitForm = async () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    formLoading.value = true
    try {
      const { id, ...payload } = formData
      if (isEdit.value) {
        await adminUpdateSeckill(formData.id, payload)
      } else {
        await adminCreateSeckill(payload)
      }
      dialogVisible.value = false
      fetchList()
    } catch (e) {
      console.error(isEdit.value ? '更新失败:' : '创建失败:', e)
    } finally {
      formLoading.value = false
    }
  })
}

onMounted(() => {
  fetchList()
  fetchTicketOptions()
  statusTimer = setInterval(() => { now.value = Date.now() }, 1000)
})

onUnmounted(() => {
  if (statusTimer) { clearInterval(statusTimer); statusTimer = null }
})
</script>

<style lang="scss" scoped>
.seckill-management {
  padding: 20px;
  background-color: #f9fafc;
  min-height: calc(100vh - 120px);

  .page-header {
    margin-bottom: 24px;
    text-align: left;
    .page-title { font-size: 24px; color: #34495e; margin: 0 0 8px 0; font-weight: 500; }
    .page-subtitle { font-size: 14px; color: #7f8c8d; margin: 0; font-style: italic; }
  }

  .action-bar {
    margin-bottom: 20px;
    display: flex;
    justify-content: flex-end;
    .add-btn { background-color: #2ecc71; border-color: #2ecc71; &:hover, &:focus { background-color: #27ae60; border-color: #27ae60; } }
  }

  .table-card {
    border-radius: 8px;
    overflow: hidden;
    box-shadow: none;
    .seckill-table {
      :deep(thead) { background-color: #ecf0f1; th { background-color: #ecf0f1; color: #34495e; font-weight: 500; } }
      :deep(tbody tr) { transition: all 0.3s; &:hover { background-color: #f8f9fa; } }
      .ticket-name { color: #2980b9; font-weight: 500; }
      .price { color: #7f8c8d; text-decoration: line-through; }
      .seckill-price { font-weight: 600; color: #e74c3c; font-size: 15px; }
      .redis-stock { font-weight: 600; color: #27ae60; }
      .redis-stock-zero { font-weight: 600; color: #e74c3c; }
      .action-btn { margin-right: 4px; }
    }
  }

  .pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; padding: 0 20px; }

  .seckill-dialog {
    :deep(.el-dialog__header) { border-bottom: 1px solid #ecf0f1; padding: 20px; .el-dialog__title { font-size: 18px; color: #34495e; font-weight: 500; } }
    :deep(.el-dialog__body) { padding: 30px 20px; }
    :deep(.el-dialog__footer) { border-top: 1px solid #ecf0f1; padding: 15px 20px; }
    .dialog-footer { display: flex; justify-content: flex-end; }
  }
}
</style>
