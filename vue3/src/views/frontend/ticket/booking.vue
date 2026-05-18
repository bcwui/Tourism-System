<template>
  <div class="booking-container">
    <!-- 现代化页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <h1 class="page-title">
          <span class="title-icon">🎫</span>
          门票预订
        </h1>
        <p class="page-subtitle">
          填写预订信息，轻松获取景点门票，开启美好旅程
        </p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="section-container">
        <el-skeleton :rows="20" animated />
      </div>
    </div>

    <!-- 预订内容区域 -->
    <div v-else class="booking-content">
      <div class="section-container">
        <div class="content-grid">
          <!-- 左侧门票信息 -->
          <div class="ticket-info-section">
            <div class="info-card ticket-info-card" v-if="ticket">
              <div class="card-header">
                <h3 class="card-title">
                  <el-icon><Ticket /></el-icon>
                  门票信息
                </h3>
              </div>

              <div class="ticket-info">
                <div class="ticket-header">
                  <div class="ticket-name">{{ ticket.ticketName }}</div>
                  <div class="ticket-type-badge">{{ ticket.ticketType }}</div>
                </div>

                <div class="price-section">
                  <div class="price-info">
                    <template v-if="ticket.discountPrice">
                      <span class="discount-price">¥{{ ticket.discountPrice }}</span>
                      <span class="original-price">¥{{ ticket.price }}</span>
                      <span class="discount-badge">特惠</span>
                    </template>
                    <template v-else>
                      <span class="normal-price">¥{{ ticket.price }}</span>
                    </template>
                  </div>
                </div>

                <div class="info-divider"></div>

                <div class="ticket-details">
                  <div class="detail-item">
                    <div class="detail-label">
                      <el-icon><Calendar /></el-icon>
                      有效期
                    </div>
                    <div class="detail-value">{{ ticket.validPeriod }}</div>
                  </div>

                  <div class="detail-item">
                    <div class="detail-label">
                      <el-icon><Goods /></el-icon>
                      库存
                    </div>
                    <div class="detail-value stock-value">{{ ticket.stock }} 张</div>
                  </div>
                </div>

                <div class="ticket-description">
                  <div class="description-header">门票说明</div>
                  <div class="description-content">{{ ticket.description || '暂无说明' }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧预订表单 -->
          <div class="booking-form-section">
            <div class="info-card booking-form-card">
              <div class="card-header">
                <h3 class="card-title">
                  <el-icon><Edit /></el-icon>
                  预订信息
                </h3>
              </div>

              <el-form
                :model="bookingForm"
                :rules="rules"
                ref="bookingFormRef"
                label-position="top"
                class="booking-form"
              >
                <div class="form-row">
                  <el-form-item label="游玩日期" prop="visitDate" class="form-item">
                    <el-date-picker
                      v-model="bookingForm.visitDate"
                      type="date"
                      placeholder="选择游玩日期"
                      :disabled-date="disabledDate"
                      size="large"
                      class="form-input"
                    />
                    <div class="form-tip">请选择您计划游玩的日期</div>
                  </el-form-item>

                  <el-form-item label="购买数量" prop="quantity" class="form-item">
                    <el-input-number
                      v-model="bookingForm.quantity"
                      :min="1"
                      :max="maxQuantity"
                      @change="calculateTotal"
                      size="large"
                      class="form-input"
                    />
                    <div class="form-tip">最多可购买 {{ maxQuantity }} 张</div>
                  </el-form-item>
                </div>

                <div class="form-row">
                  <el-form-item label="游客姓名" prop="visitorName" class="form-item">
                    <el-input
                      v-model="bookingForm.visitorName"
                      placeholder="请输入游客姓名"
                      size="large"
                      class="form-input"
                    >
                      <template #prefix>
                        <el-icon><User /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>

                  <el-form-item label="联系电话" prop="visitorPhone" class="form-item">
                    <el-input
                      v-model="bookingForm.visitorPhone"
                      placeholder="请输入联系电话"
                      size="large"
                      class="form-input"
                    >
                      <template #prefix>
                        <el-icon><Phone /></el-icon>
                      </template>
                    </el-input>
                  </el-form-item>
                </div>

                <el-form-item label="身份证号" prop="idCard" class="form-item full-width">
                  <el-input
                    v-model="bookingForm.idCard"
                    placeholder="请输入身份证号（选填）"
                    size="large"
                    class="form-input"
                  >
                    <template #prefix>
                      <el-icon><CreditCard /></el-icon>
                    </template>
                  </el-input>
                  <div class="form-tip">部分景点需要提供身份证信息</div>
                </el-form-item>

                <div class="total-section">
                  <div class="total-amount">
                    <span class="total-label">总金额:</span>
                    <span class="amount">¥{{ totalAmount }}</span>
                  </div>
                </div>

                <div class="form-actions">
                  <el-button
                    size="large"
                    @click="goBack"
                    class="back-btn"
                  >
                    <el-icon><Back /></el-icon>
                    返回
                  </el-button>
                  <el-button
                    type="primary"
                    size="large"
                    @click="submitBooking"
                    :disabled="loading"
                    class="submit-btn"
                  >
                    <el-icon><Check /></el-icon>
                    提交订单
                  </el-button>
                </div>
              </el-form>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 支付对话框 -->
    <el-dialog
      title="订单支付"
      v-model="payDialogVisible"
      width="500px"
      :close-on-click-modal="false"
      
      class="payment-dialog"
    >
      <div class="pay-dialog-content">
        <div class="order-info">
          <div class="order-header">
            <el-icon><Ticket /></el-icon>
            <span>订单详情</span>
          </div>
          
          <div class="order-details">
            <div class="order-item">
              <span class="order-label">订单号:</span>
              <span class="order-value">{{ createdOrder.orderNo }}</span>
            </div>
            <div class="order-item">
              <span class="order-label">门票名称:</span>
              <span class="order-value">{{ ticket?.ticketName }}</span>
            </div>
            <div class="order-item">
              <span class="order-label">游玩日期:</span>
              <span class="order-value">{{ formatDate(createdOrder.visitDate) }}</span>
            </div>
            <div class="order-item">
              <span class="order-label">数量:</span>
              <span class="order-value">{{ createdOrder.quantity }} 张</span>
            </div>
            <div class="order-item total-item">
              <span class="order-label">总金额:</span>
              <span class="order-value amount">¥{{ createdOrder.totalAmount }}</span>
            </div>
          </div>
        </div>
        
        <div class="payment-methods">
          <div class="payment-header">
            <el-icon><Wallet /></el-icon>
            <span>支付方式</span>
          </div>
          
          <el-radio-group v-model="paymentMethod" class="payment-options">
            <el-radio label="WECHAT">
              <div class="payment-option">
                <el-icon class="payment-icon wechat-icon"><Money /></el-icon>
                <span>微信支付</span>
              </div>
            </el-radio>
            <el-radio label="ALIPAY">
              <div class="payment-option">
                <el-icon class="payment-icon alipay-icon"><Money /></el-icon>
                <span>支付宝</span>
              </div>
            </el-radio>
            <el-radio label="BANK_CARD">
              <div class="payment-option">
                <el-icon class="payment-icon bank-icon"><CreditCard /></el-icon>
                <span>银行卡</span>
              </div>
            </el-radio>
          </el-radio-group>
        </div>
        
        <div class="payment-info" v-if="paymentMethod">
          <template v-if="paymentMethod === 'ALIPAY'">
            <div class="payment-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>点击下方"去支付"按钮，跳转到支付宝支付页面</span>
            </div>
          </template>
          <template v-else>
            <div class="qrcode-tip">
              <el-icon><InfoFilled /></el-icon>
              <span>请选择支付方式完成支付</span>
            </div>
          </template>
        </div>
      </div>
      
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="cancelOrder" :icon="Close">取消订单</el-button>
          <template v-if="paymentMethod === 'ALIPAY'">
            <el-button type="primary" @click="goToAlipay" :icon="Right">去支付</el-button>
          </template>
          <template v-else>
            <el-button type="primary" @click="confirmPayment" :icon="Check">确认已支付</el-button>
          </template>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'
import { 
  Ticket, 
  Calendar, 
  Goods, 
  Edit, 
  User, 
  Phone, 
  CreditCard, 
  Back, 
  Check, 
  Close, 
  Right, 
  Wallet, 
  Money, 
  InfoFilled 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 门票ID
const ticketId = computed(() => route.params.id)

// 门票信息
const ticket = ref(null)
const loading = ref(false)

// 表单引用
const bookingFormRef = ref(null)

// 支付对话框
const payDialogVisible = ref(false)
const paymentMethod = ref('')
const createdOrder = ref({})

// 预订表单数据
const bookingForm = reactive({
  ticketId: null,
  visitDate: null,
  quantity: 1,
  visitorName: '',
  visitorPhone: '',
  idCard: ''
})

// 表单验证规则
const rules = {
  visitDate: [
    { required: true, message: '请选择游玩日期', trigger: 'change' }
  ],
  quantity: [
    { required: true, message: '请选择购买数量', trigger: 'change' },
    { type: 'number', min: 1, message: '购买数量至少为1', trigger: 'change' }
  ],
  visitorName: [
    { required: true, message: '请输入游客姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名长度应在2-20个字符之间', trigger: 'blur' }
  ],
  visitorPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '请输入正确的身份证号码', trigger: 'blur' }
  ]
}

