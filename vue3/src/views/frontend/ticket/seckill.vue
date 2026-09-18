<template>
  <div class="seckill-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-content">
        <div class="seckill-badge-row">
          <span class="seckill-label">限时秒杀</span>
          <span class="seckill-decor">⚡</span>
        </div>
        <h1 class="page-title">门票限时秒杀</h1>
        <p class="page-subtitle">超低价格，限时限量，手慢无！</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="loading-section">
      <div class="section-container">
        <el-skeleton :rows="12" animated />
      </div>
    </div>

    <!-- 无秒杀活动 -->
    <div v-else-if="!seckill" class="empty-section">
      <div class="section-container">
        <div class="empty-state">
          <div class="empty-icon">🎫</div>
          <h3 class="empty-title">当前没有秒杀活动</h3>
          <p class="empty-desc">该门票暂无秒杀活动，去看看其他门票吧</p>
          <el-button type="primary" @click="goToTickets" class="empty-action">
            <el-icon><Back /></el-icon>
            浏览门票
          </el-button>
        </div>
      </div>
    </div>

    <!-- 秒杀内容 -->
    <div v-else class="seckill-content">
      <div class="section-container">
        <div class="content-grid">
          <!-- 左侧：倒计时 + 商品信息 -->
          <div class="seckill-info-section">
            <!-- 倒计时卡片 -->
            <div class="info-card countdown-card">
              <div class="countdown-header">
                <div class="countdown-icon">⏰</div>
                <div class="countdown-text">
                  <template v-if="countdown.status === 'upcoming'">
                    <span class="countdown-label">距离开始还有</span>
                  </template>
                  <template v-else-if="countdown.status === 'active'">
                    <span class="countdown-label">距离结束还有</span>
                    <span class="countdown-urgent">抢购中</span>
                  </template>
                  <template v-else>
                    <span class="countdown-label ended">秒杀已结束</span>
                  </template>
                </div>
              </div>
              <div v-if="countdown.status !== 'ended'" class="countdown-timer">
                <div v-if="countdown.days > 0" class="timer-block">
                  <span class="timer-num">{{ padZero(countdown.days) }}</span>
                  <span class="timer-unit">天</span>
                </div>
                <div class="timer-block">
                  <span class="timer-num">{{ padZero(countdown.hours) }}</span>
                  <span class="timer-unit">时</span>
                </div>
                <span class="timer-separator">:</span>
                <div class="timer-block">
                  <span class="timer-num">{{ padZero(countdown.minutes) }}</span>
                  <span class="timer-unit">分</span>
                </div>
                <span class="timer-separator">:</span>
                <div class="timer-block">
                  <span class="timer-num">{{ padZero(countdown.seconds) }}</span>
                  <span class="timer-unit">秒</span>
                </div>
              </div>
              <!-- 进度条 -->
              <div v-if="totalStock > 0" class="stock-progress">
                <div class="progress-info">
                  <span>已抢{{ soldCount }}件</span>
                  <span>剩余{{ currentStock }}件</span>
                </div>
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: progressPercent + '%' }"></div>
                </div>
              </div>
            </div>

            <!-- 商品信息卡片 -->
            <div class="info-card product-card">
              <div class="product-header">
                <div class="seckill-price-tag">秒杀价</div>
              </div>
              <h2 class="product-name">{{ seckill.ticketName }}</h2>
              <div class="product-meta">
                <span class="ticket-type-tag">{{ seckill.ticketType }}</span>
              </div>
              <div class="price-section">
                <div class="seckill-price">
                  <span class="currency">¥</span>
                  <span class="amount">{{ seckill.seckillPrice }}</span>
                </div>
                <div class="original-price-block">
                  <span class="original-label">原价</span>
                  <span class="original-price">¥{{ seckill.originalPrice }}</span>
                </div>
                <div class="discount-tag">
                  省¥{{ (seckill.originalPrice - seckill.seckillPrice).toFixed(2) }}
                </div>
              </div>
              <div class="info-divider"></div>
              <div class="product-details">
                <div class="detail-item">
                  <el-icon><Goods /></el-icon>
                  <span class="detail-label">每人限购</span>
                  <span class="detail-value highlight">{{ seckill.limitPerUser }} 张</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 右侧：秒杀表单 -->
          <div class="seckill-form-section">
            <div class="info-card form-card">
              <div class="card-header">
                <h3 class="card-title">
                  <el-icon><Edit /></el-icon>
                  填写信息，立即秒杀
                </h3>
              </div>

              <el-form
                :model="seckillForm"
                :rules="rules"
                ref="seckillFormRef"
                label-position="top"
                class="seckill-form"
              >
                <el-form-item label="游玩日期" prop="visitDate">
                  <el-date-picker
                    v-model="seckillForm.visitDate"
                    type="date"
                    placeholder="选择游玩日期"
                    :disabled-date="disabledDate"
                    size="large"
                    class="form-input"
                  />
                </el-form-item>

                <el-form-item label="购买数量" prop="quantity">
                  <el-input-number
                    v-model="seckillForm.quantity"
                    :min="maxBuyQty > 0 ? 1 : 0"
                    :max="maxBuyQty"
                    :disabled="maxBuyQty <= 0"
                    size="large"
                    class="form-input"
                  />
                  <div class="form-tip">
                    每人限购 {{ seckill.limitPerUser }} 张
                    <span v-if="userBoughtCount > 0">（已购 {{ userBoughtCount }} 张，还可买 {{ seckill.limitPerUser - userBoughtCount }} 张）</span>
                  </div>
                </el-form-item>

                <div class="form-row">
                  <el-form-item label="游客姓名" prop="visitorName">
                    <el-input
                      v-model="seckillForm.visitorName"
                      placeholder="请输入游客姓名"
                      size="large"
                    >
                      <template #prefix><el-icon><User /></el-icon></template>
                    </el-input>
                  </el-form-item>
                  <el-form-item label="联系电话" prop="visitorPhone">
                    <el-input
                      v-model="seckillForm.visitorPhone"
                      placeholder="请输入联系电话"
                      size="large"
                    >
                      <template #prefix><el-icon><Phone /></el-icon></template>
                    </el-input>
                  </el-form-item>
                </div>

                <el-form-item label="身份证号" prop="idCard">
                  <el-input
                    v-model="seckillForm.idCard"
                    placeholder="请输入身份证号（选填）"
                    size="large"
                  >
                    <template #prefix><el-icon><CreditCard /></el-icon></template>
                  </el-input>
                </el-form-item>

                <div class="total-section">
                  <span class="total-label">秒杀总价</span>
                  <span class="total-amount">¥{{ (seckill.seckillPrice * seckillForm.quantity).toFixed(2) }}</span>
                </div>

                <el-button
                  type="primary"
                  size="large"
                  class="seckill-btn"
                  :disabled="!canSeckill"
                  :loading="submitting"
                  @click="onSubmit"
                >
                  <el-icon v-if="!submitting"><Lightning /></el-icon>
                  {{ seckillBtnText }}
                </el-button>
              </el-form>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 秒杀成功提示 -->
    <el-dialog
      v-model="successDialogVisible"
      title="秒杀成功"
      width="440px"
      :close-on-click-modal="false"
      class="success-dialog"
    >
      <div class="success-content">
        <div class="success-icon">🎉</div>
        <h3>恭喜！秒杀成功！</h3>
        <p>订单正在生成中，请前往"我的订单"查看并支付</p>
        <p class="success-note">请在15分钟内完成支付，逾期订单将自动取消</p>
      </div>
      <template #footer>
        <el-button @click="goToOrders" type="primary" class="go-order-btn">
          查看订单
        </el-button>
        <el-button @click="successDialogVisible = false">继续秒杀</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSeckill } from '@/composables/useSeckill'
