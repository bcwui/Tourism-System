<template>
  <div class="payment-failed-container">
    <el-result
      icon="error"
      title="支付失败"
      sub-title="抱歉，您的支付未能完成，请稍后再试。"
    >
      <template #extra>
        <el-button type="primary" @click="goToOrders">查看我的订单</el-button>
        <el-button @click="goToTickets">继续浏览门票</el-button>
      </template>
    </el-result>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goToOrders = () => {
  router.push('/orders')
}

const goToTickets = () => {
  router.push('/tickets')
}
</script>

<style scoped>
.payment-failed-container {
  width: 100%;
  height: 60vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}
</style> 