// 计算属性：最大购买数量
const maxQuantity = computed(() => {
  return ticket.value ? ticket.value.stock : 1
})

// 计算属性：总金额
const totalAmount = ref(0)

// 计算总金额
const calculateTotal = () => {
  if (!ticket.value) return 0

  const price = ticket.value.discountPrice || ticket.value.price
  totalAmount.value = (price * bookingForm.quantity).toFixed(2)
}

// 获取门票详情
const fetchTicketDetail = async () => {
  loading.value = true
  try {
    await request.get(`/ticket/${ticketId.value}`, {}, {
      showDefaultMsg: false,
      onSuccess: (res) => {
        ticket.value = res
        bookingForm.ticketId = res.id
        
        // 默认选择当前日期
        if (!bookingForm.visitDate) {
          bookingForm.visitDate = new Date()
        }
        
        // 如果已登录，预填用户信息
        if (userStore.isLoggedIn && userStore.userInfo) {
          bookingForm.visitorName = userStore.userInfo.name || userStore.userInfo.nickname || ''
          bookingForm.visitorPhone = userStore.userInfo.phone || ''
        }
        
        calculateTotal()
      }
    })
  } catch (error) {
    ElMessage.error('获取门票信息失败')
    console.error('获取门票详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 禁用的日期（今天之前的日期不可选）
const disabledDate = (time) => {
  return time.getTime() < Date.now() - 8.64e7 // 不能选择今天之前的日期
}

// 提交预订
const submitBooking = async () => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login?redirect=' + encodeURIComponent(route.fullPath))
    return
  }

  bookingFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        await request.post('/order', bookingForm, {
          successMsg: '订单创建成功',
          onSuccess: (res) => {
            createdOrder.value = res
            payDialogVisible.value = true
          }
        })
      } catch (error) {
        console.error('创建订单失败:', error)
      } finally {
        loading.value = false
      }
    }
  })
}

