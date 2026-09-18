import { ref, reactive, computed, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSeckillActivity, executeSeckill, getSeckillStock } from '@/api/seckill'
import { useUserStore } from '@/store/user'

const parseTime = (str) => {
  if (!str) return 0
  return new Date(str.replace(' ', 'T')).getTime()
}

const formatDate = (d) => {
  if (!d) return null
  const dt = new Date(d)
  return dt.getFullYear() + '-' + String(dt.getMonth() + 1).padStart(2, '0') + '-' + String(dt.getDate()).padStart(2, '0')
}

const padZero = (num) => String(num).padStart(2, '0')

export function useSeckill(ticketIdRef) {
  const router = useRouter()
  const userStore = useUserStore()

  const seckill = ref(null)
  const loading = ref(false)
  const submitting = ref(false)
  const successDialogVisible = ref(false)
  const currentStock = ref(0)
  const totalStock = ref(0)
  const userBoughtCount = ref(0)

  const countdown = reactive({
    status: 'active',
    days: 0,
    hours: 0,
    minutes: 0,
    seconds: 0
  })

  let countdownTimer = null
  let stockTimer = null

  // ---- 计算属性 ----
  const maxBuyQty = computed(() => {
    if (!seckill.value) return 1
    const remaining = seckill.value.limitPerUser - userBoughtCount.value
    return Math.min(Math.max(remaining, 0), currentStock.value)
  })

  const soldCount = computed(() => {
    if (!totalStock.value) return 0
    return totalStock.value - currentStock.value
  })

  const progressPercent = computed(() => {
    if (!totalStock.value) return 0
    return Math.round((soldCount.value / totalStock.value) * 100)
  })

  const canSeckill = computed(() => {
    if (!seckill.value) return false
    if (countdown.status !== 'active') return false
    if (currentStock.value <= 0) return false
    if (userBoughtCount.value >= seckill.value.limitPerUser) return false
    return true
  })

  const seckillBtnText = computed(() => {
    if (!seckill.value) return '暂无秒杀'
    if (countdown.status === 'upcoming') return '秒杀未开始'
    if (countdown.status === 'ended') return '秒杀已结束'
    if (currentStock.value <= 0) return '已售罄'
    if (userBoughtCount.value >= seckill.value.limitPerUser) return '已达限购'
    return '立即秒杀'
  })

  // ---- 倒计时 ----
  const computeCountdown = () => {
    if (!seckill.value) return
    const now = Date.now()
    const startTime = parseTime(seckill.value.startTime)
    const endTime = parseTime(seckill.value.endTime)

    let diff
    if (now < startTime) {
      countdown.status = 'upcoming'
      diff = startTime - now
    } else if (now < endTime) {
      countdown.status = 'active'
      diff = endTime - now
    } else {
      countdown.status = 'ended'
      countdown.days = countdown.hours = countdown.minutes = countdown.seconds = 0
      return
    }

    countdown.days = Math.floor(diff / 86400000)
    countdown.hours = Math.floor((diff % 86400000) / 3600000)
    countdown.minutes = Math.floor((diff % 3600000) / 60000)
    countdown.seconds = Math.floor((diff % 60000) / 1000)
  }

  const startCountdown = () => {
    computeCountdown()
    countdownTimer = setInterval(() => {
      computeCountdown()
      if (countdown.status === 'ended') {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  }

  // ---- 库存轮询 ----
  const startStockPolling = () => {
    stockTimer = setInterval(async () => {
      if (!seckill.value || countdown.status !== 'active') return
      try {
        const res = await getSeckillStock(seckill.value.activityId)
        currentStock.value = res ?? 0
      } catch (e) { /* ignore polling errors */ }
    }, 3000)
  }

  // ---- 数据获取 ----
  const fetchSeckillInfo = async () => {
    loading.value = true
    try {
      const res = await getSeckillActivity(ticketIdRef.value)
      seckill.value = res
      if (res) {
        currentStock.value = res.seckillStock || 0
        totalStock.value = res.seckillStock || 0
        userBoughtCount.value = res.userBoughtCount || 0
        if (userStore.isLoggedIn && userStore.userInfo) {
          const info = userStore.userInfo
          return { visitorName: info.name || info.nickname || '', visitorPhone: info.phone || '' }
        }
      }
    } catch (error) {
      console.error('获取秒杀信息失败:', error)
    } finally {
      loading.value = false
    }
  }

  // ---- 执行秒杀 ----
  const handleSeckill = async (formData) => {
    if (!userStore.isLoggedIn) {
      ElMessage.warning('请先登录')
      router.push('/login?redirect=' + encodeURIComponent(router.currentRoute.value.fullPath))
      return
    }

    if (submitting.value) return
    submitting.value = true
    try {
      await executeSeckill(seckill.value.activityId, {
        activityId: seckill.value.activityId,
        quantity: formData.quantity,
        visitorName: formData.visitorName,
        visitorPhone: formData.visitorPhone,
        idCard: formData.idCard,
        visitDate: formatDate(formData.visitDate)
      })
      ElMessage.success('秒杀成功，订单生成中')
      successDialogVisible.value = true
    } catch (error) {
      const msg = error?.message || '秒杀失败，请稍后重试'
      ElMessage.error(msg)
      await fetchSeckillInfo()
    } finally {
      submitting.value = false
    }
  }

  // ---- 清理 ----
  const clearTimers = () => {
    if (countdownTimer) { clearInterval(countdownTimer); countdownTimer = null }
    if (stockTimer) { clearInterval(stockTimer); stockTimer = null }
  }

  const reset = () => {
    clearTimers()
    seckill.value = null
    currentStock.value = 0
    totalStock.value = 0
    userBoughtCount.value = 0
    countdown.status = 'active'
  }

  return {
    seckill, loading, submitting, successDialogVisible,
    currentStock, totalStock, userBoughtCount,
    countdown, maxBuyQty, soldCount, progressPercent,
    canSeckill, seckillBtnText,
    fetchSeckillInfo, startCountdown, startStockPolling,
    handleSeckill, clearTimers, reset,
    padZero, parseTime
  }
}