import {
  Back, Edit, User, Phone, CreditCard, Goods, Lightning
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const ticketId = computed(() => route.params.ticketId)

const {
  seckill, loading, submitting, successDialogVisible,
  currentStock, totalStock, userBoughtCount,
  countdown, maxBuyQty, soldCount, progressPercent,
  canSeckill, seckillBtnText,
  fetchSeckillInfo, startCountdown, startStockPolling,
  handleSeckill: doSeckill, clearTimers, reset,
  padZero
} = useSeckill(ticketId)

const seckillFormRef = ref(null)
const seckillForm = reactive({
  visitDate: null,
  quantity: 1,
  visitorName: '',
  visitorPhone: '',
  idCard: ''
})

const rules = {
  visitDate: [{ required: true, message: '请选择游玩日期', trigger: 'change' }],
  quantity: [
    { required: true, message: '请选择数量', trigger: 'change' },
    { type: 'number', min: 1, message: '至少1张', trigger: 'change' }
  ],
  visitorName: [
    { required: true, message: '请输入游客姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '姓名2-20个字符', trigger: 'blur' }
  ],
  visitorPhone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  idCard: [
    { pattern: /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/, message: '身份证格式不正确', trigger: 'blur' }
  ]
}

const disabledDate = (time) => time.getTime() < Date.now() - 8.64e7

const goToTickets = () => router.push('/tickets')
const goToOrders = () => {
  successDialogVisible.value = false
  router.push('/orders')
}

const onSubmit = async () => {
  const valid = await seckillFormRef.value.validate().catch(() => false)
  if (!valid) return
  await doSeckill(seckillForm)
  seckillForm.quantity = 1
}

const initPage = async () => {
  const prefilled = await fetchSeckillInfo()
  if (seckill.value && prefilled) {
    seckillForm.visitorName = prefilled.visitorName
    seckillForm.visitorPhone = prefilled.visitorPhone
  }
  if (seckill.value) {
    startCountdown()
    startStockPolling()
  }
}

onMounted(initPage)

watch(ticketId, () => {
  reset()
  initPage()
})

onUnmounted(clearTimers)
</script>

<style lang="scss" scoped>
.seckill-container {
  min-height: 100vh;
  background: #f8fafc;
  font-family: "思源黑体", "Source Han Sans", "Noto Sans CJK SC", sans-serif;
  color: #333;

  .section-container {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px;
  }

  // 页面头部
  .page-header {
    background: linear-gradient(135deg, #e53e3e 0%, #c53030 50%, #9b2c2c 100%);
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
      background: url('data:image/svg+xml,<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100"><defs><pattern id="grain" width="100" height="100" patternUnits="userSpaceOnUse"><circle cx="25" cy="25" r="1" fill="rgba(255,255,255,0.08)"/><circle cx="75" cy="75" r="1" fill="rgba(255,255,255,0.08)"/><circle cx="50" cy="10" r="0.5" fill="rgba(255,255,255,0.06)"/><circle cx="80" cy="40" r="0.5" fill="rgba(255,255,255,0.06)"/></pattern></defs><rect width="100" height="100" fill="url(%23grain)"/></svg>');
      opacity: 0.4;
    }

    .header-content {
      position: relative;
      z-index: 1;
      max-width: 1200px;
      margin: 0 auto;
      padding: 0 20px;
    }

    .seckill-badge-row {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      margin-bottom: 8px;

      .seckill-label {
        background: rgba(255, 255, 255, 0.2);
        backdrop-filter: blur(10px);
        padding: 6px 20px;
        border-radius: 20px;
        font-size: 14px;
        font-weight: 600;
        letter-spacing: 2px;
        border: 1px solid rgba(255, 255, 255, 0.3);
      }

      .seckill-decor {
        font-size: 24px;
        animation: pulse 1.5s ease-in-out infinite;
      }
    }

    .page-title {
      font-size: 40px;
      font-weight: 700;
      margin: 8px 0 12px;
    }

    .page-subtitle {
      font-size: 16px;
      opacity: 0.85;
      margin: 0;
    }
  }

  // 加载
  .loading-section {
    padding: 40px 0;
    .section-container { max-width: 1200px; margin: 0 auto; padding: 0 20px; }
  }

  // 空状态
  .empty-section {
    padding: 80px 0;
    .section-container { max-width: 1200px; margin: 0 auto; }

    .empty-state {
      text-align: center;
      .empty-icon { font-size: 72px; margin-bottom: 20px; }
      .empty-title { font-size: 24px; font-weight: 700; color: #2d3748; margin: 0 0 8px; }
      .empty-desc { font-size: 16px; color: #64748b; margin: 0 0 24px; }
      .empty-action {
        background: linear-gradient(45deg, #667eea, #764ba2);
        border: none;
        border-radius: 12px;
        padding: 12px 28px;
        font-weight: 600;
      }
    }
  }

  // 秒杀内容
  .seckill-content {
    padding: 40px 0;

    .content-grid {
      display: grid;
      grid-template-columns: 1fr 1fr;
      gap: 40px;
      align-items: start;
    }
  }

  // 信息卡片
  .info-card {
    background: white;
    border-radius: 16px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    border: 1px solid #e2e8f0;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    }
  }

  // 倒计时卡片
  .countdown-card {
    margin-bottom: 24px;
    padding: 24px;
    background: linear-gradient(135deg, #1a202c 0%, #2d3748 100%);
    color: white;
    border: none;

    .countdown-header {
      display: flex;
      align-items: center;
      gap: 12px;
      margin-bottom: 20px;

      .countdown-icon { font-size: 28px; }
      .countdown-text { display: flex; align-items: center; gap: 10px; }

      .countdown-label {
        font-size: 16px;
        font-weight: 600;
        &.ended { color: #fc8181; }
      }

      .countdown-urgent {
        background: #e53e3e;
        padding: 3px 10px;
        border-radius: 10px;
        font-size: 12px;
        font-weight: 600;
        animation: pulse 1s ease-in-out infinite;
      }
    }

    .countdown-timer {
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 8px;

      .timer-block {
        background: rgba(255, 255, 255, 0.1);
        border-radius: 8px;
        padding: 12px 16px;
        text-align: center;
        min-width: 60px;

        .timer-num {
          display: block;
          font-size: 28px;
          font-weight: 700;
          line-height: 1.2;
        }
        .timer-unit {
          font-size: 12px;
          opacity: 0.7;
        }
      }

      .timer-separator {
        font-size: 24px;
        font-weight: 700;
        opacity: 0.6;
      }
    }

    .stock-progress {
      margin-top: 20px;

      .progress-info {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        opacity: 0.8;
        margin-bottom: 8px;
      }

      .progress-bar {
        height: 6px;
        background: rgba(255, 255, 255, 0.15);
        border-radius: 3px;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          background: linear-gradient(90deg, #fc8181, #e53e3e);
          border-radius: 3px;
          transition: width 0.5s ease;
        }
      }
    }
  }

  // 商品卡片
  .product-card {
    padding: 24px;

    .product-header {
      margin-bottom: 8px;
      .seckill-price-tag {
        display: inline-block;
        background: linear-gradient(45deg, #fc8181, #e53e3e);
        color: white;
        padding: 4px 12px;
        border-radius: 8px;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .product-name {
      font-size: 22px;
      font-weight: 700;
      color: #2d3748;
      margin: 8px 0;
      line-height: 1.3;
    }

    .product-meta {
      margin-bottom: 16px;
      .ticket-type-tag {
        background: #edf2f7;
        color: #4a5568;
        padding: 3px 10px;
        border-radius: 6px;
        font-size: 12px;
      }
    }

    .price-section {
      display: flex;
      align-items: center;
      gap: 16px;

      .seckill-price {
        .currency { font-size: 20px; color: #e53e3e; font-weight: 700; }
        .amount { font-size: 36px; color: #e53e3e; font-weight: 700; }
      }

      .original-price-block {
        display: flex;
        flex-direction: column;
        .original-label { font-size: 11px; color: #a0aec0; }
        .original-price { font-size: 18px; color: #a0aec0; text-decoration: line-through; }
      }

      .discount-tag {
        background: linear-gradient(45deg, #f56565, #e53e3e);
        color: white;
        padding: 4px 10px;
        border-radius: 8px;
        font-size: 12px;
        font-weight: 600;
      }
    }

    .info-divider {
      height: 1px;
      background: linear-gradient(90deg, transparent, #e2e8f0, transparent);
      margin: 20px 0;
    }

    .product-details {
      .detail-item {
        display: flex;
        align-items: center;
        gap: 8px;
        font-size: 14px;
        color: #64748b;
        .el-icon { color: #667eea; }
        .detail-label { flex: 1; }
        .detail-value.highlight { color: #e53e3e; font-weight: 600; }
      }
    }
  }

  // 表单卡片
  .form-card {
    .card-header {
      padding: 24px 24px 0;
      margin-bottom: 24px;
      border-bottom: 1px solid #f1f5f9;

      .card-title {
        font-size: 20px;
        font-weight: 700;
        color: #2d3748;
        margin: 0;
        display: flex;
        align-items: center;
        gap: 8px;

        .el-icon { color: #667eea; font-size: 18px; }
      }
    }

    .seckill-form {
      padding: 0 24px 24px;

      :deep(.el-form-item__label) {
        font-weight: 600;
        color: #2d3748;
        font-size: 14px;
      }

      .form-row {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 20px;
      }

      .form-input {
        width: 100%;
        :deep(.el-input__wrapper) {
          border-radius: 8px;
          border: 2px solid #e2e8f0;
          transition: all 0.3s ease;
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

          &:hover { border-color: #667eea; }
          &.is-focus {
            border-color: #667eea;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.15);
          }
        }
        :deep(.el-input-number) { width: 100%; }
        :deep(.el-date-editor) { width: 100%; }
      }

      .form-tip {
        font-size: 12px;
        color: #94a3b8;
        margin-top: 4px;
      }

      .total-section {
        background: linear-gradient(135deg, #fff5f5, #fed7d7);
        border: 2px solid #fc8181;
        border-radius: 12px;
        padding: 16px 20px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin: 20px 0;

        .total-label { font-size: 16px; font-weight: 600; color: #2d3748; }
        .total-amount { font-size: 28px; font-weight: 700; color: #e53e3e; }
      }

      .seckill-btn {
        width: 100%;
        height: 52px;
        font-size: 18px;
        font-weight: 700;
        border: none;
        border-radius: 12px;
        background: linear-gradient(45deg, #e53e3e, #c53030);
        box-shadow: 0 4px 20px rgba(229, 62, 62, 0.4);
        transition: all 0.3s ease;

        &:hover:not(:disabled) {
          transform: translateY(-2px);
          box-shadow: 0 8px 30px rgba(229, 62, 62, 0.6);
        }

        &:disabled {
          background: #a0aec0;
          box-shadow: none;
        }
      }
    }
  }

  // 成功对话框
  :deep(.success-dialog) {
    .el-dialog__header {
      background: linear-gradient(135deg, #48bb78, #38a169);
      color: white;
      padding: 20px 24px;
      margin: 0;

      .el-dialog__title { color: white; font-weight: 600; }
    }

    .el-dialog__body { padding: 32px 24px; }
  }

  .success-content {
    text-align: center;

    .success-icon { font-size: 56px; margin-bottom: 16px; }
    h3 { font-size: 22px; font-weight: 700; color: #2d3748; margin: 0 0 12px; }
    p { font-size: 14px; color: #64748b; margin: 0 0 8px; }
    .success-note { color: #e53e3e; font-weight: 500; font-size: 13px; }
  }

  .go-order-btn {
    background: linear-gradient(45deg, #667eea, #764ba2);
    border: none;
    border-radius: 8px;
  }
}

@keyframes pulse {
  0%, 100% { opacity: 1; transform: scale(1); }
  50% { opacity: 0.7; transform: scale(1.1); }
}

@media (max-width: 1200px) {
  .seckill-content .content-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .page-header {
    padding: 40px 0 30px;
    .page-title { font-size: 28px; }
  }

  .form-card .seckill-form .form-row {
    grid-template-columns: 1fr;
  }
}
</style>