// 确认支付
const confirmPayment = async () => {
  if (!paymentMethod.value) {
    ElMessage.warning('请选择支付方式')
    return
  }

  loading.value = true
  try {
    await request.post(`/order/${createdOrder.value.id}/pay`, null, {
      params: {
        paymentMethod: paymentMethod.value
      },
      successMsg: '支付成功',
      onSuccess: () => {
        payDialogVisible.value = false
        router.push('/orders')
      }
    })
  } catch (error) {
    console.error('支付订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 跳转到支付宝支付页面
const goToAlipay = () => {
  if (createdOrder.value && createdOrder.value.id) {
    router.push(`/payment/alipay/${createdOrder.value.id}`)
  } else {
    ElMessage.error('订单信息错误')
  }
}

// 取消订单
const cancelOrder = async () => {
  loading.value = true
  try {
    await request.post(`/order/${createdOrder.value.id}/cancel`, {}, {
      successMsg: '订单已取消',
      onSuccess: () => {
        payDialogVisible.value = false
        router.push('/tickets')
      }
    })
  } catch (error) {
    console.error('取消订单失败:', error)
  } finally {
    loading.value = false
  }
}

// 返回上一页
const goBack = () => {
  router.back()
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

// 二维码占位图（模拟支付二维码）
const qrcodePlaceholder = computed(() => {
  // 生成一个简单的二维码占位图URL
  return 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB4PSI1MCIgeT0iNTAiIHdpZHRoPSIxMDAiIGhlaWdodD0iMTAwIiBzdHlsZT0iZmlsbDojZjBmMGYwO3N0cm9rZS13aWR0aDo0O3N0cm9rZTojZGRkIiAvPjxyZWN0IHg9IjcwIiB5PSI3MCIgd2lkdGg9IjYwIiBoZWlnaHQ9IjYwIiBzdHlsZT0iZmlsbDojZjhmOGY4O3N0cm9rZS13aWR0aDoyO3N0cm9rZTojY2NjIiAvPjx0ZXh0IHg9IjEwMCIgeT0iMTI3IiB0ZXh0LWFuY2hvcj0ibWlkZGxlIiBzdHlsZT0iZm9udC1mYW1pbHk6QXJpYWwsIHNhbnMtc2VyaWY7Zm9udC1zaXplOjEycHg7ZmlsbDojOTk5OTk5OyI+5pSv5LuY5LqM57u056CBPC90ZXh0Pjwvc3ZnPg=='
})

// 页面加载时获取门票详情
onMounted(() => {
  fetchTicketDetail()
})
</script>

<style lang="scss" scoped>
.booking-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

  // 页面头部
  .page-header {
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;
    padding: 60px 0 40px;
    text-align: center;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.1)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.1)"/><circle cx="20" cy="60" r="0.5" fill="rgba(255,255,255,0.1)"/><circle cx="80" cy="40" r="0.5" fill="rgba(255,255,255,0.1)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
      opacity: 0.3;
    }

    .header-content {
      position: relative;
      z-index: 1;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }

    .page-title {
      font-size: 36px;
      font-weight: 700;
      margin: 0 0 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;

      .title-icon {
        font-size: 32px;
      }
    }

    .page-subtitle {
      font-size: 16px;
      opacity: 0.9;
      margin: 0;
      max-width: 600px;
      margin: 0 auto;
    }
  }

  // 加载状态
  .loading-section {
    padding: 40px 0;

    .section-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }
  }

  // 预订内容区域
  .booking-content {
    padding: 40px 0;

    .section-container {
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }

    .content-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 40px;
      align-items: start;
    }
  }

  // 信息卡片通用样式
  .info-card {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    overflow: hidden;
    border: 1px solid #e2e8f0;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    }

    .card-header {
      padding: 24px 24px 0;
      border-bottom: 1px solid #f1f5f9;
      margin-bottom: 24px;

      .card-title {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon {
          color: #667eea;
          font-size: 18px;
        }
      }
    }
  }

  // 门票信息卡片
  .ticket-info-card {
    .ticket-info {
      padding: 0 24px 24px;
    }

    .ticket-header {
      display: flex;
      justify-content: space-between;
      align-items: flex-start;
      margin-bottom: 20px;

      .ticket-name {
        font-size: 24px;
        font-weight: 700;
        color: #2d3748;
        flex: 1;
        line-height: 1.3;
      }

      .ticket-type-badge {
        padding: 6px 12px;
        border-radius: 12px;
        font-size: 12px;
        font-weight: 600;
        background: linear-gradient(45deg, #667eea, #764ba2);
        color: white;
        margin-left: 16px;
        flex-shrink: 0;
      }
    }

    .price-section {
      margin-bottom: 24px;

      .price-info {
        display: flex;
        align-items: center;
        gap: 12px;

        .discount-price {
          font-size: 32px;
          font-weight: 700;
          color: #e53e3e;
        }

        .original-price {
          font-size: 18px;
          color: #94a3b8;
          text-decoration: line-through;
        }

        .normal-price {
          font-size: 32px;
          font-weight: 700;
          color: #2d3748;
        }

        .discount-badge {
          padding: 4px 8px;
          border-radius: 8px;
          font-size: 10px;
          font-weight: 600;
          background: linear-gradient(45deg, #f56565, #e53e3e);
          color: white;
        }
      }
    }

    .info-divider {
      height: 1px;
      background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
      margin: 24px 0;
    }

    .ticket-details {
      margin-bottom: 24px;

      .detail-item {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 0;
        border-bottom: 1px solid #f8fafc;

        &:last-child {
          border-bottom: none;
        }

        .detail-label {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 14px;
          color: #64748b;
          font-weight: 500;

          .el-icon {
            color: #667eea;
          }
        }

        .detail-value {
          font-size: 14px;
          color: #2d3748;
          font-weight: 600;

          &.stock-value {
            color: #059669;
          }
        }
      }
    }

    .ticket-description {
      .description-header {
        font-size: 16px;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 12px;
      }

      .description-content {
        font-size: 14px;
        color: #64748b;
        line-height: 1.6;
        background: #f8fafc;
        padding: 16px;
        border-radius: 8px;
        border-left: 4px solid #667eea;
      }
    }
  }

  // 预订表单卡片
  .booking-form-card {
    .booking-form {
      padding: 0 24px 24px;

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
        margin-bottom: 20px;
      }

      .form-item {
        &.full-width {
          grid-column: 1 / -1;
        }

        :deep(.el-form-item__label) {
          font-weight: 600;
          color: #2d3748;
          margin-bottom: 8px;
          font-size: 14px;
        }

        .form-input {
          :deep(.el-input__wrapper) {
            border-radius: 8px;
            border: 2px solid #e2e8f0;
            transition: all 0.3s ease;
            box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

            &:hover {
              border-color: #667eea;
            }

            &.is-focus {
              border-color: #667eea;
              box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
            }
          }

          :deep(.el-date-editor) {
            width: 100%;
          }

          :deep(.el-input-number) {
            width: 100%;

            .el-input__wrapper {
              border-radius: 8px;
              border: 2px solid #e2e8f0;
              transition: all 0.3s ease;

              &:hover {
                border-color: #667eea;
              }

              &.is-focus {
                border-color: #667eea;
                box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
              }
            }
          }
        }

        .form-tip {
          font-size: 12px;
          color: #94a3b8;
          margin-top: 4px;
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }

      .total-section {
        background: linear-gradient(135deg, #f8fafc 0%, #e2e8f0 100%);
        border-radius: 12px;
        padding: 20px;
        margin: 24px 0;
        border: 2px solid #e2e8f0;

        .total-amount {
          display: flex;
          justify-content: space-between;
          align-items: center;

          .total-label {
            font-size: 18px;
            font-weight: 600;
            color: #2d3748;
          }

          .amount {
            font-size: 28px;
            font-weight: 700;
            color: #e53e3e;
          }
        }
      }

      .form-actions {
        display: flex;
        gap: 16px;
        justify-content: flex-end;
        margin-top: 32px;

        .back-btn {
          border-radius: 12px;
          border: 2px solid #e2e8f0;
          color: #64748b;
          background: white;
          font-weight: 600;
          padding: 12px 24px;
          transition: all 0.3s ease;

          &:hover {
            border-color: #667eea;
            color: #667eea;
            background: #f8fafc;
            transform: translateY(-1px);
          }
        }

        .submit-btn {
          background: linear-gradient(45deg, #667eea, #764ba2);
          border: none;
          border-radius: 12px;
          font-weight: 600;
          padding: 12px 32px;
          box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
          }

          &:disabled {
            opacity: 0.6;
            transform: none;
            box-shadow: 0 4px 15px rgba(102, 126, 234, 0.2);
          }
        }
      }
    }
  }

  // 支付对话框样式
  :deep(.payment-dialog) {
    .el-dialog__header {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: white;
      padding: 20px 24px;
      margin: 0;

      .el-dialog__title {
        color: white;
        font-weight: 600;
      }

      .el-dialog__headerbtn {
        .el-dialog__close {
          color: white;
          font-size: 18px;

          &:hover {
            color: rgba(255, 255, 255, 0.8);
          }
        }
      }
    }

    .el-dialog__body {
      padding: 24px;
    }

    .el-dialog__footer {
      padding: 16px 24px 24px;
      border-top: 1px solid #f1f5f9;
    }
  }

  .pay-dialog-content {
    .order-info {
      margin-bottom: 24px;

      .order-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 16px;

        .el-icon {
          color: #667eea;
        }
      }

      .order-details {
        background: #f8fafc;
        border-radius: 8px;
        padding: 16px;

        .order-item {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 8px 0;
          border-bottom: 1px solid #e2e8f0;

          &:last-child {
            border-bottom: none;
          }

          &.total-item {
            margin-top: 8px;
            padding-top: 16px;
            border-top: 2px solid #e2e8f0;
            border-bottom: none;

            .order-label {
              font-size: 16px;
              font-weight: 600;
            }

            .amount {
              font-size: 20px;
              font-weight: 700;
              color: #e53e3e;
            }
          }

          .order-label {
            font-size: 14px;
            color: #64748b;
          }

          .order-value {
            font-size: 14px;
            color: #2d3748;
            font-weight: 500;
          }
        }
      }
    }

    .payment-methods {
      margin-bottom: 24px;

      .payment-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 16px;
        font-weight: 600;
        color: #2d3748;
        margin-bottom: 16px;

        .el-icon {
          color: #667eea;
        }
      }

      .payment-options {
        display: flex;
        flex-direction: column;
        gap: 12px;

        :deep(.el-radio) {
          margin: 0;
          padding: 12px;
          border: 2px solid #e2e8f0;
          border-radius: 8px;
          transition: all 0.3s ease;

          &:hover {
            border-color: #667eea;
            background: #f8fafc;
          }

          &.is-checked {
            border-color: #667eea;
            background: linear-gradient(135deg, rgba(102, 126, 234, 0.1) 0%, rgba(118, 75, 162, 0.1) 100%);
          }

          .el-radio__input {
            margin-right: 12px;
          }
        }

        .payment-option {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: 500;

          .payment-icon {
            font-size: 18px;

            &.wechat-icon {
              color: #07c160;
            }

            &.alipay-icon {
              color: #1677ff;
            }

            &.bank-icon {
              color: #667eea;
            }
          }
        }
      }
    }

    .payment-info {
      text-align: center;

      .payment-tip,
      .qrcode-tip {
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 8px;
        font-size: 14px;
        color: #64748b;
        margin-bottom: 16px;

        .el-icon {
          color: #667eea;
        }
      }

      .qrcode-image {
        display: flex;
        justify-content: center;
        padding: 20px;
        background: #f8fafc;
        border-radius: 8px;
        border: 2px dashed #e2e8f0;
      }
    }
  }


  @keyframes fadeInUp {
    from {
      opacity: 0;
      transform: translateY(30px);
    }
    to {
      opacity: 1;
      transform: translateY(0);
    }
  }

  // 响应式样式
  @media (max-width: 1200px) {
    .content-grid {
      grid-template-columns: 1fr;
      gap: 30px;
    }

    .page-header {
      padding: 40px 0 30px;

      .page-title {
        font-size: 28px;
      }
    }
  }

  @media (max-width: 768px) {
    .booking-content {
      padding: 20px 0;

      .section-container {
        padding: 0 15px;
      }
    }

    .page-header {
      padding: 30px 0 20px;

      .page-title {
        font-size: 24px;
      }

      .page-subtitle {
        font-size: 14px;
      }
    }

    .booking-form {
      .form-row {
        grid-template-columns: 1fr;
        gap: 16px;
      }

      .form-actions {
        flex-direction: column;

        .back-btn,
        .submit-btn {
          width: 100%;
        }
      }
    }

    .pay-dialog-content {
      .payment-options {
        :deep(.el-radio) {
          padding: 8px;
        }
      }
    }
  }
}
</style>